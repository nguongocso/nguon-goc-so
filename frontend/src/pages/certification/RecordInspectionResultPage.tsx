import React, {useEffect, useMemo, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import axios from "axios";
import {toast} from "sonner";
import {useSetBreadcrumb} from "@/components/common/AppBreadcrumb";
import {
    AlertTriangle,
    Calendar,
    Check,
    CheckCircle2,
    ChevronLeft,
    ChevronRight,
    ClipboardCheck,
    Eye,
    FileCheck2,
    FileText,
    FileUp,
    Info,
    LoaderCircle,
    RotateCw,
    Search,
    Save,
    ShieldAlert,
    ShieldCheck,
    Sparkles,
    X,
    XCircle,
} from "lucide-react";

import {
    getInspectionRequestDetail,
    recordInspectionRequestResults,
    uploadInspectionResultFile,
} from "@/api/certificationApi";
import {getProductionLotById} from "@/api/productionLotApi";
import type {
    InspectionRequestDetailResponse,
    InspectionRequestDetailCriterion,
} from "@/types/certification";
import type {ProductionLot} from "@/types/productionLot";

import {Badge} from "@/components/ui/badge";
import {Button} from "@/components/ui/button";
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {HelpButton} from "@/components/help/HelpButton";

const toISODate = (date: Date): string => {
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${date.getFullYear()}-${month}-${day}`;
};

const addYearsToISO = (dateStr: string, years: number): string => {
    try {
        const d = new Date(dateStr + "T00:00:00");
        d.setFullYear(d.getFullYear() + years);
        return toISODate(d);
    } catch {
        return "";
    }
};

interface CriterionRowState {
    criterionId: string;
    code: string;
    name: string;
    standardName: string | null;
    passed: boolean | null;
    resultDate: string;
    expiryDate: string;
    filePath: string;
    selectedFileName: string;
    uploading: boolean;
}

type FilterTab = "ALL" | "UNSET" | "PASSED" | "FAILED";

export const RecordInspectionResultPage: React.FC = () => {
    const navigate = useNavigate();
    const {lotId: routeLotId, requestId} = useParams<{
        lotId?: string;
        requestId: string;
    }>();

    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState<string | null>(null);

    const [detail, setDetail] = useState<InspectionRequestDetailResponse | null>(null);
    const [lot, setLot] = useState<ProductionLot | null>(null);

    // ── Breadcrumb điều hướng thống nhất (thay nút "Quay lại") ────────────────
    useSetBreadcrumb([
        {label: "Tổng quan", href: "/dashboard"},
        {label: "Lô sản xuất", href: "/production-lots"},
        ...(routeLotId || lot?.id
            ? [
                {
                    label: lot?.name || "Chi tiết lô",
                    href: `/production-lots/${routeLotId || lot?.id}`,
                },
            ]
            : []),
        {label: "Kết quả kiểm nghiệm"},
    ]);

    const [rows, setRows] = useState<CriterionRowState[]>([]);
    const [submitting, setSubmitting] = useState(false);
    const [touched, setTouched] = useState(false);

    // Bulk date fields
    const today = useMemo(() => toISODate(new Date()), []);
    const defaultNextYear = useMemo(() => addYearsToISO(today, 1), [today]);
    const [batchResultDate, setBatchResultDate] = useState<string>(today);
    const [batchExpiryDate, setBatchExpiryDate] = useState<string>(defaultNextYear);

    // Filter tab
    const [filterTab, setFilterTab] = useState<FilterTab>("ALL");
    const [searchText, setSearchText] = useState("");
    const [page, setPage] = useState(0);
    const pageSize = 10;

    // Load inspection request detail
    const loadData = async () => {
        if (!requestId) return;
        try {
            setLoading(true);
            setLoadError(null);

            const requestData = await getInspectionRequestDetail(requestId);
            setDetail(requestData);

            // Initialize rows
            const initialRows: CriterionRowState[] = requestData.criteria.map((c: InspectionRequestDetailCriterion) => ({
                criterionId: c.criterionId,
                code: c.code,
                name: c.name,
                standardName: c.standardName,
                passed: c.result?.passed ?? null,
                resultDate: c.result ? (c.result.resultDate ?? "") : today,
                expiryDate: c.result ? (c.result.expiryDate ?? "") : defaultNextYear,
                filePath: c.result?.filePath ?? "",
                selectedFileName: c.result?.filePath ? c.result.filePath.split("/").pop() || "phiếu-kết-quả" : "",
                uploading: false,
            }));
            setRows(initialRows);

            // Fetch lot info
            const effectiveLotId = routeLotId || requestData.lotId;
            if (effectiveLotId) {
                try {
                    const lotData = await getProductionLotById(effectiveLotId);
                    setLot(lotData);
                } catch {
                    // Lot fetch optional
                }
            }
        } catch (error) {
            setLoadError(
                axios.isAxiosError(error)
                    ? error.response?.data?.message || "Không thể tải chi tiết yêu cầu kiểm nghiệm"
                    : "Không thể tải chi tiết yêu cầu kiểm nghiệm"
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void loadData();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [requestId, routeLotId]);

    // Row validation errors
    const rowErrors = useMemo(() => {
        const errors: Record<string, { resultDate?: string; expiryDate?: string; passed?: string }> = {};
        for (const r of rows) {
            const err: { resultDate?: string; expiryDate?: string; passed?: string } = {};

            if (touched && r.passed === null) {
                err.passed = "Vui lòng chọn kết luận Đạt hoặc Không đạt.";
            }

            // Chỉ tiêu Không đạt không có hiệu lực thời gian nên không bắt buộc ngày.
            if (r.passed !== false) {
                if (!r.resultDate) {
                    err.resultDate = "Vui lòng chọn ngày cấp.";
                }

                if (!r.expiryDate) {
                    err.expiryDate = "Vui lòng chọn ngày hết hạn.";
                } else {
                    if (r.resultDate && r.expiryDate < r.resultDate) {
                        err.expiryDate = "Ngày hết hạn phải sau hoặc bằng ngày cấp.";
                    }
                    if (r.expiryDate < today) {
                        err.expiryDate = "Ngày hết hạn phải từ hôm nay trở đi.";
                    }
                }
            }

            if (Object.keys(err).length > 0) {
                errors[r.criterionId] = err;
            }
        }
        return errors;
    }, [rows, touched, today]);

    // Stats
    const totalCriteria = rows.length;
    const passedCount = rows.filter((r) => r.passed === true).length;
    const failedCount = rows.filter((r) => r.passed === false).length;
    const unsetCount = rows.filter((r) => r.passed === null).length;
    const filledCount = totalCriteria - unsetCount;
    const progressPercent = totalCriteria > 0 ? Math.round((filledCount / totalCriteria) * 100) : 0;

    const isAllAnswered = totalCriteria > 0 && unsetCount === 0;
    const hasNoDateErrors = Object.keys(rowErrors).every(
        (id) => !rowErrors[id].resultDate && !rowErrors[id].expiryDate
    );
    const isUploadingAny = rows.some((r) => r.uploading);
    const canSubmit = isAllAnswered && hasNoDateErrors && !isUploadingAny && !submitting;

    // Forecast state
    const willPassAll = isAllAnswered && failedCount === 0 && hasNoDateErrors;
    const willFail = failedCount > 0;

    // Handlers for individual rows
    const handleSetPassed = (criterionId: string, passed: boolean) => {
        setTouched(true);
        setRows((prev) =>
            prev.map((r) =>
                r.criterionId === criterionId
                    ? passed === false
                        ? // Chỉ tiêu Không đạt không có hiệu lực thời gian: xóa ngày và phiếu.
                        {...r, passed, resultDate: "", expiryDate: "", filePath: "", selectedFileName: ""}
                        : {...r, passed}
                    : r
            )
        );
    };

    const handleFieldChange = (
        criterionId: string,
        field: "resultDate" | "expiryDate",
        value: string
    ) => {
        setRows((prev) =>
            prev.map((r) => (r.criterionId === criterionId ? {...r, [field]: value} : r))
        );
    };

    const handleFileUpload = async (criterionId: string, file: File | null) => {
        if (!file) return;
        setRows((prev) =>
            prev.map((r) =>
                r.criterionId === criterionId
                    ? {...r, uploading: true, filePath: "", selectedFileName: file.name}
                    : r
            )
        );
        try {
            const res = await uploadInspectionResultFile(criterionId, file);
            setRows((prev) =>
                prev.map((r) =>
                    r.criterionId === criterionId
                        ? {...r, filePath: res.filePath, uploading: false}
                        : r
                )
            );
            toast.success(`Đã tải lên phiếu: ${file.name}`);
        } catch {
            setRows((prev) =>
                prev.map((r) =>
                    r.criterionId === criterionId
                        ? {...r, uploading: false, selectedFileName: ""}
                        : r
                )
            );
            toast.error("Không thể tải lên file phiếu kết quả.");
        }
    };

    // Bulk actions
    const handleSetAllPassed = () => {
        setTouched(true);
        setRows((prev) => prev.map((r) => ({...r, passed: true})));
        toast.info("Đã đánh dấu tất cả chỉ tiêu là ĐẠT.");
    };

    const handleSetAllFailed = () => {
        setTouched(true);
        // Tất cả chỉ tiêu đều Không đạt nên không cần ngày hiệu lực / phiếu kết quả.
        setRows((prev) =>
            prev.map((r) => ({
                ...r,
                passed: false,
                resultDate: "",
                expiryDate: "",
                filePath: "",
                selectedFileName: "",
            }))
        );
        toast.warning("Đã đánh dấu tất cả chỉ tiêu là KHÔNG ĐẠT.");
    };

    const handleApplyBatchDates = () => {
        if (!batchResultDate || !batchExpiryDate) {
            toast.error("Vui lòng chọn ngày cấp và ngày hết hạn hợp lệ để áp dụng.");
            return;
        }
        if (batchExpiryDate < batchResultDate) {
            toast.error("Ngày hết hạn phải sau hoặc bằng ngày cấp kết quả.");
            return;
        }
        setRows((prev) =>
            prev.map((r) =>
                // Chỉ tiêu Không đạt không có hiệu lực thời gian nên không áp dụng ngày.
                r.passed === false
                    ? r
                    : {...r, resultDate: batchResultDate, expiryDate: batchExpiryDate}
            )
        );
        toast.success("Đã áp dụng ngày đồng loạt cho các chỉ tiêu Đạt.");
    };

    // Submit all results
    const handleSubmit = async () => {
        setTouched(true);
        if (!canSubmit || !detail) {
            if (!isAllAnswered) {
                toast.error("Vui lòng chọn kết luận Đạt/Không đạt cho tất cả các chỉ tiêu.");
            } else if (!hasNoDateErrors) {
                toast.error("Vui lòng kiểm tra lại ngày cấp và ngày hết hạn của các chỉ tiêu.");
            }
            return;
        }

        setSubmitting(true);
        try {
            await recordInspectionRequestResults(detail.testRequestId, {
                results: rows.map((r) => {
                    const isFailed = r.passed === false;
                    // Chỉ tiêu Không đạt không có hiệu lực thời gian → gán null các trường này.
                    return {
                        criterionId: r.criterionId,
                        resultDate: isFailed ? null : r.resultDate || null,
                        expiryDate: isFailed ? null : r.expiryDate || null,
                        passed: r.passed as boolean,
                        filePath: isFailed ? null : r.filePath.trim() ? r.filePath : null,
                    };
                }),
            });

            toast.success("Ghi nhận kết quả kiểm nghiệm thành công!");

            const effectiveLotId = routeLotId || detail.lotId;
            if (effectiveLotId) {
                navigate(`/production-lots/${effectiveLotId}`);
            } else {
                navigate("/production-lots");
            }
        } catch (error) {
            const msg = axios.isAxiosError(error)
                ? error.response?.data?.message || "Lỗi khi lưu kết quả kiểm nghiệm"
                : "Lỗi không xác định";
            toast.error(`Không thể lưu kết quả kiểm nghiệm: ${msg}`);
        } finally {
            setSubmitting(false);
        }
    };

    // Filtered rows (bộ lọc trạng thái + từ khóa tìm kiếm trên client)
    const filteredRows = useMemo(() => {
        const tabFiltered = (() => {
            switch (filterTab) {
                case "UNSET":
                    return rows.filter((r) => r.passed === null);
                case "PASSED":
                    return rows.filter((r) => r.passed === true);
                case "FAILED":
                    return rows.filter((r) => r.passed === false);
                case "ALL":
                default:
                    return rows;
            }
        })();
        const q = searchText.trim().toLowerCase();
        if (!q) return tabFiltered;
        return tabFiltered.filter(
            (r) =>
                r.name.toLowerCase().includes(q) ||
                r.code.toLowerCase().includes(q) ||
                (r.standardName ?? "").toLowerCase().includes(q)
        );
    }, [rows, filterTab, searchText]);

    // Phân trang client-side (giống trang Khóa API bên thứ 3)
    const totalPages = Math.max(1, Math.ceil(filteredRows.length / pageSize));
    const paginatedRows = useMemo(
        () => filteredRows.slice(page * pageSize, page * pageSize + pageSize),
        [filteredRows, page, pageSize]
    );

    // Về trang đầu khi đổi bộ lọc trạng thái hoặc từ khóa tìm kiếm
    useEffect(() => {
        setPage(0);
    }, [filterTab, searchText]);

    const effectiveLotId = routeLotId || detail?.lotId;

    // Chế độ xem kết quả đã ghi nhận (chỉ đọc): khi yêu cầu đã có kết quả cuối
    // (Đạt / Không đạt / Đã hủy), không cho phép chỉnh sửa, chỉ xem ảnh chụp kết quả.
    const isReadOnly =
        detail?.status === "PASSED" ||
        detail?.status === "FAILED" ||
        detail?.status === "CANCELLED";

    const getStatusBadge = (status?: string) => {
        switch (status) {
            case "PASSED":
                return (
                    <Badge
                        className="border-emerald-300 bg-emerald-100 text-emerald-800 font-semibold px-3 py-1 rounded-full text-xs">
                        <CheckCircle2 className="mr-1.5 h-3.5 w-3.5 text-[#2E7D32]"/> Đạt chuẩn
                    </Badge>
                );
            case "FAILED":
                return (
                    <Badge
                        className="border-red-300 bg-red-100 text-red-800 font-semibold px-3 py-1 rounded-full text-xs">
                        <XCircle className="mr-1.5 h-3.5 w-3.5 text-red-600"/> Không đạt
                    </Badge>
                );
            case "CANCELLED":
                return (
                    <Badge
                        className="border-gray-300 bg-gray-100 text-gray-800 font-semibold px-3 py-1 rounded-full text-xs">
                        Đã hủy
                    </Badge>
                );
            case "PENDING":
            case "PENDING_RESULT":
            default:
                return (
                    <Badge
                        className="border-amber-300 bg-amber-100 text-amber-900 font-semibold px-3 py-1 rounded-full text-xs">
                        <LoaderCircle className="mr-1.5 h-3.5 w-3.5 animate-spin text-amber-700"/> Chờ kết quả
                    </Badge>
                );
        }
    };

    if (loading) {
        return (
            <div
                className="container mx-auto flex min-h-[60vh] flex-col items-center justify-center space-y-4 px-4 py-12">
                <LoaderCircle className="h-10 w-10 animate-spin text-[#2E7D32]"/>
                <p className="text-base font-medium text-slate-600">Đang tải thông tin yêu cầu kiểm nghiệm...</p>
            </div>
        );
    }

    if (loadError || !detail) {
        return (
            <div className="container mx-auto max-w-2xl px-4 py-12">
                <Card className="rounded-2xl border-red-200 bg-red-50/50 shadow-sm p-6">
                    <CardHeader className="p-0 pb-4">
                        <div className="flex items-center gap-3">
                            <ShieldAlert className="h-6 w-6 text-red-600"/>
                            <CardTitle className="text-red-900 text-lg">Không thể tải dữ liệu</CardTitle>
                        </div>
                        <CardDescription className="text-red-700 mt-1">
                            {loadError || "Không tìm thấy thông tin yêu cầu kiểm nghiệm tương ứng."}
                        </CardDescription>
                    </CardHeader>
                    <CardContent className="p-0 flex gap-3">
                        <Button variant="create" className="rounded-xl" onClick={() => void loadData()}>
                            <RotateCw className="mr-1.5 h-4 w-4"/> Thử lại
                        </Button>
                    </CardContent>
                </Card>
            </div>
        );
    }

    return (
        <div className="space-y-6 pb-28">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div className="flex items-center gap-3.5">
                    <div
                        className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-50 text-emerald-700 shadow-sm shrink-0">
                        <ShieldCheck className="h-6 w-6"/>
                    </div>
                    <div>
                        <div className="flex items-center gap-2">
                            <h1 className="text-xl font-bold tracking-tight text-slate-900 sm:text-2xl">
                                {isReadOnly
                                    ? "Xem kết quả kiểm nghiệm"
                                    : "Ghi nhận kết quả kiểm nghiệm"}
                            </h1>
                            {getStatusBadge(detail.status)}
                        </div>
                        <p className="text-xs text-muted-foreground sm:text-sm mt-0.5">
                            Yêu cầu: <span
                            className="font-mono font-semibold text-slate-700">#{detail.testRequestId.slice(0, 8)}</span>
                            {" • "}Lô sản xuất: <span
                            className="font-semibold text-slate-800">{lot?.name || detail.lotCode}</span>
                        </p>
                    </div>
                </div>

                <HelpButton screenKey="inspection-result-record"/>
            </div>

            {isReadOnly && (
                <div
                    className="flex flex-col gap-2 rounded-2xl border border-slate-200 bg-slate-100/80 px-4 py-3 text-sm text-slate-600 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex items-center gap-2.5">
                        <Eye className="h-4.5 w-4.5 text-slate-500"/>
                        <span>
              <strong className="font-semibold text-slate-700">
                Chế độ xem:
              </strong>{" "}
                            Kết quả cho yêu cầu này đã được ghi nhận và đóng băng. Không thể
              chỉnh sửa bất kỳ trường nào.
            </span>
                    </div>
                </div>
            )}

            {/* SECTION 1: Summary Information Cards */}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                {/* Card 1: Lô sản xuất */}
                <Card
                    className="rounded-2xl border-[#E5E7EB] bg-white shadow-[0_2px_8px_rgba(0,0,0,0.04)] hover:shadow-md transition-shadow">
                    <CardContent className="p-5">
                        <div className="flex items-center gap-3.5">
                            <div
                                className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-[#2E7D32] shrink-0">
                                <FileText className="h-5 w-5"/>
                            </div>
                            <div className="min-w-0 flex-1">
                                <p className="text-xs font-medium text-slate-500">Lô sản xuất</p>
                                <p className="truncate text-sm font-bold text-[#1F2937] mt-0.5"
                                   title={lot?.name || detail.lotCode}>
                                    {lot?.name || detail.lotCode}
                                </p>
                            </div>
                        </div>
                    </CardContent>
                </Card>

                {/* Card 2: Đơn vị kiểm nghiệm */}
                <Card
                    className="rounded-2xl border-[#E5E7EB] bg-white shadow-[0_2px_8px_rgba(0,0,0,0.04)] hover:shadow-md transition-shadow">
                    <CardContent className="p-5">
                        <div className="flex items-center gap-3.5">
                            <div
                                className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600 shrink-0">
                                <ClipboardCheck className="h-5 w-5"/>
                            </div>
                            <div className="min-w-0 flex-1">
                                <p className="text-xs font-medium text-slate-500">Đơn vị kiểm nghiệm</p>
                                <p className="truncate text-sm font-bold text-[#1F2937] mt-0.5"
                                   title={detail.testingUnit}>
                                    {detail.testingUnit}
                                </p>
                                <p className="text-xs text-slate-400 mt-0.5">Gửi mẫu: {detail.sampleSentDate}</p>
                            </div>
                        </div>
                    </CardContent>
                </Card>

                {/* Card 3: Số lượng chỉ tiêu */}
                <Card
                    className="rounded-2xl border-[#E5E7EB] bg-white shadow-[0_2px_8px_rgba(0,0,0,0.04)] hover:shadow-md transition-shadow">
                    <CardContent className="p-5">
                        <div className="flex items-center gap-3.5">
                            <div
                                className="flex h-10 w-10 items-center justify-center rounded-xl bg-purple-50 text-purple-600 shrink-0">
                                <FileCheck2 className="h-5 w-5"/>
                            </div>
                            <div className="min-w-0 flex-1">
                                <p className="text-xs font-medium text-slate-500">Tổng số chỉ tiêu</p>
                                <p className="text-sm font-bold text-[#1F2937] mt-0.5">
                                    {totalCriteria} chỉ tiêu
                                </p>
                                <p className="text-xs text-slate-400 mt-0.5 truncate"
                                   title={detail.criteria[0]?.standardName || "Tiêu chuẩn áp dụng"}>
                                    {detail.criteria[0]?.standardName || "Tiêu chuẩn áp dụng"}
                                </p>
                            </div>
                        </div>
                    </CardContent>
                </Card>

                {/* Card 4: Tiến độ nhập liệu */}
                <Card
                    className="rounded-2xl border-[#E5E7EB] bg-white shadow-[0_2px_8px_rgba(0,0,0,0.04)] hover:shadow-md transition-shadow">
                    <CardContent className="p-5">
                        <div className="space-y-2">
                            <div className="flex items-center justify-between text-xs">
                                <span className="font-medium text-slate-600">Tiến độ nhập</span>
                                <span
                                    className="font-bold text-[#2E7D32]">{filledCount}/{totalCriteria} ({progressPercent}%)</span>
                            </div>
                            {/* Progress bar */}
                            <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
                                <div
                                    className="h-full bg-[#2E7D32] transition-all duration-300 ease-out rounded-full"
                                    style={{width: `${progressPercent}%`}}
                                />
                            </div>
                            <div className="flex justify-between text-[11px] text-slate-500 pt-0.5">
                                <span className="text-emerald-700 font-medium">✓ Đạt: {passedCount}</span>
                                <span className="text-red-600 font-medium">✗ Không đạt: {failedCount}</span>
                                <span className="text-amber-600 font-medium">Chưa: {unsetCount}</span>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </div>

            {/* SECTION 2: Batch Actions Toolbar */}
            {!isReadOnly && (
                <Card className="rounded-2xl border-[#E5E7EB] bg-white shadow-[0_2px_8px_rgba(0,0,0,0.04)] p-5">
                    <div className="flex flex-wrap items-center justify-between gap-3 pb-3">
                        <div className="flex items-center gap-2">
                            <Sparkles className="h-5 w-5 text-amber-500"/>
                            <h3 className="text-base font-semibold text-[#1F2937]">
                                Thao tác nhanh cho hàng loạt chỉ tiêu
                            </h3>
                        </div>
                        <p className="text-xs text-slate-500">
                            Tiết kiệm thời gian khi toàn bộ chỉ tiêu có chung ngày cấp/hạn hoặc đồng loạt đạt chuẩn
                        </p>
                    </div>

                    <div
                        className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-100 bg-slate-50/80 p-4">
                        {/* Left: Quick Decision Buttons */}
                        <div className="flex flex-wrap items-center gap-2">
                            <span className="text-xs font-medium text-slate-700">Đánh dấu nhanh:</span>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={handleSetAllPassed}
                                className="rounded-xl border-emerald-600 text-emerald-700 hover:bg-emerald-50 hover:text-emerald-800 text-xs h-9 px-3.5 font-medium"
                            >
                                <Check className="mr-1.5 h-3.5 w-3.5 text-emerald-600"/> Đặt tất cả là ĐẠT
                            </Button>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={handleSetAllFailed}
                                className="rounded-xl border-red-400 text-red-700 hover:bg-red-50 hover:text-red-800 text-xs h-9 px-3.5 font-medium"
                            >
                                <X className="mr-1.5 h-3.5 w-3.5 text-red-600"/> Đặt tất cả KHÔNG ĐẠT
                            </Button>
                        </div>

                        {/* Right: Batch Date Application */}
                        <div className="flex flex-wrap items-center gap-2.5">
                            <div className="flex items-center gap-1.5">
                                <Label className="text-xs text-slate-600">Ngày cấp chung:</Label>
                                <Input
                                    type="date"
                                    value={batchResultDate}
                                    onChange={(e) => setBatchResultDate(e.target.value)}
                                    className="h-9 w-36 rounded-xl text-xs border-slate-300"
                                />
                            </div>
                            <div className="flex items-center gap-1.5">
                                <Label className="text-xs text-slate-600">Ngày hết hạn chung:</Label>
                                <Input
                                    type="date"
                                    value={batchExpiryDate}
                                    min={batchResultDate || today}
                                    onChange={(e) => setBatchExpiryDate(e.target.value)}
                                    className="h-9 w-36 rounded-xl text-xs border-slate-300"
                                />
                            </div>
                            <Button
                                type="button"
                                variant="create"
                                size="sm"
                                onClick={handleApplyBatchDates}
                                className="h-9 px-4 rounded-xl text-xs font-semibold"
                            >
                                <Calendar className="mr-1.5 h-3.5 w-3.5"/> Áp dụng ngày
                            </Button>
                        </div>
                    </div>
                </Card>
            )}

            {/* SECTION 3: Main Criteria Table Section */}
            <div className="space-y-3.5">
                {/* Header */}
                <div>
                    <h2 className="text-lg font-bold text-slate-900">
                        Danh sách chỉ tiêu kiểm nghiệm
                    </h2>
                    <p className="text-xs text-slate-500 mt-0.5">
                        Chọn kết luận Đạt/Không đạt và thiết lập ngày hiệu lực cho từng chỉ tiêu
                    </p>
                </div>

                {/* Filter Bar & Counter */}
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="flex flex-wrap items-center gap-3">
                        {/* Ô tìm kiếm chỉ tiêu */}
                        <div className="relative w-full sm:w-72">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground"/>
                            <Input
                                placeholder="Tìm theo tên chỉ tiêu, mã hoặc tiêu chuẩn..."
                                value={searchText}
                                onChange={(e) => setSearchText(e.target.value)}
                                className="pl-9 h-10 rounded-xl text-xs"
                            />
                        </div>
                        <div
                            className="inline-flex max-w-full overflow-x-auto items-center gap-1 rounded-2xl border border-emerald-100 bg-white/80 p-1 shadow-2xs backdrop-blur-sm">
                            <button
                                type="button"
                                onClick={() => setFilterTab("ALL")}
                                className={`rounded-xl px-4 py-2 text-sm font-medium transition-all ${
                                    filterTab === "ALL"
                                        ? "border border-emerald-700 bg-white text-emerald-800 shadow-2xs"
                                        : "border border-transparent text-slate-600 hover:text-slate-900"
                                }`}
                            >
                                Tất cả ({totalCriteria})
                            </button>
                            <button
                                type="button"
                                onClick={() => setFilterTab("UNSET")}
                                className={`rounded-xl px-4 py-2 text-sm font-medium transition-all ${
                                    filterTab === "UNSET"
                                        ? "border border-emerald-700 bg-white text-emerald-800 shadow-2xs"
                                        : "border border-transparent text-slate-600 hover:text-slate-900"
                                }`}
                            >
                                Chưa nhập ({unsetCount})
                            </button>
                            <button
                                type="button"
                                onClick={() => setFilterTab("PASSED")}
                                className={`rounded-xl px-4 py-2 text-sm font-medium transition-all ${
                                    filterTab === "PASSED"
                                        ? "border border-emerald-700 bg-white text-emerald-800 shadow-2xs"
                                        : "border border-transparent text-slate-600 hover:text-slate-900"
                                }`}
                            >
                                Đạt ({passedCount})
                            </button>
                            <button
                                type="button"
                                onClick={() => setFilterTab("FAILED")}
                                className={`rounded-xl px-4 py-2 text-sm font-medium transition-all ${
                                    filterTab === "FAILED"
                                        ? "border border-emerald-700 bg-white text-emerald-800 shadow-2xs"
                                        : "border border-transparent text-slate-600 hover:text-slate-900"
                                }`}
                            >
                                Không đạt ({failedCount})
                            </button>
                        </div>
                    </div>

                </div>

                {/* Table Container Card */}
                <Card className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
                    <CardContent className="p-0">
                        {filteredRows.length === 0 ? (
                            <div className="flex flex-col items-center justify-center py-12 text-center text-slate-400">
                                <Info className="h-8 w-8 mb-2 text-slate-300"/>
                                <p className="text-sm font-medium">Không có chỉ tiêu nào phù hợp với bộ lọc hiện
                                    tại.</p>
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => setFilterTab("ALL")}
                                    className="mt-2 text-xs text-[#2E7D32] rounded-xl"
                                >
                                    Xem tất cả chỉ tiêu
                                </Button>
                            </div>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="w-full text-left text-sm border-collapse">
                                    <thead>
                                    <tr className="border-b border-slate-200 bg-slate-50/90 text-xs font-semibold text-slate-700 uppercase tracking-wider">
                                        <th className="py-3.5 pl-5 pr-2 w-12 text-center">STT</th>
                                        <th className="py-3.5 px-4 min-w-[200px]">Chỉ tiêu kiểm nghiệm</th>
                                        <th className="py-3.5 px-4 min-w-[180px] text-center">
                                            {isReadOnly ? "Kết luận kiểm nghiệm" : "Kết luận kiểm nghiệm *"}
                                        </th>
                                        <th className="py-3.5 px-4 min-w-[160px]">
                                            {isReadOnly ? "Ngày cấp kết quả" : "Ngày cấp kết quả *"}
                                        </th>
                                        <th className="py-3.5 px-4 min-w-[160px]">
                                            {isReadOnly ? "Ngày hết hiệu lực" : "Ngày hết hiệu lực *"}
                                        </th>
                                        <th className="py-3.5 px-4 min-w-[220px]">Phiếu kết quả (Ảnh/PDF)</th>
                                        <th className="py-3.5 pr-5 pl-2 w-16 text-center">Trạng thái</th>
                                    </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-100">
                                    {paginatedRows.map((r, index) => {
                                        const err = rowErrors[r.criterionId];
                                        const isRowValid = r.passed !== null && !err?.resultDate && !err?.expiryDate;

                                        return (
                                            <tr
                                                key={r.criterionId}
                                                className={`transition-colors hover:bg-slate-50/80 ${
                                                    r.passed === false
                                                        ? "bg-red-50/25"
                                                        : r.passed === true
                                                            ? "bg-emerald-50/20"
                                                            : index % 2 === 1
                                                                ? "bg-[#F9FAFB]/60"
                                                                : "bg-white"
                                                }`}
                                            >
                                                {/* STT */}
                                                <td className="py-4 pl-5 pr-2 text-center text-xs font-medium text-slate-500 align-top">
                                                    {page * pageSize + index + 1}
                                                </td>

                                                {/* Criterion Info */}
                                                <td className="py-4 px-4 align-top">
                                                    <div className="space-y-1">
                                                        <p className="font-semibold text-slate-900 text-sm leading-snug">
                                                            {r.name}
                                                        </p>
                                                        <div
                                                            className="flex flex-wrap items-center gap-2 text-xs text-slate-500">
                                                            {r.standardName && (
                                                                <span className="truncate max-w-[180px] text-slate-500"
                                                                      title={r.standardName}>
                                    {r.standardName}
                                  </span>
                                                            )}
                                                        </div>
                                                    </div>
                                                </td>

                                                {/* Conclusion (Pass/Fail Toggle) */}
                                                <td className="py-4 px-4 text-center align-top">
                                                    <div
                                                        className="inline-flex rounded-xl border border-slate-200 bg-slate-100/90 p-1 shadow-inner">
                                                        <button
                                                            type="button"
                                                            onClick={() => handleSetPassed(r.criterionId, true)}
                                                            disabled={isReadOnly}
                                                            className={`flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-lg transition-all ${
                                                                r.passed === true && !isReadOnly
                                                                    ? "bg-[#2E7D32] text-white shadow-sm"
                                                                    : r.passed === true
                                                                        ? "bg-slate-200 text-slate-600"
                                                                        : "text-slate-500 hover:text-emerald-700 hover:bg-white/80"
                                                            } disabled:cursor-not-allowed`}
                                                        >
                                                            <Check className="h-3.5 w-3.5 stroke-[2.5]"/>
                                                            Đạt
                                                        </button>
                                                        <button
                                                            type="button"
                                                            disabled={isReadOnly}
                                                            onClick={() => handleSetPassed(r.criterionId, false)}
                                                            className={`flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-lg transition-all ${
                                                                r.passed === false && !isReadOnly
                                                                    ? "bg-[#D32F2F] text-white shadow-sm"
                                                                    : r.passed === false
                                                                        ? "bg-slate-200 text-slate-600"
                                                                        : "text-slate-500 hover:text-red-700 hover:bg-white/80"
                                                            } disabled:cursor-not-allowed`}
                                                        >
                                                            <X className="h-3.5 w-3.5 stroke-[2.5]"/>
                                                            Không đạt
                                                        </button>
                                                    </div>
                                                    {/* Vùng thông báo cố định để không đẩy chiều cao dòng/hàng */}
                                                    <div
                                                        className="mt-1 min-h-4 text-center text-[11px] leading-4 text-red-600 font-medium">
                                                        {err?.passed || ""}
                                                    </div>
                                                </td>

                                                {/* Result Date */}
                                                <td className="py-4 px-4 align-top">
                                                    <div className="space-y-1">
                                                        <Input
                                                            type="date"
                                                            value={r.resultDate}
                                                            max={r.expiryDate || undefined}
                                                            disabled={isReadOnly || r.passed === false}
                                                            onChange={(e) =>
                                                                handleFieldChange(r.criterionId, "resultDate", e.target.value)
                                                            }
                                                            className={`h-9 text-xs rounded-xl disabled:opacity-60 disabled:cursor-not-allowed ${
                                                                err?.resultDate ? "border-red-500 bg-red-50/50" : "border-slate-300"
                                                            }`}
                                                        />
                                                        {/* Vùng thông báo cố định: không làm nâng layout input khi xuất hiện lỗi */}
                                                        <div
                                                            className="mt-1 min-h-4 text-[11px] leading-4 text-red-600 font-medium">
                                                            {err?.resultDate || ""}
                                                        </div>
                                                    </div>
                                                </td>

                                                {/* Expiry Date */}
                                                <td className="py-4 px-4 align-top">
                                                    <div className="space-y-1">
                                                        <Input
                                                            type="date"
                                                            value={r.expiryDate}
                                                            min={r.resultDate || today}
                                                            disabled={isReadOnly || r.passed === false}
                                                            onChange={(e) =>
                                                                handleFieldChange(r.criterionId, "expiryDate", e.target.value)
                                                            }
                                                            className={`h-9 text-xs rounded-lg disabled:opacity-60 disabled:cursor-not-allowed ${
                                                                err?.expiryDate ? "border-red-500 bg-red-50/50" : "border-slate-300"
                                                            }`}
                                                        />
                                                        {/* Vùng thông báo cố định chiều cao để không làm đẩy layout input */}
                                                        <div
                                                            className="mt-1 min-h-4 text-[11px] leading-4 text-red-600 font-medium">
                                                            {err?.expiryDate || ""}
                                                        </div>
                                                    </div>
                                                </td>

                                                {/* File Attachment */}
                                                <td className="py-4 px-4 align-top">
                                                    <div className="space-y-1">
                                                        {r.uploading ? (
                                                            <div
                                                                className="flex items-center gap-2 text-xs text-slate-500 py-1">
                                                                <LoaderCircle
                                                                    className="h-4 w-4 animate-spin text-[#2E7D32]"/>
                                                                <span>Đang tải lên...</span>
                                                            </div>
                                                        ) : r.filePath && r.passed !== false ? (
                                                            <div
                                                                className="flex items-center gap-1.5 rounded-xl border border-emerald-200 bg-emerald-50/80 px-3 py-1.5 text-xs shadow-xs">
                                                                <FileCheck2
                                                                    className="h-4 w-4 text-[#2E7D32] shrink-0"/>
                                                                <span className="truncate text-slate-800 font-medium"
                                                                      title={r.selectedFileName || r.filePath}>
                                    {r.selectedFileName || "phiếu-kết-quả.pdf"}
                                  </span>
                                                            </div>
                                                        ) : isReadOnly || r.passed === false ? (
                                                            <span className="text-xs text-slate-400">—</span>
                                                        ) : (
                                                            <label
                                                                className="flex items-center justify-center gap-1.5 rounded-xl border border-dashed border-slate-300 bg-slate-50/80 px-3 py-2 text-xs text-slate-600 cursor-pointer hover:border-[#2E7D32] hover:bg-emerald-50/30 transition-all">
                                                                <FileUp className="h-3.5 w-3.5 text-slate-400"/>
                                                                <span>Tải phiếu kết quả</span>
                                                                <input
                                                                    type="file"
                                                                    accept="image/*,.pdf"
                                                                    className="hidden"
                                                                    onChange={(e) =>
                                                                        void handleFileUpload(
                                                                            r.criterionId,
                                                                            e.target.files?.[0] ?? null
                                                                        )
                                                                    }
                                                                />
                                                            </label>
                                                        )}
                                                    </div>
                                                </td>

                                                {/* Row Status Indicator */}
                                                <td className="py-4 pr-5 pl-2 text-center align-top">
                                                    {isRowValid ? (
                                                        <span
                                                            className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-emerald-100 text-[#2E7D32] shadow-xs"
                                                            title="Chỉ tiêu đã hợp lệ"
                                                        >
                                <Check className="h-4 w-4 stroke-[3]"/>
                              </span>
                                                    ) : (
                                                        <span
                                                            className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-amber-100 text-amber-700 shadow-xs"
                                                            title="Chưa hoàn thiện hoặc có lỗi"
                                                        >
                                <AlertTriangle className="h-4 w-4"/>
                              </span>
                                                    )}
                                                </td>
                                            </tr>
                                        );
                                    })}
                                    </tbody>
                                </table>
                            </div>
                        )}

                        {/* Phân trang — kiểu InspectionRequestHistoryModal */}
                        {filteredRows.length > 0 && (
                            <div className="flex flex-wrap items-center justify-between gap-3 px-6 py-4 border-t border-[#E5E7EB] text-xs text-muted-foreground sm:text-sm">
                                <div>
                                    Hiển thị {page * pageSize + 1} –{" "}
                                    {Math.min((page + 1) * pageSize, filteredRows.length)} trên tổng
                                    số {filteredRows.length} chỉ tiêu
                                </div>
                                <div className="flex items-center gap-1">
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        disabled={page === 0}
                                        onClick={() => setPage((p) => Math.max(0, p - 1))}
                                    >
                                        <ChevronLeft className="mr-1 h-4 w-4" />
                                        Trang trước
                                    </Button>
                                    <span className="px-2 font-medium tabular-nums">
                                        {totalPages > 0 ? page + 1 : 0}/{totalPages}
                                    </span>
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        disabled={page >= totalPages - 1}
                                        onClick={() => setPage((p) => p + 1)}
                                    >
                                        Trang sau
                                        <ChevronRight className="ml-1 h-4 w-4" />
                                    </Button>
                                </div>
                            </div>
                        )}
                    </CardContent>
                </Card>
            </div>

            {/* SECTION 4: Real-time Feedback & Forecast */}
            <div className="space-y-4">
                {willPassAll && (
                    <div
                        className="flex items-start gap-3.5 rounded-2xl border border-emerald-300 bg-[#E8F5E9] p-5 text-emerald-950 shadow-sm">
                        <CheckCircle2 className="h-6 w-6 text-[#2E7D32] shrink-0 mt-0.5"/>
                        <div className="space-y-1">
                            <h4 className="font-bold text-sm text-[#2E7D32]">
                                {isReadOnly ? "Kết quả:" : "Dự báo:"}{" "}
                                Yêu cầu kiểm nghiệm ĐẠT TIÊU CHUẨN
                            </h4>
                            <p className="text-xs text-emerald-900 leading-relaxed">
                                Tất cả {totalCriteria} chỉ tiêu của lượt yêu cầu này đều được đánh giá ĐẠT và còn
                                hiệu lực.
                                {!isReadOnly &&
                                    ` Sau khi lưu, yêu cầu kiểm nghiệm này sẽ chuyển sang trạng thái `}
                                {!isReadOnly && (
                                    <strong className="text-[#2E7D32]">PASSED.</strong>
                                )}
                            </p>
                        </div>
                    </div>
                )}

                {willFail && (
                    <div
                        className="flex items-start gap-3.5 rounded-2xl border border-red-300 bg-[#FFEBEE] p-5 text-red-950 shadow-sm">
                        <AlertTriangle className="h-6 w-6 text-[#D32F2F] shrink-0 mt-0.5"/>
                        <div className="space-y-1">
                            <h4 className="font-bold text-sm text-[#D32F2F]">
                                {isReadOnly ? "Kết quả:" : "Dự báo:"} Yêu cầu kiểm nghiệm
                                KHÔNG ĐẠT ({failedCount} chỉ tiêu không đạt)
                            </h4>
                            <p className="text-xs text-red-900 leading-relaxed">
                                Có {failedCount} chỉ tiêu bị đánh dấu Không đạt. Theo quy định
                                quản lý chất lượng, khi có bất kỳ chỉ tiêu nào không đạt, lô
                                sản xuất này sẽ{" "}
                                <strong>KHÔNG đủ điều kiện</strong> kích hoạt tem truy xuất
                                nguồn gốc đến khi có kết quả kiểm nghiệm mới đạt chuẩn.
                            </p>
                        </div>
                    </div>
                )}

                {!isAllAnswered && touched && (
                    <div
                        className="flex items-start gap-3.5 rounded-2xl border border-amber-300 bg-[#FFF8E1] p-5 text-amber-950 shadow-sm">
                        <Info className="h-6 w-6 text-[#F9A825] shrink-0 mt-0.5"/>
                        <div className="space-y-1">
                            <h4 className="font-bold text-sm text-amber-900">
                                Còn {unsetCount} chỉ tiêu chưa được nhập kết luận
                            </h4>
                            <p className="text-xs text-amber-800 leading-relaxed">
                                Backend yêu cầu ghi nhận kết quả cho toàn bộ chỉ tiêu của yêu cầu trong một lần giao
                                dịch (All-or-nothing). Vui lòng hoàn tất trước khi bấm Lưu.
                            </p>
                        </div>
                    </div>
                )}
            </div>

            {/* SECTION 5: Sticky Action Footer */}
            <div
                className="sticky bottom-4 z-20 flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white/95 p-4 shadow-lg backdrop-blur-md sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-center gap-3">
                    <div
                        className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-100 text-emerald-800 font-bold text-xs">
                        {filledCount}/{totalCriteria}
                    </div>
                    <div>
                        <p className="text-xs font-semibold text-foreground">
                            Tiến độ nhập: {filledCount}/{totalCriteria} chỉ tiêu đã hoàn tất ({progressPercent}%)
                        </p>
                        <p className="text-xs text-muted-foreground">
                            {isAllAnswered ? "Đã nhập đủ tất cả chỉ tiêu" : `Còn ${unsetCount} chỉ tiêu chưa nhập kết luận`}
                        </p>
                    </div>
                </div>

                <div className="flex items-center gap-2 self-end sm:self-auto">
                    {!isReadOnly && (
                        <Button
                            type="button"
                            variant="ghost"
                            onClick={() => {
                                if (effectiveLotId) navigate(`/production-lots/${effectiveLotId}`);
                                else navigate("/production-lots");
                            }}
                            disabled={submitting}
                            className="rounded-xl text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-muted/50"
                        >
                            Hủy bỏ
                        </Button>
                    )}

                    {!isReadOnly && (
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => void loadData()}
                            disabled={submitting}
                            className="rounded-xl border-emerald-200 text-xs font-medium text-emerald-800 bg-emerald-50/50 hover:bg-emerald-100/60"
                        >
                            <RotateCw className="mr-1.5 h-3.5 w-3.5 text-emerald-700"/> Khôi phục ban đầu
                        </Button>
                    )}

                    {!isReadOnly && (
                        <Button
                            type="button"
                            variant="create"
                            onClick={() => void handleSubmit()}
                            disabled={!canSubmit || submitting}
                            className="rounded-xl text-xs font-semibold px-5 shadow-xs"
                        >
                            {submitting ? (
                                <>
                                    <LoaderCircle className="mr-1.5 h-3.5 w-3.5 animate-spin"/>
                                    Đang lưu...
                                </>
                            ) : (
                                <>
                                    <Save className="mr-1.5 h-3.5 w-3.5"/>
                                    Lưu kết quả kiểm nghiệm
                                </>
                            )}
                        </Button>
                    )}
                </div>
            </div>
        </div>
    );
};

export default RecordInspectionResultPage;
