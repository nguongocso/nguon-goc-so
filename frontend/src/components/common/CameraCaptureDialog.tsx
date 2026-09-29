import { useCallback, useEffect, useRef, useState } from "react";
import { Camera, LoaderCircle, RotateCcw, X } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

export interface CameraCaptureDialogProps {
  /** Dialog có mở hay không. */
  open: boolean;
  /** Đóng dialog (không trả ảnh). */
  onClose: () => void;
  /** Trả về ảnh vừa chụp dưới dạng Blob. */
  onCapture: (file: File) => void;
  /** Tên file đặt cho ảnh chụp. */
  fileName?: string;
}

/** Chuyển ảnh chụp từ canvas thành File JPEG. */
const canvasToFile = (
  canvas: HTMLCanvasElement,
  fileName: string,
  quality = 0.92
): Promise<File> =>
  new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          reject(new Error("Không tạo được ảnh từ camera."));
          return;
        }
        resolve(new File([blob], fileName, { type: "image/jpeg" }));
      },
      "image/jpeg",
      quality
    );
  });

/**
 * Hộp thoại chụp ảnh bằng camera thật qua `navigator.mediaDevices.getUserMedia`.
 * Dùng cho nút "Chụp ảnh ngay": bấm vào phải mở camera, không mở hộp thoại
 * chọn tệp như nút "Chọn tệp". Nếu trình duyệt không hỗ trợ hoặc người dùng
 * từ chối quyền camera, hiển thị lý do cụ thể thay vì im lặng rơi về file picker.
 */
export const CameraCaptureDialog = ({
  open,
  onClose,
  onCapture,
  fileName = "anh-chup.jpg",
}: CameraCaptureDialogProps) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const stopStream = useCallback(() => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
  }, []);

  // Mở camera khi dialog hiển thị, tắt camera khi đóng.
  useEffect(() => {
    if (!open) {
      stopStream();
      return;
    }

    let isActive = true;
    setError(null);
    setStarting(true);

    const startCamera = async () => {
      try {
        if (!navigator.mediaDevices?.getUserMedia) {
          throw new Error("Trình duyệt không hỗ trợ mở camera.");
        }

        const stream = await navigator.mediaDevices.getUserMedia({
          audio: false,
          video: { facingMode: { ideal: "environment" } },
        });

        if (!isActive) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }

        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play().catch(() => undefined);
        }
      } catch (cameraError: unknown) {
        if (!isActive) return;

        if (cameraError instanceof DOMException) {
          if (cameraError.name === "NotAllowedError") {
            setError("Bạn chưa cho phép dùng camera. Hãy cấp quyền camera rồi thử lại.");
            return;
          }
          if (cameraError.name === "NotFoundError") {
            setError("Không tìm thấy camera trên thiết bị. Hãy dùng nút Chọn tệp.");
            return;
          }
          if (cameraError.name === "NotReadableError") {
            setError("Camera đang được ứng dụng khác sử dụng. Hãy đóng ứng dụng đó rồi thử lại.");
            return;
          }
        }

        setError("Không thể mở camera. Hãy dùng nút Chọn tệp để tải ảnh lên.");
      } finally {
        if (isActive) setStarting(false);
      }
    };

    void startCamera();

    return () => {
      isActive = false;
      stopStream();
    };
  }, [open, stopStream]);

  const handleCapture = async () => {
    const video = videoRef.current;
    if (!video || !video.videoWidth || !video.videoHeight) return;

    const canvas = document.createElement("canvas");
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const context = canvas.getContext("2d");
    if (!context) return;

    context.drawImage(video, 0, 0, canvas.width, canvas.height);

    try {
      onCapture(await canvasToFile(canvas, fileName));
      stopStream();
      onClose();
    } catch (captureError: unknown) {
      setError(
        captureError instanceof Error
          ? captureError.message
          : "Không tạo được ảnh từ camera."
      );
    }
  };

  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="w-full max-w-lg">
        <DialogHeader>
          <DialogTitle>Chụp ảnh</DialogTitle>
          <DialogDescription>
            Đưa camera về phía chứng từ cần chụp rồi bấm nút chụp.
          </DialogDescription>
        </DialogHeader>

        <div className="overflow-hidden rounded-xl border border-slate-200 bg-slate-950">
          <video
            ref={videoRef}
            playsInline
            muted
            className="h-72 w-full object-cover"
            aria-label="Khung hình camera"
          />
        </div>

        {starting && (
          <p className="flex items-center gap-2 text-sm text-muted-foreground">
            <LoaderCircle className="size-4 animate-spin" />
            Đang mở camera...
          </p>
        )}

        {error && (
          <p className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
            {error}
          </p>
        )}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="outline" onClick={onClose}>
            <X className="mr-1.5 h-4 w-4" />
            Hủy
          </Button>
          <Button
            type="button"
            variant="create"
            onClick={handleCapture}
            disabled={!!error || starting}
          >
            <Camera className="mr-1.5 h-4 w-4" />
            Chụp ảnh
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};

/** Nút mở hộp thoại chụp ảnh, dùng chung cho các màn hình cần chụp ảnh nhanh. */
export const CameraCaptureButton = ({
  disabled,
  onClick,
  className,
  label = "Chụp ảnh ngay",
  icon: Icon = Camera,
}: {
  disabled?: boolean;
  onClick: () => void;
  className?: string;
  label?: string;
  icon?: typeof RotateCcw;
}) => (
  <Button
    type="button"
    variant="outline"
    size="sm"
    onClick={onClick}
    disabled={disabled}
    title="Chụp ảnh bằng camera"
    className={className}
  >
    <Icon className="mr-2 size-4 text-slate-500" />
    {label}
  </Button>
);
