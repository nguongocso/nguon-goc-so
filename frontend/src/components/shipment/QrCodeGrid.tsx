import { useState, useMemo } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Copy, Download, ImageOff, ChevronLeft, ChevronRight } from "lucide-react";
import { toast } from "sonner";
import { getAssetBaseUrl } from "@/config/runtimeConfig";
import type { TraceCode } from "@/types/shipment";

interface QrCodeGridProps {
  traceCodes: TraceCode[];
  baseUrl?: string;
}

const PAGE_SIZE = 24;

export const QrCodeGrid = ({
  traceCodes,
  baseUrl = getAssetBaseUrl(),
}: QrCodeGridProps) => {
  const resolveUrl = (imageUrl: string) =>
    imageUrl.startsWith("http") ? imageUrl : `${baseUrl}${imageUrl}`;

  const [currentPage, setCurrentPage] = useState(1);
  const [failedIds, setFailedIds] = useState<Set<string>>(new Set());
  const markFailed = (id: string) =>
    setFailedIds((prev) => new Set(prev).add(id));

  const [preview, setPreview] = useState<{ src: string; code: string } | null>(
    null,
  );

  const totalPages = useMemo(
    () => Math.ceil(traceCodes.length / PAGE_SIZE) || 1,
    [traceCodes.length],
  );

  const paginatedTraceCodes = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return traceCodes.slice(start, start + PAGE_SIZE);
  }, [traceCodes, currentPage]);

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

  const startIdx = (currentPage - 1) * PAGE_SIZE + 1;
  const endIdx = Math.min(currentPage * PAGE_SIZE, traceCodes.length);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between text-xs text-slate-500 pb-2 border-b border-slate-100">
        <div>
          Hiển thị <span className="font-semibold text-slate-800">{startIdx}</span> -{" "}
          <span className="font-semibold text-slate-800">{endIdx}</span> trong tổng số{" "}
          <span className="font-semibold text-slate-800">{traceCodes.length.toLocaleString("vi-VN")}</span> mã QR
        </div>
        {totalPages > 1 && (
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage === 1}
              onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
              className="h-7 px-2.5 text-xs gap-1"
            >
              <ChevronLeft className="size-3.5" />
              Trước
            </Button>
            <span className="font-medium text-slate-700">
              {currentPage} / {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage === totalPages}
              onClick={() => setCurrentPage((prev) => Math.min(totalPages, prev + 1))}
              className="h-7 px-2.5 text-xs gap-1"
            >
              Sau
              <ChevronRight className="size-3.5" />
            </Button>
          </div>
        )}
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3 justify-items-center">
        {paginatedTraceCodes.map((code) => {
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
                    loading="lazy"
                    className="w-24 h-24 object-contain cursor-pointer"
                    onClick={() =>
                      setPreview({ src: qrFullUrl, code: code.codeValue })
                    }
                    onError={() => markFailed(code.id)}
                  />
                )}
                <p className="mt-2 w-full font-mono text-[10px] font-semibold text-center break-all leading-3">
                  {code.codeValue}
                </p>
                <p className="text-[10px] text-emerald-600 font-medium mt-1">
                  {code.status}
                </p>
                <div className="mt-2 flex gap-1">
                  <Button
                    size="sm"
                    variant="ghost"
                    className="h-6 w-6 p-0"
                    onClick={() => handleCopyCode(code.codeValue)}
                    title="Sao chép mã"
                  >
                    <Copy className="h-3 w-3" />
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    className="h-6 w-6 p-0"
                    disabled={!code.qrImage}
                    onClick={() =>
                      code.qrImage && handleDownload(code.codeValue, code.qrImage)
                    }
                    title="Tải ảnh QR"
                  >
                    <Download className="h-3 w-3" />
                  </Button>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-500">
          <div>
            Trang <strong className="text-slate-800">{currentPage}</strong> trên tổng {totalPages} trang
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage === 1}
              onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
              className="h-8 gap-1 text-xs"
            >
              <ChevronLeft className="size-3.5" />
              Trang trước
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage === totalPages}
              onClick={() => setCurrentPage((prev) => Math.min(totalPages, prev + 1))}
              className="h-8 gap-1 text-xs"
            >
              Trang sau
              <ChevronRight className="size-3.5" />
            </Button>
          </div>
        </div>
      )}

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
    </div>
  );
};
