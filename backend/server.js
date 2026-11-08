const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const morgan = require('morgan');
const dotenv = require('dotenv');
dotenv.config();

const app = express();
const PORT = process.env.PORT || 5000;
const MONGODB_URI = process.env.MONGODB_URI;

app.use(cors({ origin: process.env.CORS_ORIGIN || '*' }));
app.use(express.json({ limit: '2mb' }));
app.use(morgan('dev'));
mongoose.set('bufferCommands', false);

app.get('/', (req, res) => res.json({
  status: 'online',
  app: 'Reel Drama Streaming API',
  version: '1.0.1',
  database: mongoose.connection.readyState === 1 ? 'connected' : 'disconnected',
  endpoints: {
    health: '/health',
    dramas: '/api/dramas',
    singleDrama: '/api/dramas/:id',
    episodes: '/api/dramas/:id/episodes',
    likeEpisode: 'POST /api/episodes/:id/like',
    adminCreateDrama: 'POST /api/admin/dramas',
    adminCreateEpisode: 'POST /api/admin/episodes',
    adminUploadVideo: 'POST /api/admin/upload-video'
  }
}));

app.get('/health', (req, res) => {
  const connected = mongoose.connection.readyState === 1;
  res.status(connected ? 200 : 503).json({
    success: connected,
    server: 'online',
    database: connected ? 'connected' : 'disconnected',
    databaseState: mongoose.connection.readyState
  });
});

app.use('/api', (req, res, next) => {
  if (mongoose.connection.readyState !== 1) {
    return res.status(503).json({
      success: false,
      message: 'Database is not connected. Check MONGODB_URI in Render environment variables.'
    });
  }
  next();
});

app.use('/api', require('./routes/dramaRoutes'));

app.use((err, req, res, next) => {
  console.error(err.stack || err.message);
  res.status(err.status || 500).json({ success: false, message: err.message || 'Internal Server Error' });
});

app.listen(PORT, () => {
  console.log(`Reel Drama API Server running on port ${PORT}`);
  if (!MONGODB_URI) {
    console.error('MONGODB_URI is missing. Add it to Render Environment Variables.');
    return;
  }
  mongoose.connect(MONGODB_URI, { serverSelectionTimeoutMS: 10000 })
    .then(async () => {
      console.log('Connected to MongoDB Atlas / Database');
      if (process.env.AUTO_SEED === 'true') {
        try {
          const { seedIfEmpty } = require('./seed/seedData');
          const seeded = await seedIfEmpty();
          console.log(seeded ? 'Database was empty: sample dramas added.' : 'Database already has data: seeding skipped.');
        } catch (seedError) {
          console.error('Auto-seed failed:', seedError.message);
        }
      }
    })
    .catch(err => console.error('MongoDB connection error:', err.message));
});
