const mongoose = require('mongoose');
const dotenv = require('dotenv');
const Drama = require('../models/Drama');
const Episode = require('../models/Episode');

dotenv.config({ path: '../.env' });

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/reeldrama';

const sampleDramas = [
  {
    title: "Billionaire's Secret Bride",
    slug: "billionaires-secret-bride",
    synopsis: "Five years after being framed and cast out, Amelia returns as a clandestine financial titan and enters a strategic contract marriage with Ethan Vance, the city's most ruthless CEO. Neither expects the secrets they both hide.",
    genre: "CEO Romance",
    tags: ["CEO", "Contract Marriage", "Secret Identity", "Revenge", "Binge"],
    totalEpisodes: 45,
    rating: 4.9,
    viewsCount: 28400000,
    coverImageUrl: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=800&q=80",
    posterColorHex: "#8A1325",
    isTrending: true,
    status: "ongoing"
  },
  {
    title: "Hidden Dragon: The Undercover Tycoon",
    slug: "hidden-dragon-undercover-tycoon",
    synopsis: "Posing as an ordinary son-in-law to fulfill a sacred promise, Leo endured three years of humiliation. When his wife is cornered by arrogant oligarchs, the Supreme Commander finally reveals his terrifying global empire.",
    genre: "Urban Action",
    tags: ["Underdog", "Secret Billionaire", "Revenge", "Action", "High Stakes"],
    totalEpisodes: 50,
    rating: 4.8,
    viewsCount: 34100000,
    coverImageUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=800&q=80",
    posterColorHex: "#142C54",
    isTrending: true,
    status: "ongoing"
  },
  {
    title: "Revenge of the Disgraced Empress",
    slug: "revenge-of-the-disgraced-empress",
    synopsis: "Betrayed and poisoned in the cold palace, Empress Shen awakes five years earlier. Armed with knowledge of every betrayal, she plays the grand chessboard of the court with icy, flawless precision.",
    genre: "Historical Revenge",
    tags: ["Rebirth", "Palace Intrigue", "Scheming", "Drama"],
    totalEpisodes: 60,
    rating: 4.9,
    viewsCount: 19700000,
    coverImageUrl: "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=800&q=80",
    posterColorHex: "#591C3F",
    isTrending: true,
    status: "ongoing"
  }
];

const videoStreamUrls = [
  "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
  "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
  "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
  "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
  "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
  "https://res.cloudinary.com/demo/video/upload/sea-turtle.mp4",
  "https://res.cloudinary.com/demo/video/upload/dog.mp4"
];

async function seedDatabase() {
  try {
    console.log('Connecting to MongoDB...');
    await mongoose.connect(MONGODB_URI);
    console.log('MongoDB Connected successfully!');

    console.log('Cleaning existing records...');
    await Drama.deleteMany({});
    await Episode.deleteMany({});

    for (const dramaData of sampleDramas) {
      const drama = await Drama.create(dramaData);
      console.log(`Created Drama: ${drama.title}`);

      // Seed 5 sample episodes for each drama
      const episodesToCreate = [
        {
          drama: drama._id,
          episodeNumber: 1,
          title: "The Disowned Heiress Returns",
          videoUrl: videoStreamUrls[0],
          durationSec: 82,
          introHook: "Five years after being framed and cast out, she returns to claim what is rightfully hers.",
          likesCount: 48200,
          sharesCount: 1200,
          subtitles: [
            { startSec: 1, endSec: 4, text: "They thought I was gone for good." },
            { startSec: 5, endSec: 9, text: "Tonight, every debt in this ballroom will be paid in full." },
            { startSec: 10, endSec: 15, text: "Who invited that beggar to the President's table?!" }
          ]
        },
        {
          drama: drama._id,
          episodeNumber: 2,
          title: "Contract Marriage at Midnight",
          videoUrl: videoStreamUrls[1],
          durationSec: 95,
          introHook: "CEO Ethan Vance hands her a pen. 'Marry me for one year, and I'll crush your enemies.'",
          likesCount: 52100,
          sharesCount: 2100,
          subtitles: [
            { startSec: 1, endSec: 5, text: "Sign here, Miss Amelia." },
            { startSec: 6, endSec: 10, text: "One year. No emotional attachment. Unlimited power." },
            { startSec: 11, endSec: 15, text: "Deal, Mr. Vance." }
          ]
        },
        {
          drama: drama._id,
          episodeNumber: 3,
          title: "Slap in the VIP Banquet",
          videoUrl: videoStreamUrls[2],
          durationSec: 78,
          introHook: "The arrogant step-sister tries to throw wine at Amelia—until security bows at Amelia's feet.",
          likesCount: 67800,
          sharesCount: 3800
        },
        {
          drama: drama._id,
          episodeNumber: 4,
          title: "The Hidden Stockholder",
          videoUrl: videoStreamUrls[3],
          durationSec: 88,
          introHook: "The board tries to oust Ethan—until the mysterious 30% majority owner walks in.",
          likesCount: 71200,
          sharesCount: 4200
        },
        {
          drama: drama._id,
          episodeNumber: 5,
          title: "Unmasked in the Rain",
          videoUrl: videoStreamUrls[4],
          durationSec: 91,
          introHook: "Ethan finally notices the silver butterfly pendant around her neck: 'It was you all along.'",
          likesCount: 89400,
          sharesCount: 6500
        }
      ];

      await Episode.insertMany(episodesToCreate);
      console.log(`Created ${episodesToCreate.length} episodes for ${drama.title}`);
    }

    console.log('Database Seeding Complete! Enjoy Reel Drama.');
    process.exit(0);
  } catch (error) {
    console.error('Seeding Error:', error);
    process.exit(1);
  }
}

seedDatabase();
