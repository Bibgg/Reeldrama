// Reel Drama Mobile Streaming Web App & PWA
const API_BASE = 'https://reeldrama-api.onrender.com/api';

const DEFAULT_DRAMAS = [
  {
    id: "drama_billionaire_bride",
    title: "Billionaire's Secret Bride",
    genre: "CEO Romance",
    rating: 4.9,
    viewsFormatted: "28.4M",
    totalEpisodes: 45,
    posterColor: "#8A1325",
    synopsis: "Amelia was framed and discarded by her corrupt family. Five years later, she returns as a clandestine titan and enters a strategic contract marriage with Ethan Vance, the city's most ruthless CEO.",
    episodes: [
      {
        id: "ep_1",
        episodeNumber: 1,
        title: "The Disowned Heiress Returns",
        introHook: "Five years after being framed, Amelia steps into the grand gala unnoticed...",
        videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        likes: 48200,
        comments: 3120,
        shares: 1200
      },
      {
        id: "ep_2",
        episodeNumber: 2,
        title: "Contract Marriage at Midnight",
        introHook: "CEO Ethan Vance hands her a pen. 'Marry me for one year, and I'll crush your enemies.'",
        videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        likes: 52100,
        comments: 4450,
        shares: 2100
      },
      {
        id: "ep_3",
        episodeNumber: 3,
        title: "Slap in the VIP Banquet",
        introHook: "The arrogant step-sister tries to throw wine at Amelia—until security bows at Amelia's feet.",
        videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        likes: 67800,
        comments: 6200,
        shares: 3800
      },
      {
        id: "ep_4",
        episodeNumber: 4,
        title: "The Hidden Stockholder",
        introHook: "The board tries to oust Ethan—until the mysterious 30% majority owner walks in.",
        videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
        likes: 71200,
        comments: 5900,
        shares: 4200
      },
      {
        id: "ep_5",
        episodeNumber: 5,
        title: "Unmasked in the Rain",
        introHook: "Ethan finally notices the silver butterfly pendant: 'It was you all along.'",
        videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
        likes: 89400,
        comments: 8740,
        shares: 6500
      }
    ]
  },
  {
    id: "drama_undercover_tycoon",
    title: "Hidden Dragon: The Undercover Tycoon",
    genre: "Urban Action",
    rating: 4.8,
    viewsFormatted: "34.1M",
    totalEpisodes: 50,
    posterColor: "#142C54",
    synopsis: "Posing as an ordinary son-in-law to fulfill a sacred promise, Leo endured three years of humiliation. The Supreme Commander finally reveals his terrifying global empire.",
    episodes: [
      {
        id: "ep_tyc_1",
        episodeNumber: 1,
        title: "The Humble Delivery Son-in-Law",
        introHook: "They threw his scooter helmet on the ground. He quietly took out a black titanium card.",
        videoUrl: "https://res.cloudinary.com/demo/video/upload/sea-turtle.mp4",
        likes: 92000,
        comments: 7600,
        shares: 5900
      },
      {
        id: "ep_tyc_2",
        episodeNumber: 2,
        title: "One Hundred Black Sedans",
        introHook: "The city's most feared warlord arrives in person and kneels before the young delivery man.",
        videoUrl: "https://res.cloudinary.com/demo/video/upload/dog.mp4",
        likes: 104000,
        comments: 9800,
        shares: 8200
      }
    ]
  }
];

let dramas = DEFAULT_DRAMAS;
let currentDrama = dramas[0];
let currentEpisodeIndex = 0;
let isMuted = true;
let playbackSpeed = 1.0;
let lastTap = 0;

// Elements
const reelContainer = document.getElementById('reel-container');
const headerSeriesTitle = document.getElementById('header-series-title');
const seriesPill = document.getElementById('series-pill');
const btnMute = document.getElementById('btn-mute');
const muteIcon = document.getElementById('mute-icon');
const btnSpeed = document.getElementById('btn-speed');
const speedText = document.getElementById('speed-text');

