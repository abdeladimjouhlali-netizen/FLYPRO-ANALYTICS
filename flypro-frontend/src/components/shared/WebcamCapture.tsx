import { useEffect, useRef, useState } from "react";
import { Camera, RefreshCw, Video, VideoOff } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { cn } from "@/utils/cn";

export function WebcamCapture({
  onCapture,
  height = 240,
}: {
  onCapture: (dataUrl: string) => void;
  height?: number;
}) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [active, setActive] = useState(false);
  const [captured, setCaptured] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function stop() {
    streamRef.current?.getTracks().forEach((t) => t.stop());
    streamRef.current = null;
    setActive(false);
  }

  async function start() {
    setError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "user" } });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setCaptured(null);
      setActive(true);
    } catch {
      setError("Impossible d'accéder à la caméra. Vérifiez les autorisations du navigateur.");
    }
  }

  useEffect(() => () => stop(), []);

  function capture() {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas) return;
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    const dataUrl = canvas.toDataURL("image/jpeg", 0.9);
    setCaptured(dataUrl);
    onCapture(dataUrl);
    stop();
  }

  return (
    <div className="flex flex-col items-center gap-3">
      <div
        className="relative w-full max-w-sm overflow-hidden rounded-xl border border-[var(--color-border)] bg-black"
        style={{ height }}
      >
        {captured ? (
          <img src={captured} alt="Capture" className="size-full object-cover" />
        ) : (
          <video ref={videoRef} className={cn("size-full object-cover", !active && "hidden")} muted playsInline />
        )}
        {!active && !captured ? (
          <div className="flex size-full flex-col items-center justify-center gap-2 text-white/70">
            <VideoOff className="size-8" />
            <p className="text-xs">Caméra inactive</p>
          </div>
        ) : null}
      </div>
      <canvas ref={canvasRef} className="hidden" />
      {error ? <p className="text-xs text-[var(--color-danger)]">{error}</p> : null}
      <div className="flex gap-2">
        {!active && !captured ? (
          <Button type="button" size="sm" onClick={() => void start()}>
            <Video className="size-4" /> Activer la caméra
          </Button>
        ) : null}
        {active ? (
          <Button type="button" size="sm" onClick={capture}>
            <Camera className="size-4" /> Capturer
          </Button>
        ) : null}
        {captured ? (
          <Button type="button" size="sm" variant="secondary" onClick={() => void start()}>
            <RefreshCw className="size-4" /> Reprendre
          </Button>
        ) : null}
      </div>
    </div>
  );
}
