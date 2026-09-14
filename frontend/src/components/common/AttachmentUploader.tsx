import { useRef, useState } from "react";
import { Eye, File, FileText, Loader2, Trash2, Upload, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/**
 * Luật chuẩn tải lên chứng từ (NCL-05-CN-008/CN-009).
 *
 * Đồng bộ với backend:
 * - ALLOWED_ATTACHMENT_TYPES trong ShipmentHandoverServiceImpl
 *   (image/jpeg, image/png, application/pdf).
 * - app.upload.handover.max-size (mặc định 5242880 = 5MB).
 *
 * Đây là nguồn chân lý dùng chung cho mọi màn hình tải chứng từ.
 */
export const ATTACHMENT_MAX_SIZE = 5 * 1024 * 1024;
export const ATTACHMENT_MIME_TYPES: readonly string[] = [
  "image/jpeg",
  "image/png",
  "application/pdf",
];
export const ATTACHMENT_ACCEPT_ATTR = ".jpg,.jpeg,.png,.pdf";
export const ATTACHMENT_INVALID_TYPE_MESSAGE =
  "Loại file không hỗ trợ. Chỉ chấp nhận JPG, PNG, PDF";
export const ATTACHMENT_OVERSIZE_MESSAGE = "File vượt quá dung lượng cho phép (5MB)";

/**
 * Định dạng dung lượng file cho hiển thị.
 */
export const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
};

/** Thông tin file đã tải lên thành công (backend đã lưu). */
export interface AttachmentUploadedInfo {
  fileName: string;
  fileSize: number;
  previewUrl?: string;
}

interface AttachmentUploaderProps {
  /** File đang chọn (chưa upload) — điều khiển bởi parent. */
  selectedFile: File | null;
  onFileChange: (file: File | null) => void;
  /** Gọi thao tác upload thật lên backend (do parent triển khai). */
  onUpload: () => void;
  isUploading?: boolean;
  /** Thông tin file đã upload thành công (nếu có). */
  uploadedInfo?: AttachmentUploadedInfo | null;
  onRemoveUploaded?: () => void;
  /** Lỗi từ API call (upload/submit) — hiển thị dưới vùng tải lên. */
  error?: string | null;
  maxSizeBytes?: number;
  acceptedMimeTypes?: readonly string[];
  accept?: string;
  id?: string;
  className?: string;
}

/**
 * Component tải lên file dùng chung với luật chuẩn hóa duy nhất.
 *
 * Ba trạng thái hiển thị:
 * 1. Chưa có file → ô chọn file dạng dashed.
 * 2. Đã chọn file (chưa tải lên) → tile file kèm nút "Tải lên" và "Bỏ chọn".
 * 3. Đã tải lên thành công → tile file với nút "Xem" và "Gỡ chứng từ".
 *
 * Component chỉ lo chọn file + validate client-side; thao tác upload thật
 * được thực hiện bởi parent thông qua {@code onUpload} (mỗi domain có API riêng).
 */
export function AttachmentUploader({
  selectedFile,
  onFileChange,
  onUpload,
  isUploading = false,
  uploadedInfo,
  onRemoveUploaded,
  error,
  maxSizeBytes = ATTACHMENT_MAX_SIZE,
  acceptedMimeTypes = ATTACHMENT_MIME_TYPES,
  accept = ATTACHMENT_ACCEPT_ATTR,
  id = "attachment-uploader",
  className,
}: AttachmentUploaderProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [validationError, setValidationError] = useState<string>("");

  const validateFile = (file: File | null): string => {
    if (!file) return "";
    if (!acceptedMimeTypes.includes(file.type)) {
      return ATTACHMENT_INVALID_TYPE_MESSAGE;
    }
    if (file.size > maxSizeBytes) {
      return ATTACHMENT_OVERSIZE_MESSAGE;
    }
    return "";
  };

  const handleFileChange = (file: File | null) => {
    const message = validateFile(file);
    setValidationError(message);
    if (message) {
      onFileChange(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
      return;
    }
    onFileChange(file);
  };

  return (
    <div className={cn("space-y-1.5", className)}>
      {isUploading ? (
        <div className="flex items-center gap-2 rounded-lg border bg-muted/30 px-3 py-3 text-sm text-muted-foreground">
          <Loader2 className="h-4 w-4 animate-spin" />
          Đang tải lên chứng từ...
        </div>
      ) : uploadedInfo ? (
        <div className="flex items-center justify-between gap-3 rounded-lg border bg-muted/30 p-3">
          <div className="flex min-w-0 items-center gap-3">
            <FileText className="h-8 w-8 shrink-0 text-muted-foreground" />
            <div className="min-w-0">
              <p className="truncate text-sm font-medium">
                {uploadedInfo.fileName}
              </p>
              <p className="text-xs text-muted-foreground">
                {formatFileSize(uploadedInfo.fileSize)}
              </p>
            </div>
          </div>
          <div className="flex shrink-0 items-center gap-1">
            {uploadedInfo.previewUrl && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() =>
                  window.open(uploadedInfo.previewUrl!, "_blank", "noopener,noreferrer")
                }
              >
                <Eye className="mr-1.5 h-3.5 w-3.5" /> Xem
              </Button>
            )}
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-8 w-8 text-destructive hover:text-destructive"
              onClick={onRemoveUploaded}
              title="Gỡ chứng từ"
              aria-label="Gỡ chứng từ giao hàng"
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        </div>
      ) : selectedFile ? (
        <div className="flex items-center justify-between gap-3 rounded-lg border bg-muted/30 p-3">
          <div className="flex min-w-0 items-center gap-3">
            <File className="h-8 w-8 shrink-0 text-muted-foreground" />
            <div className="min-w-0">
              <p className="truncate text-sm font-medium">{selectedFile.name}</p>
              <p className="text-xs text-muted-foreground">
                {formatFileSize(selectedFile.size)}
              </p>
            </div>
          </div>
          <div className="flex shrink-0 items-center gap-1">
            <Button
              type="button"
              variant="create"
              size="sm"
              onClick={onUpload}
              disabled={isUploading}
            >
              <Upload className="mr-1.5 h-3.5 w-3.5" /> Tải lên
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={() => handleFileChange(null)}
              title="Bỏ chọn file"
              aria-label="Bỏ chọn file"
            >
              <X className="h-4 w-4" />
            </Button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          className="flex w-full flex-col items-center justify-center gap-1.5 rounded-lg border border-dashed bg-muted/20 px-4 py-5 text-center hover:bg-muted/40"
          onClick={() => fileInputRef.current?.click()}
        >
          <Upload className="h-6 w-6 text-muted-foreground" />
          <p className="text-sm text-muted-foreground">Chưa có chứng từ</p>
          <p className="text-xs text-muted-foreground">
            Hỗ trợ JPG, PNG, PDF ({formatFileSize(maxSizeBytes)})
          </p>
        </button>
      )}

      {(validationError || error) && (
        <p className="text-sm text-destructive">{validationError || error}</p>
      )}

      {!isUploading && (
        <input
          ref={fileInputRef}
          id={id}
          type="file"
          accept={accept}
          className="hidden"
          onChange={(event) => handleFileChange(event.target.files?.[0] || null)}
        />
      )}
    </div>
  );
}