import { useState, useEffect, useMemo } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Loader2, Truck } from "lucide-react";
import { toast } from "sonner";
import type { Shipment } from "@/types/shipment";
import type { Organization } from "@/types/organization";
import { getRecipientOrganizations } from "@/api/organizationApi";
import { createHandover, getRemainingQuantity, uploadHandoverAttachment } from "@/api/handoverApi";
import { getAssetUrl } from "@/config/runtimeConfig";
import { getUser } from "@/utils/storage";
import {
  ATTACHMENT_MAX_SIZE,
  ATTACHMENT_MIME_TYPES,
  AttachmentUploader,
} from "@/components/common/AttachmentUploader";

interface CreateHandoverDialogProps {
  open: boolean;
  shipment: Shipment | null;
  onClose: () => void;
  onSuccess: () => void;
}

/**
 * Tên hiển thị của tổ chức nhận: "Tên (Mã)".
 * Fallback về mã rồi ID khi tên trống, để dropdown không bao giờ
 * hiển thị UUID trần (lỗi NCL-05-CN-008: hiện mã thay vì tên).
 */
export const getRecipientDisplayName = (org: Organization): string => {
  const name = org.name?.trim();
  const code = org.code?.trim();
  if (name && code) return `${name} (${code})`;
  if (name) return name;
  if (code) return code;
  return org.id;
};

/**
 * Map "id tổ chức → tên hiển thị" cho Select.Value render label.
 *
 * Base UI v1.6 mặc định render RAW VALUE (UUID) ở trigger khi không truyền
 * `items` cho Select.Root hoặc không dùng children-function cho Select.Value
 * (xem node_modules/@base-ui/react/select/value/SelectValue.js →
 * resolveValueLabel.js: items rỗng → fallback = chính chuỗi value).
 * Hàm này cung cấp bảng label để Value render "Tên (Mã)" thay vì UUID.
 */
export const buildRecipientLabelMap = (
  orgs: Organization[],
): Map<string, string> => {
  const map = new Map<string, string>();
  for (const org of orgs) {
    map.set(org.id, getRecipientDisplayName(org));
  }
  return map;
};

/**
 * Hàm render label cho {@code Select.Value} của giỏ hàng tổ chức nhận.
 *
 * Base UI truyền thẳng RAW VALUE vào children function
 * (xem node_modules/@base-ui/react/select/value/SelectValue.js:
 * {@code childrenProp(value)}) — KHÔNG phải object {@code { value }}.
 * Vì vậy không được destructure {@code { value }} (sẽ luôn undefined
 * và trigger hiển thị placeholder mãi dù đã chọn — lỗi NCL-05-CN-008).
 *
 * @param value  giá trị đang chọn do Base UI truyền vào (có thể undefined)
 * @param labels bảng "id tổ chức → tên hiển thị"
 * @returns tên hiển thị của tổ chức, hoặc placeholder khi chưa chọn
 */
export const renderRecipientSelectValue = (
  value: string | null | undefined,
  labels?: Map<string, string> | null,
): string => {
  if (!value) return "Chọn tổ chức nhận";
  return labels?.get(String(value)) ?? String(value);
};

/**
 * Chuyển filePath backend (vd "./uploads/handovers/xxx.png" hoặc
 * "/app/uploads/handovers/xxx.png") thành URL public /uploads/... để mở xem.
 * Không hợp lệ thì trả undefined (ẩn nút Xem).
 */
export const toHandoverAssetUrl = (filePath?: string | null): string | undefined => {
  if (!filePath) return undefined;
  const normalized = filePath.replace(/\\/g, "/");
  const idx = normalized.indexOf("/uploads/");
  if (idx < 0) return undefined;
  return getAssetUrl(normalized.substring(idx));
};

