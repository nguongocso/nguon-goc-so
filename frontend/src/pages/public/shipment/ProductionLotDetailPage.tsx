import { useCallback, useEffect, useMemo, useState } from "react";
import { useParams, useNavigate, useSearchParams } from "react-router-dom";
import axios from "axios";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  AlertTriangle,
  Ban,
  ChevronLeft,
  ChevronRight,
  CheckCircle2,
  Eye,
  LoaderCircle,
  Package,
  Plus,
  Search,
  Sprout,
  Wheat,
  X,
} from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { cancelProductionLot, getProductionLotById } from "@/api/productionLotApi";
import { ShipmentList } from "@/pages/public/shipment/ShipmentList";
import { FarmLogList } from "@/components/farm-log/FarmLogList";
import { usePermission } from "@/hooks/usePermission";
import { ROLE_ACCESS } from "@/config/roleAccess";
import { HarvestForm } from "@/components/trace-event/HarvestForm";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import { HelpButton } from "@/components/help/HelpButton";
import type {
  CancelProductionLotRequest,
  ProductionLot,
} from "@/types/productionLot";
import {
  CANCELLABLE_PRODUCTION_LOT_STATUSES,
  CancelProductionLotDialog,
} from "@/components/production-lot/CancelProductionLotDialog";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { maskId } from "@/lib/utils";
import type {
  CanActivateSealCheck,
  InspectionCriterionResult,
  InspectionRequestListItem,
  InspectionRequestStatusDisplay,
  InspectionRequestStatusQuery,
  TestCriterionItem,
  ProductionLotCertification,
} from "@/types/certification";
import type { InspectionCriterion } from "@/types/inspectionCriterion";
import type { ProductCategory } from "@/types/productCategory";
import {
  checkCanActivateSeal,
  detachCertification,
  getInspectionRequestDetail,
  getInspectionRequestResults,
  getInspectionRequests,
  getLotCertifications,
  getLotTestCriteria,
} from "@/api/certificationApi";
import { getProductCategories } from "@/api/productCategoryApi";
import { getProductCategoryCriteria } from "@/api/inspectionCriterionApi";
import { CertificationList } from "@/components/certification/CertificationList";
import { AttachCertificationDialog } from "@/components/certification/AttachCertificationDialog";
import { InspectionRequestHistoryModal } from "@/components/certification/InspectionRequestHistoryModal";
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
  CANCELLED: {
    label: "Đã hủy",
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

// Số lượng yêu cầu tải tối đa một lần để tổng hợp kết quả theo chỉ tiêu
const INSIGHT_REQUEST_PAGE_SIZE = 50;

// Số yêu cầu hiển thị gọn trong cột lịch sử trước khi cần "Xem tất cả yêu cầu"
const HISTORY_COLLAPSED_COUNT = 3;

// Số dòng mỗi trang của bảng chỉ tiêu trong card "Điều kiện kích hoạt tem"
const CRITERIA_PAGE_SIZE = 10;

// ── Trạng thái tổng hợp của một chỉ tiêu trên tab Kiểm nghiệm ────────────────
type CriterionRowStatus =
  "VALID" | "EXPIRED" | "FAILED" | "WAITING" | "NOT_TESTED";

// Bộ lọc cột "Kết quả" của bảng chỉ tiêu (dựa trên kết quả mới nhất của chỉ tiêu)
type CriterionResultFilter = "ALL" | "PASSED" | "FAILED" | "NOT_TESTED";

// Nhãn tiếng Việt hiển thị cho từng giá trị bộ lọc "Kết quả"
const CRITERION_RESULT_FILTER_LABELS: Record<CriterionResultFilter, string> = {
  ALL: "Tất cả kết quả",
  PASSED: "Đạt",
  FAILED: "Không đạt",
  NOT_TESTED: "Chưa có kết quả",
};

// Nhãn tiếng Việt hiển thị cho bộ lọc "Trạng thái"
const getCriterionStatusFilterLabel = (
  status: CriterionRowStatus | "ALL",
): string =>
  status === "ALL"
    ? "Tất cả trạng thái"
    : CRITERION_ROW_STATUS_META[status].label;

const CRITERION_ROW_STATUS_META: Record<
  CriterionRowStatus,
  { label: string; className: string }
> = {
  VALID: {
    label: "Đạt",
    className: "border-emerald-200 bg-emerald-50 text-emerald-700",
  },
  EXPIRED: {
    label: "Hết hiệu lực",
    className: "border-amber-200 bg-amber-50 text-amber-800",
  },
  FAILED: {
    label: "Không đạt",
    className: "border-red-200 bg-red-50 text-red-700",
  },
  WAITING: {
    label: "Chờ kết quả",
    className: "border-yellow-200 bg-yellow-50 text-yellow-800",
  },
  NOT_TESTED: {
    label: "Chưa kiểm nghiệm",
    className: "border-amber-200 bg-amber-50 text-amber-700",
  },
};

const getCriterionRowStatusBadge = (status: CriterionRowStatus) => {
  const config = CRITERION_ROW_STATUS_META[status];
  return (
    <Badge
      variant="outline"
      className={`${config.className} border text-xs font-semibold px-2.5 py-0.5`}
    >
      {config.label}
    </Badge>
  );
};

/**
 * Một dòng chỉ tiêu trong bảng "Chỉ tiêu kiểm nghiệm" của tab Kiểm nghiệm:
 * metadata ngưỡng/đơn vị + kết quả mới nhất theo dữ liệu kiểm nghiệm thực tế.
 */
interface CriterionRowViewModel {
  criterion: TestCriterionItem;
  meta: InspectionCriterion | null;
  result: InspectionCriterionResult | null;
  /** Yêu cầu chứa kết quả này, hoặc yêu cầu đang chờ kết quả của chỉ tiêu. */
  relatedRequestId: string | null;
  status: CriterionRowStatus;
}

// Định dạng ngưỡng tối đa, bỏ số 0 và dấu thập phân thừa (VD: 0.02 thay 0.020)
const formatThreshold = (value: number) => String(Number(value.toFixed(3)));

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
  const canCancelLot = usePermission(ROLE_ACCESS.productionLotCancel);
  const [lot, setLot] = useState<ProductionLot | null>(null);
  const [loading, setLoading] = useState(true);
  const [showHarvestForm, setShowHarvestForm] = useState(false);
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false);
  const [certifications, setCertifications] = useState<
    ProductionLotCertification[]
  >([]);
  const [loadingCerts, setLoadingCerts] = useState(false);
  const [attachDialogOpen, setAttachDialogOpen] = useState(false);
  // Modal mở rộng "Lịch sử yêu cầu kiểm nghiệm" (bảng hoàn chỉnh + phân trang)
  const [showInspectionHistoryModal, setShowInspectionHistoryModal] =
    useState(false);

  const [searchParams] = useSearchParams();
  const initialTab = searchParams.get("tab") || "info";
  const [activeTab, setActiveTab] = useState(initialTab);

  useEffect(() => {
    const tabParam = searchParams.get("tab");
    if (tabParam) {
      setActiveTab(tabParam);
    }
  }, [searchParams]);

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
  // Mở rộng danh sách yêu cầu trong cột lịch sử (bấm "Xem tất cả yêu cầu")
  const [historyExpanded, setHistoryExpanded] = useState(false);
  // Trang hiện tại của bảng chỉ tiêu trong card "Điều kiện kích hoạt tem"
  const [criteriaPage, setCriteriaPage] = useState(0);
  // Tìm kiếm & bộ lọc bảng chỉ tiêu trong card "Điều kiện kích hoạt tem"
  const [criteriaSearch, setCriteriaSearch] = useState("");
  const [criteriaStatusFilter, setCriteriaStatusFilter] = useState<
    CriterionRowStatus | "ALL"
  >("ALL");
  const [criteriaResultFilter, setCriteriaResultFilter] =
    useState<CriterionResultFilter>("ALL");

  // Kiểm tra điều kiện kích hoạt tem của lô (POST /production-lots/{id}/can-activate-seal)
  const [canActivateCheck, setCanActivateCheck] =
    useState<CanActivateSealCheck | null>(null);
  const [canActivateLoading, setCanActivateLoading] = useState(false);

  // ── Dữ liệu tổng hợp cho tab Kiểm nghiệm (chỉ tải với vai trò VT-02) ────────
  // Bộ chỉ tiêu áp dụng cho lô (GET /production-lots/{id}/test-criteria)
  const [lotCriteria, setLotCriteria] = useState<TestCriterionItem[]>([]);
  const [lotStandardName, setLotStandardName] = useState<string | null>(null);
  // Metadata ngưỡng tối đa/đơn vị theo id chỉ tiêu của loại nông sản
  const [criteriaMetaById, setCriteriaMetaById] = useState<
    Record<number, InspectionCriterion>
  >({});
  // Kết quả mới nhất theo mã chỉ tiêu (tổng hợp từ các yêu cầu đã có kết quả)
  const [latestResultByCode, setLatestResultByCode] = useState<
    Record<string, InspectionCriterionResult>
  >({});
  // Yêu cầu kiểm nghiệm chứa kết quả mới nhất của từng chỉ tiêu
  const [resultSourceByCode, setResultSourceByCode] = useState<
    Record<string, string>
  >({});
  // Yêu cầu đang chờ kết quả theo mã chỉ tiêu (chưa có kết quả nào)
  const [pendingRequestByCode, setPendingRequestByCode] = useState<
    Record<string, string>
  >({});
  const [insightLoading, setInsightLoading] = useState(false);
  const [insightError, setInsightError] = useState<string | null>(null);
  // Thông tin loại nông sản (chính sách bắt buộc kiểm nghiệm)
  const [productCategoryInfo, setProductCategoryInfo] =
    useState<ProductCategory | null>(null);

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

  // NCL-02-CN-006: hủy lô sản xuất kèm lý do + diễn giải
  const handleCancelProductionLot = async (
    _id: string,
    payload: CancelProductionLotRequest,
  ) => {
    if (!lot) return;
    try {
      await cancelProductionLot(lot.id, payload);
      toast.success("Đã hủy lô sản xuất.");
      setCancelDialogOpen(false);
      await loadLot();
    } catch (error: any) {
      const message =
        error.response?.data?.message || "Không thể hủy lô sản xuất.";
      toast.error(message);
      throw error;
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
    [id],
  );

  /**
   * Tổng hợp dữ liệu tab Kiểm nghiệm:
   * - Bộ chỉ tiêu áp dụng cho lô + metadata ngưỡng/đơn vị theo loại nông sản.
   * - Kết quả kiểm nghiệm mới nhất theo từng chỉ tiêu (từ các yêu cầu đã có kết quả)
   *   và các chỉ tiêu đang chờ kết quả (thuộc yêu cầu PENDING).
   */
  const loadInspectionInsights = useCallback(async () => {
    if (!id) return;
    try {
      setInsightLoading(true);
      setInsightError(null);
      const data = await getLotTestCriteria(id);
      setLotCriteria(data.criteria);
      setLotStandardName(data.standardName ?? null);

      // Metadata ngưỡng/đơn vị + chính sách bắt buộc kiểm nghiệm theo loại nông sản
      const categoryId = lot?.productCategoryId ?? null;
      if (categoryId) {
        try {
          const categoryCriteria = await getProductCategoryCriteria(categoryId);
          const metaMap: Record<number, InspectionCriterion> = {};
          for (const item of categoryCriteria) metaMap[item.id] = item;
          setCriteriaMetaById(metaMap);
        } catch {
          // Ngưỡng/đơn vị là hiển thị phụ — không chặn toàn bộ tổng hợp
          setCriteriaMetaById({});
        }
        try {
          const categories = await getProductCategories();
          setProductCategoryInfo(
            categories.find((category) => category.id === categoryId) ?? null,
          );
        } catch {
          setProductCategoryInfo(null);
        }
      } else {
        setCriteriaMetaById({});
        setProductCategoryInfo(null);
      }

      // Toàn bộ yêu cầu kiểm nghiệm của lô để tổng hợp kết quả/chờ kết quả
      const requestsPage = await getInspectionRequests({
        lotId: id,
        page: 0,
        size: INSIGHT_REQUEST_PAGE_SIZE,
      });

      const summaries = await Promise.all(
        requestsPage.items.map(async (request) => {
          try {
            if (request.status === "PASSED" || request.status === "FAILED") {
              return {
                requestId: request.testRequestId,
                results: await getInspectionRequestResults(
                  request.testRequestId,
                ),
                pendingCodes: [] as string[],
              };
            }
            if (request.status === "PENDING") {
              const detail = await getInspectionRequestDetail(
                request.testRequestId,
              );
              return {
                requestId: request.testRequestId,
                results: detail.criteria.flatMap((item) =>
                  item.result ? [item.result] : [],
                ),
                pendingCodes: detail.criteria.map((item) => item.code),
              };
            }
          } catch {
            // Bỏ qua yêu cầu lỗi, không chặn toàn bộ tổng hợp
          }
          return {
            requestId: request.testRequestId,
            results: [] as InspectionCriterionResult[],
            pendingCodes: [] as string[],
          };
        }),
      );

      // Kết quả mới nhất theo mã chỉ tiêu (ưu tiên resultDate/updatedAt lớn nhất)
      const latest: Record<string, InspectionCriterionResult> = {};
      const source: Record<string, string> = {};
      for (const summary of summaries) {
        for (const result of summary.results) {
          const current = latest[result.criterionCode];
          const isNewer =
            !current ||
            (result.resultDate ?? "") > (current.resultDate ?? "") ||
            ((result.resultDate ?? "") === (current.resultDate ?? "") &&
              result.updatedAt > current.updatedAt);
          if (isNewer) {
            latest[result.criterionCode] = result;
            source[result.criterionCode] = summary.requestId;
          }
        }
      }

      // Chỉ tiêu đang chờ kết quả mà chưa có bất kỳ kết quả nào
      const waitingByCode: Record<string, string> = {};
      for (const summary of summaries) {
        for (const code of summary.pendingCodes) {
          if (!latest[code]) waitingByCode[code] = summary.requestId;
        }
      }

      setLatestResultByCode(latest);
      setResultSourceByCode(source);
      setPendingRequestByCode(waitingByCode);
    } catch (error) {
      setLatestResultByCode({});
      setResultSourceByCode({});
      setPendingRequestByCode({});
      setLotCriteria([]);
      setLotStandardName(null);
      setInsightError(
        getApiErrorMessage(error, "Không thể tải dữ liệu kiểm nghiệm của lô"),
      );
    } finally {
      setInsightLoading(false);
    }
  }, [id, lot?.productCategoryId]);

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
      void loadInspectionInsights();
    }
  }, [
    activeTab,
    canInspect,
    id,
    inspectionStatus,
    inspectionPage,
    loadInspectionRequests,
    loadCanActivateCheck,
    loadInspectionInsights,
  ]);

  // Đặt lại về trang đầu khi thay đổi từ khóa / bộ lọc bảng chỉ tiêu
  useEffect(() => {
    setCriteriaPage(0);
  }, [criteriaSearch, criteriaStatusFilter, criteriaResultFilter]);

  const today = toISODate(new Date());

  // Tổng hợp trạng thái từng chỉ tiêu từ dữ liệu kiểm nghiệm thực tế
  const criterionRows = useMemo<CriterionRowViewModel[]>(() => {
    if (!lot) return [];
    return lotCriteria.map((criterion) => {
      const result = latestResultByCode[criterion.code] ?? null;
      const pendingRequestId = pendingRequestByCode[criterion.code] ?? null;
      const status: CriterionRowStatus = result
        ? !result.passed
          ? "FAILED"
          : result.expiryDate && result.expiryDate >= today
            ? "VALID"
            : "EXPIRED"
        : pendingRequestId
          ? "WAITING"
          : "NOT_TESTED";
      return {
        criterion,
        meta: criteriaMetaById[criterion.criteriaId] ?? null,
        result,
        relatedRequestId:
          resultSourceByCode[criterion.code] ?? pendingRequestId,
        status,
      };
    });
  }, [
    lot,
    lotCriteria,
    latestResultByCode,
    pendingRequestByCode,
    resultSourceByCode,
    criteriaMetaById,
    today,
  ]);

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

  const openCreateDialog = () => {
    if (!id) return;
    navigate(`/production-lots/${id}/inspection-requests/create`);
  };

  // Chính sách kiểm nghiệm của loại nông sản (NCL-09-CN-009)
  const mandatoryInspection = productCategoryInfo?.requiresInspection ?? false;

  // Nhóm chỉ tiêu chưa thỏa để cảnh báo cụ thể trên banner
  const failedRows = criterionRows.filter((row) => row.status === "FAILED");
  const expiredRows = criterionRows.filter((row) => row.status === "EXPIRED");
  const waitingRows = criterionRows.filter((row) => row.status === "WAITING");
  const notTestedRows = criterionRows.filter(
    (row) => row.status === "NOT_TESTED",
  );

  /**
   * Điều kiện kích hoạt tem (mirror logic backend
   * POST /production-lots/{lotId}/can-activate-seal):
   * - Loại nông sản KHÔNG bắt buộc kiểm nghiệm → luôn đủ điều kiện.
   * - Loại nông sản CÓ bắt buộc kiểm nghiệm → TOÀN BỘ chỉ tiêu được
   *   gán cho loại nông sản phải có kết quả mới nhất ĐẠT và CÒN HIỆU
   *   LỰC (không còn FAILED / EXPIRED / WAITING / NOT_TESTED).
   */
  const hasCompleteValidResults =
    criterionRows.length > 0 &&
    failedRows.length === 0 &&
    expiredRows.length === 0 &&
    waitingRows.length === 0 &&
    notTestedRows.length === 0;

  const sealEligible = !mandatoryInspection ? true : hasCompleteValidResults;

  /*
   * "Kết quả tổng thể" xét trên TỔNG SỐ CHỈ TIÊU ĐƯỢC GÁN CHO LÔ
   * (GET /production-lots/{id}/test-criteria → lotCriteria), không phải
   * tổng số chỉ tiêu của các yêu cầu kiểm nghiệm đã tạo — vì một chỉ tiêu
   * có thể xuất hiện trong nhiều yêu cầu (kiểm thử lại) và sẽ bị đếm trùng
   * nếu cộng dồn theo yêu cầu như canActivateCheck của backend.
   */
  const totalCriteriaCount = criterionRows.length;
  const passedCriteriaCount = criterionRows.filter(
    (row) => row.status === "VALID",
  ).length;

  // Phần trăm chỉ tiêu đạt cho thanh tiến trình "Kết quả tổng thể"
  const progressPercent =
    totalCriteriaCount > 0
      ? Math.round((passedCriteriaCount / totalCriteriaCount) * 100)
      : 0;

  // Lọc bảng chỉ tiêu theo từ khóa + kết quả + trạng thái
  const filteredCriterionRows = criterionRows.filter((row) => {
    if (criteriaStatusFilter !== "ALL" && row.status !== criteriaStatusFilter) {
      return false;
    }
    if (
      criteriaResultFilter === "PASSED" &&
      !(row.result && row.result.passed)
    ) {
      return false;
    }
    if (
      criteriaResultFilter === "FAILED" &&
      !(row.result && !row.result.passed)
    ) {
      return false;
    }
    if (criteriaResultFilter === "NOT_TESTED" && row.result) {
      return false;
    }
    const q = criteriaSearch.trim().toLowerCase();
    if (q) {
      const hay =
        `${row.criterion.name} ${row.meta?.referenceStandard ?? ""}`.toLowerCase();
      if (!hay.includes(q)) {
        return false;
      }
    }
    return true;
  });

  // Phân trang bảng chỉ tiêu (client-side): mỗi trang tối đa CRITERIA_PAGE_SIZE dòng.
  // Khi tổng số dòng <= kích thước trang, thanh phân trang vẫn hiển thị "Trang 1 / 1"
  // với 2 nút Trước/Sau ở trạng thái disabled.
  const totalCriteriaPages = Math.max(
    1,
    Math.ceil(filteredCriterionRows.length / CRITERIA_PAGE_SIZE),
  );
  // Chống lỗi khi dữ liệu tải lại làm giảm số trang (ví dụ chuyển lô khác)
  const safeCriteriaPage = Math.min(criteriaPage, totalCriteriaPages - 1);
  const pagedCriterionRows = filteredCriterionRows.slice(
    safeCriteriaPage * CRITERIA_PAGE_SIZE,
    (safeCriteriaPage + 1) * CRITERIA_PAGE_SIZE,
  );
  // Khoảng dòng hiển thị của trang hiện tại (dùng cho dòng mô tả phân trang
  // theo cùng định dạng với InspectionRequestHistoryModal)
  const criteriaRangeStart =
    filteredCriterionRows.length === 0
      ? 0
      : safeCriteriaPage * CRITERIA_PAGE_SIZE + 1;
  const criteriaRangeEnd = Math.min(
    (safeCriteriaPage + 1) * CRITERIA_PAGE_SIZE,
    filteredCriterionRows.length,
  );

  // Lịch sử yêu cầu: hiển thị gọn HISTORY_COLLAPSED_COUNT yêu cầu,
  // bấm "Xem tất cả yêu cầu" để mở rộng toàn bộ danh sách của trang hiện tại
  const visibleHistory = historyExpanded
    ? inspectionRequests
    : inspectionRequests.slice(0, HISTORY_COLLAPSED_COUNT);
  const canToggleHistory = inspectionRequests.length > HISTORY_COLLAPSED_COUNT;

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
            {canCancelLot &&
              CANCELLABLE_PRODUCTION_LOT_STATUSES.includes(lot.status) && (
                <Button
                  onClick={() => setCancelDialogOpen(true)}
                  variant="delete"
                >
                  <Ban className="h-4 w-4 mr-1" />
                  Hủy lô
                </Button>
              )}
            {getStatusBadge(lot.status)}
          </div>
        </CardHeader>
        <CardContent>
          {lot.status === "CANCELLED" && (
            <div className="mb-4 rounded-xl border border-red-200 bg-red-50 p-4">
              <div className="flex items-center gap-2">
                <Ban className="h-5 w-5 text-red-600" />
                <span className="font-semibold text-red-800">
                  Lô sản xuất đã bị hủy
                </span>
              </div>
              <div className="mt-2 grid grid-cols-1 sm:grid-cols-2 gap-2 text-sm text-red-800">
                <p>
                  <span className="font-medium">Lý do hủy:</span>{" "}
                  {lot.cancellationReason ?? "—"}
                </p>
                <p>
                  <span className="font-medium">Người hủy:</span>{" "}
                  {lot.cancelledByName ?? "—"}
                </p>
                <p className="sm:col-span-2">
                  <span className="font-medium">Diễn giải:</span>{" "}
                  {lot.cancellationNote ?? "—"}
                </p>
                {lot.cancelledAt && (
                  <p className="sm:col-span-2">
                    <span className="font-medium">Thời gian hủy:</span>{" "}
                    {new Date(lot.cancelledAt).toLocaleString("vi-VN")}
                  </p>
                )}
              </div>
            </div>
          )}
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
                  <span className="text-sm text-muted-foreground">
                    Ngày tạo
                  </span>
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
            canCreate={canCreateFarmLog && lot.status !== "CANCELLED"}
            enableCorrection={lot.status !== "CANCELLED"}
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
            <div className="grid grid-cols-1 items-start gap-4 lg:grid-cols-3">
              {/* ── Cột trái: Điều kiện kích hoạt tem + bảng chỉ tiêu ────── */}
              <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm lg:col-span-2">
                <CardHeader className="pb-2">
                  <CardTitle className="text-lg font-semibold">
                    Điều kiện kích hoạt tem
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  {/* Hàng tóm tắt: mô tả + thông tin loại nông sản + tổng thể */}
                  <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
                    <p className="text-sm leading-relaxed text-muted-foreground">
                      {mandatoryInspection ? (
                        <>
                          Lô có loại nông sản{" "}
                          <span className="font-semibold text-foreground">
                            bắt buộc kiểm nghiệm
                          </span>
                          . Phải{" "}
                          <span className="font-semibold text-foreground">
                            đạt tất cả chỉ tiêu
                          </span>{" "}
                          mới đủ điều kiện kích hoạt tem.
                        </>
                      ) : (
                        <>
                          Lô có loại nông sản{" "}
                          <span className="font-semibold text-foreground">
                            không bắt buộc kiểm nghiệm
                          </span>
                          . Kết quả chỉ tiêu dùng để theo dõi chất lượng lô.
                        </>
                      )}
                    </p>
                    <div className="space-y-1.5 rounded-lg border border-sky-100 bg-sky-50 p-3 text-sm">
                      <p>
                        <span className="text-muted-foreground">
                          Loại nông sản:{" "}
                        </span>
                        <span className="font-medium">
                          {lot.productCategoryName || "—"}
                        </span>
                      </p>
                      <p>
                        <span className="text-muted-foreground">
                          Chính sách:{" "}
                        </span>
                        <span className="font-medium text-red-500">
                          {mandatoryInspection
                            ? "Bắt buộc kiểm nghiệm"
                            : "Không bắt buộc"}
                        </span>
                      </p>
                      {lotStandardName && (
                        <p>
                          <span className="text-muted-foreground">
                            Tiêu chuẩn:{" "}
                          </span>
                          <span className="font-medium">{lotStandardName}</span>
                        </p>
                      )}
                    </div>
                    <div className="rounded-lg border border-gray-200 bg-white p-3">
                      <p className="text-xs text-muted-foreground">
                        Kết quả tổng thể
                      </p>
                      <p className="mt-0.5 text-sm font-semibold">
                        {insightLoading
                          ? "Đang tải..."
                          : `${passedCriteriaCount}/${totalCriteriaCount} chỉ tiêu đạt`}
                      </p>
                      <div
                        className={`mt-2 h-2 w-full overflow-hidden rounded-full ${
                          sealEligible ? "bg-emerald-100" : "bg-red-100"
                        }`}
                        role="progressbar"
                        aria-valuenow={progressPercent}
                        aria-valuemin={0}
                        aria-valuemax={100}
                      >
                        <div
                          className={`h-full rounded-full transition-all ${
                            sealEligible ? "bg-emerald-500" : "bg-red-500"
                          }`}
                          style={{ width: `${progressPercent}%` }}
                        />
                      </div>
                    </div>
                  </div>

                  {/* Bộ chỉ tiêu kiểm nghiệm với kết quả mới nhất theo dữ liệu thực tế */}
                  {insightLoading ? (
                    <div
                      className="flex items-center justify-center gap-2 py-10 text-muted-foreground"
                      aria-live="polite"
                    >
                      <LoaderCircle className="h-5 w-5 animate-spin text-emerald-500" />
                      Đang tải dữ liệu kiểm nghiệm...
                    </div>
                  ) : insightError ? (
                    <div
                      className="flex flex-col items-center gap-3 py-10 text-muted-foreground"
                      aria-live="assertive"
                    >
                      <p>{insightError}</p>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => void loadInspectionInsights()}
                      >
                        Thử lại
                      </Button>
                    </div>
                  ) : criterionRows.length === 0 ? (
                    <div
                      className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800"
                      aria-live="polite"
                    >
                      Lô chưa có chỉ tiêu kiểm nghiệm nào được áp dụng.
                    </div>
                  ) : (
                    <>
                      {/* Tìm kiếm + bộ lọc chỉ tiêu */}
                      <div className="flex flex-col gap-2 lg:flex-row lg:items-center">
                        <div className="relative flex-1">
                          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                          <Input
                            value={criteriaSearch}
                            onChange={(e) => setCriteriaSearch(e.target.value)}
                            placeholder="Tìm theo tên chỉ tiêu hoặc tiêu chuẩn..."
                            className="h-9 pl-9 pr-8"
                          />
                          {criteriaSearch && (
                            <button
                              type="button"
                              onClick={() => setCriteriaSearch("")}
                              className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-0.5 text-muted-foreground hover:text-foreground"
                              aria-label="Xóa tìm kiếm"
                            >
                              <X className="h-4 w-4" />
                            </button>
                          )}
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <Select
                            value={criteriaResultFilter}
                            onValueChange={(val) =>
                              setCriteriaResultFilter(
                                val as CriterionResultFilter,
                              )
                            }
                          >
                            <SelectTrigger className="h-9 w-full sm:w-44">
                              <SelectValue placeholder="Lọc theo kết quả">
                                {(value) =>
                                  CRITERION_RESULT_FILTER_LABELS[
                                    value as CriterionResultFilter
                                  ]
                                }
                              </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="ALL">
                                Tất cả kết quả
                              </SelectItem>
                              <SelectItem value="PASSED">Đạt</SelectItem>
                              <SelectItem value="FAILED">Không đạt</SelectItem>
                              <SelectItem value="NOT_TESTED">
                                Chưa có kết quả
                              </SelectItem>
                            </SelectContent>
                          </Select>

                          <Select
                            value={criteriaStatusFilter}
                            onValueChange={(val) =>
                              setCriteriaStatusFilter(
                                val as CriterionRowStatus | "ALL",
                              )
                            }
                          >
                            <SelectTrigger className="h-9 w-full sm:w-44">
                              <SelectValue placeholder="Lọc theo trạng thái">
                                {(value) =>
                                  getCriterionStatusFilterLabel(
                                    value as CriterionRowStatus | "ALL",
                                  )
                                }
                              </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="ALL">
                                Tất cả trạng thái
                              </SelectItem>
                              {Object.entries(CRITERION_ROW_STATUS_META).map(
                                ([status, meta]) => (
                                  <SelectItem key={status} value={status}>
                                    {meta.label}
                                  </SelectItem>
                                ),
                              )}
                            </SelectContent>
                          </Select>
                        </div>
                      </div>

                      {filteredCriterionRows.length === 0 ? (
                        <div className="rounded-lg border border-gray-200 bg-gray-50 p-4 text-sm text-muted-foreground">
                          Không có chỉ tiêu nào khớp với tìm kiếm hoặc bộ lọc
                          hiện tại.
                        </div>
                      ) : (
                        <>
                          {/* Desktop: bảng nằm gọn trong khung bo tròn có viền */}
                          <div className="hidden overflow-x-auto rounded-md border bg-card md:block">
                            <Table className="table-fixed w-full">
                              <TableHeader>
                                <TableRow className="bg-muted/50">
                                  <TableHead className="w-12 whitespace-normal text-center">
                                    STT
                                  </TableHead>
                                  <TableHead className="w-[7rem] whitespace-normal">
                                    Chỉ tiêu bắt buộc
                                  </TableHead>
                                  <TableHead className="w-28 whitespace-normal">
                                    Ngưỡng tối đa
                                  </TableHead>
                                  <TableHead className="w-24 whitespace-normal">
                                    Đơn vị
                                  </TableHead>
                                  <TableHead className="w-24 whitespace-normal">
                                    Kết quả
                                  </TableHead>
                                  <TableHead className="w-32 whitespace-normal">
                                    Trạng thái
                                  </TableHead>
                                  <TableHead className="w-32 whitespace-normal">
                                    Hiệu lực đến
                                  </TableHead>
                                </TableRow>
                              </TableHeader>
                              <TableBody>
                                {pagedCriterionRows.map((row, index) => (
                                  <TableRow
                                    key={row.criterion.code}
                                    className="align-middle transition-colors hover:bg-muted/40"
                                  >
                                    <TableCell className="text-center text-muted-foreground">
                                      {safeCriteriaPage * CRITERIA_PAGE_SIZE +
                                        index +
                                        1}
                                    </TableCell>
                                    <TableCell className="w-[7rem] whitespace-normal align-top">
                                      <span className="block min-w-0 max-w-[7rem] whitespace-normal break-words font-medium">
                                        {row.criterion.name}
                                      </span>
                                      {row.meta?.referenceStandard && (
                                        <span className="block min-w-0 max-w-[7rem] whitespace-normal break-words text-xs text-muted-foreground">
                                          {row.meta.referenceStandard}
                                        </span>
                                      )}
                                    </TableCell>
                                    <TableCell className="whitespace-normal">
                                      {row.meta
                                        ? `≤ ${formatThreshold(row.meta.maxThreshold)}`
                                        : "—"}
                                    </TableCell>
                                    <TableCell className="whitespace-normal">
                                      {row.meta?.unit || "—"}
                                    </TableCell>
                                    <TableCell className="whitespace-normal">
                                      {row.result ? (
                                        <span
                                          className={
                                            row.result.passed
                                              ? "font-medium text-emerald-700"
                                              : "font-medium text-red-700"
                                          }
                                        >
                                          {row.result.passed
                                            ? "Đạt"
                                            : "Không đạt"}
                                        </span>
                                      ) : (
                                        <span className="text-muted-foreground">
                                          Chưa có kết quả
                                        </span>
                                      )}
                                    </TableCell>
                                    <TableCell className="whitespace-normal">
                                      {getCriterionRowStatusBadge(row.status)}
                                    </TableCell>
                                    <TableCell className="whitespace-normal">
                                      {row.result && row.result.expiryDate
                                        ? formatDateOnly(row.result.expiryDate)
                                        : "—"}
                                    </TableCell>
                                  </TableRow>
                                ))}
                              </TableBody>
                            </Table>
                          </div>

                          {/* Mobile: thẻ */}
                          <div className="md:hidden space-y-3">
                            {pagedCriterionRows.map((row) => (
                              <div
                                key={row.criterion.code}
                                className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm space-y-2"
                              >
                                <div className="flex items-start justify-between gap-2">
                                  <div>
                                    <p className="font-medium break-words">
                                      {row.criterion.name}
                                    </p>
                                    <p className="text-xs text-muted-foreground">
                                      Ngưỡng tối đa:{" "}
                                      {row.meta
                                        ? `${formatThreshold(row.meta.maxThreshold)} ${row.meta.unit || ""}`.trim()
                                        : "—"}
                                    </p>
                                  </div>
                                  {getCriterionRowStatusBadge(row.status)}
                                </div>
                                <div className="flex flex-wrap items-center justify-between gap-x-3 text-sm text-muted-foreground">
                                  <span>
                                    Kết quả:{" "}
                                    <span
                                      className={
                                        row.result
                                          ? row.result.passed
                                            ? "text-emerald-700 font-medium"
                                            : "text-red-700 font-medium"
                                          : ""
                                      }
                                    >
                                      {row.result
                                        ? row.result.passed
                                          ? "Đạt"
                                          : "Không đạt"
                                        : "Chưa có kết quả"}
                                    </span>
                                  </span>
                                  <span>
                                    Hiệu lực đến:{" "}
                                    {row.result && row.result.expiryDate
                                      ? formatDateOnly(row.result.expiryDate)
                                      : "—"}
                                  </span>
                                </div>
                                {row.relatedRequestId ? (
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    className="w-full text-xs font-semibold"
                                    onClick={() =>
                                      navigate(
                                        `/production-lots/${id}/inspection-requests/${row.relatedRequestId}/results`,
                                      )
                                    }
                                  >
                                    Xem chi tiết
                                  </Button>
                                ) : (
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    className="w-full text-xs font-semibold"
                                    disabled
                                  >
                                    Xem chi tiết
                                  </Button>
                                )}
                              </div>
                            ))}
                          </div>
                        </>
                      )}
                    </>
                  )}

                  {/* Phân trang bảng chỉ tiêu (client-side, kiểu InspectionRequestHistoryModal) */}
                  {!insightLoading &&
                    !insightError &&
                    filteredCriterionRows.length > 0 && (
                      <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-gray-100 pt-3 text-xs text-muted-foreground sm:text-sm">
                        <div>
                          Hiển thị {criteriaRangeStart} – {criteriaRangeEnd}{" "}
                          trên tổng số {filteredCriterionRows.length} chỉ tiêu
                        </div>
                        <div className="flex items-center gap-1">
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={safeCriteriaPage === 0}
                            onClick={() =>
                              setCriteriaPage((page) => Math.max(0, page - 1))
                            }
                          >
                            <ChevronLeft className="mr-1 h-4 w-4" />
                            Trang trước
                          </Button>
                          <span className="px-2 font-medium tabular-nums">
                            {safeCriteriaPage + 1}/{totalCriteriaPages}
                          </span>
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={
                              safeCriteriaPage >= totalCriteriaPages - 1
                            }
                            onClick={() =>
                              setCriteriaPage((page) =>
                                Math.min(totalCriteriaPages - 1, page + 1),
                              )
                            }
                          >
                            Trang sau
                            <ChevronRight className="ml-1 h-4 w-4" />
                          </Button>
                        </div>
                      </div>
                    )}

                  {/* Banner điều kiện kích hoạt tem (tổng hợp từ dữ liệu thực tế) */}
                  {!insightLoading &&
                    !canActivateLoading &&
                    (mandatoryInspection ? (
                      sealEligible ? (
                        <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4">
                          <p className="flex items-center gap-2 text-sm font-semibold text-emerald-800">
                            <CheckCircle2 className="h-4 w-4 shrink-0" />
                            Lô đã đủ điều kiện kích hoạt tem
                          </p>
                          <p className="mt-1 text-sm text-emerald-700">
                            Lô đã có bộ kết quả kiểm nghiệm đạt và còn hiệu lực
                            cho tất cả chỉ tiêu bắt buộc. Có thể tạo lô hàng và
                            kích hoạt tem.
                          </p>
                        </div>
                      ) : (
                        <div className="rounded-lg border border-red-200 bg-red-50 p-4">
                          <p className="flex items-center gap-2 text-sm font-semibold text-red-700">
                            <AlertTriangle className="h-4 w-4 shrink-0" />
                            Lô chưa đủ điều kiện kích hoạt tem
                          </p>
                          <p className="mt-1 text-sm text-red-600">
                            Vui lòng kiểm nghiệm và đạt tất cả chỉ tiêu bắt
                            buộc.
                          </p>
                          {(failedRows.length > 0 ||
                            expiredRows.length > 0 ||
                            waitingRows.length > 0 ||
                            notTestedRows.length > 0) && (
                            <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-red-600">
                              {failedRows.length > 0 && (
                                <li>
                                  Không đạt:{" "}
                                  {failedRows
                                    .map((row) => row.criterion.name)
                                    .join(", ")}
                                </li>
                              )}
                              {expiredRows.length > 0 && (
                                <li>
                                  Kết quả đã hết hiệu lực:{" "}
                                  {expiredRows
                                    .map((row) => row.criterion.name)
                                    .join(", ")}
                                </li>
                              )}
                              {waitingRows.length > 0 && (
                                <li>
                                  Đang chờ kết quả kiểm nghiệm:{" "}
                                  {waitingRows
                                    .map((row) => row.criterion.name)
                                    .join(", ")}
                                </li>
                              )}
                              {notTestedRows.length > 0 && (
                                <li>
                                  Chưa có kết quả kiểm nghiệm:{" "}
                                  {notTestedRows
                                    .map((row) => row.criterion.name)
                                    .join(", ")}
                                </li>
                              )}
                            </ul>
                          )}
                          {canActivateCheck?.reason && (
                            <p className="mt-2 text-sm italic text-red-600">
                              {canActivateCheck.reason}
                            </p>
                          )}
                        </div>
                      )
                    ) : (
                      <div className="rounded-lg border border-gray-200 bg-gray-50 p-4 text-sm text-muted-foreground">
                        Loại nông sản này không bắt buộc kiểm nghiệm trước khi
                        kích hoạt tem. Danh sách chỉ tiêu hiển thị để theo dõi
                        chất lượng lô.
                      </div>
                    ))}
                </CardContent>
              </Card>

              {/* ── Cột phải: Lịch sử yêu cầu kiểm nghiệm ────────────────── */}
              <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
                <CardHeader className="pb-2">
                  <div className="flex items-center justify-between gap-2">
                    <CardTitle className="text-lg font-semibold">
                      Lịch sử yêu cầu kiểm nghiệm
                    </CardTitle>
                    <Button
                      variant="outline"
                      size="sm"
                      className="h-7 px-2 text-xs font-semibold text-emerald-700 hover:bg-emerald-50 hover:text-emerald-800"
                      title="Mở rộng lịch sử yêu cầu kiểm nghiệm"
                      onClick={() => setShowInspectionHistoryModal(true)}
                    >
                      &gt;&gt;&gt;
                    </Button>
                  </div>
                  <Button
                    onClick={openCreateDialog}
                    variant="create"
                    size="sm"
                    className="self-end"
                  >
                    <Plus className="h-4 w-4 mr-1" /> Tạo yêu cầu
                  </Button>
                </CardHeader>
                <CardContent className="space-y-3">
                  {/* Bộ lọc trạng thái */}
                  <div
                    className="flex flex-wrap gap-1.5"
                    role="group"
                    aria-label="Lọc theo trạng thái"
                  >
                    {INSPECTION_FILTER_OPTIONS.map((option) => (
                      <Button
                        key={option.value}
                        size="sm"
                        variant={
                          inspectionStatus === option.value
                            ? "create"
                            : "outline"
                        }
                        className="h-7 px-2.5 text-xs"
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
                      className="flex items-center justify-center gap-2 py-10 text-muted-foreground"
                      aria-live="polite"
                    >
                      <LoaderCircle className="h-5 w-5 animate-spin text-emerald-500" />
                      Đang tải yêu cầu kiểm nghiệm...
                    </div>
                  ) : inspectionError ? (
                    <div
                      className="flex flex-col items-center gap-3 py-10 text-muted-foreground"
                      aria-live="assertive"
                    >
                      <p>{inspectionError}</p>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          void loadInspectionRequests(
                            inspectionStatus,
                            inspectionPage,
                          )
                        }
                      >
                        Thử lại
                      </Button>
                    </div>
                  ) : inspectionRequests.length === 0 ? (
                    <div className="rounded-lg border border-dashed border-gray-200 py-8 text-center text-sm text-muted-foreground">
                      Chưa có yêu cầu kiểm nghiệm nào cho lô này.
                    </div>
                  ) : (
                    <>
                      {/* Danh sách yêu cầu dạng thẻ gọn */}
                      <div
                        className={
                          historyExpanded
                            ? "max-h-96 space-y-3 overflow-y-auto pr-1"
                            : "space-y-3"
                        }
                      >
                        {visibleHistory.map((request) => (
                          <div
                            key={request.testRequestId}
                            className="space-y-1.5 rounded-lg border border-gray-200 bg-white p-3"
                          >
                            <div className="flex items-center justify-between gap-2">
                              <span
                                className="font-mono text-sm font-semibold"
                                title={request.testRequestId}
                              >
                                #{request.testRequestId.slice(0, 8)}
                              </span>
                              {getInspectionStatusBadge(request.status)}
                            </div>
                            <p className="break-words text-sm">
                              <span className="text-muted-foreground">
                                Đơn vị kiểm nghiệm:{" "}
                              </span>
                              {request.testingUnit}
                            </p>
                            <p className="text-sm">
                              <span className="text-muted-foreground">
                                Ngày gửi mẫu:{" "}
                              </span>
                              {formatDateOnly(request.sampleSentDate)}
                            </p>
                            <div className="flex items-center justify-between gap-2 pt-0.5">
                              <p className="text-sm">
                                <span className="text-muted-foreground">
                                  Số chỉ tiêu:{" "}
                                </span>
                                {request.criteriaCount}
                                {request.failedCriteriaCount > 0 && (
                                  <span className="text-red-600">
                                    {" "}
                                    · {request.failedCriteriaCount} không đạt
                                  </span>
                                )}
                              </p>
                              {canInspect &&
                                (request.status === "PASSED" ||
                                request.status === "FAILED" ? (
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    className="h-7 w-7 p-0 text-slate-600 hover:text-emerald-800 hover:bg-emerald-50"
                                    title="Xem chi tiết kết quả kiểm nghiệm"
                                    onClick={() =>
                                      navigate(
                                        `/production-lots/${id}/inspection-requests/${request.testRequestId}/results`,
                                      )
                                    }
                                  >
                                    <Eye className="h-3.5 w-3.5" />
                                  </Button>
                                ) : (
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    className="text-xs font-semibold"
                                    onClick={() =>
                                      navigate(
                                        `/production-lots/${id}/inspection-requests/${request.testRequestId}/results`,
                                      )
                                    }
                                  >
                                    {request.status === "PENDING"
                                      ? "Nhận kết quả"
                                      : "Xem chi tiết"}
                                  </Button>
                                ))}
                            </div>
                          </div>
                        ))}
                      </div>

                      {/* Chân lịch sử: số lượng hiển thị + mở rộng toàn bộ */}
                      <div className="flex items-center justify-between gap-2 border-t border-gray-100 pt-3">
                        <span className="text-xs text-muted-foreground">
                          Hiển thị {inspectionRequests.length} yêu cầu
                        </span>
                        {canToggleHistory && (
                          <Button
                            variant="outline"
                            size="sm"
                            className="text-xs font-semibold"
                            onClick={() => setHistoryExpanded((v) => !v)}
                          >
                            {historyExpanded ? "Thu gọn" : "Xem tất cả yêu cầu"}
                          </Button>
                        )}
                      </div>

                      {/* Phân trang danh sách yêu cầu kiểm nghiệm (server-side) */}
                      {inspectionPageData &&
                        inspectionPageData.totalPages > 1 && (
                          <div className="flex items-center justify-center gap-3 border-t border-gray-100 pt-3">
                            <Button
                              variant="outline"
                              size="sm"
                              disabled={inspectionPageData.first}
                              onClick={() =>
                                setInspectionPage((page) =>
                                  Math.max(0, page - 1),
                                )
                              }
                            >
                              <ChevronLeft className="h-4 w-4 mr-1" /> Trước
                            </Button>
                            <span className="min-w-12 select-none text-center text-sm font-medium tabular-nums text-muted-foreground">
                              {inspectionPageData.page + 1}/
                              {inspectionPageData.totalPages}
                            </span>
                            <Button
                              variant="outline"
                              size="sm"
                              disabled={inspectionPageData.last}
                              onClick={() =>
                                setInspectionPage((page) => page + 1)
                              }
                            >
                              Sau <ChevronRight className="h-4 w-4 ml-1" />
                            </Button>
                          </div>
                        )}
                    </>
                  )}
                </CardContent>
              </Card>
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

      <InspectionRequestHistoryModal
        open={showInspectionHistoryModal}
        onClose={() => setShowInspectionHistoryModal(false)}
        lotId={id!}
        canInspect={canInspect}
      />

      <CancelProductionLotDialog
        open={cancelDialogOpen}
        lot={lot}
        onClose={() => setCancelDialogOpen(false)}
        onCancel={handleCancelProductionLot}
      />
    </div>
  );
};
