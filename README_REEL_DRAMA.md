# ReelDrama — Full-Stack Short Video Streaming Architecture

A high-performance "Reel Drama" vertical short-video streaming platform (NetShort / DramaBox / TikTok style) featuring episodic binge-worthy series, full-screen reel player, smooth auto-play, and pure video watching experience (zero user upload clutter).

---

## 📱 1. Live Native Android Application

The project includes a **production-ready native Android Jetpack Compose app** running directly in this AI Studio streaming emulator!

### Android Features:
- **Vertical Reel Pager**: Smooth vertical snapping (`VerticalPager`) with auto-play, tap-to-pause, and blooming double-tap heart animations.
- **Episodes Drawer / Bottom Sheet**: Quick jump to Episode 1, Episode 2, ... Episode 45 with animated equalizer indicators for the currently playing episode.
- **Series Hub Drawer**: Switch between binge-worthy drama series (*Billionaire's Secret Bride*, *Hidden Dragon: The Undercover Tycoon*, *Revenge of the Disgraced Empress*, *My Mafia Husband's Double Life*).
- **Controls & Overlay**: Mute/Unmute, Speed Switcher (1.0x, 1.25x, 1.5x, 2.0x), CC Subtitles toggle, scrubber timeline bar, Like, Bookmark, and Community Comments.
- **Zero Upload UI**: Pure viewer experience with no regular user upload buttons or clutter.
- **Backend API Sync**: Built-in dialog to configure and test any remote Node.js/Express & MongoDB Atlas endpoint.

---

## 🚀 2. Node.js & Express Backend API (MongoDB Atlas & Cloudinary)

The backend code is organized in `/backend`:
- `server.js`: Express server setup with CORS, morgan logging, and error handling.
- `models/Drama.js`: Mongoose model for Drama series with title, genre, synopsis, ratings, and views.
- `models/Episode.js`: Mongoose model for episodic video streams with Cloudinary URLs, duration, and subtitles.
- `routes/dramaRoutes.js`:
  - `GET /api/dramas`: Public endpoint to fetch all dramas.
  - `GET /api/dramas/:id`: Public endpoint to fetch drama details with episodes.
  - `GET /api/dramas/:id/episodes`: Public episode list.
  - `POST /api/episodes/:id/like`: Public viewer like action.
  - `POST /api/admin/dramas`: Admin-only content creation (`x-admin-key`).
  - `POST /api/admin/episodes`: Admin-only episode upload with Cloudinary stream.
- `seed/seedData.js`: One-command database seeder populating sample dramas and Cloudinary/CDN video streams.

### How to Run the Backend Locally:

1. **Navigate to the backend directory**:
   ```bash
   cd backend
   ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

3. **Configure Environment Variables**:
   Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
   Provide your MongoDB Atlas URI:
   ```env
   PORT=5000
   MONGODB_URI=mongodb+srv://<username>:<password>@cluster0.mongodb.net/reeldrama?retryWrites=true&w=majority
   CLOUDINARY_CLOUD_NAME=your_cloud_name
   CLOUDINARY_API_KEY=your_api_key
   CLOUDINARY_API_SECRET=your_api_secret
   ADMIN_API_KEY=reel_drama_secure_admin_2026
   ```

4. **Seed Database with Sample Dramas & Episodes**:
   ```bash
   npm run seed
   ```

5. **Start the API Server**:
   ```bash
   npm start
   # Server runs at http://localhost:5000
   ```

---

## 🌐 3. Web & Mobile Browser PWA Frontend (HTML, Tailwind CSS, JS)

The web frontend is located in `/web-pwa`:
- `index.html`: Mobile viewport optimized with Tailwind CSS and Material Symbols.
- `styles.css`: Vertical reel CSS scroll snapping (`scroll-snap-type: y mandatory`), equalizer animations, and floating heart particles.
- `app.js`: High-performance touch gesture handling, IntersectionObserver for auto-play/pause, episode drawer, and series switching.
- `manifest.json` & `sw.js`: PWA configuration for "Add to Home Screen" standalone app mode.

### How to Run and Test on Mobile Phone or Browser:

1. **Using Python local HTTP server**:
   ```bash
   cd web-pwa
   python3 -m http.server 3000
   ```

2. **Open in Mobile Browser**:
   - On PC: Open `http://localhost:3000` (press F12 and toggle Device Toolbar for mobile view).
   - On Mobile Phone: Find your computer's local IP address (e.g. `192.168.1.50`) and open `http://192.168.1.50:3000` on your mobile browser (Chrome/Safari).
   - Tap **"Add to Home Screen"** to install as a native-like PWA!

---

## ☁️ 4. Free Online Hosting Guide (Zero Cost Deployment)

### Backend (Render or Railway):
1. Push your repository to GitHub.
2. Go to [render.com](https://render.com) and create a **New Web Service**.
3. Select your repository, set Root Directory to `backend`, Build Command: `npm install`, Start Command: `node server.js`.
4. Add your `MONGODB_URI` environment variable from your free [MongoDB Atlas](https://www.mongodb.com/cloud/atlas) cluster.

### Frontend Web & PWA (Vercel or Netlify):
1. Go to [vercel.com](https://vercel.com) or [netlify.com](https://netlify.com).
2. Set Root Directory to `web-pwa`.
3. Deploy! You will get an instant HTTPS URL (e.g. `https://reeldrama.vercel.app`) that works on any smartphone with full PWA installation and hardware accelerated vertical reel streaming.