export const CreateHandoverDialog = ({
  open,
  shipment,
  onClose,
  onSuccess,
}: CreateHandoverDialogProps) => {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [remainingQuantity, setRemainingQuantity] = useState<number>(0);
  const [selectedOrgId, setSelectedOrgId] = useState<string>("");
  const [quantity, setQuantity] = useState<string>("");
  const [plannedAt, setPlannedAt] = useState<string>("");
  const [vehicleInfo, setVehicleInfo] = useState<string>("");
  const [carrierName, setCarrierName] = useState<string>("");
  const [note, setNote] = useState<string>("");
  const [attachmentPath, setAttachmentPath] = useState<string>("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadedFileName, setUploadedFileName] = useState<string>("");
  const [uploadedFileSize, setUploadedFileSize] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loadingData, setLoadingData] = useState(false);
  const [error, setError] = useState<string>("");

  const orgLabels = useMemo(() => buildRecipientLabelMap(organizations), [organizations]);
  const handoverAssetUrl = useMemo(
    () => toHandoverAssetUrl(attachmentPath),
    [attachmentPath],
  );

  useEffect(() => {
    if (open && shipment) {
      loadData();
    }
  }, [open, shipment]);

  const getBackendMessage = (err: any, fallback: string) =>
    err?.response?.data?.message || err?.message || fallback;

  const loadData = async () => {
    setLoadingData(true);
    setError("");
    try {
      const [orgsResult, remainingResult] = await Promise.allSettled([
        getRecipientOrganizations(),
        getRemainingQuantity(shipment!.id),
      ]);

      if (orgsResult.status === "fulfilled") {
        const currentOrgId = getUser()?.organizationId;
        const filtered = (orgsResult.value ?? [])
          .filter((o) => o.status === "ACTIVE")
          .filter((o) => (currentOrgId ? o.id !== currentOrgId : true))
          // Tương thích cả payload cũ dùng organizationID/organizationName
          .map((o: any) => ({
            id: o.id ?? o.organizationID,
            name: o.name ?? o.organizationName,
            code: o.code ?? o.organizationCode,
            type: o.type ?? o.organizationType,
            status: o.status,
            createdAt: o.createdAt,
            updatedAt: o.updatedAt,
          })) as Organization[];
        setOrganizations(filtered);
      } else {
        setError(
          getBackendMessage(orgsResult.reason, "Không thể tải danh sách tổ chức nhận"),
        );
      }

      if (remainingResult.status === "fulfilled") {
        setRemainingQuantity(remainingResult.value);
      } else if (orgsResult.status === "fulfilled") {
        setError(getBackendMessage(remainingResult.reason, "Không thể tải số lượng còn lại"));
      }
    } catch (err: any) {
      setError(getBackendMessage(err, "Không thể tải dữ liệu"));
    } finally {
      setLoadingData(false);
    }
  };

  const handleClose = () => {
    setSelectedOrgId("");
    setQuantity("");
    setPlannedAt("");
    setVehicleInfo("");
    setCarrierName("");
    setNote("");
    setAttachmentPath("");
    setSelectedFile(null);
    setIsUploading(false);
    setUploadedFileName("");
    setUploadedFileSize(0);
    setError("");
    onClose();
  };

  /**
   * Tải file chứng từ giao hàng lên backend, lưu filePath vào attachmentPath.
   */
  const handleUpload = async () => {
    if (!selectedFile) return;

    if (!ATTACHMENT_MIME_TYPES.includes(selectedFile.type)) {
      setError("Loại file không hỗ trợ. Chỉ chấp nhận JPG, PNG, PDF");
      return;
    }
    if (selectedFile.size > ATTACHMENT_MAX_SIZE) {
      setError("File vượt quá dung lượng cho phép (5MB)");
      return;
    }

    setIsUploading(true);
    setError("");
    try {
      const filePath = await uploadHandoverAttachment(selectedFile);
      setAttachmentPath(filePath);
      setUploadedFileName(selectedFile.name);
      setUploadedFileSize(selectedFile.size);
      setSelectedFile(null);
      toast.success("Tải lên chứng từ giao hàng thành công");
    } catch (err: any) {
      setError(getBackendMessage(err, "Tải lên chứng từ thất bại"));
    } finally {
      setIsUploading(false);
    }
  };

  /** Gỡ chứng từ đã đính kèm. */
  const handleRemoveAttachment = () => {
    setAttachmentPath("");
    setUploadedFileName("");
    setUploadedFileSize(0);
    setSelectedFile(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!shipment || !selectedOrgId || !quantity) return;

    setLoading(true);
    setError("");

    try {
      await createHandover({
        shipmentId: shipment.id,
        toOrganizationId: selectedOrgId,
        quantity: Number(quantity),
        plannedAt: plannedAt || undefined,
        vehicleInfo: vehicleInfo || undefined,
        carrierName: carrierName || undefined,
        note: note || undefined,
        attachmentPath: attachmentPath || undefined,
      });
      toast.success("Tạo phiếu bàn giao thành công");
      onSuccess();
      handleClose();
    } catch (err: any) {
      setError(getBackendMessage(err, "Không thể tạo phiếu bàn giao"));
    } finally {
      setLoading(false);
    }
  };

  const quantityNum = Number(quantity);
  const isValidQuantity = quantityNum > 0 && quantityNum <= remainingQuantity;

  return (
    <Dialog open={open} onOpenChange={(nextOpen) => !nextOpen && handleClose()}>
      <DialogContent className="max-w-xl">
        <DialogHeader>
          <DialogTitle>Tạo phiếu bàn giao</DialogTitle>
        </DialogHeader>
        {loadingData ? (
          <div className="flex justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin" />
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label>Tổ chức nhận</Label>
              <Select value={selectedOrgId} onValueChange={(value) => setSelectedOrgId(value ?? "")}>
                <SelectTrigger className="w-full">
                  <SelectValue>
                    {(value) => renderRecipientSelectValue(value, orgLabels)}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {organizations.length === 0 ? (
                    <p className="px-3.5 py-3 text-sm text-muted-foreground">
                      Không có tổ chức nhận khả dụng.
                    </p>
                  ) : (
                    organizations.map((org) => {
                      const displayName = getRecipientDisplayName(org);
                      return (
                        <SelectItem key={org.id} value={org.id} label={displayName}>
                          {displayName}
                        </SelectItem>
                      );
                    })
                  )}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label>Số lượng (kg)</Label>
                <Input
                  type="number"
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                  placeholder="Nhập số lượng"
                  min={1}
                />
                <p className="text-xs text-muted-foreground">
                  Còn lại có thể bàn giao: {remainingQuantity} kg
                </p>
                {remainingQuantity <= 0 && (
                  <p className="text-xs text-destructive">
                    Lô hàng đã bàn giao hết, không thể tạo thêm phiếu.
                  </p>
                )}
                {quantity && !isValidQuantity && remainingQuantity > 0 && (
                  <p className="text-xs text-destructive">
                    Số lượng phải lớn hơn 0 và không vượt quá {remainingQuantity} kg
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label>Thời điểm dự kiến</Label>
                <Input
                  type="datetime-local"
                  value={plannedAt}
                  onChange={(e) => setPlannedAt(e.target.value)}
                />
              </div>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label>Phương tiện</Label>
                <Input
                  value={vehicleInfo}
                  onChange={(e) => setVehicleInfo(e.target.value)}
                  placeholder="Biển số xe, loại phương tiện..."
                />
              </div>
              <div className="space-y-2">
                <Label>Người áp tải</Label>
                <Input
                  value={carrierName}
                  onChange={(e) => setCarrierName(e.target.value)}
                  placeholder="Tên người áp tải"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label>Chứng từ giao hàng</Label>
              <AttachmentUploader
                id="handover-attachment-file"
                selectedFile={selectedFile}
                onFileChange={setSelectedFile}
                isUploading={isUploading}
                onUpload={() => void handleUpload()}
                uploadedInfo={
                  attachmentPath
                    ? {
                        fileName: uploadedFileName,
                        fileSize: uploadedFileSize,
                        previewUrl: handoverAssetUrl,
                      }
                    : null
                }
                onRemoveUploaded={handleRemoveAttachment}
              />
            </div>
            <div className="space-y-2">
              <Label>Ghi chú</Label>
              <Textarea
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Ghi chú thêm (nếu có)"
                rows={2}
              />
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={handleClose}>
                Hủy
              </Button>
              <Button
                type="submit"
                disabled={loading || !selectedOrgId || !isValidQuantity}
              >
                {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                <Truck className="mr-2 h-4 w-4" />
                Tạo phiếu
              </Button>
            </div>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
};
