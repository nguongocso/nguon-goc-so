import { useState, useEffect, useMemo } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  ClipboardList,
  FileText,
  Pencil,
  Clock,
  User,
  Calendar,
  Package,
  AlertTriangle,
  Paperclip,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { DetailSection } from "@/components/common/detail/DetailSection";
import { AttachmentManager } from "@/components/farm-log/AttachmentManager";
import {
  getFarmLogById,
  getAllFarmLogsByProductionLot,
} from "@/api/farmLogApi";
import { useAuth } from "@/hooks/useAuth";
import { ROLE_ACCESS, hasAnyRole } from "@/config/roleAccess";
import { cn } from "@/lib/utils";
import {
  ACTIVITY_TYPE_ICONS,
  getActivityLabel,
  formatDateTime,
  buildFarmLogGroups,
  type FarmLogGroup,
} from "@/utils/farmLogCorrection";
import type { FarmLog } from "@/types/farmLog";

export default function FarmLogDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [log, setLog] = useState<FarmLog | null>(null);
  const [lotLogs, setLotLogs] = useState<FarmLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    let isMounted = true;
    setLoading(true);
    setError(null);

    getFarmLogById(id)
      .then(async (data) => {
        if (!isMounted) return;
        setLog(data);
        if (data.productionLotId) {
          try {
            const allLogs = await getAllFarmLogsByProductionLot(
              data.productionLotId,
            );
            if (isMounted) setLotLogs(allLogs);
          } catch (e) {
            console.error("Lỗi khi tải lịch sử lô sản xuất:", e);
          }
        }
      })
      .catch((err) => {
        if (!isMounted) return;
        console.error("Lỗi khi tải chi tiết nhật ký:", err);
        setError(
          "Không tìm thấy nhật ký canh tác hoặc bạn không có quyền truy cập.",
        );
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [id]);

  const groups = useMemo(() => buildFarmLogGroups(lotLogs), [lotLogs]);

  const currentGroup = useMemo<FarmLogGroup | null>(() => {
    if (!log) return null;
    const rootId = log.originalFarmLogId || log.id;
    return groups.find((g) => g.original.id === rootId) || null;
  }, [log, groups]);

  const canCorrect = useMemo(() => {
    if (!log || !user) return false;
    if (!hasAnyRole(user.roleCode, ROLE_ACCESS.farmLogCorrect)) return false;
    if (log.isCorrected) return false;
    const isManager = user.roleCode === "VT-02";
    if (!isManager) return !log.createdById || log.createdById === user.userId;
    return true;
  }, [log, user]);

  const breadcrumbItems = useMemo(() => {
    return [
      { label: "Dashboard", href: "/dashboard" },
      { label: "Lô sản xuất", href: "/production-lots" },
      ...(log?.productionLotId
        ? [
            {
              label: log.productionLotName || "Chi tiết lô",
              href: `/production-lots/${log.productionLotId}`,
            },
          ]
        : []),
      { label: "Chi tiết nhật ký" },
    ];
  }, [log]);

  useSetBreadcrumb(log ? breadcrumbItems : null);

  if (loading) {
    return (
      <div className="container mx-auto py-8 max-w-7xl space-y-4">
        <Card className="h-64 flex items-center justify-center">
          <div className="flex flex-col items-center gap-2">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent"></div>
            <p className="text-sm text-muted-foreground">
              Đang tải thông tin nhật ký...
            </p>
          </div>
        </Card>
      </div>
    );
  }

  if (error || !log) {
    return (
      <div className="container mx-auto py-8 max-w-7xl space-y-4">
        <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
          <ArrowLeft className="mr-2 h-4 w-4" /> Quay lại
        </Button>
        <Card className="border-destructive/30 bg-destructive/5 text-center py-12">
          <CardContent className="space-y-4 pt-6">
            <AlertTriangle className="mx-auto h-12 w-12 text-destructive" />
            <h2 className="text-xl font-bold text-destructive">
              Không tìm thấy nhật ký
            </h2>
            <p className="text-sm text-muted-foreground">
              {error || "Nhật ký không tồn tại."}
            </p>
            <Button onClick={() => navigate(-1)}>Quay lại danh sách</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const Icon = ACTIVITY_TYPE_ICONS[log.activityType] ?? ClipboardList;
  const hasCorrections = currentGroup
    ? currentGroup.corrections.length > 0
    : false;
  const originalLog = currentGroup ? currentGroup.original : log;
  const latestEffectiveLog =
    currentGroup && hasCorrections ? currentGroup.corrections[0] : log;

  return (
    <div className="container mx-auto py-6 max-w-7xl space-y-6">
      <Card className="shadow-xs">
        <CardHeader className="border-b bg-muted/20 pb-4">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-100 text-emerald-800 border border-emerald-200">
                <Icon className="h-6 w-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h1 className="text-xl font-bold tracking-tight text-foreground">
                    {getActivityLabel(log.activityType)}
                  </h1>
                  {log.isCorrection ? (
                    <Badge className="bg-emerald-100 text-emerald-800 hover:bg-emerald-100 border border-emerald-300">
                      Bản đính chính
                    </Badge>
                  ) : log.isCorrected ? (
                    <Badge className="bg-amber-100 text-amber-800 hover:bg-amber-100 border border-amber-300">
                      Đã đính chính
                    </Badge>
                  ) : (
                    <Badge
                      variant="secondary"
                      className="bg-slate-100 text-slate-700 border border-slate-200"
                    >
                      Chưa đính chính
                    </Badge>
                  )}
                </div>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Ngày thực hiện:{" "}
                  <span className="font-semibold text-foreground">
                    {log.executedDate}
                  </span>
                </p>
              </div>
            </div>

            {canCorrect && (
              <Button
                onClick={() => navigate(`/farm-logs/${log.id}/correct`)}
                className="bg-amber-600 hover:bg-amber-700 text-white"
              >
                <Pencil className="mr-2 h-4 w-4" />
                {hasCorrections ? "Đính chính tiếp" : "Đính chính nhật ký này"}
              </Button>
            )}
          </div>
        </CardHeader>

        <CardContent className="pt-6 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 rounded-lg border bg-card p-4 text-sm">
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-muted-foreground">
                <Calendar className="h-4 w-4 text-emerald-600" />
                <span>Ngày thực hiện:</span>
                <strong className="text-foreground">{log.executedDate}</strong>
              </div>
              <div className="flex items-center gap-2 text-muted-foreground">
                <Package className="h-4 w-4 text-emerald-600" />
                <span>Vật tư:</span>
                <strong className="text-foreground">
                  {log.material || "Không có"}
                </strong>
              </div>
              <div className="flex items-center gap-2 text-muted-foreground">
                <ClipboardList className="h-4 w-4 text-emerald-600" />
                <span>Số lượng & Đơn vị:</span>
                <strong className="text-foreground">
                  {log.quantity != null
                    ? `${log.quantity} ${log.unit ?? ""}`.trim()
                    : "Không có"}
                </strong>
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex items-center gap-2 text-muted-foreground">
                <User className="h-4 w-4 text-emerald-600" />
                <span>Người ghi nhận:</span>
                <strong className="text-foreground">
                  {log.createdByName || "Hệ thống"}
                </strong>
              </div>
              <div className="flex items-center gap-2 text-muted-foreground">
                <Clock className="h-4 w-4 text-emerald-600" />
                <span>Thời gian tạo:</span>
                <strong className="text-foreground">
                  {formatDateTime(log.createdAt)}
                </strong>
              </div>
              {log.productionLotId && (
                <div className="flex items-center gap-2 text-muted-foreground">
                  <span className="text-xs text-muted-foreground">
                    Mã lô sản xuất:
                  </span>
                  <code className="bg-muted px-1.5 py-0.5 rounded text-xs font-mono">
                    {log.productionLotId.slice(0, 8)}...
                  </code>
                </div>
              )}
            </div>
          </div>

          {log.notes && (
            <DetailSection title="Ghi chú hoạt động">
              <p className="whitespace-pre-wrap text-sm text-foreground/90 bg-muted/30 p-3 rounded-md border">
                {log.notes}
              </p>
            </DetailSection>
          )}

          {log.isCorrection && (
            <div className="rounded-lg border border-amber-200 bg-amber-50/70 p-4 space-y-2 text-xs text-amber-950">
              <div className="flex items-center gap-2 text-amber-900 font-semibold text-sm">
                <Pencil className="h-4 w-4" /> Bản ghi đính chính
              </div>
              <p>
                <strong>Lý do đính chính:</strong>{" "}
                {log.correctionReason || "Không có lý do"}
              </p>
              <div className="flex flex-wrap items-center gap-4 text-amber-800/80 pt-1 border-t border-amber-200/60">
                <span>
                  👤 Người sửa:{" "}
                  <strong>{log.correctedByName || "Hệ thống"}</strong>
                </span>
                <span>🕐 Ngày đính chính: {formatDateTime(log.createdAt)}</span>
              </div>
            </div>
          )}

          {/* Tabs */}
          <Tabs defaultValue="history" className="w-full">
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="history" className="flex items-center gap-2">
                <FileText className="h-4 w-4" />
                Lịch sử đính chính
              </TabsTrigger>
              <TabsTrigger
                value="attachments"
                className="flex items-center gap-2"
              >
                <Paperclip className="h-4 w-4" />
                Chứng từ
                {log.attachmentCount !== undefined &&
                  log.attachmentCount > 0 && (
                    <Badge
                      variant="secondary"
                      className="ml-1 h-5 w-5 rounded-full p-0 text-xs flex items-center justify-center"
                    >
                      {log.attachmentCount}
                    </Badge>
                  )}
              </TabsTrigger>
            </TabsList>

            <TabsContent value="history" className="mt-4 space-y-4">
              {currentGroup && hasCorrections ? (
                <>
                  <div className="rounded-lg border border-amber-200 bg-card overflow-hidden shadow-xs">
                    <div className="bg-amber-50/90 px-5 py-3 border-b border-amber-200 flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <ClipboardList className="h-4 w-4 text-amber-700" />
                        <span className="text-sm font-semibold uppercase tracking-wide text-amber-950">
                          So sánh chi tiết đính chính (Chuẩn GAP)
                        </span>
                      </div>
                      <Badge
                        variant="outline"
                        className="border-amber-300 bg-amber-100/80 text-amber-900 text-sm font-medium px-2.5 py-0.5"
                      >
                        {currentGroup.corrections.length} lần đính chính
                      </Badge>
                    </div>

                    <div className="overflow-x-auto">
                      <Table className="border-collapse">
                        <TableHeader className="bg-muted/40">
                          <TableRow className="text-sm border-b border-slate-200">
                            <TableHead className="w-1/4 font-semibold text-foreground border-r border-slate-200 px-4 py-3">
                              Trường thông tin
                            </TableHead>
                            <TableHead className="w-3/8 font-semibold text-red-700 border-r border-slate-200 px-4 py-3">
                              GIÁ TRỊ GỐC
                            </TableHead>
                            <TableHead className="w-3/8 font-semibold text-emerald-700 px-4 py-3">
                              GIÁ TRỊ SAU ĐÍNH CHÍNH
                            </TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody className="text-sm divide-y divide-slate-200">
                          {(() => {
                            const origAct = getActivityLabel(
                              originalLog.activityType,
                            );
                            const effAct = getActivityLabel(
                              latestEffectiveLog.activityType,
                            );
                            const actChanged =
                              originalLog.activityType !==
                              latestEffectiveLog.activityType;

                            const origDate = originalLog.executedDate;
                            const effDate = latestEffectiveLog.executedDate;
                            const dateChanged =
                              originalLog.executedDate !==
                              latestEffectiveLog.executedDate;

                            const origMat = originalLog.material || "Không có";
                            const effMat =
                              latestEffectiveLog.material || "Không có";
                            const matChanged =
                              (originalLog.material ?? "") !==
                              (latestEffectiveLog.material ?? "");

                            const origQtyUnit =
                              originalLog.quantity != null
                                ? `${originalLog.quantity} ${originalLog.unit ?? ""}`.trim()
                                : "Không có";
                            const effQtyUnit =
                              latestEffectiveLog.quantity != null
                                ? `${latestEffectiveLog.quantity} ${latestEffectiveLog.unit ?? ""}`.trim()
                                : "Không có";
                            const qtyChanged =
                              originalLog.quantity !==
                                latestEffectiveLog.quantity ||
                              originalLog.unit !== latestEffectiveLog.unit;

                            const origNotes = originalLog.notes || "Không có";
                            const effNotes =
                              latestEffectiveLog.notes || "Không có";
                            const notesChanged =
                              (originalLog.notes ?? "") !==
                              (latestEffectiveLog.notes ?? "");

                            const compRows = [
                              {
                                label: "Loại hoạt động",
                                orig: origAct,
                                eff: effAct,
                                changed: actChanged,
                              },
                              {
                                label: "Ngày thực hiện",
                                orig: origDate,
                                eff: effDate,
                                changed: dateChanged,
                              },
                              {
                                label: "Vật tư",
                                orig: origMat,
                                eff: effMat,
                                changed: matChanged,
                              },
                              {
                                label: "Số lượng & Đơn vị",
                                orig: origQtyUnit,
                                eff: effQtyUnit,
                                changed: qtyChanged,
                              },
                              {
                                label: "Ghi chú",
                                orig: origNotes,
                                eff: effNotes,
                                changed: notesChanged,
                              },
                            ];

                            return compRows.map((r) => (
                              <TableRow
                                key={r.label}
                                className={cn(
                                  r.changed
                                    ? "bg-amber-50/60"
                                    : "hover:bg-muted/30",
                                  "border-b border-slate-200 text-sm",
                                )}
                              >
                                <TableCell className="font-semibold text-foreground/90 border-r border-slate-200 px-4 py-3">
                                  {r.label}
                                </TableCell>
                                <TableCell className="border-r border-slate-200 px-4 py-3">
                                  <span
                                    className={cn(
                                      r.changed
                                        ? "line-through decoration-red-500 text-slate-500 font-medium"
                                        : "text-foreground",
                                    )}
                                  >
                                    {r.orig}
                                  </span>
                                </TableCell>
                                <TableCell className="px-4 py-3">
                                  <span
                                    className={cn(
                                      r.changed
                                        ? "font-bold text-emerald-900 bg-emerald-100/80 border border-emerald-300 px-3 py-1 rounded-md inline-block shadow-2xs"
                                        : "text-foreground",
                                    )}
                                  >
                                    {r.eff}
                                  </span>
                                </TableCell>
                              </TableRow>
                            ));
                          })()}
                        </TableBody>
                      </Table>
                    </div>
                  </div>

                  <div className="space-y-2 pt-2">
                    <p className="text-sm font-bold uppercase tracking-wide text-muted-foreground flex items-center gap-1.5 pt-2">
                      <FileText className="h-4 w-4" /> LỊCH SỬ ĐÍNH CHÍNH
                    </p>
                    {currentGroup.corrections.map((c) => (
                      <div
                        key={c.id}
                        className="rounded-lg border border-amber-200 bg-amber-50/60 p-4 text-sm space-y-2"
                      >
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <span className="font-medium text-amber-950 text-sm">
                            <strong className="text-amber-900">
                              Lý do đính chính:
                            </strong>{" "}
                            {c.correctionReason || "Không có lý do"}
                          </span>
                          <span className="text-amber-800/90 text-xs font-medium">
                            {c.createdAt ? formatDateTime(c.createdAt) : "—"}
                          </span>
                        </div>
                        <div className="flex flex-wrap items-center justify-between text-muted-foreground gap-2 pt-2 border-t border-amber-200/60 text-xs">
                          <span>
                            👤 Người thực hiện:{" "}
                            <strong className="text-foreground font-semibold">
                              {c.correctedByName || "Hệ thống"}
                            </strong>
                          </span>
                          <span className="text-xs text-muted-foreground flex items-center gap-1">
                            Mã bản đính chính:
                            <code
                              className="bg-amber-100/50 px-1.5 py-0.5 rounded font-mono text-[10px] text-amber-700/70"
                              title={c.id}
                            >
                              {c.id.slice(0, 8)}
                            </code>
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <div className="text-center py-8 text-muted-foreground">
                  <FileText className="mx-auto h-8 w-8 text-muted-foreground/50" />
                  <p className="mt-2 text-sm font-medium">
                    Chưa có lịch sử đính chính
                  </p>
                  <p className="text-xs text-muted-foreground">
                    Bản ghi này là bản gốc và chưa được đính chính.
                  </p>
                </div>
              )}
            </TabsContent>

            <TabsContent value="attachments" className="mt-4">
              <DetailSection
                title="Chứng từ đính kèm"
                contentClassName="bg-card"
              >
                <AttachmentManager logId={log.id} />
              </DetailSection>
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  );
}
