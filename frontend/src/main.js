import './style.css'

const canvas = document.getElementById('visualizer');
const ctx = canvas.getContext('2d');
const micBtn = document.getElementById('mic-toggle');
const btnText = micBtn.querySelector('.btn-text');
const transcriptionText = document.getElementById('transcription-text');
const transcriptionPanel = document.querySelector('.transcription-panel');

let audioContext;
let analyser;
let source;
let dataArray;
let bufferLength;
let isRecording = false;
let animationId;

// Resize canvas to match display size
function resizeCanvas() {
  const container = canvas.parentElement;
  canvas.width = container.clientWidth;
  canvas.height = container.clientHeight;
}
window.addEventListener('resize', resizeCanvas);
resizeCanvas();

async function initAudio() {
  try {
    audioContext = new (window.AudioContext || window.webkitAudioContext)();
    analyser = audioContext.createAnalyser();

    // FFT Size determines frequency resolution. 2048 gives 1024 bins.
    analyser.fftSize = 256;
    bufferLength = analyser.frequencyBinCount;
    dataArray = new Uint8Array(bufferLength);

    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    source = audioContext.createMediaStreamSource(stream);
    source.connect(analyser);

    return true;
  } catch (err) {
    console.error('Error accessing microphone:', err);
    alert('Could not access microphone. Please allow permissions.');
    return false;
  }
}

function drawVisualizer() {
  if (!isRecording) return;
  animationId = requestAnimationFrame(drawVisualizer);

  analyser.getByteFrequencyData(dataArray);

  ctx.clearRect(0, 0, canvas.width, canvas.height);

  const centerX = canvas.width / 2;
  const centerY = canvas.height / 2;
  // Radius for the inner circle
  const radius = Math.min(centerX, centerY) * 0.4;

  // Draw base circle
  ctx.beginPath();
  ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI);
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)';
  ctx.lineWidth = 2;
  ctx.stroke();

  const bars = 80; // Number of bars to draw around the circle
  const step = (Math.PI * 2) / bars;

  for (let i = 0; i < bars; i++) {
    // Map i to index in dataArray. We use lower frequencies which are more active.
    // dataArray has 128 items (fftSize/2). We can spread them or pick a range.
    const dataIndex = Math.floor((i / bars) * (bufferLength * 0.7));
    // Smooth the value slightly? Raw is fine for responsiveness.
    const value = dataArray[dataIndex];

    // Bar height based on value. Max value is 255.
    // Scale it.
    const barHeight = (value / 255) * (Math.min(centerX, centerY) * 0.5);

    const angle = i * step;

    // Start point on circle
    const x1 = centerX + Math.cos(angle) * radius;
    const y1 = centerY + Math.sin(angle) * radius;

    // End point pointing outwards
    const x2 = centerX + Math.cos(angle) * (radius + barHeight + 5); // +5 min length
    const y2 = centerY + Math.sin(angle) * (radius + barHeight + 5);

    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);

    // Gradient color based on angle or frequency
    // Let's use frequency (value) for color intensity or angle for hue
    const hue = (i / bars) * 360;
    ctx.strokeStyle = `hsl(180, 100%, 50%)`; // Cyan base
    // Use dynamic color:
    const grad = ctx.createLinearGradient(x1, y1, x2, y2);
    grad.addColorStop(0, '#00f0ff');
    grad.addColorStop(1, '#7000ff');
    ctx.strokeStyle = grad;

    ctx.lineWidth = 4;
    ctx.lineCap = 'round';
    ctx.stroke();
  }

  // Optional: Inner glow pulse
  const avgVolume = dataArray.reduce((num, a) => num + a, 0) / bufferLength;
  ctx.beginPath();
  ctx.arc(centerX, centerY, radius - 5, 0, 2 * Math.PI);
  ctx.fillStyle = `rgba(0, 240, 255, ${avgVolume / 255 * 0.2})`;
  ctx.fill();
}

async function toggleListening() {
  if (isRecording) {
    // Stop
    isRecording = false;
    micBtn.classList.remove('recording');
    btnText.textContent = 'Start Listening';
    transcriptionPanel.classList.remove('active');
    cancelAnimationFrame(animationId);
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Close WebSocket
    if (window.ws) {
      window.ws.close();
      window.ws = null;
    }

    // Disconnect nodes
    if (window.processor) {
      window.processor.disconnect();
      window.processor = null;
    }

    if (audioContext) audioContext.close();
    audioContext = null;

  } else {
    // Start
    const ready = await initAudio();
    if (ready) {
      isRecording = true;
      micBtn.classList.add('recording');
      btnText.textContent = 'Stop Listening';
      transcriptionPanel.classList.add('active');
      drawVisualizer();

      transcriptionText.innerHTML = "<i>Connecting to server...</i>";

      // Connect to Backend
      try {
        // Assuming backend runs on 8080
        const ws = new WebSocket('ws://localhost:8080/stream');
        window.ws = ws;

        ws.onopen = () => {
          transcriptionText.innerHTML = "<i>Connected. Listening...</i>";

          // Setup Audio Streaming
          // simple script processor for raw PCM
          const bufferSize = 4096;
          const processor = audioContext.createScriptProcessor(bufferSize, 1, 1);
          window.processor = processor;

          source.connect(processor);
          processor.connect(audioContext.destination); // needed for chrome? sometimes.

          processor.onaudioprocess = (e) => {
            if (ws.readyState === WebSocket.OPEN) {
              const inputData = e.inputBuffer.getChannelData(0);
              // Convert Float32 to Int16 usually required or send raw float
              // Let's send raw float32 bytes for simplicity or convert.
              // We'll send raw bytes of the Float32Array.
              ws.send(inputData.buffer);
            }
          };
        };

        ws.onmessage = (event) => {
          transcriptionText.innerText = event.data;
        };

        ws.onerror = (error) => {
          console.error("WebSocket Error:", error);
          transcriptionText.innerHTML = "<i>Connection Error. Is Backend running?</i>";
        };

      } catch (e) {
        console.error(e);
      }
    }
  }
}

micBtn.addEventListener('click', toggleListening);
