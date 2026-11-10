const express = require('express');
const router = express.Router();
const Drama = require('../models/Drama');
const Episode = require('../models/Episode');
const multer = require('multer');
const { cloudinary, configured: cloudinaryConfigured } = require('../config/cloudinary');

const videoUpload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 100 * 1024 * 1024 },
  fileFilter: (req, file, cb) => {
    if (!file.mimetype || !file.mimetype.startsWith('video/')) return cb(new Error('Only video files are allowed.'));
    cb(null, true);
  }
});

// Admin Auth Middleware
// Key सिर्फ़ header (x-admin-key) से लिया जाता है, URL में नहीं (URL logs में दिख जाता है)।
// ADMIN_API_KEY सर्वर के environment variables में सेट होना ज़रूरी है, कोड में कोई default नहीं।
const requireAdmin = (req, res, next) => {
  const adminKey = req.headers['x-admin-key'];
  const configuredKey = process.env.ADMIN_API_KEY;

  if (!configuredKey) {
    return res.status(503).json({
      success: false,
      message: 'Admin API is disabled: ADMIN_API_KEY is not configured on the server.'
    });
  }

  if (!adminKey || adminKey !== configuredKey) {
    return res.status(403).json({
      success: false,
      message: 'Access denied: Admin credentials required. Regular users cannot upload content.'
    });
  }
  next();
};

// -------------------------------------------------------------
// DRAMABOS CATALOG SEARCH (Admin-only; metadata discovery only)
// Requires DRAMABOS_API_KEY in Render environment variables.
// This endpoint does not download, mirror, or re-host provider videos.
// -------------------------------------------------------------
// Provider slugs listed by DramaBos. Most providers use the standard
// /{provider}/api/v1/search?keyword=... route. ShortMax and FlickReels
// document their own route/query format.
const DRAMABOS_PROVIDER_SLUGS = [
  'starshort', 'dramabite', 'freereels', 'fundrama', 'microdrama', 'vigloo',
  'bilitv', 'dramabox', 'dramawave', 'netshort', 'idrama', 'shortmax',
  'goodshort', 'melolo', 'velolo', 'reelshort', 'flickreels', 'stardusttv',
  'serialplus', 'dotdrama', 'rapidtv', 'shortswave', 'dramanova', 'cubetv',
  'reelbuzz', 'flareflow', 'moboreels', 'happyshort', 'reelife', 'pinedrama',
  'flextv', 'reelala', 'anyreel', 'raptdrama', 'bonustv', 'minitv',
  'golddrama', 'iqiyi', 'bstation', 'joyreels', 'kalostv', 'vibeshort'
];

const getDramaBosSearchConfig = (provider) => {
  if (!DRAMABOS_PROVIDER_SLUGS.includes(provider)) return null;
  if (provider === 'shortmax') return { path: '/shortmax/api/v1/search', queryParam: 'q' };
  if (provider === 'flickreels') return { path: '/flickreels/api/flickreels/search', queryParam: 'q' };
  return { path: `/${provider}/api/v1/search`, queryParam: 'keyword' };
};

router.get('/admin/catalog/search', requireAdmin, async (req, res) => {
  const provider = String(req.query.provider || 'dramabox').toLowerCase();
  const keyword = String(req.query.keyword || '').trim();

  const config = getDramaBosSearchConfig(provider);
  if (!config) {
    return res.status(400).json({
      success: false,
      message: 'Unsupported provider slug. Choose one of the 42 documented DramaBos provider slugs.'
    });
  }
  if (keyword.length < 2 || keyword.length > 80) {
    return res.status(400).json({ success: false, message: 'keyword must be 2 to 80 characters.' });
  }

  const apiKey = process.env.DRAMABOS_API_KEY;
  if (!apiKey) {
    return res.status(503).json({
      success: false,
      message: 'DramaBos is not configured. Add DRAMABOS_API_KEY to Render Environment Variables.'
    });
  }

  const url = new URL(config.path, 'https://dramabos.live');
  url.searchParams.set(config.queryParam, keyword);

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 12000);
  try {
    const upstream = await fetch(url, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${apiKey}`,
        Accept: 'application/json'
      },
      signal: controller.signal
    });
    const bodyText = await upstream.text();
    let payload;
    try {
      payload = JSON.parse(bodyText);
    } catch {
      payload = { raw: bodyText.slice(0, 2000) };
    }

    if (!upstream.ok) {
      return res.status(upstream.status === 429 ? 429 : 502).json({
        success: false,
        provider,
        upstreamStatus: upstream.status,
        message: upstream.status === 401 || upstream.status === 403
          ? 'DramaBos rejected the API key. Check DRAMABOS_API_KEY.'
          : upstream.status === 429
            ? 'DramaBos free request limit/rate limit reached.'
            : 'DramaBos search request failed.',
        details: typeof payload === 'object' ? payload : undefined
      });
    }

    return res.json({
      success: true,
      provider,
      keyword,
      source: 'DramaBos',
      data: payload
    });
  } catch (error) {
    const timedOut = error.name === 'AbortError';
    return res.status(timedOut ? 504 : 502).json({
      success: false,
      provider,
      message: timedOut ? 'DramaBos request timed out.' : 'Could not reach DramaBos API.'
    });
  } finally {
    clearTimeout(timeout);
  }
});

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

// @route POST /api/admin/upload-video
// Upload a video to Cloudinary. Send multipart/form-data with field name "video".
router.post('/admin/upload-video', requireAdmin, (req, res) => {
  if (!cloudinaryConfigured) {
    return res.status(503).json({ success: false, message: 'Cloudinary environment variables are not configured on Render.' });
  }

  videoUpload.single('video')(req, res, (uploadError) => {
    if (uploadError) return res.status(400).json({ success: false, message: uploadError.message });
    if (!req.file) return res.status(400).json({ success: false, message: 'Attach a video file in the "video" field.' });

    const uploadStream = cloudinary.uploader.upload_stream(
      { resource_type: 'video', folder: 'reeldrama/episodes' },
      (error, result) => {
        if (error) return res.status(502).json({ success: false, message: error.message });
        return res.status(201).json({
          success: true,
          videoUrl: result.secure_url,
          publicId: result.public_id,
          durationSec: result.duration || null,
          bytes: result.bytes
        });
      }
    );
    uploadStream.end(req.file.buffer);
  });
});

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

// @route   PATCH /api/admin/episodes/:id
// @desc    Replace an existing episode's video URL without creating a duplicate episode
router.patch('/admin/episodes/:id', requireAdmin, async (req, res) => {
  try {
    const { videoUrl, durationSec } = req.body;
    if (!videoUrl || typeof videoUrl !== 'string' || !/^https:\/\//i.test(videoUrl)) {
      return res.status(400).json({ success: false, message: 'A valid HTTPS videoUrl is required.' });
    }

    const update = { videoUrl };
    if (Number.isFinite(Number(durationSec)) && Number(durationSec) > 0) {
      update.durationSec = Number(durationSec);
    }

    const episode = await Episode.findByIdAndUpdate(
      req.params.id,
      { $set: update },
      { new: true, runValidators: true }
    );

    if (!episode) {
      return res.status(404).json({ success: false, message: 'Episode not found' });
    }

    res.json({ success: true, message: 'Episode video updated.', data: episode });
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
