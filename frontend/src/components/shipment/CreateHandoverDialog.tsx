import { useState, useEffect, useMemo, useRef } from "react";
import { Link } from "react-router-dom";
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
import { Alert, AlertTitle, AlertDescription } from "@/components/ui/alert";
import { Loader2, Truck, CheckCircle2, ChevronDown, Search } from "lucide-react";
import { toast } from "sonner";
import type { Shipment } from "@/types/shipment";
import type { Organization } from "@/types/organization";
import { getRecipientOrganizations } from "@/api/organizationApi";
import { createHandover, getRemainingQuantity, uploadHandoverAttachment } from "@/api/handoverApi";
import { getAssetUrl } from "@/config/runtimeConfig";
import { getUser } from "@/utils/storage";
import { normalizeVietnamese } from "@/utils/string";
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
 * Hàm render label cho giá trị đã chọn của dropdown tổ chức nhận.
 *
 * @param value  giá trị đang chọn (có thể undefined)
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

  // YC1: State cho searchable dropdown tổ chức nhận
  const [orgDropdownOpen, setOrgDropdownOpen] = useState(false);
  const [orgSearchTerm, setOrgSearchTerm] = useState("");
  const dropdownRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // YC3: State lưu ID phiếu vừa tạo thành công để hiển thị inline alert
  const [createdHandoverId, setCreatedHandoverId] = useState<string | null>(null);

  const orgLabels = useMemo(() => buildRecipientLabelMap(organizations), [organizations]);
  const handoverAssetUrl = useMemo(
    () => toHandoverAssetUrl(attachmentPath),
    [attachmentPath],
  );

  // YC1: Lọc danh sách tổ chức theo từ khóa tìm kiếm (bỏ dấu tiếng Việt)
  const filteredOrganizations = useMemo(() => {
    if (!orgSearchTerm.trim()) return organizations;
    const keyword = normalizeVietnamese(orgSearchTerm);
    return organizations.filter((org) => {
      const nameNorm = normalizeVietnamese(org.name ?? "");
      const codeNorm = normalizeVietnamese(org.code ?? "");
      const idNorm = normalizeVietnamese(org.id);
      return (
        nameNorm.includes(keyword) ||
        codeNorm.includes(keyword) ||
        idNorm.includes(keyword)
      );
    });
  }, [organizations, orgSearchTerm]);

  // YC1: Đóng dropdown khi click ra ngoài
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setOrgDropdownOpen(false);
      }
    };
    if (orgDropdownOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [orgDropdownOpen]);

  // YC1: Focus ô tìm kiếm khi mở dropdown
  useEffect(() => {
    if (orgDropdownOpen && searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, [orgDropdownOpen]);

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
          // Phiếu bàn giao chỉ nhắm tới Doanh nghiệp thu mua (VT-04 / ENTERPRISE)
          .filter((o) => o.type === "ENTERPRISE")
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
    // YC1: Reset trạng thái tìm kiếm
    setOrgSearchTerm("");
    setOrgDropdownOpen(false);
    // YC3: Reset thông báo thành công
    setCreatedHandoverId(null);
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
    // YC3: Reset thông báo cũ trước khi submit mới
    setCreatedHandoverId(null);

    try {
      const result = await createHandover({
        shipmentId: shipment.id,
        toOrganizationId: selectedOrgId,
        quantity: Number(quantity),
        plannedAt: plannedAt || undefined,
        vehicleInfo: vehicleInfo || undefined,
        carrierName: carrierName || undefined,
        note: note || undefined,
        attachmentPath: attachmentPath || undefined,
      });
      // YC3: Lưu ID phiếu vừa tạo, KHÔNG đóng dialog, KHÔNG toast
      setCreatedHandoverId(result.id);
      onSuccess();
    } catch (err: any) {
      setError(getBackendMessage(err, "Không thể tạo phiếu bàn giao"));
    } finally {
      setLoading(false);
    }
  };

  const quantityNum = Number(quantity);
  const isValidQuantity = quantityNum > 0 && quantityNum <= remainingQuantity;

  // Tên tổ chức đang được chọn để hiển thị trên trigger
  const selectedOrgLabel = selectedOrgId
    ? orgLabels.get(selectedOrgId) ?? selectedOrgId
    : "";

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
            {/* YC1: Dropdown tìm kiếm tổ chức nhận */}
            <div className="space-y-2">
              <Label>Tổ chức nhận</Label>
              <div className="relative" ref={dropdownRef}>
                <button
                  type="button"
                  className="flex h-9 w-full items-center justify-between rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                  onClick={() => setOrgDropdownOpen(!orgDropdownOpen)}
                >
                  <span className={selectedOrgLabel ? "text-foreground" : "text-muted-foreground"}>
                    {selectedOrgLabel || "Chọn tổ chức nhận"}
                  </span>
                  <ChevronDown className="h-4 w-4 opacity-50" />
                </button>

                {orgDropdownOpen && (
                  <div className="absolute z-50 mt-1 w-full rounded-md border bg-popover text-popover-foreground shadow-md">
                    <div className="flex items-center border-b px-3">
                      <Search className="mr-2 h-4 w-4 shrink-0 opacity-50" />
                      <input
                        ref={searchInputRef}
                        type="text"
                        className="flex h-9 w-full bg-transparent py-2 text-sm outline-none placeholder:text-muted-foreground"
                        placeholder="Tìm theo tên, mã, mã số thuế..."
                        value={orgSearchTerm}
                        onChange={(e) => setOrgSearchTerm(e.target.value)}
                      />
                    </div>
                    <div className="max-h-60 overflow-auto p-1">
                      {filteredOrganizations.length === 0 ? (
                        <p className="px-3.5 py-3 text-sm text-muted-foreground">
                          Không tìm thấy tổ chức phù hợp.
                        </p>
                      ) : (
                        filteredOrganizations.map((org) => {
                          const displayName = getRecipientDisplayName(org);
                          const isSelected = org.id === selectedOrgId;
                          return (
                            <button
                              key={org.id}
                              type="button"
                              className={`flex w-full items-center rounded-sm px-3 py-2 text-sm transition-colors hover:bg-accent hover:text-accent-foreground ${
                                isSelected ? "bg-accent text-accent-foreground" : ""
                              }`}
                              onClick={() => {
                                setSelectedOrgId(org.id);
                                setOrgDropdownOpen(false);
                                setOrgSearchTerm("");
                              }}
                            >
                              {displayName}
                            </button>
                          );
                        })
                      )}
                    </div>
                  </div>
                )}
              </div>
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

            {/* YC3: Thông báo tạo thành công dạng inline alert thay vì toast */}
            {createdHandoverId && (
              <Alert variant="success">
                <CheckCircle2 className="h-4 w-4" />
                <AlertTitle>Tạo phiếu bàn giao thành công</AlertTitle>
                <AlertDescription>
                  Mã phiếu: {createdHandoverId}.{" "}
                  <Link
                    to={`/shipment-handovers/${createdHandoverId}`}
                    className="font-medium underline underline-offset-3 hover:text-foreground"
                  >
                    Xem chi tiết phiếu
                  </Link>
                </AlertDescription>
              </Alert>
            )}

            {error && <p className="text-sm text-destructive">{error}</p>}
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={handleClose}>
                Đóng
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
