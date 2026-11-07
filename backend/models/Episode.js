const mongoose = require('mongoose');

const SubtitleLineSchema = new mongoose.Schema({
  startSec: { type: Number, required: true },
  endSec: { type: Number, required: true },
  text: { type: String, required: true }
}, { _id: false });

const EpisodeSchema = new mongoose.Schema(
  {
    drama: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Drama',
      required: true
    },
    episodeNumber: {
      type: Number,
      required: true
    },
    title: {
      type: String,
      required: [true, 'Please provide episode title'],
      trim: true
    },
    introHook: {
      type: String,
      default: ''
    },
    videoUrl: {
      type: String,
      required: [true, 'Please provide Cloudinary streaming video URL']
    },
    cloudinaryPublicId: {
      type: String,
      default: ''
    },
    durationSec: {
      type: Number,
      default: 85
    },
    thumbnailUrl: {
      type: String,
      default: ''
    },
    likesCount: {
      type: Number,
      default: 0
    },
    sharesCount: {
      type: Number,
      default: 0
    },
    subtitles: [SubtitleLineSchema]
  },
  {
    timestamps: true
  }
);

// Compound index to guarantee uniqueness of episodeNumber per drama
EpisodeSchema.index({ drama: 1, episodeNumber: 1 }, { unique: true });

module.exports = mongoose.model('Episode', EpisodeSchema);
