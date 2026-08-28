import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import axios from "axios";
import { toast } from "sonner";
import {
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  Check,
  ClipboardCheck,
  Clock,
  ExternalLink,
  FileText,
  FlaskConical,
  Info,
  Layers,
  LoaderCircle,
  Package,
  RotateCw,
  Save,
  Search,
  Send,
  Trash2,
  Upload,
  X,
} from "lucide-react";

import {
  createInspectionRequest,
  getAccreditationScopes,
  getInspectionRequestDetail,
  getInspectionRequests,
  getLotTestCriteria,
  getTestingUnits,
} from "@/api/certificationApi";
import { getProductionLotById } from "@/api/productionLotApi";
import { getProductCategoryCriteria } from "@/api/inspectionCriterionApi";
import type { LotTestCriteriaResult, TestingUnit } from "@/types/certification";
import type { InspectionCriterion } from "@/types/inspectionCriterion";
import type { ProductionLot } from "@/types/productionLot";
import { TestingUnitSelect } from "@/components/testing-unit/TestingUnitSelect";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  AlertDialog,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogPopup,
  AlertDialogTitle,
  AlertDialogDescription,
} from "@/components/ui/alert-dialog";
import { Pagination } from "@/components/common/Pagination";
import { HelpButton } from "@/components/help/HelpButton";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";

// Helper chuyển ngày sang định dạng ISO YYYY-MM-DD
const toISODate = (date: Date) => {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
};

// Helper trích xuất thông báo lỗi từ API
const getApiErrorMessage = (error: unknown, fallback: string): string => {
  if (axios.isAxiosError(error)) {
    const serverMessage = error.response?.data?.message;
    if (typeof serverMessage === "string" && serverMessage.trim()) {
      return serverMessage;
    }
    const status = error.response?.status;
    if (status === 400) return "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại thông tin.";
    if (status === 403) return "Bạn không có quyền thực hiện thao tác này (yêu cầu vai trò VT-02).";
    if (status === 404) return "Không tìm thấy thông tin lô sản xuất hoặc chỉ tiêu.";
    if (status === 409) {
      return "Yêu cầu kiểm nghiệm trùng lặp với yêu cầu đang chờ kết quả cho cùng bộ chỉ tiêu. Vui lòng xác nhận để tạo thêm yêu cầu.";
    }
  }
  return fallback;
};

const CRITERIA_PER_PAGE = 8;
const HISTORY_PAGE_SIZE = 50;

/**
 * Khóa đối chiếu giữa chỉ tiêu của lô (GET /test-criteria) và
 * chỉ tiêu snapshot trong các yêu cầu kiểm nghiệm cũ. Cả hai phía
 * đều sinh từ `name` của chỉ tiêu trong danh mục dùng chung nên
 * khớp chính xác sau khi chuẩn hóa.
 */
const normalizeCriterionKey = (value: string) => value.trim().toLowerCase();

/** Một dòng chỉ tiêu trên bảng lựa chọn (đã gộp dữ liệu danh mục). */
interface CriterionRow {
  criteriaId: number;
  code: string;
  name: string;
  unit: string | null;
  maxThreshold: number | null;
  isCreated: boolean;
}

// Định dạng kích thước tệp đính kèm
const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

interface AttachedFileItem {
  id: string;
  file: File;
  name: string;
  size: number;
  type: string;
}

