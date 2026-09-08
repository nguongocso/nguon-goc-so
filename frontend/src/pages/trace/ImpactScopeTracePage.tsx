import React, {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {
    Search,
    Download,
    FileSpreadsheet,
    RefreshCw,
    ChevronDown,
    Eye,
} from 'lucide-react';
import {exportImpactScopeReport, getImpactScopeTrace} from '@/api/impactScopeTraceApi';
import type {ImpactScopeTraceResponse} from '@/types/impactScopeTrace';
import {useSetBreadcrumb} from '@/components/common/AppBreadcrumb';
import {HelpButton} from '@/components/help/HelpButton';
import {BulkRecallRequestModal} from '@/components/recall/BulkRecallRequestModal';

export const ImpactScopeTracePage: React.FC = () => {
    const navigate = useNavigate();

    useSetBreadcrumb([
        {label: 'Tổng quan', href: '/dashboard'},
        {label: 'Truy vết phạm vi ảnh hưởng'},
    ]);

    const [searchCode, setSearchCode] = useState<string>('');
    const [loading, setLoading] = useState<boolean>(false);
    const [exporting, setExporting] = useState<boolean>(false);
    const [errorMsg, setErrorMsg] = useState<string | null>(null);
    const [traceData, setTraceData] = useState<ImpactScopeTraceResponse | null>(null);
    const [showExportMenu, setShowExportMenu] = useState<boolean>(false);
    const [bulkRecallModalOpen, setBulkRecallModalOpen] = useState<boolean>(false);

    const formatStatus = (status: string): string => {
        if (!status) return '';
        switch (status.toUpperCase()) {
            case 'ACTIVATED':
                return 'Đã kích hoạt';
            case 'DRAFT':
                return 'Dự thảo';
            case 'RECALLED':
                return 'Đã thu hồi';
            case 'CODE_PRINTED':
                return 'Đã in mã';
            case 'APPROVED':
                return 'Đã phê duyệt';
            case 'PACKAGED':
                return 'Đã đóng gói';
            case 'CANCELLED':
                return 'Đã hủy';
            default:
                return status;
        }
    };

    const formatEventType = (type?: string): string => {
        if (!type) return '';
        switch (type.toUpperCase()) {
            case 'TRANSPORT':
                return 'Vận chuyển';
            case 'PROCUREMENT':
                return 'Thu mua';
            case 'WAREHOUSE_RECEIPT':
                return 'Nhập kho';
            case 'PACKAGING':
                return 'Đóng gói';
            case 'PREPROCESSING':
                return 'Sơ chế';
            case 'HARVEST':
                return 'Thu hoạch';
            case 'STORAGE_CONDITION':
                return 'Bảo quản';
            case 'CORRECTION':
                return 'Đính chính';
            default:
                return type;
        }
    };

    const handleSearch = async (e?: React.FormEvent) => {
        if (e) e.preventDefault();
        if (!searchCode.trim()) {
            setErrorMsg('Vui lòng nhập Mã lô sản xuất, Mã lô hàng hoặc Mã tem QR.');
            return;
        }

        setLoading(true);
        setErrorMsg(null);
        setTraceData(null);

        try {
            const data = await getImpactScopeTrace(searchCode.trim());
            setTraceData(data);
        } catch (err: any) {
            const msg = err.response?.data?.message || err.message || 'Mã truy vết không tồn tại trên hệ thống. Vui lòng kiểm tra lại.';
            setErrorMsg(msg);
        } finally {
            setLoading(false);
        }
    };

    const handleExport = async (format: 'EXCEL' | 'PDF') => {
        if (!searchCode.trim() && !traceData?.searchedCode) return;
        const targetCode = traceData?.searchedCode || searchCode.trim();

        setExporting(true);
        setShowExportMenu(false);
        try {
            const blob = await exportImpactScopeReport(targetCode, format);
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            const ext = format === 'EXCEL' ? 'xlsx' : 'pdf';
            link.setAttribute('download', `Truy_vet_pham_vi_anh_huong_${targetCode}.${ext}`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (err: any) {
            alert('Lỗi khi xuất tệp báo cáo: ' + (err.response?.data?.message || err.message));
        } finally {
            setExporting(false);
        }
    };

    return (
        <div className="space-y-6">

            {/* Page Header (Căn lề trái chuẩn như các trang hệ thống) */}
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                        Truy vết phạm vi ảnh hưởng của lô
                    </h1>
                    <p className="text-muted-foreground text-sm mt-1">
                        Nhập mã lô sản xuất, lô hàng hoặc tem để xác định chính xác các mắt xích và đối tác cần thu hồi
                        khi có sự cố.
                    </p>
                </div>
                <div className="flex items-center gap-3">
                    <HelpButton screenKey="impact-scope-trace"/>
                    {traceData && (
                        <div className="relative">
                            <button
                                type="button"
                                onClick={() => setShowExportMenu(!showExportMenu)}
                                disabled={exporting}
                                className="px-3.5 py-2 bg-white hover:bg-slate-50 text-slate-700 border border-slate-300 rounded-lg flex items-center gap-2 text-sm font-medium shadow-xs transition-colors disabled:opacity-50"
                            >
                                <Download className="w-4 h-4 text-slate-500"/>
                                <span>Xuất tệp</span>
                                <ChevronDown className="w-3.5 h-3.5 text-slate-400"/>
                            </button>

                            {showExportMenu && (
                                <div
                                    className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-slate-200 py-1 z-20">
                                    <button
                                        type="button"
                                        onClick={() => handleExport('EXCEL')}
                                        className="w-full px-4 py-2.5 text-left text-xs font-medium text-slate-700 hover:bg-slate-50 flex items-center gap-2"
                                    >
                                        <FileSpreadsheet className="w-4 h-4 text-emerald-600"/>
                                        <span>Xuất Excel (.xlsx)</span>
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => handleExport('PDF')}
                                        className="w-full px-4 py-2.5 text-left text-xs font-medium text-slate-700 hover:bg-slate-50 flex items-center gap-2"
                                    >
                                        <FileSpreadsheet className="w-4 h-4 text-red-600"/>
                                        <span>Xuất PDF (.pdf)</span>
                                    </button>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>

            {/* Search Bar Input */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
                <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3">
                    <div className="relative flex-1">
                        <input
                            type="text"
                            value={searchCode}
                            onChange={(e) => setSearchCode(e.target.value)}
                            placeholder="Nhập Mã lô sản xuất, Mã lô hàng hoặc Mã tem (ví dụ: LOT-2026-001, SHIP-8821, NCL0001)..."
                            className="w-full px-4 py-3 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 text-sm font-medium transition-all"
                        />
                    </div>
                    {/* Nút thao tác được giữ Icon */}
                    <button
                        type="submit"
                        disabled={loading}
                        className="px-6 py-3 bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white font-semibold rounded-lg shadow-sm flex items-center justify-center gap-2 transition-all disabled:opacity-50 min-w-[150px]"
                    >
                        {loading ? (
                            <>
                                <RefreshCw className="w-4 h-4 animate-spin"/>
                                <span>Đang truy vết...</span>
                            </>
                        ) : (
                            <>
                                <Search className="w-4 h-4"/>
                                <span>Mở truy vết</span>
                            </>
                        )}
                    </button>
                </form>

                {/* Error Banner (TC-05) */}
                {errorMsg && (
                    <div className="mt-4 p-4 bg-amber-50 border border-amber-200 rounded-lg text-amber-800">
                        <p className="font-semibold text-sm">{errorMsg}</p>
                        <p className="text-xs text-amber-700 mt-0.5">
                            Gợi ý: Hãy kiểm tra lại tính chính xác của mã lô hoặc mã tem QR. Nếu vừa khởi tạo, hãy thử
                            tải lại trang.
                        </p>
                    </div>
                )}
            </div>

            {/* Results Container */}
            {traceData && (
                <div className="space-y-6">

                    {/* Top Summary Metrics Header (Không dùng Icon trang trí) */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                            <p className="text-xs text-slate-500 font-medium">Tổng Lô hàng sinh ra</p>
                            <p className="text-2xl font-bold text-slate-900 mt-1">{traceData.summary.totalShipments}</p>
                        </div>

                        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                            <p className="text-xs text-slate-500 font-medium">Tem đã Kích hoạt</p>
                            <p className="text-2xl font-bold text-slate-900 mt-1">{traceData.summary.totalActivatedStamps.toLocaleString()}</p>
                        </div>

                        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                            <p className="text-xs text-slate-500 font-medium">Tổ chức đã nhận hàng</p>
                            <p className="text-2xl font-bold text-slate-900 mt-1">{traceData.summary.totalReceivingOrganizations}</p>
                        </div>

                        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                            <p className="text-xs text-slate-500 font-medium">Lô hàng Đã Thu hồi</p>
                            <p className="text-2xl font-bold text-slate-900 mt-1">{traceData.summary.totalRecalledShipments}</p>
                        </div>
                    </div>

                    {/* Tree View Graphic Section */}
                    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">

                        {/* UPSTREAM BRANCH (Lô sản xuất -> Nguồn gốc Vùng trồng) - Left Column */}
                        <div className="lg:col-span-4 space-y-4">
                            <div className="flex items-center justify-between px-1">
                                <h2 className="font-bold text-sm text-slate-900">Chiều ngược (upstream)</h2>
                                <span className="text-xs text-slate-500 font-medium">Nguồn gốc canh tác</span>
                            </div>

                            {/* Production Lot Card (Mốc tra cứu trung tâm) */}
                            <div
                                className="bg-white p-5 rounded-xl border border-blue-200 shadow-sm border-l-4 border-l-blue-500">
                                <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded">
                      LÔ SẢN XUẤT HẠT NHÂN
                    </span>
                                    <span className={`text-xs px-2 py-0.5 rounded font-semibold ${
                                        traceData.productionLot.status === 'APPROVED' ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-800'
                                    }`}>
                      {formatStatus(traceData.productionLot.status)}
                    </span>
                                </div>
                                <h3 className="font-bold text-slate-900 text-base mt-2">{traceData.productionLot.name}</h3>
                                <div className="text-xs text-slate-600 mt-2 space-y-1.5">
                                    <p>Sản
                                        lượng: <strong>{traceData.productionLot.expectedQuantity} {traceData.productionLot.expectedQuantityUnit}</strong>
                                    </p>
                                    {traceData.productionLot.plantingDate && (
                                        <p>Xuống giống: {traceData.productionLot.plantingDate}</p>
                                    )}
                                    {traceData.productionLot.harvestDate && (
                                        <p>Thu hoạch: {traceData.productionLot.harvestDate}</p>
                                    )}
                                </div>
                            </div>

                            <div
                                className="flex items-center justify-center gap-2 text-xs font-semibold text-slate-400">
                                <span>↑ Tra ngược nguồn gốc</span>
                            </div>

                            {/* Vùng trồng Card (Nguồn gốc) */}
                            {traceData.farmArea ? (
                                <div
                                    className="bg-white p-5 rounded-xl border border-emerald-200 shadow-sm border-l-4 border-l-emerald-500">
                                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded">
                        VÙNG TRỒNG GỐC
                      </span>
                                    </div>
                                    <h3 className="font-bold text-slate-900 text-base mt-2">{traceData.farmArea.name}</h3>
                                    <div className="text-xs text-slate-500 mt-2 space-y-1">
                                        <p><span
                                            className="font-medium text-slate-700">Mã vùng:</span> {traceData.farmArea.code}
                                        </p>
                                        <p><span
                                            className="font-medium text-slate-700">Vị trí:</span> {traceData.farmArea.location}
                                        </p>
                                        {traceData.farmArea.areaSize && (
                                            <p><span
                                                className="font-medium text-slate-700">Diện tích:</span> {traceData.farmArea.areaSize} m²
                                            </p>
                                        )}
                                    </div>
                                </div>
                            ) : (
                                <div
                                    className="bg-white p-4 rounded-xl border border-slate-200 text-slate-500 text-xs italic">
                                    Chưa liên kết vùng trồng cố định.
                                </div>
                            )}
                        </div>

                        {/* DOWNSTREAM BRANCH (Lô sản xuất -> Lô hàng -> Tem -> Sự kiện -> Đối tác) - Right Column */}
                        <div className="lg:col-span-8 space-y-4">
                            <div className="flex items-center justify-between px-1">
                                <h2 className="font-bold text-sm text-slate-900">Chiều xuôi (downstream) · phạm vi ảnh
                                    hưởng</h2>
                                <span className="text-xs text-slate-500 font-medium">
                    {traceData.shipments.length} lô hàng
                  </span>
                            </div>

                            {/* Empty State Banner (TC-03) */}
                            {traceData.shipments.length === 0 ? (
                                <div className="bg-white rounded-xl border border-slate-200 p-8 text-center space-y-3">
                                    <h3 className="font-bold text-slate-800 text-base">Lô chưa phát sinh lô hàng</h3>
                                    <p className="text-xs text-slate-500 max-w-md mx-auto">
                                        Lô sản xuất này hiện chưa tạo lô hàng thương mại nào để phân phối ra thị trường.
                                        Nhánh truy vết xuôi hiện đang rỗng.
                                    </p>
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    {traceData.shipments.map((ship, idx) => (
                                        <div
                                            key={ship.id || idx}
                                            className={`bg-white rounded-xl border p-5 shadow-sm transition-all hover:shadow-md ${
                                                ship.status === 'RECALLED'
                                                    ? 'border-red-300 bg-red-50/20'
                                                    : ship.status === 'ACTIVATED'
                                                        ? 'border-emerald-200'
                                                        : 'border-slate-200'
                                            }`}
                                        >
                                            {/* Shipment Header */}
                                            <div
                                                className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
                                                <div>
                                                    <div className="flex items-center gap-2">
                                                        <span
                                                            className="font-mono text-xs font-bold text-slate-400">#{idx + 1}</span>
                                                        <h3 className="font-bold text-slate-900 text-base">{ship.name}</h3>
                                                    </div>
                                                    <p className="text-xs text-slate-500 mt-0.5">
                                                        Tạo
                                                        ngày: {new Date(ship.createdAt).toLocaleDateString('vi-VN')} •
                                                        Quy cách: {ship.packagingInfo || 'N/A'}
                                                    </p>
                                                </div>

                                                <div className="flex items-center gap-2">
                            <span className={`text-xs px-2.5 py-1 rounded-full font-bold ${
                                ship.status === 'RECALLED'
                                    ? 'bg-red-100 text-red-700'
                                    : ship.status === 'ACTIVATED'
                                        ? 'bg-emerald-100 text-emerald-700'
                                        : 'bg-slate-100 text-slate-700'
                            }`}>
                              {formatStatus(ship.status)}
                            </span>

                                                    {ship.id && (
                                                        <button
                                                            type="button"
                                                            onClick={() => navigate(`/shipments/${ship.id}`)}
                                                            className="px-3 py-1 bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-700 font-medium rounded-lg text-xs flex items-center gap-1.5 transition-colors"
                                                            title="Xem chi tiết lô hàng, tem QR và thông tin nhật ký"
                                                        >
                                                            <Eye className="w-3.5 h-3.5 text-slate-600"/>
                                                            <span>Chi tiết lô hàng</span>
                                                        </button>
                                                    )}
                                                </div>
                                            </div>

                                            {/* Stats Badges Row */}
                                            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 my-4">
                                                <div
                                                    className="bg-slate-50 p-2.5 rounded-lg text-xs border border-slate-100">
                                                    <span className="text-slate-500 block">Số lượng lô:</span>
                                                    <span
                                                        className="font-bold text-slate-800">{ship.totalQuantity.toLocaleString()}</span>
                                                </div>
                                                <div
                                                    className="bg-slate-50 p-2.5 rounded-lg text-xs border border-slate-100">
                                                    <span className="text-slate-500 block">Tem kích hoạt:</span>
                                                    <span
                                                        className="font-bold text-slate-800">{ship.activatedStampsCount.toLocaleString()} tem</span>
                                                </div>
                                                <div
                                                    className="bg-slate-50 p-2.5 rounded-lg text-xs col-span-2 sm:col-span-1 border border-slate-100">
                                                    <span className="text-slate-500 block">Lượt quét công khai:</span>
                                                    <span
                                                        className="font-bold text-slate-800">{ship.scanStats?.totalScans || 0} lượt</span>
                                                </div>
                                            </div>

                                            {/* Sự kiện Vận chuyển & Thu mua / Nhập kho (Refactored Timeline) */}
                                            <div className="mb-5">
                                                <p className="text-xs font-semibold text-slate-700 mb-3">
                                                    Sự kiện vận chuyển và thu mua
                                                </p>

                                                {!ship.events || ship.events.length === 0 ? (
                                                    <div
                                                        className="text-xs text-slate-400 italic bg-slate-50 p-2.5 rounded-lg border border-slate-100">
                                                        Chưa có sự kiện vận chuyển, thu mua hoặc giao nhận.
                                                    </div>
                                                ) : (
                                                    <div
                                                        className="relative pl-3 space-y-3.5 my-2 before:absolute before:left-[5px] before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200">
                                                        {ship.events.map((ev, eIdx) => {
                                                            const typeUpper = (ev.eventType || ev.eventTypeName || '').toUpperCase();
                                                            const isProc = typeUpper.includes('PROCUREMENT') || typeUpper.includes('THU MUA');
                                                            const isWh = typeUpper.includes('WAREHOUSE') || typeUpper.includes('NHẬP KHO');

                                                            return (
                                                                <div key={ev.id || eIdx} className="relative pl-4">
                                                                    {/* Timeline Dot */}
                                                                    <div
                                                                        className={`absolute -left-[9px] top-1.5 w-2.5 h-2.5 rounded-full border-2 border-white shadow-xs ${
                                                                            isProc ? 'bg-emerald-600' : isWh ? 'bg-purple-600' : 'bg-blue-600'
                                                                        }`}
                                                                    />

                                                                    <div className="flex items-center gap-2 flex-wrap">
                                      <span className="font-bold text-xs text-slate-900">
                                        {formatEventType(ev.eventTypeName) || formatEventType(ev.eventType)}
                                      </span>
                                                                        {ev.isCorrection && (
                                                                            <span
                                                                                className="text-[10px] bg-amber-100 text-amber-800 px-1.5 py-0.2 rounded font-medium">
                                          Đính chính
                                        </span>
                                                                        )}
                                                                    </div>

                                                                    <div
                                                                        className="text-[11px] text-slate-500 mt-0.5 space-x-1.5">
                                      <span>
                                        {new Date(ev.recordedAt).toLocaleTimeString('vi-VN', {
                                            hour: '2-digit',
                                            minute: '2-digit',
                                            second: '2-digit'
                                        })} • {new Date(ev.recordedAt).toLocaleDateString('vi-VN')}
                                      </span>
                                                                        {ev.location && <span>• {ev.location}</span>}
                                                                    </div>
                                                                </div>
                                                            );
                                                        })}
                                                    </div>
                                                )}
                                            </div>

                                            {/* Receiving Organizations Timeline (TC-04 & QTN-01) */}
                                            <div>
                                                <p className="text-xs font-semibold text-slate-700 mb-2">
                                                    Tổ chức đã nhận hàng
                                                </p>

                                                {ship.receivingOrganizations.length === 0 ? (
                                                    <div
                                                        className="text-xs text-slate-400 italic bg-slate-50 p-2.5 rounded-lg border border-slate-100">
                                                        Chưa ghi nhận sự kiện giao nhận tới đối tác bên ngoài HTX.
                                                    </div>
                                                ) : (
                                                    <div className="space-y-2">
                                                        {ship.receivingOrganizations.map((org, oIdx) => (
                                                            <div
                                                                key={oIdx}
                                                                className="flex flex-col sm:flex-row sm:items-center justify-between p-2.5 bg-slate-50/80 border border-slate-200 rounded-lg text-xs gap-1 sm:gap-2"
                                                            >
                                                                <div className="flex items-center gap-2 flex-wrap">
                                                                    <span
                                                                        className="font-bold text-slate-900">{org.organizationName}</span>
                                                                    <span
                                                                        className="text-[10px] bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full font-medium">
                                      {formatEventType(org.eventTypeName) || formatEventType(org.eventType)}
                                    </span>
                                                                    {org.receivedQuantity != null && (
                                                                        <span
                                                                            className="text-xs text-slate-700 font-medium">
                                        • Thực nhận: <strong className="text-slate-900">{org.receivedQuantity.toLocaleString('vi-VN')}</strong>
                                      </span>
                                                                    )}
                                                                </div>
                                                                <div
                                                                    className="text-slate-500 text-[11px] whitespace-nowrap">
                                                                    {new Date(org.receivedAt).toLocaleTimeString('vi-VN', {
                                                                        hour: '2-digit',
                                                                        minute: '2-digit'
                                                                    })} • {new Date(org.receivedAt).toLocaleDateString('vi-VN')}
                                                                </div>
                                                            </div>
                                                        ))}
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                </div>
            )}

            {/* Nút tạo đề nghị thu hồi theo phạm vi ảnh hưởng (NCL-08-CN-011) */}
            {traceData && traceData.shipments && traceData.shipments.length > 0 && (
                <div className="mt-6 flex justify-end">
                    <button
                        onClick={() => setBulkRecallModalOpen(true)}
                        className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                    >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 5v14m-7-7h14"/>
                        </svg>
                        Tạo đề nghị thu hồi
                    </button>
                </div>
            )}

            {/* Modal tạo đề nghị thu hồi hàng loạt (NCL-08-CN-011) */}
            {traceData && (
                <BulkRecallRequestModal
                    open={bulkRecallModalOpen}
                    onOpenChange={setBulkRecallModalOpen}
                    productionLotId={traceData.productionLot.id}
                    productionLotName={traceData.productionLot.code}
                    shipments={traceData.shipments}
                    onSuccess={(requestId) => {
                        setBulkRecallModalOpen(false);
                        navigate(`/recall-requests/bulk/${requestId}`);
                    }}
                />
            )}
        </div>
    );
};

export default ImpactScopeTracePage;
