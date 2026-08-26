import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  ChevronLeft,
  ChevronRight,
  LoaderCircle,
  Package,
  Plus,
  Sprout,
  Wheat,
} from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { getProductionLotById } from "@/api/productionLotApi";
import { ShipmentList } from "@/pages/public/shipment/ShipmentList";
import { FarmLogList } from "@/components/farm-log/FarmLogList";
import { usePermission } from "@/hooks/usePermission";
import { ROLE_ACCESS } from "@/config/roleAccess";
import { HarvestForm } from "@/components/trace-event/HarvestForm";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import { HelpButton } from "@/components/help/HelpButton";
import type { ProductionLot } from "@/types/productionLot";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { maskId } from "@/lib/utils";
import type {
  CanActivateSealCheck,
  InspectionRequestListItem,
  InspectionRequestStatusDisplay,
  InspectionRequestStatusQuery,
  LotTestCriteriaResult,
  ProductionLotCertification,
} from "@/types/certification";
import {
  checkCanActivateSeal,
  createInspectionRequest,
  detachCertification,
  getInspectionRequests,
  getLotCertifications,
  getLotTestCriteria,
  getTestingUnits,
} from "@/api/certificationApi";
import type { TestingUnit } from "@/types/certification";
import { TestingUnitSelect } from "@/components/testing-unit/TestingUnitSelect";
import { CertificationList } from "@/components/certification/CertificationList";
import { AttachCertificationDialog } from "@/components/certification/AttachCertificationDialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogPopup,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { PageResponse } from "@/types/common";

// Ánh xạ trạng thái sang tiếng Việt và màu sắc
const STATUS_MAP: Record<string, { label: string; className: string }> = {
  DRAFT: {
    label: "Bản nháp",
    className: "bg-gray-200 text-gray-700",
  },
  PENDING: {
    label: "Chờ duyệt",
    className: "bg-yellow-100 text-yellow-800 border-yellow-300",
  },
  APPROVED: {
    label: "Đã duyệt",
    className: "bg-emerald-100 text-emerald-800 border-emerald-300",
  },
  HARVESTED: {
    label: "Đã thu hoạch",
    className: "bg-lime-100 text-lime-800 border-lime-300",
  },
  PREPROCESSED: {
    label: "Đã sơ chế",
    className: "bg-teal-100 text-teal-800 border-teal-300",
  },
  PACKAGED: {
    label: "Đã đóng gói",
    className: "bg-sky-100 text-sky-800 border-sky-300",
  },
  SHIPPED: {
    label: "Đang vận chuyển",
    className: "bg-indigo-100 text-indigo-800 border-indigo-300",
  },
  RECALLED: {
    label: "Đã thu hồi",
    className: "bg-red-100 text-red-800 border-red-300",
  },
  COMPLETED: {
    label: "Hoàn thành",
    className: "bg-purple-100 text-purple-800 border-purple-300",
  },
};

const getStatusBadge = (status: string) => {
  const config = STATUS_MAP[status] || {
    label: status,
    className: "bg-gray-100 text-gray-700",
  };
  return (
    <Badge
      variant="outline"
      className={`${config.className} border text-xs font-semibold px-2.5 py-0.5`}
    >
      {config.label}
    </Badge>
  );
};

// Ánh xạ trạng thái yêu cầu kiểm nghiệm (trả về từ backend)
const INSPECTION_STATUS_MAP: Record<
  InspectionRequestStatusDisplay,
  { label: string; className: string }
> = {
  PENDING: {
    label: "Chờ kết quả",
    className: "bg-yellow-100 text-yellow-800 border-yellow-300",
  },
  PASSED: {
    label: "Đạt",
    className: "bg-emerald-100 text-emerald-800 border-emerald-300",
  },
  FAILED: {
    label: "Không đạt",
    className: "bg-red-100 text-red-800 border-red-300",
  },
  CANCELLED: {
    label: "Đã hủy",
    className: "bg-gray-100 text-gray-700 border-gray-300",
  },
};

// Bộ lọc trạng thái; "Chờ kết quả" gửi PENDING_RESULT (enum backend)
const INSPECTION_FILTER_OPTIONS: Array<{
  value: InspectionRequestStatusQuery | "ALL";
  label: string;
}> = [
  { value: "ALL", label: "Tất cả" },
  { value: "PENDING_RESULT", label: "Chờ kết quả" },
  { value: "PASSED", label: "Đạt" },
  { value: "FAILED", label: "Không đạt" },
  { value: "CANCELLED", label: "Đã hủy" },
];

const getInspectionStatusBadge = (status: InspectionRequestStatusDisplay) => {
  const config = INSPECTION_STATUS_MAP[status];
  if (!config) {
    return <Badge variant="outline">{status}</Badge>;
  }
  return (
    <Badge
      variant="outline"
      className={`${config.className} border text-xs font-semibold px-2.5 py-0.5`}
    >
      {config.label}
    </Badge>
  );
};

// Định dạng ngày kiểu "YYYY-MM-DD" sang tiếng Việt
const formatDateOnly = (dateStr: string) => {
  if (!dateStr) return "—";
  const date = new Date(`${dateStr}T00:00:00`);
  if (Number.isNaN(date.getTime())) return dateStr;
  return date.toLocaleDateString("vi-VN");
};

const toISODate = (date: Date) => {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
};