// Modals
const episodesModal = document.getElementById('episodes-modal');
const closeEpisodesBtn = document.getElementById('close-episodes-btn');
const episodesList = document.getElementById('episodes-list');
const seriesModal = document.getElementById('series-modal');
const closeSeriesBtn = document.getElementById('close-series-btn');
const seriesList = document.getElementById('series-list');
const commentsModal = document.getElementById('comments-modal');
const closeCommentsBtn = document.getElementById('close-comments-btn');
const commentsList = document.getElementById('comments-list');
const toast = document.getElementById('toast');
const toastText = document.getElementById('toast-text');

async function init() {
  // Try fetching dynamic videos from backend API
  try {
    const res = await fetch(`${API_BASE}/dramas`);
    if (res.ok) {
      const json = await res.json();
      if (json.data && json.data.length > 0) {
        dramas = json.data;
        currentDrama = dramas[0];
      }
    }
  } catch (err) {
    console.log('Using offline curated drama list:', err.message);
  }

  renderReels();
  setupObserver();
  setupEvents();
}

function renderReels() {
  headerSeriesTitle.innerText = currentDrama.title;
  reelContainer.innerHTML = '';

  currentDrama.episodes.forEach((ep, idx) => {
    const section = document.createElement('section');
    section.className = 'w-full h-full snap-start relative flex items-center justify-center bg-black overflow-hidden';
    section.dataset.index = idx;

    section.innerHTML = `
      <!-- Video Element -->
      <video class="w-full h-full object-cover pointer-events-none" 
             src="${ep.videoUrl}" 
             loop 
             playsinline 
             webkit-playsinline 
             ${isMuted ? 'muted' : ''} 
             preload="${idx < 2 ? 'auto' : 'metadata'}">
      </video>

      <!-- Click Area for Tap to Play/Pause and Double Tap -->
      <div class="video-touch-overlay absolute inset-0 z-10 cursor-pointer"></div>

      <!-- Play / Pause Flash Icon -->
      <div class="play-flash absolute inset-0 z-20 flex items-center justify-center pointer-events-none opacity-0 transition-opacity duration-300">
        <div class="w-16 h-16 rounded-full bg-black/60 flex items-center justify-center text-white">
          <span class="material-symbols-rounded text-4xl">play_arrow</span>
        </div>
      </div>

      <!-- Right Action Sidebar -->
      <div class="absolute right-3 bottom-20 z-20 flex flex-col items-center gap-4">
        <!-- Series Avatar -->
        <button class="open-series-action relative mb-2 active:scale-90 transition">
          <div class="w-11 h-11 rounded-full border-2 border-white flex items-center justify-center font-black text-sm text-white" 
               style="background-color: ${currentDrama.posterColor || '#8A1325'}">
            ${currentDrama.title.slice(0, 2).toUpperCase()}
          </div>
          <div class="absolute -bottom-1.5 left-1/2 -translate-x-1/2 w-5 h-5 rounded-full bg-brandRed flex items-center justify-center text-[11px] font-bold text-white">
            +
          </div>
        </button>

        <!-- Like Button -->
        <button class="like-btn flex flex-col items-center active:scale-75 transition">
          <div class="w-11 h-11 rounded-full bg-black/50 backdrop-blur-sm flex items-center justify-center text-white">
            <span class="like-icon material-symbols-rounded text-2xl">favorite</span>
          </div>
          <span class="like-count text-[11px] font-semibold mt-1 drop-shadow">${formatCount(ep.likes)}</span>
        </button>

        <!-- Episodes Jump Button -->
        <button class="open-episodes-action flex flex-col items-center active:scale-75 transition">
          <div class="w-11 h-11 rounded-full bg-black/50 backdrop-blur-sm flex items-center justify-center text-brandGold">
            <span class="material-symbols-rounded text-2xl">layers</span>
          </div>
          <span class="text-[11px] font-semibold mt-1 drop-shadow text-brandGold">Ep.${ep.episodeNumber}</span>
        </button>

        <!-- Comments Button -->
        <button class="open-comments-action flex flex-col items-center active:scale-75 transition">
          <div class="w-11 h-11 rounded-full bg-black/50 backdrop-blur-sm flex items-center justify-center text-white">
            <span class="material-symbols-rounded text-2xl">chat</span>
          </div>
          <span class="text-[11px] font-semibold mt-1 drop-shadow">${formatCount(ep.comments)}</span>
        </button>

        <!-- Share Button -->
        <button class="share-btn flex flex-col items-center active:scale-75 transition">
          <div class="w-11 h-11 rounded-full bg-black/50 backdrop-blur-sm flex items-center justify-center text-white">
            <span class="material-symbols-rounded text-2xl">share</span>
          </div>
          <span class="text-[11px] font-semibold mt-1 drop-shadow">${formatCount(ep.shares)}</span>
        </button>
      </div>

      <!-- Bottom Info Overlay -->
      <div class="absolute left-0 bottom-4 right-16 z-20 px-4 flex flex-col gap-1.5 text-left pointer-events-none">
        <!-- Episode Quick Jump Bar -->
        <div class="open-episodes-action pointer-events-auto inline-flex items-center gap-1.5 bg-gradient-to-r from-brandRed/90 to-brandSurface/90 border border-white/10 rounded-lg px-2.5 py-1 w-fit cursor-pointer active:scale-95 transition">
          <span class="material-symbols-rounded text-sm text-white">layers</span>
          <span class="text-xs font-bold text-white tracking-wide">EPISODE ${ep.episodeNumber} OF ${currentDrama.totalEpisodes}</span>
          <span class="text-[10px] font-semibold text-brandGold">• All Episodes &gt;</span>
        </div>

        <!-- Episode Title & Hook -->
        <h2 class="text-sm font-bold text-white leading-tight drop-shadow">
          Ep.${ep.episodeNumber} - ${ep.title}
        </h2>
        <p class="text-xs text-white/80 line-clamp-2 leading-relaxed drop-shadow">
          ${ep.introHook || ''}
        </p>

        <!-- Progress Bar -->
        <div class="w-full bg-white/20 h-1 rounded-full overflow-hidden mt-1">
          <div class="progress-bar w-0 h-full bg-brandRed transition-all duration-200"></div>
        </div>
      </div>
    `;

    reelContainer.appendChild(section);
  });

  // Attach event listeners for each reel
  document.querySelectorAll('section').forEach((sec) => {
    const video = sec.querySelector('video');
    const touchOverlay = sec.querySelector('.video-touch-overlay');
    const playFlash = sec.querySelector('.play-flash');
    const progressBar = sec.querySelector('.progress-bar');
    const likeBtn = sec.querySelector('.like-btn');
    const likeIcon = sec.querySelector('.like-icon');
    const likeCount = sec.querySelector('.like-count');

    // Tap & Double Tap handling
    touchOverlay.addEventListener('click', (e) => {
      const now = Date.now();
      if (now - lastTap < 300) {
        // Double Tap -> Like & bloom heart
        triggerHeartBloom(sec, e.clientX, e.clientY);
        if (!sec.dataset.liked) {
          sec.dataset.liked = "true";
          likeIcon.classList.add('text-brandRed');
          likeCount.innerText = formatCount(parseInt(likeCount.innerText.replace(/[^\d]/g, '')) + 1);
          showToast("Added to Liked Reels ❤️");
        }
      } else {
        // Single Tap -> Play / Pause
        if (video.paused) {
          video.play();
          playFlash.classList.add('opacity-0');
        } else {
          video.pause();
          playFlash.classList.remove('opacity-0');
        }
      }
      lastTap = now;
    });

    // Update progress bar
    video.addEventListener('timeupdate', () => {
      if (video.duration) {
        const pct = (video.currentTime / video.duration) * 100;
        progressBar.style.width = `${pct}%`;
      }
    });

    // Like button click
    likeBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      const isLiked = sec.dataset.liked === "true";
      sec.dataset.liked = isLiked ? "false" : "true";
      if (!isLiked) {
        likeIcon.classList.add('text-brandRed');
        showToast("Added to Liked Reels ❤️");
      } else {
        likeIcon.classList.remove('text-brandRed');
        showToast("Removed Like");
      }
    });

    // Episodes action
    sec.querySelectorAll('.open-episodes-action').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        openEpisodesModal();
      });
    });

    // Series action
    sec.querySelectorAll('.open-series-action').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        openSeriesModal();
      });
    });

    // Comments action
    sec.querySelectorAll('.open-comments-action').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        openCommentsModal();
      });
    });

    // Share action
    sec.querySelectorAll('.share-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        if (navigator.share) {
          navigator.share({
            title: currentDrama.title,
            text: `Watch ${currentDrama.title} on ReelDrama!`,
            url: window.location.href
          });
        } else {
          navigator.clipboard.writeText(window.location.href);
          showToast("Link copied to clipboard! 📋");
        }
      });
    });
  });
}

