const express = require('express');
const router = express.Router();
const Drama = require('../models/Drama');
const Episode = require('../models/Episode');

// Admin Auth Middleware
const requireAdmin = (req, res, next) => {
  const adminKey = req.headers['x-admin-key'] || req.query.adminKey;
  const configuredKey = process.env.ADMIN_API_KEY || 'reel_drama_secure_admin_2026';
  
  if (!adminKey || adminKey !== configuredKey) {
    return res.status(403).json({
      success: false,
      message: 'Access denied: Admin credentials required. Regular users cannot upload content.'
    });
  }
  next();
};

// -------------------------------------------------------------
// PUBLIC VIEWER ENDPOINTS (Zero Upload - Pure Watching Experience)
// -------------------------------------------------------------

// @route   GET /api/dramas
// @desc    Get all drama series with optional genre filter
router.get('/dramas', async (req, res) => {
  try {
    const { genre, trending } = req.query;
    const filter = {};
    if (genre && genre !== 'All') filter.genre = genre;
    if (trending === 'true') filter.isTrending = true;

    const dramas = await Drama.find(filter)
      .sort({ isTrending: -1, viewsCount: -1 })
      .populate({
        path: 'episodes',
        select: 'episodeNumber title videoUrl durationSec likesCount introHook subtitles',
        options: { sort: { episodeNumber: 1 } }
      });

    res.json({
      success: true,
      count: dramas.length,
      data: dramas
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// @route   GET /api/dramas/:id
// @desc    Get drama series by ID with full episodes list
router.get('/dramas/:id', async (req, res) => {
  try {
    const drama = await Drama.findById(req.params.id).populate({
      path: 'episodes',
      options: { sort: { episodeNumber: 1 } }
    });

    if (!drama) {
      return res.status(404).json({ success: false, message: 'Drama series not found' });
    }

    res.json({ success: true, data: drama });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// @route   GET /api/dramas/:id/episodes
// @desc    Get all episodes for a specific drama series
router.get('/dramas/:id/episodes', async (req, res) => {
  try {
    const episodes = await Episode.find({ drama: req.params.id }).sort({ episodeNumber: 1 });
    res.json({ success: true, count: episodes.length, data: episodes });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// @route   POST /api/episodes/:id/like
// @desc    Like/Unlike an episode (increment/decrement count)
router.post('/episodes/:id/like', async (req, res) => {
  try {
    const { action } = req.body; // 'like' or 'unlike'
    const delta = action === 'unlike' ? -1 : 1;

    const episode = await Episode.findByIdAndUpdate(
      req.params.id,
      { $inc: { likesCount: delta } },
      { new: true }
    );

    if (!episode) {
      return res.status(404).json({ success: false, message: 'Episode not found' });
    }

    res.json({ success: true, likesCount: Math.max(0, episode.likesCount) });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// @route   POST /api/dramas/:id/view
// @desc    Increment view count on drama series
router.post('/dramas/:id/view', async (req, res) => {
  try {
    const drama = await Drama.findByIdAndUpdate(
      req.params.id,
      { $inc: { viewsCount: 1 } },
      { new: true }
    );
    res.json({ success: true, viewsCount: drama ? drama.viewsCount : 0 });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// -------------------------------------------------------------
// ADMIN-ONLY CONTENT MANAGEMENT (Strictly Admin Uploads)
// -------------------------------------------------------------

// @route   POST /api/admin/dramas
// @desc    Create new drama series
router.post('/admin/dramas', requireAdmin, async (req, res) => {
  try {
    const drama = await Drama.create(req.body);
    res.status(201).json({ success: true, data: drama });
  } catch (error) {
    res.status(400).json({ success: false, message: error.message });
  }
});

// @route   POST /api/admin/episodes
// @desc    Add episode with Cloudinary video URL
router.post('/admin/episodes', requireAdmin, async (req, res) => {
  try {
    const { dramaId, episodeNumber, title, videoUrl, introHook, durationSec, subtitles } = req.body;

    const drama = await Drama.findById(dramaId);
    if (!drama) {
      return res.status(404).json({ success: false, message: 'Drama series not found' });
    }

    const episode = await Episode.create({
      drama: dramaId,
      episodeNumber,
      title,
      videoUrl,
      introHook,
      durationSec: durationSec || 85,
      subtitles: subtitles || []
    });

    // Update total episodes count in drama
    await Drama.findByIdAndUpdate(dramaId, {
      totalEpisodes: Math.max(drama.totalEpisodes, episodeNumber)
    });

    res.status(201).json({ success: true, data: episode });
  } catch (error) {
    res.status(400).json({ success: false, message: error.message });
  }
});

module.exports = router;
