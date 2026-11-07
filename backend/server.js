const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const morgan = require('morgan');
const dotenv = require('dotenv');

dotenv.config();

const app = express();
const PORT = process.env.PORT || 5000;
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/reeldrama';

// Middlewares
app.use(cors({ origin: process.env.CORS_ORIGIN || '*' }));
app.use(express.json());
app.use(morgan('dev'));

// MongoDB Connection
mongoose.connect(MONGODB_URI)
  .then(() => console.log('Connected to MongoDB Atlas / Database'))
  .catch((err) => {
    console.error('MongoDB connection error:', err.message);
    console.log('App will continue running with in-memory fallback if database is not reachable.');
  });

// Health check
app.get('/', (req, res) => {
  res.json({
    status: 'online',
    app: 'Reel Drama Streaming API',
    version: '1.0.0',
    endpoints: {
      dramas: '/api/dramas',
      singleDrama: '/api/dramas/:id',
      episodes: '/api/dramas/:id/episodes',
      likeEpisode: 'POST /api/episodes/:id/like',
      adminCreateDrama: 'POST /api/admin/dramas (Admin-only)',
      adminCreateEpisode: 'POST /api/admin/episodes (Admin-only)'
    }
  });
});

// Drama & Reel Routes
const dramaRoutes = require('./routes/dramaRoutes');
app.use('/api', dramaRoutes);

// Error Handling Middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(err.status || 500).json({
    success: false,
    message: err.message || 'Internal Server Error'
  });
});

// Start Server
app.listen(PORT, () => {
  console.log(`Reel Drama API Server running on port ${PORT}`);
  console.log(`API URL: http://localhost:${PORT}/api/dramas`);
});
