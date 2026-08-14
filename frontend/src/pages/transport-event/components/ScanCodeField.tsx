import { useEffect, useRef, useState } from "react";
import { BrowserQRCodeReader } from "@zxing/browser";
import { Camera, ScanLine } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface ScanCodeFieldProps {
  value: string;
  onChange: (value: string) => void;
  error?: string;
  disabled?: boolean;
}

export function ScanCodeField({
  value,
  onChange,
  error,
  disabled = false,
}: ScanCodeFieldProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const controlsRef = useRef<{ stop: () => void } | null>(null);

  const [scannerOpen, setScannerOpen] = useState(false);
  const [scannerError, setScannerError] = useState("");

  const stopScanning = () => {
    controlsRef.current?.stop();
    controlsRef.current = null;

    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;

    setScannerOpen(false);
  };

  useEffect(() => {
    if (!scannerOpen) return;

    let isActive = true;
    const codeReader = new BrowserQRCodeReader();

    const startScanning = async () => {
      try {
       await new Promise((resolve) => window.setTimeout(resolve, 150));

        const video = videoRef.current;

        if (!video) {
        throw new Error("Không tìm thấy vùng hiển thị camera.");
        }

        const stream = await navigator.mediaDevices.getUserMedia({
          audio: false,
          video: {
            facingMode: {
              ideal: "environment",
            },
          },
        });

        if (!isActive) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }

        streamRef.current = stream;
        video.srcObject = stream;

        const controls = await codeReader.decodeFromVideoElement(
          video,
          (result) => {
            if (!result || !isActive) return;

            onChange(result.getText());
            toast.success("Đã quét mã lô hàng.");
            stopScanning();
          },
        );

        if (!isActive) {
          controls.stop();
          return;
        }

        controlsRef.current = controls;
      } catch (scanError: unknown) {
        if (!isActive) return;

        if (
          scanError instanceof DOMException &&
          scanError.name === "NotAllowedError"
        ) {
          setScannerError(
            "Bạn chưa cho phép dùng camera. Hãy cấp quyền camera rồi thử lại.",
          );
          return;
        }

        if (
          scanError instanceof DOMException &&
          scanError.name === "NotReadableError"
        ) {
          setScannerError(
            "Camera đang được ứng dụng khác sử dụng. Hãy đóng ứng dụng đó rồi thử lại.",
          );
          return;
        }

        setScannerError(
          "Không thể mở camera. Hãy kiểm tra camera hoặc nhập mã thủ công.",
        );
      }
    };

    void startScanning();

    return () => {
      isActive = false;
      controlsRef.current?.stop();
      controlsRef.current = null;

      streamRef.current?.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    };
  }, [scannerOpen, onChange]);

  return (
    <div className="space-y-2">
      <Label htmlFor="codeValue">Mã truy xuất lô hàng *</Label>

      <div className="flex flex-col gap-2 sm:flex-row">
        <Input
          id="codeValue"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder="Ví dụ: HX00000029"
          autoComplete="off"
          disabled={disabled}
        />

        <Button
          type="button"
          variant="outline"
          disabled={disabled}
          onClick={() => {
            setScannerError("");
            setScannerOpen(true);
          }}
          className="shrink-0"
        >
          <ScanLine className="mr-2 h-4 w-4" />
          Quét bằng camera
        </Button>
      </div>

      <p className="text-xs text-muted-foreground">
        Bạn có thể quét QR bằng camera hoặc nhập mã thủ công.
      </p>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <Dialog
        open={scannerOpen}
        onOpenChange={(open) => {
          if (open) {
            setScannerOpen(true);
          } else {
            stopScanning();
          }
        }}
      >
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Camera className="h-5 w-5" />
              Quét mã truy xuất
            </DialogTitle>
            <DialogDescription>
              Đưa mã QR vào trong khung hình để hệ thống tự điền mã lô hàng.
            </DialogDescription>
          </DialogHeader>

          <div className="overflow-hidden rounded-lg bg-black">
            <video
              ref={videoRef}
              className="aspect-video w-full object-cover"
              muted
              playsInline
            />
          </div>

          {scannerError && (
            <p className="text-sm text-destructive">{scannerError}</p>
          )}

          <Button type="button" variant="outline" onClick={stopScanning}>
            Hủy quét
          </Button>
        </DialogContent>
      </Dialog>
    </div>
  );
}