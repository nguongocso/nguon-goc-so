import React, { useState } from 'react';
import {
  Search,
  GitFork,
  Download,
  Building2,
  ShieldAlert,
  CheckCircle2,
  QrCode,
  Calendar,
  MapPin,
  Package,
  Layers,
  AlertCircle,
  ArrowRight,
  ArrowUpRight,
  FileSpreadsheet,
  RefreshCw,
  Info,
  ChevronDown,
} from 'lucide-react';
import { exportImpactScopeReport, getImpactScopeTrace } from '@/api/impactScopeTraceApi';
import type { ImpactScopeTraceResponse } from '@/types/impactScopeTrace';

export const ImpactScopeTracePage: React.FC = () => {
  const [searchCode, setSearchCode] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [exporting, setExporting] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [traceData, setTraceData] = useState<ImpactScopeTraceResponse | null>(null);
  const [exportFormat, setExportFormat] = useState<'EXCEL' | 'PDF'>('EXCEL');
  const [showExportMenu, setShowExportMenu] = useState<boolean>(false);

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
    <div className="min-h-screen bg-slate-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto space-y-6">
        
        {/* Header Title */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 text-emerald-600 font-semibold text-sm">
              <GitFork className="w-5 h-5" />
              <span>NCL-08-CN-010 • Năng lực Truy vết 2 chiều</span>
            </div>
            <h1 className="text-2xl font-bold text-slate-900 mt-1">
              Truy vết Phạm vi Ảnh hưởng của Lô
            </h1>
            <p className="text-slate-500 text-sm mt-1">
              Nhập mã lô sản xuất, lô hàng hoặc tem để xác định chính xác các mắt xích và đối tác cần thu hồi khi có sự cố.
            </p>
          </div>

          {/* Quick Search Example Buttons */}
          <div className="flex flex-wrap gap-2 text-xs text-slate-500">
            <span className="self-center font-medium text-slate-700">Mã mẫu:</span>
            <button
              onClick={() => { setSearchCode('LOT-2026-001'); }}
              className="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-md transition-colors"
            >
              LOT-2026-001 (Nhiều lô)
            </button>
            <button
              onClick={() => { setSearchCode('LOT-2026-999'); }}
              className="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-md transition-colors"
            >
              LOT-2026-999 (Chưa có lô)
            </button>
          </div>
        </div>

        {/* Search Bar Input */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3">
            <div className="relative flex-1">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                <Search className="w-5 h-5" />
              </div>
              <input
                type="text"
                value={searchCode}
                onChange={(e) => setSearchCode(e.target.value)}
                placeholder="Nhập Mã lô sản xuất, Mã lô hàng hoặc Mã tem (ví dụ: LOT-2026-001, SHIP-8821, NCL0001)..."
                className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 text-sm font-medium transition-all"
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="px-6 py-3 bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white font-semibold rounded-lg shadow-sm flex items-center justify-center gap-2 transition-all disabled:opacity-50 min-w-[140px]"
            >
              {loading ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Đang truy vết...</span>
                </>
              ) : (
                <>
                  <GitFork className="w-4 h-4" />
                  <span>Mở truy vết</span>
                </>
              )}
            </button>
          </form>

          {/* Error Banner (TC-05) */}
          {errorMsg && (
            <div className="mt-4 p-4 bg-amber-50 border border-amber-200 rounded-lg flex items-start gap-3 text-amber-800">
              <AlertCircle className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-sm">{errorMsg}</p>
                <p className="text-xs text-amber-700 mt-0.5">
                  Gợi ý: Hãy kiểm tra lại tính chính xác của mã lô hoặc mã tem QR. Nếu vừa khởi tạo, hãy thử tải lại trang.
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Results Container */}
        {traceData && (
          <div className="space-y-6 animate-fadeIn">
            
            {/* Top Summary Metrics Header */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center gap-4">
                <div className="p-3 bg-blue-50 text-blue-600 rounded-lg">
                  <Package className="w-6 h-6" />
                </div>
                <div>
                  <p className="text-xs text-slate-500 font-medium">Tổng Lô hàng sinh ra</p>
                  <p className="text-2xl font-bold text-slate-900">{traceData.summary.totalShipments}</p>
                </div>
              </div>

              <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center gap-4">
                <div className="p-3 bg-emerald-50 text-emerald-600 rounded-lg">
                  <QrCode className="w-6 h-6" />
                </div>
                <div>
                  <p className="text-xs text-slate-500 font-medium">Tem đã Kích hoạt</p>
                  <p className="text-2xl font-bold text-slate-900">{traceData.summary.totalActivatedStamps.toLocaleString()}</p>
                </div>
              </div>

              <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center gap-4">
                <div className="p-3 bg-purple-50 text-purple-600 rounded-lg">
                  <Building2 className="w-6 h-6" />
                </div>
                <div>
                  <p className="text-xs text-slate-500 font-medium">Tổ chức đã nhận hàng</p>
                  <p className="text-2xl font-bold text-slate-900">{traceData.summary.totalReceivingOrganizations}</p>
                </div>
              </div>

              <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-red-50 text-red-600 rounded-lg">
                    <ShieldAlert className="w-6 h-6" />
                  </div>
                  <div>
                    <p className="text-xs text-slate-500 font-medium">Lô hàng Đã Thu hồi</p>
                    <p className="text-2xl font-bold text-slate-900">{traceData.summary.totalRecalledShipments}</p>
                  </div>
                </div>

                {/* Export Dropdown */}
                <div className="relative">
                  <button
                    onClick={() => setShowExportMenu(!showExportMenu)}
                    disabled={exporting}
                    className="p-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg flex items-center gap-1.5 text-xs font-semibold transition-colors"
                  >
                    <Download className="w-4 h-4" />
                    <span>Xuất tệp</span>
                    <ChevronDown className="w-3 h-3" />
                  </button>

                  {showExportMenu && (
                    <div className="absolute right-0 mt-2 w-44 bg-white rounded-lg shadow-lg border border-slate-200 py-1 z-10">
                      <button
                        onClick={() => handleExport('EXCEL')}
                        className="w-full px-4 py-2 text-left text-xs font-medium text-slate-700 hover:bg-slate-50 flex items-center gap-2"
                      >
                        <FileSpreadsheet className="w-4 h-4 text-emerald-600" />
                        <span>Xuất Excel (.xlsx)</span>
                      </button>
                      <button
                        onClick={() => handleExport('PDF')}
                        className="w-full px-4 py-2 text-left text-xs font-medium text-slate-700 hover:bg-slate-50 flex items-center gap-2"
                      >
                        <FileSpreadsheet className="w-4 h-4 text-red-600" />
                        <span>Xuất PDF (.pdf)</span>
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Tree View Graphic Section */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              
              {/* UPSTREAM BRANCH (Vùng trồng -> Lô sản xuất) - Left Column */}
              <div className="lg:col-span-4 space-y-4">
                <div className="bg-slate-900 text-white p-4 rounded-xl flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Layers className="w-5 h-5 text-emerald-400" />
                    <h2 className="font-bold text-sm">CHIỀU NGƯỢC (UPSTREAM)</h2>
                  </div>
                  <span className="text-xs bg-slate-800 text-slate-300 px-2.5 py-1 rounded-full font-mono">
                    Nguồn gốc canh tác
                  </span>
                </div>

                {/* Vùng trồng Card */}
                {traceData.farmArea ? (
                  <div className="bg-white p-5 rounded-xl border border-emerald-200 shadow-sm relative border-l-4 border-l-emerald-500">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded">
                        VÙNG TRỒNG GỐC
                      </span>
                      <MapPin className="w-4 h-4 text-emerald-500" />
                    </div>
                    <h3 className="font-bold text-slate-900 text-base mt-2">{traceData.farmArea.name}</h3>
                    <div className="text-xs text-slate-500 mt-2 space-y-1">
                      <p><span className="font-medium text-slate-700">Mã vùng:</span> {traceData.farmArea.code}</p>
                      <p><span className="font-medium text-slate-700">Vị trí:</span> {traceData.farmArea.location}</p>
                      {traceData.farmArea.areaSize && (
                        <p><span className="font-medium text-slate-700">Diện tích:</span> {traceData.farmArea.areaSize} m²</p>
                      )}
                    </div>
                  </div>
                ) : (
                  <div className="bg-white p-4 rounded-xl border border-slate-200 text-slate-500 text-xs italic">
                    Chưa liên kết vùng trồng cố định.
                  </div>
                )}

                <div className="flex justify-center">
                  <ArrowRight className="w-5 h-5 text-slate-400 rotate-90" />
                </div>

                {/* Production Lot Card */}
                <div className="bg-white p-5 rounded-xl border border-blue-200 shadow-sm relative border-l-4 border-l-blue-500">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded">
                      LÔ SẢN XUẤT HẠT NHÂN
                    </span>
                    <span className={`text-xs px-2 py-0.5 rounded font-semibold ${
                      traceData.productionLot.status === 'APPROVED' ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-800'
                    }`}>
                      {traceData.productionLot.status}
                    </span>
                  </div>
                  <h3 className="font-bold text-slate-900 text-base mt-2">{traceData.productionLot.name}</h3>
                  <div className="text-xs text-slate-600 mt-2 space-y-1.5">
                    <p className="flex items-center gap-1.5">
                      <Package className="w-3.5 h-3.5 text-slate-400" />
                      <span>Sản lượng: <strong>{traceData.productionLot.expectedQuantity} {traceData.productionLot.expectedQuantityUnit}</strong></span>
                    </p>
                    {traceData.productionLot.plantingDate && (
                      <p className="flex items-center gap-1.5">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        <span>Xuống giống: {traceData.productionLot.plantingDate}</span>
                      </p>
                    )}
                    {traceData.productionLot.harvestDate && (
                      <p className="flex items-center gap-1.5">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        <span>Thu hoạch: {traceData.productionLot.harvestDate}</span>
                      </p>
                    )}
                  </div>
                </div>
              </div>

              {/* DOWNSTREAM BRANCH (Lô sản xuất -> Lô hàng -> Tem -> Đối tác) - Right Column */}
              <div className="lg:col-span-8 space-y-4">
                <div className="bg-slate-900 text-white p-4 rounded-xl flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <GitFork className="w-5 h-5 text-blue-400" />
                    <h2 className="font-bold text-sm">CHIỀU XUÔI (DOWNSTREAM) • PHẠM VI ẢNH HƯỞNG</h2>
                  </div>
                  <span className="text-xs bg-slate-800 text-slate-300 px-2.5 py-1 rounded-full font-mono">
                    {traceData.shipments.length} Lô hàng
                  </span>
                </div>

                {/* Empty State Banner (TC-03) */}
                {traceData.shipments.length === 0 ? (
                  <div className="bg-white rounded-xl border border-slate-200 p-8 text-center space-y-3">
                    <div className="w-12 h-12 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-400">
                      <Box className="w-6 h-6" />
                    </div>
                    <h3 className="font-bold text-slate-800 text-base">Lô chưa phát sinh lô hàng</h3>
                    <p className="text-xs text-slate-500 max-w-md mx-auto">
                      Lô sản xuất này hiện chưa tạo lô hàng thương mại nào để phân phối ra thị trường. Nhánh truy vết xuôi hiện đang rỗng.
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
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-mono text-xs font-bold text-slate-400">#{idx + 1}</span>
                              <h3 className="font-bold text-slate-900 text-base">{ship.name}</h3>
                            </div>
                            <p className="text-xs text-slate-500 mt-0.5">
                              Tạo ngày: {new Date(ship.createdAt).toLocaleDateString('vi-VN')} • Quy cách: {ship.packagingInfo || 'N/A'}
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
                              {ship.status === 'RECALLED' ? 'ĐÃ THU HỒI' : ship.status}
                            </span>
                          </div>
                        </div>

                        {/* Stats Badges Row */}
                        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 my-4">
                          <div className="bg-slate-50 p-2.5 rounded-lg text-xs">
                            <span className="text-slate-500 block">Số lượng lô:</span>
                            <span className="font-bold text-slate-800">{ship.totalQuantity.toLocaleString()}</span>
                          </div>
                          <div className="bg-emerald-50/60 p-2.5 rounded-lg text-xs">
                            <span className="text-emerald-700 block">Tem kích hoạt:</span>
                            <span className="font-bold text-emerald-900">{ship.activatedStampsCount.toLocaleString()} tem</span>
                          </div>
                          <div className="bg-blue-50/60 p-2.5 rounded-lg text-xs col-span-2 sm:col-span-1">
                            <span className="text-blue-700 block">Lượt quét công khai:</span>
                            <span className="font-bold text-blue-900">{ship.scanStats?.totalScans || 0} lượt</span>
                          </div>
                        </div>

                        {/* Receiving Organizations Timeline (TC-04 & QTN-01) */}
                        <div>
                          <p className="text-xs font-bold text-slate-700 mb-2 flex items-center gap-1.5">
                            <Building2 className="w-3.5 h-3.5 text-purple-600" />
                            <span>Tổ chức đã nhận hàng (Bên thứ ba):</span>
                          </p>

                          {ship.receivingOrganizations.length === 0 ? (
                            <div className="text-xs text-slate-400 italic bg-slate-50 p-2.5 rounded-lg">
                              Chưa ghi nhận sự kiện giao nhận tới đối tác bên ngoài HTX.
                            </div>
                          ) : (
                            <div className="space-y-2">
                              {ship.receivingOrganizations.map((org, oIdx) => (
                                <div
                                  key={oIdx}
                                  className="flex items-center justify-between p-2.5 bg-purple-50/50 border border-purple-100 rounded-lg text-xs"
                                >
                                  <div className="flex items-center gap-2">
                                    <div className="w-2 h-2 rounded-full bg-purple-500" />
                                    <span className="font-semibold text-purple-950">{org.organizationName}</span>
                                    <span className="text-[10px] bg-purple-100 text-purple-700 px-1.5 py-0.5 rounded font-mono">
                                      {org.eventType}
                                    </span>
                                  </div>
                                  <div className="text-purple-700 text-[11px] font-medium">
                                    Thời điểm nhận: {new Date(org.receivedAt).toLocaleString('vi-VN')}
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

      </div>
    </div>
  );
};

export default ImpactScopeTracePage;
