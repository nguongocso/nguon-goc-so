import { useEffect, useRef, useState } from 'react';
import { BrowserQRCodeReader } from '@zxing/browser';
import { Camera } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

interface TraceCodeQrScanModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onScanSuccess: (code: string) => void;
}

export function TraceCodeQrScanModal({
  open,
  onOpenChange,
  onScanSuccess,
}: TraceCodeQrScanModalProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const controlsRef = useRef<{ stop: () => void } | null>(null);
  const [error, setError] = useState<string>('');

  const stopScanning = () => {
    controlsRef.current?.stop();
    controlsRef.current = null;

    streamRef.current?.getTracks().forEach((t) => t.stop());
    streamRef.current = null;
  };

  useEffect(() => {
    if (!open) {
      stopScanning();
      setError('');
      return;
    }

    let isActive = true;
    const codeReader = new BrowserQRCodeReader();

    const start = async () => {
      try {
        await new Promise((resolve) => window.setTimeout(resolve, 150));
        const video = videoRef.current;
        if (!video || !isActive) return;

        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'environment' },
        });

        if (!isActive) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }

        streamRef.current = stream;
        video.srcObject = stream;
        await video.play();

        const controls = await codeReader.decodeFromVideoElement(
          video,
          (result) => {
            if (!isActive || !result) return;
            const text = result.getText();
            if (text) {
              // Xử lý URL tra cứu để lấy ra codeValue nếu có
              let extractedCode = text.trim();
              if (extractedCode.includes('/public/trace/')) {
                const parts = extractedCode.split('/public/trace/');
                extractedCode = parts[parts.length - 1].split('?')[0].split('#')[0];
              } else if (extractedCode.includes('/trace/')) {
                const parts = extractedCode.split('/trace/');
                extractedCode = parts[parts.length - 1].split('?')[0].split('#')[0];
              }

              stopScanning();
              onOpenChange(false);
              onScanSuccess(extractedCode);
            }
          },
        );

        controlsRef.current = controls;
      } catch (err: any) {
        if (!isActive) return;
        setError(
          err?.name === 'NotAllowedError'
            ? 'Trình duyệt chưa được cấp quyền sử dụng camera.'
            : 'Không thể khởi động camera quét mã QR.',
        );
      }
    };

    void start();

    return () => {
      isActive = false;
      stopScanning();
    };
  }, [open]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md p-6">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Camera className="h-5 w-5 text-emerald-600" />
            Quét mã QR tem truy xuất
          </DialogTitle>
          <DialogDescription>
            Hướng camera vào mã QR in trên tem để tra cứu nhanh thông tin và lịch sử.
          </DialogDescription>
        </DialogHeader>

        <div className="relative aspect-square overflow-hidden rounded-xl border bg-black flex items-center justify-center">
          <video
            ref={videoRef}
            playsInline
            muted
            className="h-full w-full object-cover"
          />
          {/* Overlay scanner frame */}
          <div className="pointer-events-none absolute inset-8 rounded-lg border-2 border-dashed border-emerald-400/80" />

          {error && (
            <div className="absolute inset-0 bg-black/80 flex items-center justify-center p-4 text-center text-white text-sm">
              <p>{error}</p>
            </div>
          )}
        </div>

        <div className="flex justify-end pt-2">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Đóng
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
