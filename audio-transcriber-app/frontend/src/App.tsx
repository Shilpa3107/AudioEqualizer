import React, { useEffect, useRef, useState } from 'react';
import './App.css';

const Visualizer: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const [isRecording, setIsRecording] = useState(false);
  const [transcription, setTranscription] = useState('');
  const animationFrameRef = useRef<number>();

  const socketRef = useRef<WebSocket | null>(null);

  const startStreaming = async () => {
    try {
      // Connect to WebSocket
      socketRef.current = new WebSocket('ws://localhost:8080/audio-stream');

      socketRef.current.onmessage = (event) => {
        setTranscription(prev => prev + event.data);
      };

      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      audioContextRef.current = new AudioContext({ sampleRate: 16000 });
      analyserRef.current = audioContextRef.current.createAnalyser();
      const source = audioContextRef.current.createMediaStreamSource(stream);

      // Use ScriptProcessor or AudioWorklet for raw data (ScriptProcessor is simpler for demo)
      const processor = audioContextRef.current.createScriptProcessor(4096, 1, 1);

      source.connect(analyserRef.current);
      analyserRef.current.connect(processor);
      processor.connect(audioContextRef.current.destination);

      processor.onaudioprocess = (e) => {
        if (socketRef.current?.readyState === WebSocket.OPEN) {
          const inputData = e.inputBuffer.getChannelData(0);
          // Convert to 16-bit PCM for Gemini
          const pcmData = convertFloat32ToInt16(inputData);
          socketRef.current.send(pcmData);
        }
      };

      analyserRef.current.fftSize = 256;
      const bufferLength = analyserRef.current.frequencyBinCount;
      const dataArray = new Uint8Array(bufferLength);

      setIsRecording(true);
      draw(dataArray, bufferLength);
    } catch (err) {
      console.error('Error accessing microphone:', err);
    }
  };

  const convertFloat32ToInt16 = (buffer: Float32Array) => {
    let l = buffer.length;
    let buf = new Int16Array(l);
    while (l--) {
      buf[l] = Math.min(1, buffer[l]) * 0x7FFF;
    }
    return buf.buffer;
  };

  const stopStreaming = () => {
    if (animationFrameRef.current) {
      cancelAnimationFrame(animationFrameRef.current);
    }
    if (audioContextRef.current) {
      audioContextRef.current.close();
    }
    if (socketRef.current) {
      socketRef.current.close();
    }
    setIsRecording(false);
  };

  const draw = (dataArray: Uint8Array, bufferLength: number) => {
    const canvas = canvasRef.current;
    if (!canvas || !analyserRef.current) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const renderFrame = () => {
      animationFrameRef.current = requestAnimationFrame(renderFrame);
      analyserRef.current!.getByteFrequencyData(dataArray);

      ctx.clearRect(0, 0, canvas.width, canvas.height);

      const centerX = canvas.width / 2;
      const centerY = canvas.height / 2;
      const radius = 80;

      // Draw outer glowing circle
      ctx.beginPath();
      ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI);
      ctx.strokeStyle = 'rgba(100, 200, 255, 0.2)';
      ctx.lineWidth = 2;
      ctx.stroke();

      for (let i = 0; i < bufferLength; i++) {
        const barHeight = (dataArray[i] / 255) * 100;
        const angle = (i * 2 * Math.PI) / bufferLength;

        const x1 = centerX + Math.cos(angle) * radius;
        const y1 = centerY + Math.sin(angle) * radius;
        const x2 = centerX + Math.cos(angle) * (radius + barHeight);
        const y2 = centerY + Math.sin(angle) * (radius + barHeight);

        // Gradient for bars
        const gradient = ctx.createLinearGradient(x1, y1, x2, y2);
        gradient.addColorStop(0, '#60a5fa');
        gradient.addColorStop(1, '#a855f7');

        ctx.strokeStyle = gradient;
        ctx.lineWidth = 3;
        ctx.lineCap = 'round';
        ctx.beginPath();
        ctx.moveTo(x1, y1);
        ctx.lineTo(x2, y2);
        ctx.stroke();
      }
    };

    renderFrame();
  };

  useEffect(() => {
    return () => {
      if (animationFrameRef.current) cancelAnimationFrame(animationFrameRef.current);
    };
  }, []);

  return (
    <div className="app-container">
      <header className="header">
        <h1>Voice Transcription AI</h1>
        <p>Real-time circular equalizer and Gemini-powered transcription</p>
      </header>

      <main className="main-content">
        <div className="visualizer-card">
          <canvas
            ref={canvasRef}
            width={400}
            height={400}
            className="equalizer-canvas"
          />
          <div className="controls">
            {!isRecording ? (
              <button className="btn btn-primary" onClick={startStreaming}>
                Start Recording
              </button>
            ) : (
              <button className="btn btn-secondary" onClick={stopStreaming}>
                Stop Recording
              </button>
            )}
          </div>
        </div>

        <div className="transcription-card">
          <h3>Transcription</h3>
          <div className="transcription-text">
            {transcription || 'Speak something to see the magic...'}
          </div>
        </div>
      </main>

      <footer className="footer">
        Built for PrepXL Assignment
      </footer>
    </div>
  );
};

export default Visualizer;