function setupObserver() {
  const options = {
    root: reelContainer,
    threshold: 0.65
  };

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      const video = entry.target.querySelector('video');
      const idx = parseInt(entry.target.dataset.index);

      if (entry.isIntersecting) {
        currentEpisodeIndex = idx;
        video.currentTime = 0;
        video.muted = isMuted;
        video.playbackRate = playbackSpeed;
        video.play().catch(() => {});
      } else {
        video.pause();
      }
    });
  }, options);

  document.querySelectorAll('section').forEach(sec => observer.observe(sec));
}

function triggerHeartBloom(container, clientX, clientY) {
  const heart = document.createElement('div');
  heart.className = 'floating-heart text-brandRed material-symbols-rounded text-6xl drop-shadow-2xl';
  heart.innerText = 'favorite';
  heart.style.left = `${clientX}px`;
  heart.style.top = `${clientY}px`;
  container.appendChild(heart);
  setTimeout(() => heart.remove(), 800);
}

function setupEvents() {
  // Mute button
  btnMute.addEventListener('click', () => {
    isMuted = !isMuted;
    muteIcon.innerText = isMuted ? 'volume_off' : 'volume_up';
    document.querySelectorAll('video').forEach(v => v.muted = isMuted);
    showToast(isMuted ? 'Sound Muted' : 'Sound On');
  });

  // Speed button
  btnSpeed.addEventListener('click', () => {
    playbackSpeed = playbackSpeed === 1.0 ? 1.25 : playbackSpeed === 1.25 ? 1.5 : playbackSpeed === 1.5 ? 2.0 : 1.0;
    speedText.innerText = `${playbackSpeed}x`;
    document.querySelectorAll('video').forEach(v => v.playbackRate = playbackSpeed);
    showToast(`Speed ${playbackSpeed}x`);
  });

  // Header series pill click
  seriesPill.addEventListener('click', () => {
    openSeriesModal();
  });

  // Close modals
  closeEpisodesBtn.addEventListener('click', () => episodesModal.classList.add('hidden'));
  closeSeriesBtn.addEventListener('click', () => seriesModal.classList.add('hidden'));
  closeCommentsBtn.addEventListener('click', () => commentsModal.classList.add('hidden'));
}