export const CreateInspectionRequestPage: React.FC = () => {
  const { lotId: paramLotId, id: paramId } = useParams<{
    lotId?: string;
    id?: string;
  }>();
  const effectiveLotId = paramLotId || paramId || "";

  const navigate = useNavigate();
  const today = useMemo(() => toISODate(new Date()), []);

  // --- Dữ liệu tải từ server ---
  const [lot, setLot] = useState<ProductionLot | null>(null);

  // ── Breadcrumb điều hướng thống nhất ───────────────────────────────────────
  // Ghi đè breadcrumb tự sinh để BỎ crumb trung gian "Yêu cầu kiểm nghiệm"
  // (/production-lots/:id/inspection-requests) vì route này không tồn tại
  // (không có trang danh sách). Nhãn crumb lô dùng tên lô khi đã tải xong.
  useSetBreadcrumb([
    { label: "Dashboard", href: "/dashboard" },
    { label: "Lô sản xuất", href: "/production-lots" },
    ...(effectiveLotId
      ? [
          {
            label: lot?.name || "Chi tiết lô sản xuất",
            href: `/production-lots/${effectiveLotId}`,
          },
        ]
      : []),
    { label: "Tạo yêu cầu kiểm nghiệm" },
  ]);

  const [criteriaData, setCriteriaData] = useState<LotTestCriteriaResult | null>(null);
  /** Map catalog criteriaId -> {unit, maxThreshold} lấy từ bộ chỉ tiêu của loại nông sản. */
  const [catalogCriteriaMap, setCatalogCriteriaMap] = useState<Map<number, InspectionCriterion>>(new Map());
  /**
   * Tập khóa chuẩn hóa các chỉ tiêu đã từng thuộc yêu cầu kiểm nghiệm
   * của lô (deriving từ GET /test-requests + chi tiết từng request).
   */
  const [createdCriterionKeys, setCreatedCriterionKeys] = useState<Set<string>>(new Set());
  const [isRefreshingHistory, setIsRefreshingHistory] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  // --- Form State ---
  const [testingUnit, setTestingUnit] = useState("");
  // NCL-11-CN-006 Phase 1: chọn đơn vị kiểm nghiệm từ danh mục dùng chung
  const [testingUnitId, setTestingUnitId] = useState("");
  const [testingUnits, setTestingUnits] = useState<TestingUnit[]>([]);
  const [unitsLoading, setUnitsLoading] = useState(false);
  const [unitsError, setUnitsError] = useState<string | null>(null);
  // NCL-11-CN-006 Phase 2: phạm vi công nhận của đơn vị đã chọn
  const [accreditedCriterionIds, setAccreditedCriterionIds] = useState<Set<number>>(new Set());
  const [scopeLoading, setScopeLoading] = useState(false);
  const [sampleSentDate, setSampleSentDate] = useState(today);
  const [sampleWeight, setSampleWeight] = useState("");
  const [deliveryMethod, setDeliveryMethod] = useState<"LAB_PICKUP" | "COURIER" | "SELF_DELIVERY">("LAB_PICKUP");
  const [notes, setNotes] = useState("");
  const [selectedCriteriaIds, setSelectedCriteriaIds] = useState<number[]>([]);
  const [attachedFiles, setAttachedFiles] = useState<AttachedFileItem[]>([]);

  // Search & Filter trong danh sách chỉ tiêu
  const [criteriaSearch, setCriteriaSearch] = useState("");
  const [criteriaFilter, setCriteriaFilter] = useState<"ALL" | "CREATED" | "NOT_CREATED">("ALL");
  const [criteriaPage, setCriteriaPage] = useState(1);

  // Validation Touch State
  const [touched, setTouched] = useState({
    testingUnit: false,
    sampleSentDate: false,
    criteria: false,
  });

  const [submitting, setSubmitting] = useState(false);

  // --- Duplicate Conflict Dialog State (HTTP 409) ---
  const [duplicateOpen, setDuplicateOpen] = useState(false);
  const [duplicateMessage, setDuplicateMessage] = useState("");

  // Draft Key lưu LocalStorage theo lotId
  const draftStorageKey = useMemo(
    () => `draft_inspection_request_${effectiveLotId}`,
    [effectiveLotId]
  );

  /**
   * Thu thập tập khóa các chỉ tiêu đang BỊ CHẶN chọn lại khi tạo yêu cầu mới
   * (tập "Đã tạo" của lô), dựa trên toàn bộ lịch sử YÊU CẦU KIỂM NGHIỆM.
   *
   * Quy tắc:
   * - Chỉ tiêu chưa có kết quả hoặc kết quả chốt gần nhất là ĐẠT  -> bị chặn.
   * - Chỉ tiêu được ghi nhận KHÔNG ĐẠT ở lần xét gần nhất -> KHÔNG bị chặn:
   *   trạng thái hiển thị đặt lại thành "Chưa tạo" và cho phép chọn lại
   *   để tạo yêu cầu kiểm nghiệm mới.
   *
   * "Gần nhất" xác định bằng createdAt của InspectionCriterionResult nên
   * không phụ thuộc thứ tự sắp xếp danh sách request trả về.
   *
   * Duyệt phân trang GET /test-requests?lotId=... rồi lấy chi tiết từng
   * request (GET /inspection-requests/{id}) để đọc kết quả snapshot.
   * Toàn bộ dùng API sẵn có — không tạo endpoint mới.
   */
  const fetchCreatedCriterionKeys = useCallback(async (): Promise<Set<string>> => {
    // Kết quả chốt gần nhất theo từng khóa chỉ tiêu (code đã chuẩn hóa)
    const latestResultByKey = new Map<string, { passed: boolean; at: number }>();
    // Khóa thuộc request nhưng chưa ghi kết quả (đang chờ / chưa hoàn tất)
    const keysWithoutResult = new Set<string>();
    let page = 0;
    let totalPages = 1;
    const requestIds: string[] = [];

    while (page < totalPages) {
      const pageRes = await getInspectionRequests({
        lotId: effectiveLotId,
        page,
        size: HISTORY_PAGE_SIZE,
      });
      totalPages = pageRes.totalPages ?? 0;
      requestIds.push(...pageRes.items.map((item) => item.testRequestId));
      page += 1;
    }

    await Promise.all(
      requestIds.map(async (requestId) => {
        try {
          const detail = await getInspectionRequestDetail(requestId);
          detail.criteria?.forEach((criterion) => {
            const key = normalizeCriterionKey(criterion.code || criterion.name);
            if (!key) return;
            if (criterion.result == null) {
              keysWithoutResult.add(key);
              return;
            }
            const at = Date.parse(criterion.result.createdAt ?? "") || 0;
            const prev = latestResultByKey.get(key);
            if (!prev || at >= prev.at) {
              latestResultByKey.set(key, { passed: criterion.result.passed, at });
            }
          });
        } catch {
          // Bỏ qua lỗi lẻ của một request để không mất dữ liệu lịch sử còn lại
        }
      })
    );

    // Khóa bị chặn = chưa có kết quả, HOẶC kết quả chốt gần nhất là ĐẠT.
    // Riêng kết quả chốt gần nhất KHÔNG ĐẠT -> mở lại lựa chọn ("Chưa tạo").
    return new Set<string>([
      ...keysWithoutResult,
      ...[...latestResultByKey.entries()]
        .filter(([, value]) => value.passed)
        .map(([key]) => key),
    ]);
  }, [effectiveLotId]);

  /**
   * Tải lại dữ liệu bảng chỉ tiêu: chỉ tiêu áp dụng cho lô + đơn vị/ngưỡng
   * từ bộ chỉ tiêu của loại nông sản + trạng thái Đã tạo/Chưa tạo từ lịch sử.
   *
   * Lưu ý: tập khóa trả về bởi fetchCreatedCriterionKeys là các khóa BỊ CHẶN.
   * Chỉ tiêu từng bị ghi nhận KHÔNG ĐẠT ở lần xét gần nhất sẽ không nằm trong
   * tập này -> hiển thị "Chưa tạo" và cho phép chọn để tạo yêu cầu kiểm tra lại.
   */
  const loadCriteriaSectionData = useCallback(
    async (
      productCategoryId: string
    ): Promise<{ criteriaRes: LotTestCriteriaResult; createdKeys: Set<string> }> => {
      const [criteriaRes, catalogRes, createdKeys] = await Promise.all([
        getLotTestCriteria(effectiveLotId),
        getProductCategoryCriteria(productCategoryId, true).catch(
          () => [] as InspectionCriterion[]
        ),
        fetchCreatedCriterionKeys(),
      ]);

      setCriteriaData(criteriaRes);
      setCatalogCriteriaMap(new Map(catalogRes.map((c) => [c.id, c])));
      setCreatedCriterionKeys(createdKeys);

      return { criteriaRes, createdKeys };
    },
    [effectiveLotId, fetchCreatedCriterionKeys]
  );

  /** Danh sách id chỉ tiêu còn được phép chọn trong danh sách đã tải. */
  const getSelectableIdsFromRows = (
    rows: { code: string; name: string; criteriaId: number }[],
    createdKeys: Set<string>
  ) =>
    new Set(
      rows
        .filter((c) => !createdKeys.has(normalizeCriterionKey(c.code || c.name)))
        .map((c) => c.criteriaId)
    );

  // --- Tải dữ liệu ban đầu ---
  const loadInitialData = useCallback(async () => {
    if (!effectiveLotId) {
      setLoadError("Mã lô sản xuất không hợp lệ.");
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setLoadError(null);

    try {
      const lotRes = await getProductionLotById(effectiveLotId);
      setLot(lotRes);

      // NCL-11-CN-006: tải danh mục đơn vị kiểm nghiệm (nếu lỗi thì fallback nhập tự do)
      setUnitsLoading(true);
      try {
        const unitsRes = await getTestingUnits({ isActive: true });
        setTestingUnits(unitsRes.items ?? []);
        setUnitsError(null);
      } catch {
        setTestingUnits([]);
        setUnitsError("Không thể tải danh mục đơn vị kiểm nghiệm.");
      } finally {
        setUnitsLoading(false);
      }

      const { criteriaRes, createdKeys } = await loadCriteriaSectionData(
        lotRes.productCategoryId
      );

      const selectableIds = getSelectableIdsFromRows(criteriaRes.criteria ?? [], createdKeys);

      // Kiểm tra xem có bản nháp nào đã lưu trước đó không
      const savedDraft = localStorage.getItem(draftStorageKey);
      if (savedDraft) {
        try {
          const parsed = JSON.parse(savedDraft);
          if (parsed.testingUnit) setTestingUnit(parsed.testingUnit);
          if (parsed.testingUnitId) setTestingUnitId(parsed.testingUnitId);
          if (parsed.sampleSentDate) setSampleSentDate(parsed.sampleSentDate);
          if (parsed.sampleWeight) setSampleWeight(parsed.sampleWeight);
          if (parsed.deliveryMethod) setDeliveryMethod(parsed.deliveryMethod);
          if (parsed.notes) setNotes(parsed.notes);
          if (Array.isArray(parsed.selectedCriteriaIds)) {
            // Chỉ khôi phục những chỉ tiêu chưa từng tạo yêu cầu cho lô
            setSelectedCriteriaIds(
              parsed.selectedCriteriaIds.filter((id: number) => selectableIds.has(id))
            );
          }
          toast.info("Đã khôi phục bản nháp chưa gửi của lô sản xuất này.", {
            duration: 4000,
          });
        } catch {
          // ignore corrupted draft
        }
      } else {
        // Mặc định chọn toàn bộ chỉ tiêu CHƯA từng tạo yêu cầu kiểm nghiệm
        setSelectedCriteriaIds([...selectableIds]);
      }
    } catch (err: unknown) {
      setLoadError(
        getApiErrorMessage(
          err,
          "Không thể tải thông tin lô sản xuất hoặc danh sách chỉ tiêu kiểm nghiệm."
        )
      );
    } finally {
      setIsLoading(false);
    }
  }, [effectiveLotId, draftStorageKey, loadCriteriaSectionData]);

  useEffect(() => {
    void loadInitialData();
  }, [loadInitialData]);

  /**
   * Refetch lại toàn bộ dữ liệu bảng chỉ tiêu từ server sau khi tạo
   * yêu cầu thành công để trạng thái Đã tạo/Chưa tạo phản ánh chính xác
   * dữ liệu backend (không dựa vào state local).
   */
  const refreshCriteriaSection = async () => {
    if (!lot?.productCategoryId) return;
    setIsRefreshingHistory(true);
    try {
      await loadCriteriaSectionData(lot.productCategoryId);
    } catch {
      // Không đổi trạng thái giả thành công; dữ liệu server sẽ được đồng bộ ở lần truy cập kế
      toast.error("Không thể làm mới trạng thái chỉ tiêu sau khi tạo yêu cầu.");
    } finally {
      setIsRefreshingHistory(false);
    }
  };

  // --- Dữ liệu dòng bảng chỉ tiêu (gộp đơn vị/ngưỡng + trạng thái Đã tạo từ lịch sử server) ---
  const criterionRows = useMemo<CriterionRow[]>(() => {
    if (!criteriaData?.criteria) return [];
    return criteriaData.criteria.map((item) => {
      const catalogEntry = catalogCriteriaMap.get(item.criteriaId);
      return {
        criteriaId: item.criteriaId,
        code: item.code,
        name: item.name,
        unit: catalogEntry?.unit ?? null,
        maxThreshold: catalogEntry?.maxThreshold ?? null,
        isCreated: createdCriterionKeys.has(
          normalizeCriterionKey(item.code || item.name)
        ),
      };
    });
  }, [criteriaData, catalogCriteriaMap, createdCriterionKeys]);

  const totalCriteriaCount = criterionRows.length;
  const createdCriteriaCount = criterionRows.filter((row) => row.isCreated).length;

  // --- Filter danh sách chỉ tiêu ---
  const filteredCriteria = useMemo(() => {
    return criterionRows.filter((item) => {
      const keyword = criteriaSearch.trim().toLowerCase();
      const matchSearch =
        keyword === "" ||
        item.name.toLowerCase().includes(keyword) ||
        item.code.toLowerCase().includes(keyword);

      if (criteriaFilter === "CREATED") return matchSearch && item.isCreated;
      if (criteriaFilter === "NOT_CREATED") return matchSearch && !item.isCreated;
      return matchSearch;
    });
  }, [criterionRows, criteriaSearch, criteriaFilter]);

  // Phân trang danh sách chỉ tiêu
  const totalCriteriaPages = Math.max(1, Math.ceil(filteredCriteria.length / CRITERIA_PER_PAGE));

  const paginatedCriteria = useMemo(() => {
    const start = (criteriaPage - 1) * CRITERIA_PER_PAGE;
    return filteredCriteria.slice(start, start + CRITERIA_PER_PAGE);
  }, [filteredCriteria, criteriaPage]);

  // Tự động chuyển về trang 1 khi lọc hoặc tìm kiếm thay đổi
  useEffect(() => {
    setCriteriaPage(1);
  }, [criteriaSearch, criteriaFilter]);

  // --- Xử lý chọn chỉ tiêu (checkbox chỉ thể hiện lựa chọn cho LẦN TẠO HIỆN TẠI) ---
  const toggleCriterion = (criteriaId: number, checked: boolean) => {
    // Chỉ tiêu Đã tạo không được chọn lại cho cùng lô
    const row = criterionRows.find((r) => r.criteriaId === criteriaId);
    if (!row || row.isCreated || isRefreshingHistory) return;

    setTouched((prev) => ({ ...prev, criteria: true }));
    setSelectedCriteriaIds((prev) => {
      if (checked) {
        return prev.includes(criteriaId) ? prev : [...prev, criteriaId];
      }
      return prev.filter((id) => id !== criteriaId);
    });
  };

  const handleSelectAllCriteria = () => {
    if (isRefreshingHistory) return;
    setTouched((prev) => ({ ...prev, criteria: true }));
    setSelectedCriteriaIds((prev) => {
      // Chỉ chọn các chỉ tiêu Chưa tạo trong kết quả lọc/tìm kiếm hiện tại
      const next = new Set(prev);
      filteredCriteria.forEach((row) => {
        if (!row.isCreated) next.add(row.criteriaId);
      });
      return [...next];
    });
  };

  const handleDeselectAllCriteria = () => {
    setTouched((prev) => ({ ...prev, criteria: true }));
    setSelectedCriteriaIds([]);
  };

  // --- Xử lý File Upload client-side (NCL-11-CN-006) ---
  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files || files.length === 0) return;

    const newItems: AttachedFileItem[] = [];
    for (let i = 0; i < files.length; i++) {
      const f = files[i];
      if (f.size > 10 * 1024 * 1024) {
        toast.error(`Tệp "${f.name}" vượt quá dung lượng tối đa 10MB.`);
        continue;
      }
      newItems.push({
        id: `${Date.now()}_${i}`,
        file: f,
        name: f.name,
        size: f.size,
        type: f.type,
      });
    }

    setAttachedFiles((prev) => [...prev, ...newItems]);
    event.target.value = "";
  };

  const handleRemoveFile = (fileId: string) => {
    setAttachedFiles((prev) => prev.filter((item) => item.id !== fileId));
  };

  // --- Xử lý Lưu bản nháp (Draft) ---
  const handleSaveDraft = () => {
    const draftData = {
      testingUnit,
      testingUnitId,
      sampleSentDate,
      sampleWeight,
      deliveryMethod,
      notes,
      selectedCriteriaIds,
      updatedAt: new Date().toISOString(),
    };

    localStorage.setItem(draftStorageKey, JSON.stringify(draftData));
    toast.success("Đã lưu bản nháp thành công vào trình duyệt.", {
      description: "Dữ liệu sẽ được tự động giữ lại khi bạn quay lại trang này.",
    });
  };

  const handleClearDraft = () => {
    localStorage.removeItem(draftStorageKey);
    setTestingUnit("");
    setTestingUnitId("");
    setSampleSentDate(today);
    setSampleWeight("");
    setDeliveryMethod("LAB_PICKUP");
    setNotes("");
    setAttachedFiles([]);
    setSelectedCriteriaIds([...getSelectableIdsFromRows(criterionRows, createdCriterionKeys)]);
    setTouched({ testingUnit: false, sampleSentDate: false, criteria: false });
    toast.info("Đã xóa trắng form và làm mới bản nháp.");
  };

  // --- Validation Form ---
  const trimmedTestingUnit = testingUnit.trim();
  // NCL-11-CN-006 Phase 1: khi danh mục khả dụng thì bắt buộc chọn từ dropdown;
  // nếu danh mục rỗng hoặc tải lỗi thì fallback về nhập tự do.
  const useUnitCatalog = !unitsError && !unitsLoading && testingUnits.length > 0;
  // NCL-11-CN-006: chỉ hiển thị đơn vị còn hạn công nhận trong dropdown
  const availableUnits = useMemo(() => {
    const t = toISODate(new Date());
    return testingUnits.filter(
      (u) => !u.accreditationExpiryDate || u.accreditationExpiryDate >= t
    );
  }, [testingUnits]);
  const selectedTestingUnit =
    availableUnits.find((unit) => unit.id === testingUnitId) || null;

  // NCL-11-CN-006 Phase 2: khi chọn đơn vị từ danh mục, tải phạm vi công nhận.
  useEffect(() => {
    if (!testingUnitId) {
      setAccreditedCriterionIds(new Set());
      return;
    }
    let cancelled = false;
    setScopeLoading(true);
    getAccreditationScopes(testingUnitId)
      .then((summary) => {
        if (!cancelled) {
          setAccreditedCriterionIds(
            new Set(summary.accreditedCriteria.map((c) => c.id))
          );
        }
      })
      .catch(() => {
        // Không chặn tạo yêu cầu khi không tải được phạm vi.
        if (!cancelled) setAccreditedCriterionIds(new Set());
      })
      .finally(() => {
        if (!cancelled) setScopeLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [testingUnitId]);

  // NCL-11-CN-006 Phase 2: các chỉ tiêu đã chọn nằm NGOÀI phạm vi công nhận.
  const nonAccreditedCriteria = useMemo(() => {
    if (!testingUnitId || accreditedCriterionIds.size === 0) return [];
    return criterionRows.filter(
      (row) =>
        selectedCriteriaIds.includes(row.criteriaId) &&
        !accreditedCriterionIds.has(row.criteriaId)
    );
  }, [
    testingUnitId,
    accreditedCriterionIds,
    criterionRows,
    selectedCriteriaIds,
  ]);

  const isSampleDateValid = sampleSentDate !== "" && sampleSentDate <= today;
  const isTestingUnitValid = useUnitCatalog
    ? testingUnitId !== ""
    : trimmedTestingUnit !== "";
  const isCriteriaSelectedValid = selectedCriteriaIds.length > 0;

  const canSubmit =
    !submitting &&
    !isLoading &&
    !loadError &&
    isTestingUnitValid &&
    isSampleDateValid &&
    isCriteriaSelectedValid;

  // --- Gửi yêu cầu kiểm nghiệm ---
  const handleSubmit = async (confirmDuplicate = false) => {
    if (submitting) return; // chống double submit

    setTouched({ testingUnit: true, sampleSentDate: true, criteria: true });

    // Loại bỏ an toàn các chỉ tiêu đã từng tạo yêu cầu trước khi gửi
    // (phòng trường hợp state bị can thiệp từ devtools/DOM)
    const selectableIds = getSelectableIdsFromRows(criterionRows, createdCriterionKeys);
    const effectiveCriteriaIds = selectedCriteriaIds.filter((id) =>
      selectableIds.has(id)
    );

    if (selectedCriteriaIds.length > 0 && effectiveCriteriaIds.length === 0) {
      toast.error(
        "Các chỉ tiêu đã chọn đều đã được tạo yêu cầu kiểm nghiệm cho lô này. Vui lòng chọn chỉ tiêu khác."
      );
      return;
    }

    if (!canSubmit && !confirmDuplicate) {
      if (!isTestingUnitValid)
        toast.error(
          useUnitCatalog
            ? "Vui lòng chọn đơn vị kiểm nghiệm từ danh mục."
            : "Vui lòng nhập đơn vị phòng kiểm nghiệm."
        );
      else if (!isSampleDateValid) toast.error("Ngày gửi mẫu không được lớn hơn ngày hiện tại.");
      else if (!isCriteriaSelectedValid) toast.error("Vui lòng chọn ít nhất một chỉ tiêu kiểm nghiệm.");
      return;
    }

    if (effectiveCriteriaIds.length === 0) {
      toast.error("Vui lòng chọn ít nhất một chỉ tiêu kiểm nghiệm.");
      return;
    }

    setSubmitting(true);
    try {
      await createInspectionRequest(effectiveLotId, {
        testingUnitId: useUnitCatalog ? testingUnitId : null,
        // Khi chọn từ danh mục, gửi tên snapshot của đơn vị để tương thích ngược
        testingUnit: selectedTestingUnit?.name || trimmedTestingUnit,
        sampleSentDate,
        criteriaIds: effectiveCriteriaIds,
        confirmDuplicate,
      });

      // Xóa bản nháp sau khi gửi thành công
      localStorage.removeItem(draftStorageKey);

      toast.success("Tạo yêu cầu kiểm nghiệm thành công!", {
        description: `Đã gửi yêu cầu cho lô ${lot?.name || effectiveLotId} tới ${trimmedTestingUnit}.`,
      });

      // Cập nhật tức thời các chỉ tiêu vừa gửi thành Đã tạo, sau đó
      // refetch toàn bộ dữ liệu từ server để đảm bảo trạng thái chính xác
      const submittedKeys = new Set(
        criterionRows
          .filter((row) => effectiveCriteriaIds.includes(row.criteriaId))
          .map((row) => normalizeCriterionKey(row.code || row.name))
      );
      setCreatedCriterionKeys((prev) => new Set([...prev, ...submittedKeys]));

      // Xóa lựa chọn của lần tạo hiện tại sau khi thành công
      setSelectedCriteriaIds([]);
      setTouched((prev) => ({ ...prev, criteria: false }));
      setCriteriaFilter("ALL");
      setCriteriaPage(1);

      await refreshCriteriaSection();
    } catch (error: unknown) {
      // API thất bại: giữ nguyên lựa chọn và trạng thái Chưa tạo để retry
      if (axios.isAxiosError(error) && error.response?.status === 409) {
        setDuplicateMessage(
          getApiErrorMessage(
            error,
            "Yêu cầu kiểm nghiệm trùng lặp với yêu cầu đang chờ kết quả cho cùng bộ chỉ tiêu. Vui lòng xác nhận để tạo thêm yêu cầu."
          )
        );
        setDuplicateOpen(true);
      } else {
        toast.error(getApiErrorMessage(error, "Không thể tạo yêu cầu kiểm nghiệm."));
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleFormSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    void handleSubmit(false);
  };

  // --- RENDER GIAO DIỆN ---
  if (isLoading) {
    return (
      <div className="flex min-h-[400px] flex-col items-center justify-center space-y-4">
        <LoaderCircle className="h-10 w-10 animate-spin text-emerald-600" />
        <p className="text-sm font-medium text-muted-foreground">
          Đang tải thông tin lô sản xuất và danh mục chỉ tiêu...
        </p>
      </div>
    );
  }

  if (loadError || !lot) {
    return (
      <div className="mx-auto max-w-2xl py-12">
        <Card className="rounded-xl border-red-200 bg-red-50/50 p-8 shadow-xs">
          <CardContent className="flex flex-col items-center justify-center p-0 text-center">
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-red-100 text-red-600 mb-4">
              <AlertTriangle className="h-7 w-7" />
            </div>
            <h2 className="text-lg font-bold text-red-900">
              Không thể tải thông tin yêu cầu kiểm nghiệm
            </h2>
            <p className="text-sm text-red-700 mt-2 max-w-md">
              {loadError || "Lô sản xuất không tồn tại hoặc tài khoản không có quyền truy cập."}
            </p>
            <div className="mt-6 flex items-center gap-3">
              <Button
                variant="outline"
                className="rounded-xl border-input bg-white"
                onClick={() => navigate("/production-lots")}
              >
                <ArrowLeft className="mr-1.5 h-4 w-4" /> Quay lại danh sách lô
              </Button>
              <Button
                variant="create"
                className="rounded-xl"
                onClick={() => void loadInitialData()}
              >
                <RotateCw className="mr-1.5 h-4 w-4" /> Thử lại
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  const selectedCount = selectedCriteriaIds.length;

  return (
    <form onSubmit={handleFormSubmit} className="space-y-6 pb-28">
      {/* Standard Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-3.5">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-50 text-emerald-700 shadow-sm shrink-0">
            <FlaskConical className="h-6 w-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold tracking-tight text-slate-900 sm:text-2xl">
                Tạo yêu cầu kiểm nghiệm chất lượng
              </h1>
              <Badge
                variant="outline"
                className="rounded-full border-emerald-300 bg-emerald-50 text-xs font-semibold text-emerald-800 px-2.5 py-0.5"
              >
                VT-02
              </Badge>
            </div>
            <p className="text-xs text-muted-foreground sm:text-sm mt-0.5">
              Lập hồ sơ gửi mẫu phân tích chỉ tiêu an toàn thực phẩm cho lô:{" "}
              <span className="font-semibold text-slate-900">{lot.name}</span>
            </p>
          </div>
        </div>
        <div>
          <HelpButton screenKey="inspection-request-create" />
        </div>
      </div>

      {/* SECTION 1: 2-COLUMN MAIN CONTENT (70% Form Trái / 30% Đối Soát Phải) */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* CỘT TRÁI (70% - lg:col-span-8) */}
        <div className="space-y-6 lg:col-span-8">
          {/* CARD 1: Bộ quy chuẩn & Lựa chọn chỉ tiêu phân tích */}
          <Card className="rounded-xl border-slate-200 bg-white shadow-sm overflow-hidden">
            <CardHeader className="border-b border-border bg-muted/40 p-5">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                    <Layers className="h-5 w-5" />
                  </div>
                  <div>
                    <CardTitle className="text-base font-semibold text-foreground">
                      1. Bộ quy chuẩn & Chỉ tiêu phân tích
                    </CardTitle>
                    <CardDescription className="text-xs text-muted-foreground mt-0.5">
                      Tiêu chuẩn áp dụng:{" "}
                      <span className="font-semibold text-emerald-800">
                        {criteriaData?.standardName || "Tiêu chuẩn kỹ thuật áp dụng cho lô"}
                      </span>
                    </CardDescription>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <Badge
                    variant="outline"
                    className={`rounded-full px-3 py-1 text-xs font-semibold ${
                      selectedCount > 0
                        ? "border-emerald-300 bg-emerald-50 text-emerald-800"
                        : "border-red-300 bg-red-50 text-red-700"
                    }`}
                  >
                    Chọn cho lần này: {selectedCount} chỉ tiêu
                  </Badge>
                  <Badge
                    variant="outline"
                    className="rounded-full border-slate-300 bg-slate-50 px-3 py-1 text-xs font-semibold text-slate-700"
                  >
                    Đã tạo: {createdCriteriaCount}/{totalCriteriaCount} chỉ tiêu
                  </Badge>
                </div>
              </div>
            </CardHeader>

            <CardContent className="p-5 space-y-4">
              {/* Toolbar Tìm kiếm & Lọc & Chọn nhanh */}
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex flex-1 flex-wrap items-center gap-2">
                  <div className="relative flex-1 min-w-[200px] max-w-xs">
                    <Search className="absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      placeholder="Tìm theo tên hoặc mã chỉ tiêu..."
                      value={criteriaSearch}
                      onChange={(e) => setCriteriaSearch(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === "Enter") e.preventDefault();
                      }}
                      className="h-9 pl-9 text-xs rounded-xl border-border"
                    />
                    {criteriaSearch && (
                      <button
                        type="button"
                        onClick={() => setCriteriaSearch("")}
                        className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground"
                      >
                        <X className="h-3.5 w-3.5" />
                      </button>
                    )}
                  </div>

                  {/* Filter Pills theo trạng thái của lô */}
                  <div className="inline-flex items-center rounded-xl border border-slate-200 bg-white p-1">
                    {(
                      [
                        { value: "ALL", label: `Tất cả (${totalCriteriaCount})` },
                        { value: "CREATED", label: `Đã tạo (${createdCriteriaCount})` },
                        {
                          value: "NOT_CREATED",
                          label: `Chưa tạo (${totalCriteriaCount - createdCriteriaCount})`,
                        },
                      ] as const
                    ).map((pill) => (
                      <button
                        key={pill.value}
                        type="button"
                        onClick={() => setCriteriaFilter(pill.value)}
                        className={`rounded-lg px-3.5 py-1.5 text-sm transition-all ${
                          criteriaFilter === pill.value
                            ? "border border-primary bg-white font-semibold text-primary shadow-2xs"
                            : "border border-transparent font-medium text-foreground/60 hover:text-foreground"
                        }`}
                      >
                        {pill.label}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleSelectAllCriteria}
                    className="h-8 rounded-lg text-xs font-medium border-emerald-200 text-emerald-700 hover:bg-emerald-50"
                  >
                    <Check className="mr-1 h-3 w-3" /> Chọn tất cả
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={handleDeselectAllCriteria}
                    className="h-8 rounded-lg text-xs font-medium text-muted-foreground hover:text-foreground"
                  >
                    Bỏ chọn
                  </Button>
                </div>
              </div>

              {/* Danh sách chỉ tiêu (Phân trang) */}
              {totalCriteriaCount === 0 ? (
                <div className="rounded-xl border border-amber-200 bg-amber-50/80 p-5 text-center text-sm text-amber-800">
                  <Info className="mx-auto h-6 w-6 text-amber-600 mb-2" />
                  <p className="font-semibold">Chưa có chỉ tiêu kiểm nghiệm nào được thiết lập</p>
                  <p className="text-xs text-amber-700 mt-1">
                    Lô sản xuất chưa được gắn tiêu chuẩn hoặc tiêu chuẩn chưa có danh mục chỉ tiêu. Vui lòng liên hệ Quản trị viên để cấu hình.
                  </p>
                </div>
              ) : filteredCriteria.length === 0 ? (
                <div className="rounded-xl border border-dashed border-border p-8 text-center text-xs text-muted-foreground">
                  {criteriaSearch.trim()
                    ? `Không tìm thấy chỉ tiêu nào phù hợp với từ khóa "${criteriaSearch}".`
                    : "Không có chỉ tiêu nào ở trạng thái này."}
                </div>
              ) : (
                <div className="space-y-4">
                  {/* Banner khi không còn chỉ tiêu Chưa tạo nào cho lô */}
                  {createdCriteriaCount > 0 && createdCriteriaCount === totalCriteriaCount && (
                    <div className="flex items-start gap-2.5 rounded-xl border border-amber-200 bg-amber-50/80 p-4 text-xs text-amber-800">
                      <Info className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
                      <span>
                        Tất cả chỉ tiêu của lô này đã được tạo yêu cầu kiểm nghiệm. Bạn không thể
                        tạo thêm yêu cầu mới cho lô này.
                      </span>
                    </div>
                  )}

                  <div className="rounded-md border overflow-hidden">
                    <Table>
                      <TableHeader>
                        <TableRow className="bg-muted/50">
                          <TableHead className="w-12 text-center">STT</TableHead>
                          <TableHead>Tên chỉ tiêu</TableHead>
                          <TableHead>Đơn vị</TableHead>
                          <TableHead>Ngưỡng tối đa</TableHead>
                          <TableHead>Trạng thái</TableHead>
                          <TableHead className="w-24 text-center">Thao tác</TableHead>
                        </TableRow>
                      </TableHeader>
                    <TableBody>
                      {paginatedCriteria.map((criterion, index) => {
                        const isChecked = selectedCriteriaIds.includes(criterion.criteriaId);
                        return (
                          <TableRow
                            key={criterion.criteriaId}
                            onClick={(e) => {
                              // Click checkbox trong cột Thao tác tự xử lý qua onCheckedChange
                              if ((e.target as HTMLElement).closest("button")) return;
                              toggleCriterion(criterion.criteriaId, !isChecked);
                            }}
                            className={`transition-colors ${
                              criterion.isCreated
                                ? "cursor-not-allowed bg-muted/40 opacity-70"
                                : isChecked
                                  ? "cursor-pointer bg-emerald-50/40 hover:bg-muted/40"
                                  : "cursor-pointer hover:bg-muted/40"
                            }`}
                          >
                            <TableCell className="text-center font-medium text-muted-foreground">
                              {(criteriaPage - 1) * CRITERIA_PER_PAGE + index + 1}
                            </TableCell>
                            <TableCell>
                              <div className="min-w-0">
                                <p className="text-xs font-semibold leading-snug text-foreground">
                                  {criterion.name}
                                </p>
                                <p className="mt-0.5 font-mono text-[11px] text-muted-foreground">
                                  {criterion.code}
                                </p>
                              </div>
                            </TableCell>
                            <TableCell className="whitespace-nowrap text-xs">
                              {criterion.unit ?? "—"}
                            </TableCell>
                            <TableCell className="whitespace-nowrap text-xs">
                              {criterion.maxThreshold !== null
                                ? Number(criterion.maxThreshold)
                                : "—"}
                            </TableCell>
                            <TableCell>
                              {criterion.isCreated ? (
                                <Badge
                                  variant="outline"
                                  className="rounded-full border-amber-300 bg-amber-50 px-2.5 py-0.5 text-[11px] font-semibold text-amber-800"
                                >
                                  Đã tạo
                                </Badge>
                              ) : (
                                <Badge
                                  variant="outline"
                                  className="rounded-full border-emerald-300 bg-emerald-50 px-2.5 py-0.5 text-[11px] font-semibold text-emerald-800"
                                >
                                  Chưa tạo
                                </Badge>
                              )}
                            </TableCell>
                            <TableCell className="text-center">
                              <Checkbox
                                id={`criterion-${criterion.criteriaId}`}
                                checked={isChecked}
                                onCheckedChange={(c) =>
                                  toggleCriterion(criterion.criteriaId, c === true)
                                }
                                disabled={criterion.isCreated || isRefreshingHistory}
                                aria-label={`Chọn chỉ tiêu ${criterion.name}`}
                                title={
                                  criterion.isCreated
                                    ? "Chỉ tiêu đã từng thuộc yêu cầu kiểm nghiệm của lô này"
                                    : undefined
                                }
                              />
                            </TableCell>
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </div>

                  {/* Phân trang danh sách chỉ tiêu */}
                  <Pagination
                    currentPage={criteriaPage - 1}
                    totalPages={totalCriteriaPages}
                    totalElements={filteredCriteria.length}
                    pageSize={CRITERIA_PER_PAGE}
                    itemLabel="chỉ tiêu"
                    onPageChange={(page) => setCriteriaPage(page + 1)}
                  />
                </div>
              )}

              {touched.criteria && selectedCount === 0 && (
                <p className="text-xs font-medium text-red-600 flex items-center gap-1 mt-1">
                  <AlertCircle className="h-3.5 w-3.5" /> Bắt buộc phải chọn ít nhất một chỉ tiêu kiểm nghiệm.
                </p>
              )}
            </CardContent>
          </Card>

          {/* NCL-11-CN-006 Phase 2: Cảnh báo phạm vi công nhận */}
          {nonAccreditedCriteria.length > 0 && (
            <Alert
              variant="warning"
              className="rounded-xl border-amber-200 bg-amber-50 text-amber-800"
            >
              <AlertTriangle className="h-4 w-4 text-amber-600" />
              <AlertTitle className="text-amber-800">
                Cảnh báo phạm vi công nhận
              </AlertTitle>
              <AlertDescription className="text-amber-700">
                Các chỉ tiêu sau{" "}
                <strong>chưa được công nhận</strong> bởi đơn vị "
                {selectedTestingUnit?.name ?? "đã chọn"}":
                <ul className="mt-2 list-disc pl-4">
                  {nonAccreditedCriteria.map((c) => (
                    <li key={c.criteriaId}>
                      <span className="font-mono">{c.code}</span> – {c.name}
                    </li>
                  ))}
                </ul>
                <p className="mt-2 text-amber-600">
                  Bạn vẫn có thể tạo yêu cầu, nhưng kết quả sẽ không được
                  tự động công nhận bởi đơn vị này.
                </p>
                {scopeLoading && (
                  <p className="mt-1 flex items-center gap-1 text-amber-600">
                    <LoaderCircle className="h-3 w-3 animate-spin" />
                    Đang tải phạm vi công nhận...
                  </p>
                )}
              </AlertDescription>
            </Alert>
          )}


          {/* CARD 2: Thông tin gửi mẫu & Đơn vị kiểm nghiệm */}
          <Card className="rounded-xl border-slate-200 bg-white shadow-sm overflow-hidden">
            <CardHeader className="border-b border-border bg-muted/40 p-5">
              <div className="flex items-center gap-2.5">
                <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-100 text-blue-700">
                  <ClipboardCheck className="h-5 w-5" />
                </div>
                <div>
                  <CardTitle className="text-base font-semibold text-foreground">
                    2. Thông tin gửi mẫu & Đơn vị phòng kiểm nghiệm
                  </CardTitle>
                  <CardDescription className="text-xs text-muted-foreground mt-0.5">
                    Thông tin phòng Lab tiếp nhận, ngày bàn giao và các ghi chú gửi mẫu
                  </CardDescription>
                </div>
              </div>
            </CardHeader>

            <CardContent className="p-5 space-y-4">
              {/* Đơn vị kiểm nghiệm (Phòng Lab) - danh mục dùng chung NCL-11-CN-006 */}
              <div className="space-y-1.5">
                <Label htmlFor="testingUnit" className="text-xs font-semibold text-foreground">
                  Đơn vị phòng Lab tiếp nhận <span className="text-red-600">*</span>
                </Label>

                {useUnitCatalog ? (
                  <>
                    <TestingUnitSelect
                      id="testingUnit"
                      units={availableUnits}
                      value={testingUnitId}
                      onChange={(unit) => {
                        setTestingUnitId(unit?.id || "");
                        setTouched((prev) => ({ ...prev, testingUnit: true }));
                      }}
                      invalid={touched.testingUnit && !isTestingUnitValid}
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
                      <div className="flex h-10 items-center gap-2 rounded-xl border border-input bg-white px-3 text-xs text-muted-foreground">
                        <LoaderCircle className="h-3.5 w-3.5 animate-spin" />
                        Đang tải danh mục đơn vị kiểm nghiệm...
                      </div>
                    ) : (
                      <>
                        <p className="text-xs font-medium text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-2.5 py-1.5">
                          Danh mục đơn vị kiểm nghiệm chưa có dữ liệu. Vui lòng liên hệ
                          Quản trị viên, hoặc nhập tên đơn vị tạm thời.
                        </p>
                        <Input
                          id="testingUnit"
                          value={testingUnit}
                          onChange={(e) => {
                            setTestingUnit(e.target.value);
                            setTouched((prev) => ({ ...prev, testingUnit: true }));
                          }}
                          placeholder="Nhập tên phòng thí nghiệm hoặc đơn vị kiểm nghiệm..."
                          className="h-10 rounded-xl text-xs border-input"
                          maxLength={200}
                        />
                      </>
                    )}
                  </>
                )}
                {touched.testingUnit && !isTestingUnitValid && (
                  <p className="text-xs font-medium text-red-600 flex items-center gap-1">
                    <AlertCircle className="h-3.5 w-3.5" />{" "}
                    {useUnitCatalog
                      ? "Vui lòng chọn đơn vị phòng Lab kiểm nghiệm từ danh mục."
                      : "Vui lòng nhập tên đơn vị phòng Lab kiểm nghiệm."}
                  </p>
                )}
              </div>

              {/* Ngày gửi mẫu */}
              <div className="space-y-1.5">
                <Label htmlFor="sampleSentDate" className="text-xs font-semibold text-foreground">
                  Ngày gửi mẫu <span className="text-red-600">*</span>
                </Label>
                <Input
                  id="sampleSentDate"
                  type="date"
                  max={today}
                  value={sampleSentDate}
                  onChange={(e) => {
                    setSampleSentDate(e.target.value);
                    setTouched((prev) => ({ ...prev, sampleSentDate: true }));
                  }}
                  className="h-10 rounded-xl text-xs border-input max-w-sm"
                />
                <p className="text-xs text-muted-foreground">
                  Ngày gửi mẫu không được lớn hơn ngày hiện tại ({today}).
                </p>
                {touched.sampleSentDate && !isSampleDateValid && (
                  <p className="text-xs font-medium text-red-600 flex items-center gap-1">
                    <AlertCircle className="h-3.5 w-3.5" /> Ngày gửi mẫu không hợp lệ hoặc lớn hơn ngày hiện tại.
                  </p>
                )}
              </div>

              {/* Ghi chú bảo quản & yêu cầu đặc biệt */}
              <div className="space-y-1.5">
                <Label htmlFor="notes" className="text-xs font-semibold text-foreground">
                  Ghi chú bảo quản & Yêu cầu phân tích bổ sung
                </Label>
                <Textarea
                  id="notes"
                  rows={3}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Ghi chú điều kiện nhiệt độ bảo quản mẫu, hạn thử nghiệm, yêu cầu trả kết quả gấp..."
                  className="rounded-xl text-xs border-input resize-none"
                />
              </div>

              {/* Khối lượng mẫu gửi */}
              <div className="space-y-1.5">
                <Label htmlFor="sampleWeight" className="text-xs font-semibold text-foreground">
                  Khối lượng mẫu gửi
                </Label>
                <Input
                  id="sampleWeight"
                  value={sampleWeight}
                  onChange={(e) => setSampleWeight(e.target.value)}
                  placeholder="Ví dụ: 2 kg / 500 g / 1 lít..."
                  className="h-10 rounded-xl text-xs border-input max-w-sm"
                  maxLength={100}
                />
                <p className="text-xs text-muted-foreground">
                  Nhập khối lượng hoặc dung tích mẫu thực tế bàn giao cho phòng Lab.
                </p>
              </div>

              {/* Phương thức bàn giao mẫu */}
              <div className="space-y-1.5">
                <Label className="text-xs font-semibold text-foreground">
                  Phương thức bàn giao mẫu
                </Label>
                <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
                  {[
                    { id: "LAB_PICKUP", label: "Lab đến lấy mẫu" },
                    { id: "COURIER", label: "Gửi qua chuyển phát" },
                    { id: "SELF_DELIVERY", label: "Tự mang đến Lab" },
                  ].map((m) => (
                    <label
                      key={m.id}
                      className={`flex cursor-pointer items-center gap-2 rounded-xl border px-3 py-2.5 text-xs transition-all ${
                        deliveryMethod === m.id
                          ? "border-emerald-400 bg-emerald-50 text-emerald-900"
                          : "border-input bg-white text-muted-foreground hover:border-emerald-300"
                      }`}
                    >
                      <input
                        type="radio"
                        name="deliveryMethod"
                        className="h-3.5 w-3.5 accent-emerald-600"
                        checked={deliveryMethod === m.id}
                        onChange={() => setDeliveryMethod(m.id as "LAB_PICKUP" | "COURIER" | "SELF_DELIVERY")}
                      />
                      {m.label}
                    </label>
                  ))}
                </div>
              </div>

              {/* Tệp đính kèm (phiếu yêu cầu, ảnh mẫu...) */}
              <div className="space-y-1.5">
                <Label htmlFor="attachedFiles" className="text-xs font-semibold text-foreground">
                  Tệp đính kèm
                </Label>
                <label
                  htmlFor="attachedFiles"
                  className="flex cursor-pointer items-center justify-center gap-2 rounded-xl border border-dashed border-emerald-300 bg-emerald-50/40 px-3 py-3 text-xs font-medium text-emerald-700 hover:bg-emerald-50 transition-colors"
                >
                  <Upload className="h-4 w-4" />
                  Chọn tệp đính kèm (phiếu yêu cầu, ảnh mẫu...)
                  <input
                    id="attachedFiles"
                    type="file"
                    multiple
                    className="hidden"
                    onChange={handleFileChange}
                  />
                </label>
                {attachedFiles.length > 0 && (
                  <div className="space-y-1.5">
                    <p className="text-xs text-muted-foreground">
                      Tệp đính kèm đã chọn ({attachedFiles.length}):
                    </p>
                    {attachedFiles.map((item) => (
                      <div
                        key={item.id}
                        className="flex items-center justify-between gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2"
                      >
                        <div className="flex items-center gap-2.5 min-w-0">
                          <FileText className="h-4 w-4 text-emerald-600 shrink-0" />
                          <div className="truncate">
                            <p className="font-medium text-foreground truncate">{item.name}</p>
                            <p className="text-xs text-muted-foreground">{formatFileSize(item.size)}</p>
                          </div>
                        </div>
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={() => handleRemoveFile(item.id)}
                          className="h-8 w-8 p-0 text-muted-foreground hover:text-red-600 rounded-lg"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* CỘT PHẢI (30% - lg:col-span-4) - Sidebar Thông Tin & Quy Trình */}
        <div className="space-y-6 lg:col-span-4">
          {/* SIDEBAR CARD 1: Đối soát thông tin lô sản xuất */}
          <Card className="rounded-xl border-slate-200 bg-white shadow-sm overflow-hidden">
            <CardHeader className="border-b border-border bg-muted/40 p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Package className="h-4 w-4 text-emerald-700" />
                  <CardTitle className="text-sm font-semibold text-foreground">
                    Đối soát lô sản xuất
                  </CardTitle>
                </div>
                <Link
                  to={`/production-lots/${effectiveLotId}`}
                  target="_blank"
                  className="flex items-center gap-1 text-xs font-medium text-emerald-700 hover:underline"
                >
                  Chi tiết <ExternalLink className="h-3 w-3" />
                </Link>
              </div>
            </CardHeader>

            <CardContent className="p-4 space-y-3 text-xs">
              <div className="flex justify-between items-start border-b border-border pb-2">
                <span className="text-muted-foreground">Tên lô sản xuất:</span>
                <span className="font-semibold text-foreground text-right max-w-[160px] truncate" title={lot.name}>
                  {lot.name}
                </span>
              </div>
              <div className="flex justify-between items-center border-b border-border pb-2">
                <span className="text-muted-foreground">Mã lô:</span>
                <span className="font-mono font-medium text-foreground">{lot.code || lot.id.slice(0, 8)}</span>
              </div>
              <div className="flex justify-between items-center border-b border-border pb-2">
                <span className="text-muted-foreground">Vùng trồng:</span>
                <span className="font-medium text-foreground">{lot.farmAreaName || "—"}</span>
              </div>
              <div className="flex justify-between items-center border-b border-border pb-2">
                <span className="text-muted-foreground">Loại nông sản:</span>
                <span className="font-medium text-foreground">{lot.productCategoryName || "—"}</span>
              </div>
              <div className="flex justify-between items-center border-b border-border pb-2">
                <span className="text-muted-foreground">Sản lượng dự kiến:</span>
                <span className="font-medium text-foreground">
                  {lot.expectedQuantity ? `${lot.expectedQuantity.toLocaleString()} ${lot.expectedQuantityUnit || "kg"}` : "—"}
                </span>
              </div>
              <div className="flex justify-between items-center pt-1">
                <span className="text-muted-foreground">Trạng thái lô:</span>
                <Badge
                  variant="outline"
                  className="border-emerald-300 bg-emerald-50 text-xs font-semibold text-emerald-800"
                >
                  {lot.status || "APPROVED"}
                </Badge>
              </div>
            </CardContent>
          </Card>

          {/* SIDEBAR CARD 2: Pipeline 4 bước quy trình kiểm nghiệm */}
          <Card className="rounded-xl border-slate-200 bg-white shadow-sm overflow-hidden">
            <CardHeader className="border-b border-border bg-muted/40 p-4">
              <div className="flex items-center gap-2">
                <Clock className="h-4 w-4 text-emerald-700" />
                <CardTitle className="text-sm font-semibold text-foreground">
                  Tiến trình kiểm nghiệm (Pipeline)
                </CardTitle>
              </div>
            </CardHeader>

            <CardContent className="p-4 space-y-4">
              <div className="relative pl-6 space-y-5 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-border">
                {/* Bước 1: Đang thực hiện */}
                <div className="relative">
                  <div className="absolute -left-6 top-0 flex h-5 w-5 items-center justify-center rounded-full bg-emerald-600 text-white shadow-xs">
                    <span className="text-xs font-bold">1</span>
                  </div>
                  <div>
                    <p className="text-xs font-bold text-emerald-800">1. Tạo yêu cầu kiểm nghiệm</p>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      Lập danh sách chỉ tiêu & thông tin gửi mẫu (Đang thao tác)
                    </p>
                  </div>
                </div>

                {/* Bước 2: Chờ tiếp nhận */}
                <div className="relative">
                  <div className="absolute -left-6 top-0 flex h-5 w-5 items-center justify-center rounded-full border border-input bg-white text-muted-foreground">
                    <span className="text-xs font-medium">2</span>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-foreground">2. Bàn giao mẫu & Phòng Lab tiếp nhận</p>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      Chuyển mẫu đến Lab phân tích chất lượng
                    </p>
                  </div>
                </div>

                {/* Bước 3: Phân tích */}
                <div className="relative">
                  <div className="absolute -left-6 top-0 flex h-5 w-5 items-center justify-center rounded-full border border-input bg-white text-muted-foreground">
                    <span className="text-xs font-medium">3</span>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-foreground">3. Phân tích & Ghi nhận kết quả</p>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      Nhập kết luận Đạt/Không đạt cho từng chỉ tiêu
                    </p>
                  </div>
                </div>

                {/* Bước 4: Hoàn tất */}
                <div className="relative">
                  <div className="absolute -left-6 top-0 flex h-5 w-5 items-center justify-center rounded-full border border-input bg-white text-muted-foreground">
                    <span className="text-xs font-medium">4</span>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-foreground">4. Hoàn tất & Cấp mã QR</p>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      Đồng bộ kết quả vào chuỗi truy xuất nguồn gốc
                    </p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* SECTION 2: STICKY BOTTOM ACTION BAR */}
      <div className="sticky bottom-4 z-20 flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white/95 p-4 shadow-lg backdrop-blur-md sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-100 text-emerald-800 font-bold text-xs">
            {selectedCount}
          </div>
          <div>
            <p className="text-xs font-semibold text-foreground">
              {selectedCount > 0
                ? `Đã chọn ${selectedCount} chỉ tiêu cho yêu cầu hiện tại`
                : "Chưa chọn chỉ tiêu nào cho yêu cầu hiện tại"}
            </p>
            <p className="text-xs text-muted-foreground">
              {createdCriteriaCount}/{totalCriteriaCount} chỉ tiêu đã tạo yêu cầu kiểm nghiệm ·{" "}
              {testingUnit.trim()
                ? `Đơn vị: ${testingUnit.trim()}`
                : "Chưa nhập đơn vị kiểm nghiệm"}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 self-end sm:self-auto">
          <Button
            type="button"
            variant="ghost"
            onClick={handleClearDraft}
            disabled={submitting}
            className="rounded-xl text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-muted/50"
          >
            Làm mới form
          </Button>

          <Button
            type="button"
            variant="outline"
            onClick={handleSaveDraft}
            disabled={submitting}
            className="rounded-xl border-emerald-200 text-xs font-medium text-emerald-800 bg-emerald-50/50 hover:bg-emerald-100/60"
          >
            <Save className="mr-1.5 h-3.5 w-3.5 text-emerald-700" /> Lưu bản nháp
          </Button>

          <Button
            type="submit"
            variant="create"
            disabled={!canSubmit}
            className="rounded-xl text-xs font-semibold px-5 shadow-xs"
          >
            {submitting ? (
              <>
                <LoaderCircle className="mr-1.5 h-3.5 w-3.5 animate-spin" /> Đang tạo...
              </>
            ) : (
              <>
                <Send className="mr-1.5 h-3.5 w-3.5" /> Tạo yêu cầu kiểm nghiệm
              </>
            )}
          </Button>
        </div>
      </div>

      {/* SECTION 3: ALERT DIALOG XÁC NHẬN TẠO TRÙNG LẶP (HTTP 409) */}
      <AlertDialog
        open={duplicateOpen}
        onOpenChange={(open) => {
          if (!open && !submitting) setDuplicateOpen(false);
        }}
      >
        <AlertDialogPopup>
          <AlertDialogHeader>
            <div className="flex items-center gap-2.5 text-amber-600 mb-1">
              <AlertTriangle className="h-5 w-5" />
              <AlertDialogTitle className="text-base font-bold text-foreground">
                Yêu cầu kiểm nghiệm trùng lặp
              </AlertDialogTitle>
            </div>
            <AlertDialogDescription className="text-xs text-muted-foreground leading-relaxed">
              {duplicateMessage ||
                "Hệ thống phát hiện lô sản xuất này đã có yêu cầu kiểm nghiệm đang chờ kết quả cho cùng bộ chỉ tiêu. Bạn có chắc chắn muốn tạo thêm yêu cầu mới không?"}
              {/* NCL-11-CN-006 Phase 2: nhắc lại cảnh báo phạm vi công nhận khi xác nhận tạo trùng */}
              {nonAccreditedCriteria.length > 0 && (
                <div className="mt-3 rounded-lg border border-amber-200 bg-amber-50 p-3 text-amber-800">
                  <p className="font-semibold text-amber-800">
                    ⚠️ Lưu ý phạm vi công nhận
                  </p>
                  <p className="mt-1 text-amber-700">
                    {nonAccreditedCriteria.length} chỉ tiêu đang chọn chưa
                    được công nhận bởi đơn vị "
                    {selectedTestingUnit?.name ?? "đã chọn"}".
                    Kết quả sẽ không được tự động công nhận.
                  </p>
                </div>
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setDuplicateOpen(false)}
              disabled={submitting}
              className="rounded-xl text-xs"
            >
              Quay lại chỉnh sửa
            </Button>
            <Button
              type="button"
              variant="create"
              onClick={() => {
                setDuplicateOpen(false);
                void handleSubmit(true);
              }}
              disabled={submitting}
              className="rounded-xl text-xs font-semibold"
            >
              {submitting ? "Đang gửi..." : "Vẫn tạo yêu cầu"}
            </Button>
          </AlertDialogFooter>
        </AlertDialogPopup>
      </AlertDialog>
    </form>
  );
};

export default CreateInspectionRequestPage;
