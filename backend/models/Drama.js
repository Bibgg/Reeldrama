const mongoose = require('mongoose');

const DramaSchema = new mongoose.Schema(
  {
    title: {
      type: String,
      required: [true, 'Please provide drama series title'],
      trim: true,
      maxlength: [120, 'Title cannot exceed 120 characters']
    },
    slug: {
      type: String,
      unique: true,
      lowercase: true,
      trim: true
    },
    synopsis: {
      type: String,
      required: [true, 'Please provide synopsis'],
      maxlength: [1000, 'Synopsis cannot exceed 1000 characters']
    },
    genre: {
      type: String,
      required: true,
      enum: ['CEO Romance', 'Historical Revenge', 'Urban Action', 'Dark Romance', 'Suspense', 'Fantasy Rebirth']
    },
    tags: [{
      type: String,
      trim: true
    }],
    totalEpisodes: {
      type: Number,
      required: true,
      default: 1
    },
    rating: {
      type: Number,
      default: 4.9,
      min: 1.0,
      max: 5.0
    },
    viewsCount: {
      type: Number,
      default: 0
    },
    coverImageUrl: {
      type: String,
      required: true
    },
    posterColorHex: {
      type: String,
      default: '#7A1426'
    },
    isTrending: {
      type: Boolean,
      default: true
    },
    status: {
      type: String,
      enum: ['ongoing', 'completed'],
      default: 'ongoing'
    }
  },
  {
    timestamps: true,
    toJSON: { virtuals: true },
    toObject: { virtuals: true }
  }
);

// Virtual populate for episodes
DramaSchema.virtual('episodes', {
  ref: 'Episode',
  localField: '_id',
  foreignField: 'drama',
  justOne: false
});

module.exports = mongoose.model('Drama', DramaSchema);
