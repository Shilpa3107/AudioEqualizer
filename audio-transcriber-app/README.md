# Real-Time Audio Transcriber with Gemini

A full-stack application featuring a real-time circular audio equalizer and low-latency transcription powered by Google Gemini 1.5 Flash.

## 🚀 Features
- **Circular Equalizer**: A high-performance 60FPS visualizer built with Canvas and Web Audio API.
- **Real-Time Streaming**: Binary audio chunks (PCM 16k) sent via WebSockets.
- **Spring Boot Backend**: Reactive architecture using WebFlux for low-latency processing.
- **Gemini AI Integration**: Uses Gemini 1.5 Flash for near-instant transcription.
- **Premium UI**: Modern dark theme with smooth animations and responsive design.

## 🛠️ Tech Stack
- **Frontend**: React, TypeScript, Vite, CSS3 (Vanilla).
- **Backend**: Spring Boot 3.x, WebFlux, Java 17.
- **AI**: Google Cloud Vertex AI (Gemini 1.5).

## 📦 Installation & Setup

### Backend
1. Navigate to `backend/`.
2. Configure your Google Cloud Application Default Credentials.
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

### Frontend
1. Navigate to `frontend/`.
2. Install dependencies:
   ```bash
   npm install
   ```
3. Run the development server:
   ```bash
   npm run dev
   ```

## 🎨 UI/UX Audit Summary for PrepXL
A detailed audit was performed for [www.prepxl.app](https://www.prepxl.app). 
**Improvements identified:**
- Enhance text-over-image readability with overlays.
- Standardize heading typography.
- Populate empty sections like the "Q&A Library".
- Expand the footer for better SEO and navigation.
