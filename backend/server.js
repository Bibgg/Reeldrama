const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const morgan = require('morgan');
const dotenv = require('dotenv');
dotenv.config();

const app = express();
const PORT = process.env.PORT || 5000;
const MONGODB_URI = process.env.MONGODB_URI;

// Render runs behind a trusted reverse proxy. This makes req.ip use the client IP.
app.set('trust proxy', 1);
app.disable('x-powered-by');

// Basic security headers without adding another runtime dependency.
app.use((req, res, next) => {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Referrer-Policy', 'no-referrer');
  res.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');
  next();
});

const createRateLimiter = ({ windowMs, max, message }) => {
  const requests = new Map();
  return (req, res, next) => {
    const now = Date.now();
    const key = req.ip || req.socket.remoteAddress || 'unknown';
    let entry = requests.get(key);
    if (!entry || now >= entry.resetAt) {
      entry = { count: 0, resetAt: now + windowMs };
      requests.set(key, entry);
    }
    entry.count += 1;
    if (requests.size > 5000) {
      for (const [ip, value] of requests) {
        if (now >= value.resetAt) requests.delete(ip);
      }
    }
    res.setHeader('RateLimit-Limit', String(max));
    res.setHeader('RateLimit-Remaining', String(Math.max(0, max - entry.count)));
    if (entry.count > max) {
      res.setHeader('Retry-After', String(Math.ceil((entry.resetAt - now) / 1000)));
      return res.status(429).json({ success: false, message });
    }
    next();
  };
};

app.use(cors({ origin: process.env.CORS_ORIGIN || '*' }));
app.use(express.json({ limit: '2mb' }));
app.use(morgan('dev'));
mongoose.set('bufferCommands', false);

app.get('/', (req, res) => res.json({
  status: 'online',
  app: 'Reel Drama Streaming API',
  version: '1.0.2',
  database: mongoose.connection.readyState === 1 ? 'connected' : 'disconnected',
  endpoints: {
    health: '/health',
    dramas: '/api/dramas',
    singleDrama: '/api/dramas/:id',
    episodes: '/api/dramas/:id/episodes',
    likeEpisode: 'POST /api/episodes/:id/like',
    adminCreateDrama: 'POST /api/admin/dramas',
    adminCreateEpisode: 'POST /api/admin/episodes',
    adminUploadVideo: 'POST /api/admin/upload-video',
    adminCatalogSearch: 'GET /api/admin/catalog/search?provider=starshort&keyword=love',
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

// Keep public catalogue traffic usable while protecting admin and upload routes.
app.use('/api', createRateLimiter({
  windowMs: 60 * 1000,
  max: 120,
  message: 'Too many requests. Please try again in a minute.'
}));
app.use('/api/admin', createRateLimiter({
  windowMs: 10 * 60 * 1000,
  max: 30,
  message: 'Too many admin requests. Please try again later.'
}));

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
  const status = Number.isInteger(err.status) && err.status >= 400 && err.status < 500 ? err.status : 500;
  res.status(status).json({
    success: false,
    message: status === 500 ? 'Internal Server Error' : (err.message || 'Request failed')
  });
});

const server = app.listen(PORT, () => {
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

const shutdown = (signal) => {
  console.log(`${signal} received: shutting down gracefully`);
  server.close(() => {
    mongoose.disconnect().finally(() => process.exit(0));
  });
  setTimeout(() => process.exit(1), 10000).unref();
};
process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));