const getApiErrorMessage = (error: unknown, fallback: string): string => {
  if (axios.isAxiosError(error)) {
    const serverMessage = error.response?.data?.message;
    if (typeof serverMessage === "string" && serverMessage.trim()) {
      return serverMessage;
    }
    const status = error.response?.status;
    if (status === 400) {
      return "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại thông tin.";
    }
    if (status === 403) {
      return "Bạn không có quyền thực hiện thao tác này.";
    }
    if (status === 404) {
      return "Không tìm thấy dữ liệu yêu cầu.";
    }
    if (status === 409) {
      return "Yêu cầu kiểm nghiệm trùng lặp với yêu cầu đang chờ kết quả cho cùng bộ chỉ tiêu. Vui lòng xác nhận để tạo thêm yêu cầu.";
    }
  }
  return fallback;
};

export const ProductionLotDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const canCreateFarmLog = usePermission(ROLE_ACCESS.farmLogCreate);
  const canCreatePreprocessing = usePermission(
    ROLE_ACCESS.preprocessingEventCreate,
  );
  const canInspect = usePermission(ROLE_ACCESS.inspectionRequest);
  const [lot, setLot] = useState<ProductionLot | null>(null);
  const [loading, setLoading] = useState(true);
  const [showHarvestForm, setShowHarvestForm] = useState(false);
  const [certifications, setCertifications] = useState<
    ProductionLotCertification[]
  >([]);
  const [loadingCerts, setLoadingCerts] = useState(false);
  const [attachDialogOpen, setAttachDialogOpen] = useState(false);

  const [activeTab, setActiveTab] = useState("info");

  // Danh sách yêu cầu kiểm nghiệm (chỉ tải khi mở tab với vai trò VT-02)
  const [inspectionRequests, setInspectionRequests] = useState<
    InspectionRequestListItem[]
  >([]);
  const [inspectionPageData, setInspectionPageData] =
    useState<PageResponse<InspectionRequestListItem> | null>(null);
  const [inspectionLoading, setInspectionLoading] = useState(false);
  const [inspectionError, setInspectionError] = useState<string | null>(null);
  const [inspectionStatus, setInspectionStatus] = useState<
    InspectionRequestStatusQuery | "ALL"
  >("ALL");
  const [inspectionPage, setInspectionPage] = useState(0);
  const [inspectionReloadKey, setInspectionReloadKey] = useState(0);

  // Kiểm tra điều kiện kích hoạt tem của lô (POST /production-lots/{id}/can-activate-seal)
  const [canActivateCheck, setCanActivateCheck] =
    useState<CanActivateSealCheck | null>(null);
  const [canActivateLoading, setCanActivateLoading] = useState(false);

  // Dialog tạo yêu cầu kiểm nghiệm
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [criteriaData, setCriteriaData] = useState<LotTestCriteriaResult | null>(
    null
  );
  const [criteriaLoading, setCriteriaLoading] = useState(false);
  const [criteriaError, setCriteriaError] = useState<string | null>(null);
  const [testingUnit, setTestingUnit] = useState("");
  // NCL-11-CN-006 Phase 1: chọn đơn vị kiểm nghiệm từ danh mục dùng chung
  const [testingUnitId, setTestingUnitId] = useState("");
  const [testingUnits, setTestingUnits] = useState<TestingUnit[]>([]);
  const [unitsLoading, setUnitsLoading] = useState(false);
  const [sampleSentDate, setSampleSentDate] = useState("");
  const [selectedCriteriaIds, setSelectedCriteriaIds] = useState<number[]>([]);
  const [touched, setTouched] = useState({
    unit: false,
    date: false,
    criteria: false,
  });
  const [submitting, setSubmitting] = useState(false);
  const [duplicateOpen, setDuplicateOpen] = useState(false);
  const [duplicateMessage, setDuplicateMessage] = useState("");

  // ── Breadcrumb điều hướng thống nhất (thay nút "Quay lại") ────────────────
  useSetBreadcrumb(
    lot
      ? [
          { label: "Dashboard", href: "/dashboard" },
          { label: "Lô sản xuất", href: "/production-lots" },
          { label: lot.name || "Chi tiết lô sản xuất" },
        ]
      : null,
  );



  const loadLot = async () => {
    if (!id) return;
    try {
      const data = await getProductionLotById(id);
      setLot(data);
    } catch (error) {
      toast.error("Không thể tải thông tin lô sản xuất");
    } finally {
      setLoading(false);
    }
  };

  const loadCertifications = async () => {
    if (!id) return;
    try {
      setLoadingCerts(true);
      const data = await getLotCertifications(id);
      setCertifications(data);
    } catch (error: any) {
      toast.error("Không thể tải danh sách chứng nhận");
    } finally {
      setLoadingCerts(false);
    }
  };

  const loadInspectionRequests = useCallback(
    async (status: InspectionRequestStatusQuery | "ALL", page: number) => {
      if (!id) return;
      try {
        setInspectionLoading(true);
        setInspectionError(null);
        const data = await getInspectionRequests({
          lotId: id,
          status: status === "ALL" ? undefined : status,
          page,
          size: 20,
        });
        setInspectionRequests(data.items);
        setInspectionPageData(data);
      } catch (error) {
        setInspectionRequests([]);
        setInspectionPageData(null);
        setInspectionError("Không thể tải danh sách yêu cầu kiểm nghiệm");
      } finally {
        setInspectionLoading(false);
      }
    },
    [id]
  );

  const loadCriteria = async () => {
    if (!id) return;
    try {
      setCriteriaLoading(true);
      setCriteriaError(null);
      const data = await getLotTestCriteria(id);
      setCriteriaData(data);
      setSelectedCriteriaIds([]);
    } catch (error) {
      setCriteriaData(null);
      setCriteriaError(
        getApiErrorMessage(error, "Không thể tải chỉ tiêu kiểm nghiệm")
      );
    } finally {
      setCriteriaLoading(false);
    }
  };

  const loadCanActivateCheck = useCallback(async () => {
    if (!id) return;
    try {
      setCanActivateLoading(true);
      const data = await checkCanActivateSeal(id);
      setCanActivateCheck(data);
    } catch {
      setCanActivateCheck(null);
    } finally {
      setCanActivateLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadLot();
  }, [id]);

  useEffect(() => {
    if (id) {
      loadCertifications();
    }
  }, [id]);

  useEffect(() => {
    if (activeTab === "inspection" && canInspect && id) {
      void loadInspectionRequests(inspectionStatus, inspectionPage);
      void loadCanActivateCheck();
    }
  }, [
    activeTab,
    canInspect,
    id,
    inspectionStatus,
    inspectionPage,
    inspectionReloadKey,
    loadInspectionRequests,
    loadCanActivateCheck,
  ]);

  // Tải danh mục đơn vị kiểm nghiệm khi mở dialog tạo yêu cầu kiểm nghiệm
  // (NCL-11-CN-006 Phase 1)
  useEffect(() => {
    if (!createDialogOpen) return;
    let cancelled = false;
    setUnitsLoading(true);
    getTestingUnits({ isActive: true })
      .then((res) => {
        if (!cancelled) setTestingUnits(res.items ?? []);
      })
      .catch(() => {
        if (!cancelled) setTestingUnits([]);
      })
      .finally(() => {
        if (!cancelled) setUnitsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [createDialogOpen]);

  // NCL-11-CN-006: chỉ hiển thị đơn vị còn hạn công nhận trong dropdown.
  // Hook phải nằm trên mọi early return (Rules of Hooks).
  const availableUnits = useMemo(() => {
    const t = toISODate(new Date());
    return testingUnits.filter(
      (u) => !u.accreditationExpiryDate || u.accreditationExpiryDate >= t
    );
  }, [testingUnits]);

  const canRecordHarvest =
    user?.roleCode === "VT-02" || user?.roleCode === "VT-03";

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20 text-muted-foreground">
        <LoaderCircle className="h-5 w-5 animate-spin mr-2 text-emerald-500" />
        Đang tải...
      </div>
    );
  }

  if (!lot) {
    return (
      <div className="text-center py-20 text-muted-foreground">
        Không tìm thấy lô sản xuất
      </div>
    );
  }

  const canCreateShipment =
    user?.roleCode === "VT-02" && lot.status === "PACKAGED";
  const canActivateShipment = user?.roleCode === "VT-02";
  const canRecallShipment = user?.roleCode === "VT-02";
  const canRecordPackaging =
    (user?.roleCode === "VT-02" || user?.roleCode === "VT-03") &&
    (lot.status === "HARVESTED" || lot.status === "PREPROCESSED");
  const canRecordPreprocessing =
    canCreatePreprocessing && lot.status === "HARVESTED";
  const canManageCert = user?.roleCode === "VT-02";

  const handleDetach = async (certificationId: string) => {
    if (!id) {
      toast.error("Không tìm thấy ID lô sản xuất");
      return;
    }
    if (!confirm("Bạn có chắc chắn muốn gỡ chứng nhận này?")) return;
    try {
      await detachCertification(id, certificationId);
      toast.success("Gỡ chứng nhận thành công");
      await loadCertifications();
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Không thể gỡ chứng nhận");
    }
  };

  const today = toISODate(new Date());
  const trimmedTestingUnit = testingUnit.trim();
  // NCL-11-CN-006 Phase 1: khi danh mục khả dụng thì bắt buộc chọn từ dropdown;
  // nếu danh mục rỗng hoặc tải lỗi thì fallback về nhập tự do.
  const useUnitCatalog = !unitsLoading && testingUnits.length > 0;
  const selectedTestingUnit =
    availableUnits.find((unit) => unit.id === testingUnitId) || null;
  const unitFieldValid = useUnitCatalog
    ? testingUnitId !== ""
    : trimmedTestingUnit !== "";
  const isSampleDateValid =
    sampleSentDate !== "" && sampleSentDate <= today;
  const canSubmitCreate =
    !submitting &&
    !criteriaLoading &&
    criteriaError === null &&
    criteriaData !== null &&
    criteriaData.criteria.length > 0 &&
    unitFieldValid &&
    isSampleDateValid &&
    selectedCriteriaIds.length > 0;

  const openCreateDialog = () => {
    if (!id) return;
    navigate(`/production-lots/${id}/inspection-requests/create`);
  };

  const handleCloseCreateDialog = () => {
    if (submitting) return;
    setCreateDialogOpen(false);
    setDuplicateOpen(false);
    setDuplicateMessage("");
  };

  const toggleCriterion = (criteriaId: number, checked: boolean) => {
    setTouched((prev) => ({ ...prev, criteria: true }));
    setSelectedCriteriaIds((prev) => {
      if (checked) {
        return prev.includes(criteriaId) ? prev : [...prev, criteriaId];
      }
      return prev.filter((id) => id !== criteriaId);
    });
  };

  const handleSelectAllCriteria = () => {
    if (!criteriaData) return;
    setTouched((prev) => ({ ...prev, criteria: true }));
    setSelectedCriteriaIds(
      criteriaData.criteria.map((criterion) => criterion.criteriaId)
    );
  };

  const handleClearAllCriteria = () => {
    setTouched((prev) => ({ ...prev, criteria: true }));
    setSelectedCriteriaIds([]);
  };

  const handleCreated = () => {
    setCreateDialogOpen(false);
    setDuplicateOpen(false);
    setDuplicateMessage("");
    setTestingUnit("");
    setTestingUnitId("");
    setSampleSentDate("");
    setSelectedCriteriaIds([]);
    setTouched({ unit: false, date: false, criteria: false });
    setInspectionPage(0);
    setInspectionReloadKey((key) => key + 1);
  };

  const buildUnitPayload = () => ({
    testingUnitId: useUnitCatalog ? testingUnitId : null,
    // Khi chọn từ danh mục, gửi tên snapshot của đơn vị để tương thích ngược
    testingUnit: selectedTestingUnit?.name || trimmedTestingUnit,
  });

  const submitCreate = async () => {
    if (!id || !canSubmitCreate) return;
    setSubmitting(true);
    try {
      await createInspectionRequest(id, {
        ...buildUnitPayload(),
        sampleSentDate,
        criteriaIds: selectedCriteriaIds,
        confirmDuplicate: false,
      });
      toast.success("Tạo yêu cầu kiểm nghiệm thành công");
      handleCreated();
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 409) {
        // Giữ nguyên dữ liệu form và mở hộp xác nhận tạo trùng
        setDuplicateMessage(
          getApiErrorMessage(
            error,
            "Yêu cầu kiểm nghiệm trùng lặp với yêu cầu đang chờ kết quả cho cùng bộ chỉ tiêu. Vui lòng xác nhận để tạo thêm yêu cầu."
          )
        );
        setDuplicateOpen(true);
      } else {
        toast.error(
          getApiErrorMessage(error, "Không thể tạo yêu cầu kiểm nghiệm")
        );
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreateFormSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void submitCreate();
  };

  const confirmDuplicateSubmit = async () => {
    if (!id || submitting) return;
    setSubmitting(true);
    try {
      await createInspectionRequest(id, {
        ...buildUnitPayload(),
        sampleSentDate,
        criteriaIds: selectedCriteriaIds,
        confirmDuplicate: true,
      });
      toast.success("Tạo yêu cầu kiểm nghiệm thành công");
      handleCreated();
    } catch (error) {
      // Không mở lại hộp xác nhận nếu lần gửi thứ hai tiếp tục lỗi
      setDuplicateOpen(false);
      toast.error(
        getApiErrorMessage(error, "Không thể tạo yêu cầu kiểm nghiệm")
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top action row */}
      <div className="flex items-center justify-between">
        <Button
          variant="outline"
          size="sm"
          onClick={() => navigate("/production-lots")}
          className="gap-1.5 text-slate-700"
        >
          <ChevronLeft className="h-4 w-4" />
          Quay lại danh sách lô sản xuất
        </Button>
        <HelpButton screenKey="production-lot-detail" />
      </div>

      {/* Thông tin chính */}
      <Card className="border-slate-200 bg-white shadow-sm rounded-xl">
        <CardHeader className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4">
          <div>
            <CardTitle className="text-xl font-bold text-slate-900">
              {lot.name}
            </CardTitle>
            <p className="text-sm text-muted-foreground mt-1">
              Mã lô: {maskId(lot.id)}
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {canRecordHarvest &&
              lot.status === "APPROVED" &&
              !showHarvestForm && (
                <Button
                  onClick={() => setShowHarvestForm(true)}
                  variant="create"
                >
                  <Sprout className="h-4 w-4 mr-1" />
                  Ghi nhận thu hoạch
                </Button>
              )}
            {canRecordPreprocessing && (
              <Button
                onClick={() =>
                  navigate(
                    `/preprocessing-events/create?productionLotId=${encodeURIComponent(lot.id)}`,
                  )
                }
                variant="create"
              >
                <Wheat className="h-4 w-4 mr-1" />
                Ghi sơ chế
              </Button>
            )}
            {canRecordPackaging && (
              <Button
                onClick={() =>
                  navigate(`/packaging-events/create?productionLotId=${lot.id}`)
                }
                variant="create"
              >
                <Package className="h-4 w-4 mr-1" />
                Ghi đóng gói
              </Button>
            )}
            {getStatusBadge(lot.status)}
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Ô thông tin */}
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Sản lượng dự kiến
              </span>
              <p className="mt-1 text-lg font-semibold text-emerald-800">
                {lot.expectedQuantity} {lot.expectedQuantityUnit}
              </p>
            </div>
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Sản lượng thực tế
              </span>
              <p className="mt-1 text-lg font-semibold text-emerald-800">
                {lot.actualQuantity ? `${lot.actualQuantity} kg` : "—"}
              </p>
            </div>
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Vùng trồng
              </span>
              <p className="mt-1 text-lg font-semibold text-emerald-800">
                {lot.farmAreaName || "—"}
              </p>
            </div>
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Nông sản
              </span>
              <p className="mt-1 text-lg font-semibold text-emerald-800">
                {lot.productCategoryName || "—"}
              </p>
            </div>
          </div>

          {/* Ngày quan trọng */}
          <div className="mt-6 grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="flex items-center gap-3 rounded-lg bg-emerald-50 p-3">
              <Sprout className="h-5 w-5 text-emerald-600" />
              <div>
                <p className="text-xs text-muted-foreground">Ngày trồng</p>
                <p className="font-medium">
                  {lot.plantingDate
                    ? new Date(lot.plantingDate).toLocaleDateString("vi-VN")
                    : "—"}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3 rounded-lg bg-amber-50 p-3">
              <Package className="h-5 w-5 text-amber-600" />
              <div>
                <p className="text-xs text-muted-foreground">Ngày thu hoạch</p>
                <p className="font-medium">
                  {lot.harvestDate
                    ? new Date(lot.harvestDate).toLocaleDateString("vi-VN")
                    : "—"}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3 rounded-lg bg-blue-50 p-3">
              <Package className="h-5 w-5 text-blue-600" />
              <div>
                <p className="text-xs text-muted-foreground">Người tạo</p>
                <p className="font-medium">{lot.createdByName || "—"}</p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Harvest Form (nếu bật) */}
      {showHarvestForm && (
        <HarvestForm
          productionLotId={lot.id}
          productionLotName={lot.name}
          onSuccess={() => {
            setShowHarvestForm(false);
            loadLot();
          }}
          onCancel={() => setShowHarvestForm(false)}
        />
      )}

      {/* Tabs chi tiết */}
      <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
        <TabsList className="bg-white/80 backdrop-blur-sm border border-emerald-100 p-1 rounded-xl gap-1 min-h-11 max-w-full overflow-x-auto overflow-y-hidden">
          <TabsTrigger
            value="info"
            className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white"
          >
            Thông tin chung
          </TabsTrigger>
          <TabsTrigger
            value="farmlogs"
            className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white"
          >
            Nhật ký canh tác
          </TabsTrigger>
          <TabsTrigger
            value="shipments"
            className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white"
          >
            Lô hàng & Mã QR
          </TabsTrigger>
          <TabsTrigger
            value="certifications"
            className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white"
          >
            Chứng nhận
          </TabsTrigger>
          {canInspect && (
            <TabsTrigger
              value="inspection"
              className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white"
            >
              Kiểm nghiệm
            </TabsTrigger>
          )}
        </TabsList>

        <TabsContent value="info" className="mt-4">
          <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
            <CardContent className="pt-6">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="rounded-lg bg-muted/50 p-4">
                  <span className="text-sm text-muted-foreground">ID</span>
                  <p className="font-mono text-sm">{maskId(lot.id)}</p>
                </div>
                <div className="rounded-lg bg-muted/50 p-4">
                  <span className="text-sm text-muted-foreground">Ngày tạo</span>
                  <p className="font-medium">
                    {new Date(lot.createdAt).toLocaleString("vi-VN")}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="farmlogs" className="mt-4">
          <FarmLogList
            productionLotId={lot.id}
            productionLotName={lot.name}
            canCreate={canCreateFarmLog}
          />
        </TabsContent>

        <TabsContent value="shipments" className="mt-4">
          <ShipmentList
            productionLotId={lot.id}
            productionLotStatus={lot.status}
            canCreate={canCreateShipment}
            canActivate={canActivateShipment}
            canRecall={canRecallShipment}
          />
        </TabsContent>

        <TabsContent value="certifications" className="mt-4">
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <h2 className="text-lg font-semibold text-emerald-800">
                Chứng nhận của lô
              </h2>
              {canManageCert && (
                <Button
                  onClick={() => setAttachDialogOpen(true)}
                  variant="create"
                >
                  <Plus className="h-4 w-4 mr-1" /> Gắn chứng nhận
                </Button>
              )}
            </div>
            <CertificationList
              certifications={certifications}
              onDetach={handleDetach}
              canManage={canManageCert}
              loading={loadingCerts}
            />
          </div>
        </TabsContent>

        {canInspect && (
          <TabsContent value="inspection" className="mt-4">
            <div className="space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                <h2 className="text-lg font-semibold text-emerald-800">
                  Yêu cầu kiểm nghiệm
                </h2>
                <Button
                  onClick={openCreateDialog}
                  variant="create"
                  disabled={submitting}
                >
                  <Plus className="h-4 w-4 mr-1" /> Tạo yêu cầu
                </Button>
              </div>

              <div
                className="flex flex-wrap gap-2"
                role="group"
                aria-label="Lọc theo trạng thái"
              >
                {INSPECTION_FILTER_OPTIONS.map((option) => (
                  <Button
                    key={option.value}
                    size="sm"
                    variant={
                      inspectionStatus === option.value ? "create" : "outline"
                    }
                    onClick={() => {
                      setInspectionStatus(option.value);
                      setInspectionPage(0);
                    }}
                  >
                    {option.label}
                  </Button>
                ))}
              </div>

              {inspectionLoading ? (
                <div
                  className="flex items-center justify-center gap-2 py-12 text-muted-foreground"
                  aria-live="polite"
                >
                  <LoaderCircle className="h-5 w-5 animate-spin text-emerald-500" />
                  Đang tải yêu cầu kiểm nghiệm...
                </div>
              ) : inspectionError ? (
                <div
                  className="flex flex-col items-center gap-3 py-12 text-muted-foreground"
                  aria-live="assertive"
                >
                  <p>{inspectionError}</p>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      void loadInspectionRequests(
                        inspectionStatus,
                        inspectionPage
                      )
                    }
                  >
                    Thử lại
                  </Button>
                </div>
              ) : inspectionRequests.length === 0 ? (
                <div className="text-center py-12 text-muted-foreground">
                  Chưa có yêu cầu kiểm nghiệm nào cho lô này.
                </div>
              ) : (
                <>
                  {/* Desktop: bảng */}
                  <div className="hidden md:block overflow-x-auto">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Mã yêu cầu</TableHead>
                          <TableHead>Đơn vị kiểm nghiệm</TableHead>
                          <TableHead>Ngày gửi mẫu</TableHead>
                          <TableHead>Số chỉ tiêu</TableHead>
                          <TableHead>Trạng thái</TableHead>
                          <TableHead>Thao tác</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {inspectionRequests.map((request) => (
                          <TableRow key={request.testRequestId}>
                            <TableCell>
                              <span
                                className="font-mono text-xs"
                                title={request.testRequestId}
                              >
                                #{request.testRequestId.slice(0, 8)}
                              </span>
                            </TableCell>
                            <TableCell>{request.testingUnit}</TableCell>
                            <TableCell>
                              {formatDateOnly(request.sampleSentDate)}
                            </TableCell>
                            <TableCell>
                              <div className="flex items-center gap-2">
                                <span>{request.criteriaCount}</span>
                                {request.failedCriteriaCount > 0 && (
                                  <Badge
                                    variant="outline"
                                    className="border-red-200 bg-red-50 text-red-700 text-xs font-semibold px-2 py-0.5"
                                    title={`${request.failedCriteriaCount}/${request.criteriaCount} chỉ tiêu không đạt (${request.failedRatio}%)`}
                                  >
                                    {request.failedCriteriaCount} không đạt ·{" "}
                                    {request.failedRatio}%
                                  </Badge>
                                )}
                              </div>
                            </TableCell>
                            <TableCell>
                              <div className="flex flex-wrap items-center gap-1.5">
                                {getInspectionStatusBadge(request.status)}
                                {request.status === "PASSED" &&
                                  !canActivateLoading &&
                                  canActivateCheck?.canActivate && (
                                    <Badge
                                      variant="outline"
                                      className="border-sky-200 bg-sky-50 text-sky-800 text-xs font-semibold px-2.5 py-0.5"
                                      title="Lô đạt kết quả kiểm nghiệm và còn hiệu lực"
                                    >
                                      Đủ điều kiện kích hoạt tem
                                    </Badge>
                                  )}
                                {request.status === "PASSED" &&
                                  !canActivateLoading &&
                                  canActivateCheck &&
                                  !canActivateCheck.canActivate && (
                                    <Badge
                                      variant="outline"
                                      className="border-red-200 bg-red-50 text-red-700 text-xs font-semibold px-2.5 py-0.5"
                                      title={canActivateCheck.reason ?? undefined}
                                    >
                                      Chưa đủ điều kiện kích hoạt tem
                                    </Badge>
                                  )}
                              </div>
                            </TableCell>
                            <TableCell>
                              {canInspect && (
                                <Button
                                  size="sm"
                                  variant={request.status === "PASSED" ? "outline" : "create"}
                                  className="text-xs font-semibold"
                                  onClick={() =>
                                    navigate(
                                      `/production-lots/${id}/inspection-requests/${request.testRequestId}/results`
                                    )
                                  }
                                >
                                  {request.status === "PASSED"
                                    ? "Xem / Sửa kết quả"
                                    : "Nhập kết quả"}
                                </Button>
                              )}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>

                  {/* Mobile: thẻ */}
                  <div className="md:hidden space-y-3">
                    {inspectionRequests.map((request) => (
                      <div
                        key={request.testRequestId}
                        className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm space-y-2"
                      >
                        <div className="flex items-center justify-between gap-2">
                          <span
                            className="font-mono text-xs text-muted-foreground"
                            title={request.testRequestId}
                          >
                            #{request.testRequestId.slice(0, 8)}
                          </span>
                          <div className="flex flex-wrap items-center gap-1.5">
                            {getInspectionStatusBadge(request.status)}
                          </div>
                        </div>
                        <p className="font-medium break-words">
                          {request.testingUnit}
                        </p>
                        <div className="flex items-center justify-between text-sm text-muted-foreground">
                          <span>
                            Ngày gửi mẫu:{" "}
                            {formatDateOnly(request.sampleSentDate)}
                          </span>
                          <span>
                            {request.criteriaCount} chỉ tiêu
                            {request.failedCriteriaCount > 0 &&
                              ` · ${request.failedCriteriaCount} không đạt (${request.failedRatio}%)`}
                          </span>
                        </div>
                        {request.status === "PASSED" &&
                          !canActivateLoading &&
                          canActivateCheck?.canActivate && (
                            <Badge
                              variant="outline"
                              className="border-sky-200 bg-sky-50 text-sky-800 text-xs font-semibold px-2.5 py-0.5"
                            >
                              Đủ điều kiện kích hoạt tem
                            </Badge>
                          )}
                        {request.status === "PASSED" &&
                          !canActivateLoading &&
                          canActivateCheck &&
                          !canActivateCheck.canActivate && (
                            <Badge
                              variant="outline"
                              className="border-red-200 bg-red-50 text-red-700 text-xs font-semibold px-2.5 py-0.5"
                              title={canActivateCheck.reason ?? undefined}
                            >
                              Chưa đủ điều kiện kích hoạt tem
                            </Badge>
                          )}
                        {canInspect && (
                          <div className="flex gap-2 pt-1">
                            <Button
                              size="sm"
                              variant={request.status === "PASSED" ? "outline" : "create"}
                              className="w-full text-xs font-semibold"
                              onClick={() =>
                                navigate(
                                  `/production-lots/${id}/inspection-requests/${request.testRequestId}/results`
                                )
                              }
                            >
                              {request.status === "PASSED"
                                ? "Xem / Sửa kết quả"
                                : "Nhập kết quả"}
                            </Button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>

                  {/* Phân trang */}
                  <div className="flex items-center justify-between pt-2">
                    <p className="text-sm text-muted-foreground">
                      Trang {inspectionPageData ? inspectionPageData.page + 1 : 1}{" "}
                      / {inspectionPageData?.totalPages ?? 1}
                    </p>
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={
                          !inspectionPageData || inspectionPageData.first
                        }
                        onClick={() =>
                          setInspectionPage((page) =>
                            Math.max(0, page - 1)
                          )
                        }
                      >
                        <ChevronLeft className="h-4 w-4 mr-1" /> Trước
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={
                          !inspectionPageData || inspectionPageData.last
                        }
                        onClick={() =>
                          setInspectionPage((page) => page + 1)
                        }
                      >
                        Sau <ChevronRight className="h-4 w-4 ml-1" />
                      </Button>
                    </div>
                  </div>
                </>
              )}
            </div>
          </TabsContent>
        )}
      </Tabs>

      <AttachCertificationDialog
        open={attachDialogOpen}
        onClose={() => setAttachDialogOpen(false)}
        lotId={id!}
        onSuccess={loadCertifications}
      />

      {/* Dialog tạo yêu cầu kiểm nghiệm */}
      <Dialog
        open={createDialogOpen}
        onOpenChange={(open) => {
          if (!open) handleCloseCreateDialog();
        }}
      >
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Tạo yêu cầu kiểm nghiệm</DialogTitle>
            <DialogDescription>
              Lập yêu cầu kiểm nghiệm cho lô “{lot.name}”.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleCreateFormSubmit} className="grid gap-4">
            <div className="space-y-5 py-2">
              {/* Chỉ tiêu kiểm nghiệm */}
              <div className="space-y-2">
                <div className="flex items-center justify-between gap-2">
                  <Label className="font-medium">
                    Chỉ tiêu kiểm nghiệm *
                  </Label>
                  {criteriaData?.standardName && (
                    <span className="text-xs text-muted-foreground">
                      Tiêu chuẩn: {criteriaData.standardName}
                    </span>
                  )}
                </div>

                {criteriaLoading ? (
                  <div
                    className="flex items-center gap-2 rounded-lg border border-emerald-100 bg-muted/30 p-4 text-sm text-muted-foreground"
                    aria-live="polite"
                  >
                    <LoaderCircle className="h-4 w-4 animate-spin text-emerald-500" />
                    Đang tải chỉ tiêu kiểm nghiệm...
                  </div>
                ) : criteriaError ? (
                  <div
                    className="space-y-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700"
                    aria-live="assertive"
                  >
                    <p>{criteriaError}</p>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => void loadCriteria()}
                    >
                      Thử lại
                    </Button>
                  </div>
                ) : criteriaData && criteriaData.criteria.length === 0 ? (
                  <div
                    className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800"
                    aria-live="polite"
                  >
                    {criteriaData.standardName
                      ? "Tiêu chuẩn hiện tại chưa có chỉ tiêu kiểm nghiệm nào. Không thể tạo yêu cầu."
                      : "Lô chưa được gắn tiêu chuẩn nên chưa có chỉ tiêu kiểm nghiệm nào. Không thể tạo yêu cầu."}
                  </div>
                ) : criteriaData ? (
                  <>
                    <div className="flex items-center justify-end gap-2">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={handleSelectAllCriteria}
                        disabled={submitting}
                      >
                        Chọn tất cả
                      </Button>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={handleClearAllCriteria}
                        disabled={submitting}
                      >
                        Bỏ chọn tất cả
                      </Button>
                    </div>
                    <div className="max-h-64 space-y-2 overflow-y-auto pr-1">
                      {criteriaData.criteria.map((criterion) => (
                        <div
                          key={criterion.criteriaId}
                          className="flex items-start gap-3 rounded-lg border border-emerald-100 p-3"
                        >
                          <Checkbox
                            id={`test-criterion-${criterion.criteriaId}`}
                            checked={selectedCriteriaIds.includes(
                              criterion.criteriaId
                            )}
                            onCheckedChange={(checked) =>
                              toggleCriterion(
                                criterion.criteriaId,
                                checked === true
                              )
                            }
                            disabled={submitting}
                            className="mt-0.5"
                          />
                          <Label
                            htmlFor={`test-criterion-${criterion.criteriaId}`}
                            className="cursor-pointer font-normal leading-snug"
                          >
                            <span className="font-medium">{criterion.name}</span>
                            <span className="block text-xs text-muted-foreground">
                              {criterion.code}
                            </span>
                          </Label>
                        </div>
                      ))}
                    </div>
                    {selectedCriteriaIds.length === 0 && (
                      <p className="text-sm text-red-500" role="alert">
                        Vui lòng chọn ít nhất một chỉ tiêu kiểm nghiệm
                      </p>
                    )}
                  </>
                ) : null}
              </div>

              {/* Đơn vị kiểm nghiệm - danh mục dùng chung NCL-11-CN-006 */}
              <div className="space-y-2">
                <Label htmlFor="testingUnit">Đơn vị kiểm nghiệm *</Label>

                {useUnitCatalog ? (
                  <>
                    <TestingUnitSelect
                      id="testingUnit"
                      units={availableUnits}
                      value={testingUnitId}
                      onChange={(unit) => {
                        setTestingUnitId(unit?.id || "");
                        setTouched((prev) => ({ ...prev, unit: true }));
                      }}
                      invalid={touched.unit && !unitFieldValid}
                      disabled={submitting}
                    />
                    {selectedTestingUnit?.contactInfo && (
                      <p className="text-xs text-muted-foreground">
                        Liên hệ: {selectedTestingUnit.contactInfo}
                      </p>
                    )}
                  </>
                ) : (
                  <>
                    {unitsLoading ? (
                      <div className="flex h-10 items-center gap-2 rounded-lg border border-input bg-white px-3 text-sm text-muted-foreground">
                        <LoaderCircle className="h-4 w-4 animate-spin" />
                        Đang tải danh mục đơn vị kiểm nghiệm...
                      </div>
                    ) : (
                      <>
                        <p className="text-xs font-medium text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-2.5 py-1.5">
                          Danh mục đơn vị kiểm nghiệm chưa có dữ liệu. Vui lòng liên
                          hệ Quản trị viên, hoặc nhập tên đơn vị tạm thời.
                        </p>
                        <Input
                          id="testingUnit"
                          value={testingUnit}
                          onChange={(event) => {
                            setTestingUnit(event.target.value);
                            setTouched((prev) => ({ ...prev, unit: true }));
                          }}
                          placeholder="VD: Phòng thí nghiệm Trung tâm..."
                          maxLength={200}
                          disabled={submitting}
                        />
                      </>
                    )}
                  </>
                )}

                {touched.unit && !unitFieldValid && (
                  <p className="text-sm text-red-500" role="alert">
                    {useUnitCatalog
                      ? "Vui lòng chọn đơn vị kiểm nghiệm từ danh mục"
                      : "Vui lòng nhập đơn vị kiểm nghiệm"}
                  </p>
                )}
              </div>

              {/* Ngày gửi mẫu */}
              <div className="space-y-2">
                <Label htmlFor="sampleSentDate">Ngày gửi mẫu *</Label>
                <Input
                  id="sampleSentDate"
                  type="date"
                  value={sampleSentDate}
                  max={today}
                  onChange={(event) => {
                    setSampleSentDate(event.target.value);
                    setTouched((prev) => ({ ...prev, date: true }));
                  }}
                  disabled={submitting}
                />
                {touched.date && sampleSentDate === "" && (
                  <p className="text-sm text-red-500" role="alert">
                    Vui lòng chọn ngày gửi mẫu
                  </p>
                )}
                {sampleSentDate !== "" && sampleSentDate > today && (
                  <p className="text-sm text-red-500" role="alert">
                    Ngày gửi mẫu không được lớn hơn ngày hiện tại
                  </p>
                )}
              </div>
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={handleCloseCreateDialog}
                disabled={submitting}
              >
                Hủy
              </Button>
              <Button type="submit" variant="create" disabled={!canSubmitCreate}>
                {submitting ? "Đang tạo..." : "Tạo yêu cầu"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Xác nhận tạo yêu cầu trùng */}
      <AlertDialog
        open={duplicateOpen}
        onOpenChange={(open) => {
          if (!open && !submitting) setDuplicateOpen(false);
        }}
      >
        <AlertDialogPopup>
          <AlertDialogHeader>
            <AlertDialogTitle>
              Yêu cầu kiểm nghiệm trùng lặp
            </AlertDialogTitle>
            <AlertDialogDescription>
              {duplicateMessage ||
                "Yêu cầu kiểm nghiệm trùng lặp với yêu cầu đang chờ kết quả cho cùng bộ chỉ tiêu. Vui lòng xác nhận để tạo thêm yêu cầu."}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <Button
              variant="outline"
              onClick={() => setDuplicateOpen(false)}
              disabled={submitting}
            >
              Quay lại
            </Button>
            <Button
              variant="create"
              onClick={() => void confirmDuplicateSubmit()}
              disabled={submitting}
            >
              {submitting ? "Đang tạo..." : "Vẫn tạo yêu cầu"}
            </Button>
          </AlertDialogFooter>
        </AlertDialogPopup>
      </AlertDialog>


    </div>
  );
};
