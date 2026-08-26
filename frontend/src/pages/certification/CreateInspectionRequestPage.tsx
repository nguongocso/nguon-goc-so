import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import axios from "axios";
import { toast } from "sonner";
import {
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  Check,
  ChevronLeft,
  ChevronRight,
  ClipboardCheck,
  Clock,
  ExternalLink,
  FlaskConical,
  Info,
  Layers,
  LoaderCircle,
  Package,
  RotateCw,
  Save,
  Search,
  Send,
  X,
} from "lucide-react";

import {
  createInspectionRequest,
  getLotTestCriteria,
} from "@/api/certificationApi";
import { getProductionLotById } from "@/api/productionLotApi";
import type { LotTestCriteriaResult } from "@/types/certification";
import type { ProductionLot } from "@/types/productionLot";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  AlertDialog,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogPopup,
  AlertDialogTitle,
  AlertDialogDescription,
} from "@/components/ui/alert-dialog";
import { HelpButton } from "@/components/help/HelpButton";

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
  const [criteriaData, setCriteriaData] = useState<LotTestCriteriaResult | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  // --- Form State ---
  const [testingUnit, setTestingUnit] = useState("");
  const [sampleSentDate, setSampleSentDate] = useState(today);
  const [notes, setNotes] = useState("");
  const [selectedCriteriaIds, setSelectedCriteriaIds] = useState<number[]>([]);

  // Search & Filter trong danh sách chỉ tiêu
  const [criteriaSearch, setCriteriaSearch] = useState("");
  const [criteriaFilter, setCriteriaFilter] = useState<"ALL" | "SELECTED" | "UNSELECTED">("ALL");
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
      const [lotRes, criteriaRes] = await Promise.all([
        getProductionLotById(effectiveLotId),
        getLotTestCriteria(effectiveLotId),
      ]);

      setLot(lotRes);
      setCriteriaData(criteriaRes);

      // Kiểm tra xem có bản nháp nào đã lưu trước đó không
      const savedDraft = localStorage.getItem(draftStorageKey);
      if (savedDraft) {
        try {
          const parsed = JSON.parse(savedDraft);
          if (parsed.testingUnit) setTestingUnit(parsed.testingUnit);
          if (parsed.sampleSentDate) setSampleSentDate(parsed.sampleSentDate);
          if (parsed.notes) setNotes(parsed.notes);
          if (Array.isArray(parsed.selectedCriteriaIds)) {
            setSelectedCriteriaIds(parsed.selectedCriteriaIds);
          }
          toast.info("Đã khôi phục bản nháp chưa gửi của lô sản xuất này.", {
            duration: 4000,
          });
        } catch {
          // ignore corrupted draft
        }
      } else {
        // Mặc định chọn toàn bộ chỉ tiêu của tiêu chuẩn
        if (criteriaRes?.criteria?.length > 0) {
          setSelectedCriteriaIds(criteriaRes.criteria.map((c) => c.criteriaId));
        }
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
  }, [effectiveLotId, draftStorageKey]);

  useEffect(() => {
    void loadInitialData();
  }, [loadInitialData]);

  // --- Filter danh sách chỉ tiêu ---
  const filteredCriteria = useMemo(() => {
    if (!criteriaData?.criteria) return [];

    return criteriaData.criteria.filter((item) => {
      const matchSearch =
        criteriaSearch === "" ||
        item.name.toLowerCase().includes(criteriaSearch.toLowerCase()) ||
        item.code.toLowerCase().includes(criteriaSearch.toLowerCase());

      const isSelected = selectedCriteriaIds.includes(item.criteriaId);
      if (criteriaFilter === "SELECTED") return matchSearch && isSelected;
      if (criteriaFilter === "UNSELECTED") return matchSearch && !isSelected;
      return matchSearch;
    });
  }, [criteriaData, criteriaSearch, criteriaFilter, selectedCriteriaIds]);

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

  // --- Xử lý chọn chỉ tiêu ---
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
    if (!criteriaData?.criteria) return;
    setTouched((prev) => ({ ...prev, criteria: true }));
    setSelectedCriteriaIds(criteriaData.criteria.map((c) => c.criteriaId));
  };

  const handleDeselectAllCriteria = () => {
    setTouched((prev) => ({ ...prev, criteria: true }));
    setSelectedCriteriaIds([]);
  };

  // --- Xử lý Lưu bản nháp (Draft) ---
  const handleSaveDraft = () => {
    const draftData = {
      testingUnit,
      sampleSentDate,
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
    setSampleSentDate(today);
    setNotes("");
    if (criteriaData?.criteria) {
      setSelectedCriteriaIds(criteriaData.criteria.map((c) => c.criteriaId));
    } else {
      setSelectedCriteriaIds([]);
    }
    setTouched({ testingUnit: false, sampleSentDate: false, criteria: false });
    toast.info("Đã xóa trắng form và làm mới bản nháp.");
  };

  // --- Validation Form ---
  const trimmedTestingUnit = testingUnit.trim();
  const isSampleDateValid = sampleSentDate !== "" && sampleSentDate <= today;
  const isTestingUnitValid = trimmedTestingUnit !== "";
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
    setTouched({ testingUnit: true, sampleSentDate: true, criteria: true });

    if (!canSubmit && !confirmDuplicate) {
      if (!isTestingUnitValid) toast.error("Vui lòng nhập đơn vị phòng kiểm nghiệm.");
      else if (!isSampleDateValid) toast.error("Ngày gửi mẫu không được lớn hơn ngày hiện tại.");
      else if (!isCriteriaSelectedValid) toast.error("Vui lòng chọn ít nhất một chỉ tiêu kiểm nghiệm.");
      return;
    }

    setSubmitting(true);
    try {
      await createInspectionRequest(effectiveLotId, {
        testingUnit: trimmedTestingUnit,
        sampleSentDate,
        criteriaIds: selectedCriteriaIds,
        confirmDuplicate,
      });

      // Xóa bản nháp sau khi gửi thành công
      localStorage.removeItem(draftStorageKey);

      toast.success("Tạo yêu cầu kiểm nghiệm thành công!", {
        description: `Đã gửi yêu cầu cho lô ${lot?.name || effectiveLotId} tới ${trimmedTestingUnit}.`,
      });

      // Chuyển hướng về tab kiểm nghiệm của lô sản xuất
      navigate(`/production-lots/${effectiveLotId}?tab=inspection`);
    } catch (error: unknown) {
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

  const totalCriteriaCount = criteriaData?.criteria?.length || 0;
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
        <div className="flex items-center gap-2">
          <HelpButton screenKey="inspection-request-create" />
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={loadInitialData}
            disabled={isLoading}
          >
            <RotateCw className={`h-4 w-4 mr-1 ${isLoading ? "animate-spin" : ""}`} />
            Tải lại
          </Button>
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
                    Đã chọn: {selectedCount}/{totalCriteriaCount} chỉ tiêu
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

                  {/* Filter Pills */}
                  <div className="inline-flex items-center rounded-xl border border-slate-200 bg-white p-1">
                    <button
                      type="button"
                      onClick={() => setCriteriaFilter("ALL")}
                      className={`rounded-lg px-3.5 py-1.5 text-sm transition-all ${
                        criteriaFilter === "ALL"
                          ? "border border-primary bg-white font-semibold text-primary shadow-2xs"
                          : "border border-transparent font-medium text-foreground/60 hover:text-foreground"
                      }`}
                    >
                      Tất cả
                    </button>
                    <button
                      type="button"
                      onClick={() => setCriteriaFilter("SELECTED")}
                      className={`rounded-lg px-3.5 py-1.5 text-sm transition-all ${
                        criteriaFilter === "SELECTED"
                          ? "border border-primary bg-white font-semibold text-primary shadow-2xs"
                          : "border border-transparent font-medium text-foreground/60 hover:text-foreground"
                      }`}
                    >
                      Đã chọn ({selectedCount})
                    </button>
                    <button
                      type="button"
                      onClick={() => setCriteriaFilter("UNSELECTED")}
                      className={`rounded-lg px-3.5 py-1.5 text-sm transition-all ${
                        criteriaFilter === "UNSELECTED"
                          ? "border border-primary bg-white font-semibold text-primary shadow-2xs"
                          : "border border-transparent font-medium text-foreground/60 hover:text-foreground"
                      }`}
                    >
                      Chưa chọn ({totalCriteriaCount - selectedCount})
                    </button>
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
                  Không tìm thấy chỉ tiêu nào phù hợp với từ khóa "{criteriaSearch}".
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
                    {paginatedCriteria.map((criterion) => {
                      const isChecked = selectedCriteriaIds.includes(criterion.criteriaId);
                      return (
                        <div
                          key={criterion.criteriaId}
                          onClick={() => toggleCriterion(criterion.criteriaId, !isChecked)}
                          className={`flex cursor-pointer items-start gap-3 rounded-xl border p-3.5 transition-all ${
                            isChecked
                              ? "border-emerald-500 bg-emerald-50/40 shadow-2xs"
                              : "border-border bg-white hover:border-input hover:bg-muted/40"
                          }`}
                        >
                          <Checkbox
                            id={`criterion-${criterion.criteriaId}`}
                            checked={isChecked}
                            onCheckedChange={(c) =>
                              toggleCriterion(criterion.criteriaId, c === true)
                            }
                            className="mt-0.5"
                          />
                          <div className="min-w-0 flex-1">
                            <Label
                              htmlFor={`criterion-${criterion.criteriaId}`}
                              className="cursor-pointer text-xs font-semibold text-foreground leading-snug"
                            >
                              {criterion.name}
                            </Label>
                            <p className="font-mono text-xs text-muted-foreground mt-0.5">
                              Mã: {criterion.code}
                            </p>
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {/* Thanh phân trang chỉ tiêu */}
                  {totalCriteriaPages > 1 && (
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pt-3 border-t border-border text-xs text-muted-foreground">
                      <div>
                        Hiển thị {(criteriaPage - 1) * CRITERIA_PER_PAGE + 1} -{" "}
                        {Math.min(criteriaPage * CRITERIA_PER_PAGE, filteredCriteria.length)} trong tổng số{" "}
                        <span className="font-semibold text-foreground">{filteredCriteria.length}</span> chỉ tiêu
                      </div>
                      <div className="flex items-center gap-1 self-end sm:self-auto">
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          disabled={criteriaPage <= 1}
                          onClick={() => setCriteriaPage((p) => Math.max(1, p - 1))}
                          className="h-8 px-2.5 rounded-lg text-xs"
                        >
                          <ChevronLeft className="h-3.5 w-3.5 mr-1" /> Trước
                        </Button>
                        <div className="flex items-center gap-1 px-1">
                          {Array.from({ length: totalCriteriaPages }, (_, i) => i + 1).map((p) => (
                            <button
                              key={p}
                              type="button"
                              onClick={() => setCriteriaPage(p)}
                              className={`h-8 min-w-[32px] px-2 rounded-lg text-xs font-medium transition-colors ${
                                criteriaPage === p
                                  ? "bg-emerald-600 text-white shadow-xs"
                                  : "bg-white border border-border text-foreground hover:bg-muted/50"
                              }`}
                            >
                              {p}
                            </button>
                          ))}
                        </div>
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          disabled={criteriaPage >= totalCriteriaPages}
                          onClick={() => setCriteriaPage((p) => Math.min(totalCriteriaPages, p + 1))}
                          className="h-8 px-2.5 rounded-lg text-xs"
                        >
                          Sau <ChevronRight className="h-3.5 w-3.5 ml-1" />
                        </Button>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {touched.criteria && selectedCount === 0 && (
                <p className="text-xs font-medium text-red-600 flex items-center gap-1 mt-1">
                  <AlertCircle className="h-3.5 w-3.5" /> Bắt buộc phải chọn ít nhất một chỉ tiêu kiểm nghiệm.
                </p>
              )}
            </CardContent>
          </Card>

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
              {/* Đơn vị kiểm nghiệm (Phòng Lab) */}
              <div className="space-y-1.5">
                <Label htmlFor="testingUnit" className="text-xs font-semibold text-foreground">
                  Đơn vị phòng Lab tiếp nhận <span className="text-red-600">*</span>
                </Label>
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
                {touched.testingUnit && !isTestingUnitValid && (
                  <p className="text-xs font-medium text-red-600 flex items-center gap-1">
                    <AlertCircle className="h-3.5 w-3.5" /> Vui lòng nhập tên đơn vị phòng Lab kiểm nghiệm.
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
                ? `Đã chọn ${selectedCount}/${totalCriteriaCount} chỉ tiêu kiểm nghiệm`
                : "Chưa chọn chỉ tiêu nào"}
            </p>
            <p className="text-xs text-muted-foreground">
              {testingUnit.trim() ? `Đơn vị: ${testingUnit.trim()}` : "Chưa nhập đơn vị kiểm nghiệm"}
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
