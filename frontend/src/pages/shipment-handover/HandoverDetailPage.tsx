import { useCallback, useEffect, useState } from "react";
import type { ReactNode } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import {
  CalendarClock,
  CheckCheck,
  FileSignature,
  FileText,
  Loader2,
  PackageOpen,
  StickyNote,
  Truck,
  UserRound,
  XCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { acceptHandover, getHandoverById, rejectHandover } from "@/api/handoverApi";
import { useAuth } from "@/hooks/useAuth";
import { toHandoverAssetUrl } from "@/components/shipment/CreateHandoverDialog";
import { HandoverStatusBadge } from "@/components/shipment/HandoverStatusBadge";
import type { HandoverDetailResponse } from "@/types/shipmentHandover";

const formatDateTime = (iso?: string | null): string => {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
};

/**
 * Trang chi tiết phiếu bàn giao lô hàng (NCL-05-CN-008/CN-009).
 *
 * Mở từ: thông báo (khi notification có entityId), nút xem phiếu trong danh
 * sách lô hàng của tổ chức nhận (Dashboard thu mua), hoặc truy cập trực tiếp.
 * Backend chặn nếu tổ chức hiện tại không phải bên giao/bên nhận.
 */
export const HandoverDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [handover, setHandover] = useState<HandoverDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [accepting, setAccepting] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [rejecting, setRejecting] = useState(false);

  const load = useCallback(async () => {
    if (!id) return;
    try {
      setLoading(true);
      const data = await getHandoverById(id);
      setHandover(data);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || "Không thể tải chi tiết phiếu bàn giao.",
      );
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  const handleAccept = async () => {
    if (!id) return;
    try {
      setAccepting(true);
      await acceptHandover(id);
      toast.success("Đã xác nhận nhận hàng lô bàn giao.");
      await load();
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || "Không thể xác nhận phiếu bàn giao.",
      );
    } finally {
      setAccepting(false);
    }
  };

  const handleReject = async () => {
    if (!id || !rejectReason.trim()) return;
    try {
      setRejecting(true);
      await rejectHandover(id, rejectReason.trim());
      toast.success("Đã từ chối nhận hàng lô bàn giao.");
      setRejectDialogOpen(false);
      setRejectReason("");
      await load();
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || "Không thể từ chối phiếu bàn giao.",
      );
    } finally {
      setRejecting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24 text-muted-foreground">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        Đang tải phiếu bàn giao...
      </div>
    );
  }

  if (!handover) {
    return (
      <div className="space-y-4 py-16 text-center">
        <PackageOpen className="mx-auto h-12 w-12 text-muted-foreground/50" />
        <p className="font-medium text-muted-foreground">
          Không tìm thấy phiếu bàn giao hoặc bạn không có quyền xem phiếu này.
        </p>
        <Button variant="outline" onClick={() => navigate("/dashboard")}>
          Về trang chủ
        </Button>
      </div>
    );
  }

const assetUrl = toHandoverAssetUrl(handover.attachmentPath);

  const canRespond =
    user?.organizationId != null &&
    handover.status === "PENDING_CONFIRMATION" &&
    user.organizationId === handover.toOrganizationId &&
    (user.roleCode === "VT-02" || user.roleCode === "VT-04");

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-600">
              <FileSignature className="h-5 w-5" />
            </span>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                Phiếu bàn giao
              </h1>
              <p className="text-sm text-muted-foreground">
                Lô hàng: {handover.shipmentName}
              </p>
            </div>
          </div>
        </div>
        <HandoverStatusBadge
          status={handover.status}
          className="px-3 py-1 text-sm"
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Thông tin chung</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-x-8 gap-y-4 sm:grid-cols-2">
          <DetailItem
            icon={<FileSignature className="h-4 w-4" />}
            label="Bên giao"
            value={handover.fromOrganizationName}
          />
          <DetailItem
            icon={<PackageOpen className="h-4 w-4" />}
            label="Bên nhận"
            value={handover.toOrganizationName}
          />
          <DetailItem
            icon={<PackageOpen className="h-4 w-4" />}
            label="Số lượng"
            value={`${handover.quantity.toLocaleString("vi-VN")} kg`}
          />
          <DetailItem
            icon={<CalendarClock className="h-4 w-4" />}
            label="Ngày tạo"
            value={formatDateTime(handover.createdAt)}
          />
          <DetailItem
            icon={<CalendarClock className="h-4 w-4" />}
            label="Thời điểm bàn giao dự kiến"
            value={formatDateTime(handover.plannedAt)}
          />
          <DetailItem
            icon={<CalendarClock className="h-4 w-4" />}
            label="Hạn xác nhận"
            value={formatDateTime(handover.expiresAt)}
          />
          {handover.vehicleInfo && (
            <DetailItem
              icon={<Truck className="h-4 w-4" />}
              label="Phương tiện"
              value={handover.vehicleInfo}
            />
          )}
          {handover.carrierName && (
            <DetailItem
              icon={<UserRound className="h-4 w-4" />}
              label="Người vận chuyển"
              value={handover.carrierName}
            />
          )}
          {handover.note && (
            <DetailItem
              icon={<StickyNote className="h-4 w-4" />}
              label="Ghi chú"
              value={handover.note}
            />
          )}
          {handover.confirmedAt && (
            <DetailItem
              icon={<FileSignature className="h-4 w-4" />}
              label="Thời điểm xác nhận"
              value={formatDateTime(handover.confirmedAt)}
            />
          )}
          {handover.rejectedAt && (
            <DetailItem
              icon={<XCircle className="h-4 w-4" />}
              label="Thời điểm từ chối"
              value={formatDateTime(handover.rejectedAt)}
            />
          )}
          {handover.cancelledAt && (
            <DetailItem
              icon={<XCircle className="h-4 w-4" />}
              label="Đã hủy"
              value={formatDateTime(handover.cancelledAt)}
            />
          )}
          {handover.cancelReason && (
            <DetailItem
              icon={<StickyNote className="h-4 w-4" />}
              label="Lý do hủy/từ chối"
              value={handover.cancelReason}
            />
          )}
        </CardContent>
      </Card>

      {assetUrl && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Chứng từ giao hàng</CardTitle>
          </CardHeader>
          <CardContent>
            <a
              href={assetUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 text-sm font-medium text-emerald-700 hover:text-emerald-800 hover:underline"
            >
              <FileText className="h-4 w-4" />
              Xem chứng từ giao hàng
            </a>
          </CardContent>
        </Card>
      )}

      {canRespond && (
        <Card className="border-emerald-200 bg-emerald-50/40">
          <CardHeader>
            <CardTitle className="text-base">Xác nhận nhận hàng</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm text-muted-foreground">
              Bạn đang là tổ chức nhận của phiếu bàn giao này. Xác nhận nhận
              hàng sẽ ghi sự kiện bàn giao vào chuỗi hành trình và chuyển
              trách nhiệm với lô hàng cho tổ chức của bạn. Từ chối sẽ nhả lại
              số lượng bàn giao cho bên giao.
            </p>
            <div className="flex flex-wrap items-center justify-end gap-3 pt-2">
              <Button
                variant="outline"
                className="w-full sm:w-auto text-red-600 hover:text-red-700 hover:bg-red-50 border-red-200"
                disabled={accepting}
                onClick={() => setRejectDialogOpen(true)}
              >
                <XCircle className="mr-2 h-4 w-4" />
                Từ chối nhận hàng
              </Button>
              <Button
                onClick={handleAccept}
                disabled={accepting}
                className="w-full sm:w-auto bg-emerald-600 hover:bg-emerald-700 text-white"
              >
                {accepting ? (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                ) : (
                  <CheckCheck className="mr-2 h-4 w-4" />
                )}
                {accepting ? "Đang xác nhận..." : "Xác nhận nhận hàng"}
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      <div className="flex gap-2">
        <Button variant="outline" onClick={() => navigate("/dashboard")}>
          Về trang chủ
        </Button>
        <Button
          variant="outline"
          onClick={() => navigate(`/shipments/${handover.shipmentId}`)}
        >
          Xem lô hàng
        </Button>
      </div>

      <Dialog
        open={rejectDialogOpen}
        onOpenChange={(open) => !open && !rejecting && setRejectDialogOpen(false)}
      >
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Từ chối nhận hàng</DialogTitle>
            <DialogDescription>
              Nhập lý do từ chối nhận hàng của phiếu bàn giao này. Phiếu sẽ
              chuyển sang trạng thái “Đã từ chối” và số lượng bàn giao được nhả
              lại cho bên giao.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="rejectReason">Lý do từ chối</Label>
            <Textarea
              id="rejectReason"
              value={rejectReason}
              onChange={(event) => setRejectReason(event.target.value)}
              placeholder="Ví dụ: Chứng từ kiểm dịch chưa đầy đủ, hàng không đạt chất lượng..."
              rows={4}
              disabled={rejecting}
            />
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setRejectDialogOpen(false)}
              disabled={rejecting}
            >
              Hủy
            </Button>
            <Button
              onClick={handleReject}
              disabled={rejecting || !rejectReason.trim()}
              className="bg-red-600 text-white hover:bg-red-700"
            >
              {rejecting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {rejecting ? "Đang từ chối..." : "Xác nhận từ chối"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

function DetailItem({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="space-y-1">
      <p className="flex items-center gap-1.5 text-xs font-medium uppercase tracking-wide text-muted-foreground/70">
        {icon}
        {label}
      </p>
      <p className="text-sm font-medium text-slate-900">{value}</p>
    </div>
  );
}