function openEpisodesModal() {
  document.getElementById('episodes-modal-series-title').innerText = currentDrama.title;
  document.getElementById('episodes-modal-count').innerText = `${currentDrama.totalEpisodes} Episodes • ${currentDrama.viewsFormatted} Views`;

  episodesList.innerHTML = '';
  currentDrama.episodes.forEach((ep, idx) => {
    const isPlaying = idx === currentEpisodeIndex;
    const item = document.createElement('div');
    item.className = `flex items-center justify-between p-3 rounded-xl border cursor-pointer active:scale-98 transition ${
      isPlaying ? 'bg-brandRed/20 border-brandRed' : 'bg-brandCard border-white/10 hover:border-white/20'
    }`;
    item.innerHTML = `
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-lg ${isPlaying ? 'bg-brandRed' : 'bg-white/10'} flex items-center justify-center font-bold text-xs">
          ${isPlaying ? '<span class="material-symbols-rounded text-sm">play_arrow</span>' : ep.episodeNumber}
        </div>
        <div>
          <h4 class="text-xs font-bold ${isPlaying ? 'text-brandRed' : 'text-white'}">Ep.${ep.episodeNumber} - ${ep.title}</h4>
          <span class="text-[10px] text-white/50">${ep.durationSec || 85}s • VIP HD</span>
        </div>
      </div>
      <span class="material-symbols-rounded text-sm text-white/40">chevron_right</span>
    `;

    item.addEventListener('click', () => {
      episodesModal.classList.add('hidden');
      const targetSec = document.querySelectorAll('section')[idx];
      if (targetSec) targetSec.scrollIntoView({ behavior: 'smooth' });
    });

    episodesList.appendChild(item);
  });

  episodesModal.classList.remove('hidden');
}

