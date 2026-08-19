import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Copy, Download, ImageOff } from "lucide-react";
import { toast } from "sonner";
import { getAssetBaseUrl } from "@/config/runtimeConfig";
import type { TraceCode } from "@/types/shipment";

interface QrCodeGridProps {
  traceCodes: TraceCode[];
  baseUrl?: string;
}

export const QrCodeGrid = ({
  traceCodes,
  baseUrl = getAssetBaseUrl(),
}: QrCodeGridProps) => {
  const resolveUrl = (imageUrl: string) =>
    imageUrl.startsWith("http") ? imageUrl : `${baseUrl}${imageUrl}`;

  const [failedIds, setFailedIds] = useState<Set<string>>(new Set());
  const markFailed = (id: string) =>
    setFailedIds((prev) => new Set(prev).add(id));

  const [preview, setPreview] = useState<{ src: string; code: string } | null>(
    null,
  );

  const handleCopyCode = (code: string) => {
    navigator.clipboard.writeText(code).then(
      () => toast.success("Đã sao chép mã"),
      () => toast.error("Sao chép thất bại"),
    );
  };

  const handleDownload = async (code: string, imageUrl: string) => {
    const fullUrl = resolveUrl(imageUrl);
    try {
      const response = await fetch(fullUrl);
      if (!response.ok) throw new Error("Fetch failed");
      const blob = await response.blob();
      const objectUrl = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = objectUrl;
      link.download = `${code}.png`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(objectUrl);
    } catch {
      toast.error("Tải mã QR thất bại");
    }
  };

  if (!traceCodes || traceCodes.length === 0) {
    return <p className="text-sm text-muted-foreground">Chưa có mã QR nào.</p>;
  }

  return (
    <>
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4 justify-items-center">
      {traceCodes.map((code) => {
        const qrFullUrl = code.qrImage ? resolveUrl(code.qrImage) : "";
        const hasFailed = !code.qrImage || failedIds.has(code.id);
        return (
          <Card
            key={code.id}
            className="w-full max-w-[160px] rounded-xl shadow-sm border hover:shadow-md transition-shadow"
          >
            <CardContent className="p-3 flex flex-col items-center">
              {hasFailed ? (
                <div className="w-24 h-24 mx-auto flex flex-col items-center justify-center gap-1 rounded bg-muted text-muted-foreground">
                  <ImageOff className="h-6 w-6" />
                  <span className="text-[10px]">Lỗi tải ảnh</span>
                </div>
              ) : (
                <img
                  src={qrFullUrl}
                  alt={code.codeValue}
                  title="Click để phóng to"
                  className="w-28 h-28 object-contain cursor-pointer"
                  onClick={() =>
                    setPreview({ src: qrFullUrl, code: code.codeValue })
                  }
                  onError={() => markFailed(code.id)}
                />
              )}
              <p className="mt-2 w-full font-mono text-[11px] font-semibold text-center break-all leading-4">
                {code.codeValue}
              </p>
              <p className="text-xs text-emerald-600 font-medium mt-1">
                {code.status}
              </p>
              <div className="mt-2 flex gap-1">
                <Button
                  size="sm"
                  variant="ghost"
                  className="h-7 w-7 p-0"
                  onClick={() => handleCopyCode(code.codeValue)}
                >
                  <Copy className="h-3 w-3" />
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  className="h-7 w-7 p-0"
                  disabled={!code.qrImage}
                  onClick={() =>
                    code.qrImage && handleDownload(code.codeValue, code.qrImage)
                  }
                >
                  <Download className="h-3 w-3" />
                </Button>
              </div>
            </CardContent>
          </Card>
        );
      })}
    </div>

      {/* QR preview dialog: zoom the QR image on click */}
      {preview && (
        <Dialog open onOpenChange={(open) => !open && setPreview(null)}>
          <DialogContent className="w-full max-w-sm sm:max-w-md">
            <DialogHeader>
              <DialogTitle>Mã QR: {preview.code}</DialogTitle>
              <DialogDescription>
                Xem mã QR ở kích thước lớn.
              </DialogDescription>
            </DialogHeader>
            <img
              src={preview.src}
              alt={preview.code}
              className="mx-auto w-full max-h-[70vh] object-contain rounded-md"
            />
          </DialogContent>
        </Dialog>
      )}
    </>
  );
};