function openSeriesModal() {
  seriesList.innerHTML = '';
  dramas.forEach(drama => {
    const isCurrent = drama.id === currentDrama.id;
    const item = document.createElement('div');
    item.className = `flex items-center gap-3 p-3 rounded-xl border cursor-pointer active:scale-98 transition ${
      isCurrent ? 'bg-brandRed/20 border-brandRed' : 'bg-brandCard border-white/10'
    }`;
    item.innerHTML = `
      <div class="w-16 h-20 rounded-lg flex items-center justify-center font-black text-white text-lg shrink-0" 
           style="background-color: ${drama.posterColor || '#8A1325'}">
        ${drama.title.slice(0, 1)}
      </div>
      <div class="flex-1 overflow-hidden">
        <div class="flex items-center justify-between">
          <h4 class="text-sm font-bold text-white truncate">${drama.title}</h4>
          ${isCurrent ? '<span class="text-[9px] bg-brandRed text-white px-1.5 py-0.5 rounded font-black">PLAYING</span>' : ''}
        </div>
        <p class="text-[10px] text-brandGold font-semibold mt-0.5">${drama.genre} • ${drama.rating} ★</p>
        <p class="text-xs text-white/60 line-clamp-2 mt-1 leading-snug">${drama.synopsis}</p>
      </div>
    `;

    item.addEventListener('click', () => {
      seriesModal.classList.add('hidden');
      if (drama.id !== currentDrama.id) {
        currentDrama = drama;
        currentEpisodeIndex = 0;
        renderReels();
        setupObserver();
        showToast(`Loaded: ${drama.title}`);
      }
    });

    seriesList.appendChild(item);
  });

  seriesModal.classList.remove('hidden');
}

function openCommentsModal() {
  const sampleComments = [
    { author: "Elena_R", text: "Wait she signed without reading clause 4?! 😱", time: "2m ago", likes: 243 },
    { author: "DramaQueen", text: "That slap in episode 3 was legendary!! 🔥", time: "15m ago", likes: 189 },
    { author: "Marcus_C", text: "Can't stop watching, finished 5 episodes already!", time: "1h ago", likes: 95 }
  ];

  commentsList.innerHTML = '';
  sampleComments.forEach(c => {
    const item = document.createElement('div');
    item.className = 'flex items-start gap-3 p-2.5 rounded-lg bg-white/5';
    item.innerHTML = `
      <div class="w-8 h-8 rounded-full bg-brandRed/30 text-brandRed font-bold text-xs flex items-center justify-center shrink-0">
        ${c.author.slice(0, 2).toUpperCase()}
      </div>
      <div class="flex-1">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold text-white/90">${c.author}</span>
          <span class="text-[10px] text-white/40">${c.time}</span>
        </div>
        <p class="text-xs text-white/80 mt-1">${c.text}</p>
      </div>
    `;
    commentsList.appendChild(item);
  });

  commentsModal.classList.remove('hidden');
}

function showToast(msg) {
  toastText.innerText = msg;
  toast.classList.remove('opacity-0');
  setTimeout(() => toast.classList.add('opacity-0'), 2000);
}

function formatCount(num) {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
  return num;
}

// Register service worker for PWA
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('sw.js').catch(() => {});
  });
}

document.addEventListener('DOMContentLoaded', init);
