import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { getShipmentById } from '@/api/shipmentApi';
import { cancelTraceCodes } from '@/api/labelCancellationApi';
import type { Shipment, CancelTraceCodesPayload } from '@/types/shipment';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { HelpButton } from '@/components/help/HelpButton';
import { maskId } from '@/lib/utils';
import {
  AlertCircle,
  Ban,
  CheckCircle2,
  FileSpreadsheet,
  HelpCircle,
  Info,
  Loader2,
  QrCode,
  ShieldAlert,
} from 'lucide-react';
import { toast } from 'sonner';

export default function CancelLabelsPage() {
  const { id: shipmentId, lotId } = useParams<{ id: string; lotId?: string }>();
  const navigate = useNavigate();

  const [shipment, setShipment] = useState<Shipment | null>(null);
  const [loadingShipment, setLoadingShipment] = useState(true);
  const [shipmentError, setShipmentError] = useState<string | null>(null);

  // Form State
  const [cancelType, setCancelType] = useState<'RANGE' | 'SINGLE'>('RANGE');
  const [fromCode, setFromCode] = useState('');
  const [toCode, setToCode] = useState('');
  const [singleCodesInput, setSingleCodesInput] = useState('');
  const [reasonType, setReasonType] = useState<'PRINT_ERROR' | 'PRINT_MISALIGNED' | 'PEELED_OFF_DAMAGED' | 'OTHER'>('PRINT_ERROR');
  const [reasonNote, setReasonNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const backUrl = lotId
    ? `/production-lots/${lotId}/shipments/${shipmentId}`
    : `/shipments/${shipmentId}`;

  const historyUrl = lotId
    ? `/production-lots/${lotId}/shipments/${shipmentId}/cancellation-history`
    : `/shipments/${shipmentId}/cancellation-history`;

  useSetBreadcrumb(
    shipment
      ? [
        { label: 'Dashboard', href: '/dashboard' },
        { label: 'Lô sản xuất', href: '/production-lots' },
        ...(shipment.productionLotId
          ? [
            {
              label: shipment.productionLotName || 'Chi tiết lô sản xuất',
              href: `/production-lots/${shipment.productionLotId}`,
            },
          ]
          : []),
        { label: shipment.name || 'Chi tiết lô hàng', href: backUrl },
        { label: 'Hủy tem in hỏng' },
      ]
      : null,
  );

  useEffect(() => {
    async function loadShipmentData() {
      if (!shipmentId) return;
      setLoadingShipment(true);
      setShipmentError(null);
      try {
        const data = await getShipmentById(shipmentId);
        setShipment(data);
      } catch (err: any) {
        setShipmentError(
          err.response?.data?.message || err.message || 'Không thể tải thông tin lô hàng.',
        );
      } finally {
        setLoadingShipment(false);
      }
    }
    loadShipmentData();
  }, [shipmentId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!shipment) return;
    setErrorMsg(null);

    let codeValues: string[] = [];
    if (cancelType === 'SINGLE') {
      codeValues = singleCodesInput
        .split(/[\n,;]+/)
        .map((s) => s.trim())
        .filter((s) => s.length > 0);

      if (codeValues.length === 0) {
        setErrorMsg('Vui lòng nhập hoặc quét ít nhất 1 mã tem cần hủy.');
        return;
      }
    } else {
      if (!fromCode.trim() || !toCode.trim()) {
        setErrorMsg('Vui lòng nhập đầy đủ mã bắt đầu (From Code) và mã kết thúc (To Code).');
        return;
      }
    }

    if (reasonType === 'OTHER' && reasonNote.trim().length < 10) {
      setErrorMsg('Với lý do khác, vui lòng nhập ghi chú chi tiết ít nhất 10 ký tự.');
      return;
    }

    const payload: CancelTraceCodesPayload = {
      cancelType,
      fromCode: cancelType === 'RANGE' ? fromCode.trim() : undefined,
      toCode: cancelType === 'RANGE' ? toCode.trim() : undefined,
      codeValues: cancelType === 'SINGLE' ? codeValues : undefined,
      reasonType,
      reasonNote: reasonNote.trim() || undefined,
    };

    setSubmitting(true);
    try {
      const result = await cancelTraceCodes(shipment.id, payload);
      toast.success(
        `Hủy thành công ${result.totalCancelled} tem in hỏng! Đã hoàn lại ${result.refundedQuota} hạn mức dải mã.`,
      );
      navigate(historyUrl);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Lỗi khi thực hiện hủy tem.';
      setErrorMsg(msg);
    } finally {
      setSubmitting(false);
    }
  };

  if (loadingShipment) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-3">
        <Loader2 className="size-8 animate-spin text-emerald-600" />
        <p className="text-sm font-medium text-slate-500">Đang tải thông tin lô hàng...</p>
      </div>
    );
  }

  if (shipmentError || !shipment) {
    return (
      <div className="mx-auto max-w-4xl p-6">
        <div className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 p-5 text-red-700">
          <AlertCircle className="mt-0.5 size-5 shrink-0" />
          <div>
            <p className="font-semibold">Không thể tải dữ liệu lô hàng</p>
            <p className="mt-1 text-sm">{shipmentError || 'Lô hàng không tồn tại.'}</p>
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigate('/production-lots')}
              className="mt-4 border-red-300 text-red-700 hover:bg-red-100"
            >
              Quay lại danh sách lô
            </Button>
          </div>
        </div>
      </div>
    );
  }

  const parsedSingleCount = singleCodesInput
    .split(/[\n,;]+/)
    .map((s) => s.trim())
    .filter((s) => s.length > 0).length;

  return (
    <div className="mx-auto max-w-6xl space-y-6 p-4 md:p-6">
      {/* Top Header Card */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold text-slate-900">
              Hủy tem in hỏng & hoàn hạn mức
            </h1>
            <Badge variant="outline" className="border-red-200 bg-red-50 text-red-700">
              Lô: {shipment.name}
            </Badge>
          </div>
          <p className="mt-1 text-xs text-slate-500">
            Khai báo mã tem bị lỗi in nhòe, lệch viền hoặc bong tróc để thu hồi hạn mức dải mã nguyên tử.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <HelpButton screenKey="cancel-labels" />
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate(historyUrl)}
            className="gap-2 text-xs"
          >
            Xem lịch sử hủy tem
          </Button>
        </div>
      </div>

      {/* Main Content Layout: 2 Columns */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Left Column (2/3 width): Main Form Card */}
        <div className="lg:col-span-2">
          <Card className="border-slate-200 shadow-sm">
            <CardHeader className="border-b border-slate-100 bg-slate-50/50 pb-4">
              <div className="flex items-center gap-2">
                <div className="flex size-8 items-center justify-center rounded-lg bg-red-100 text-red-600">
                  <Ban className="size-4" />
                </div>
                <div>
                  <CardTitle className="text-base font-bold text-slate-800">
                    Khai báo đợt hủy tem in hỏng
                  </CardTitle>
                  <CardDescription className="text-xs">
                    Vui lòng chọn phương thức nhập và điền đầy đủ lý do tiêu hủy.
                  </CardDescription>
                </div>
              </div>
            </CardHeader>

            <CardContent className="p-6">
              <form onSubmit={handleSubmit} className="space-y-6">
                {errorMsg && (
                  <div className="flex items-start gap-2.5 rounded-xl border border-red-200 bg-red-50/80 p-4 text-xs text-red-700">
                    <AlertCircle className="size-4 shrink-0 mt-0.5" />
                    <div className="font-medium">{errorMsg}</div>
                  </div>
                )}

                {/* Section 1: Phương thức hủy */}
                <div className="space-y-2">
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700">
                    1. Chọn phương thức nhập danh sách tem <span className="text-red-500">*</span>
                  </label>

                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                    <button
                      type="button"
                      onClick={() => {
                        setCancelType('RANGE');
                        setErrorMsg(null);
                      }}
                      className={`flex flex-col items-start gap-1 rounded-xl border p-4 text-left transition ${cancelType === 'RANGE'
                        ? 'border-red-500 bg-red-50/50 ring-2 ring-red-500/20'
                        : 'border-slate-200 bg-white hover:bg-slate-50'
                        }`}
                    >
                      <div className="flex items-center gap-2 font-semibold text-xs text-slate-900">
                        <FileSpreadsheet className={`size-4 ${cancelType === 'RANGE' ? 'text-red-600' : 'text-slate-500'}`} />
                        Theo khoảng mã (Sequence Range)
                      </div>
                      <p className="text-[11px] text-slate-500">
                        Phù hợp khi hủy 1 dải tem in hỏng liên tiếp từ mã A đến mã B.
                      </p>
                    </button>

                    <button
                      type="button"
                      onClick={() => {
                        setCancelType('SINGLE');
                        setErrorMsg(null);
                      }}
                      className={`flex flex-col items-start gap-1 rounded-xl border p-4 text-left transition ${cancelType === 'SINGLE'
                        ? 'border-red-500 bg-red-50/50 ring-2 ring-red-500/20'
                        : 'border-slate-200 bg-white hover:bg-slate-50'
                        }`}
                    >
                      <div className="flex items-center gap-2 font-semibold text-xs text-slate-900">
                        <QrCode className={`size-4 ${cancelType === 'SINGLE' ? 'text-red-600' : 'text-slate-500'}`} />
                        Quét / Nhập từng mã lẻ
                      </div>
                      <p className="text-[11px] text-slate-500">
                        Dùng máy quét QR/Barcode hoặc dán danh sách mã tem bị lộn xộn.
                      </p>
                    </button>
                  </div>
                </div>

                {/* Section 2: Nhập mã tem */}
                <div className="space-y-3 rounded-xl border border-slate-100 bg-slate-50/50 p-4">
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700">
                    2. Thông tin mã tem cần hủy <span className="text-red-500">*</span>
                  </label>

                  {cancelType === 'RANGE' ? (
                    <div className="space-y-3">
                      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                        <div>
                          <label className="mb-1 block text-xs font-medium text-slate-600">
                            Từ mã tem (From Code) <span className="text-red-500">*</span>
                          </label>
                          <Input
                            type="text"
                            placeholder="Ví dụ: HTX01-00000010"
                            value={fromCode}
                            onChange={(e) => setFromCode(e.target.value)}
                            className="bg-white text-xs"
                            required
                          />
                        </div>
                        <div>
                          <label className="mb-1 block text-xs font-medium text-slate-600">
                            Đến mã tem (To Code) <span className="text-red-500">*</span>
                          </label>
                          <Input
                            type="text"
                            placeholder="Ví dụ: HTX01-00000025"
                            value={toCode}
                            onChange={(e) => setToCode(e.target.value)}
                            className="bg-white text-xs"
                            required
                          />
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <label className="text-xs font-medium text-slate-600">
                          Danh sách mã tem lẻ (Phân cách bằng dấu xuống dòng hoặc phẩy)
                        </label>
                        {parsedSingleCount > 0 && (
                          <Badge variant="outline" className="border-red-200 bg-red-50 text-red-700 text-[10px]">
                            Đã nhận diện {parsedSingleCount} mã
                          </Badge>
                        )}
                      </div>
                      <textarea
                        rows={5}
                        placeholder="Quét mã QR bằng đầu quét hoặc dán danh sách mã vào đây..."
                        value={singleCodesInput}
                        onChange={(e) => setSingleCodesInput(e.target.value)}
                        className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-xs font-mono focus:border-red-500 focus:outline-none focus:ring-1 focus:ring-red-500"
                        required
                      />
                    </div>
                  )}
                </div>

                {/* Section 3: Lý do hủy & Ghi chú */}
                <div className="space-y-3">
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700">
                    3. Lý do tiêu hủy tem <span className="text-red-500">*</span>
                  </label>

                  <div>
                    <select
                      value={reasonType}
                      onChange={(e: any) => setReasonType(e.target.value)}
                      className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 focus:border-red-500 focus:outline-none"
                    >
                      <option value="PRINT_ERROR">Tem in hỏng / in mờ / nhòe mực không đọc được QR</option>
                      <option value="PRINT_MISALIGNED">Tem in lệch / lệch lề cắt đứt viền</option>
                      <option value="PEELED_OFF_DAMAGED">Tem bị rách / bong tróc trong quá trình dán</option>
                      <option value="OTHER">Lý do khác (Cần ghi rõ giải trình)</option>
                    </select>
                  </div>

                  <div>
                    <label className="mb-1 block text-xs font-medium text-slate-600">
                      Ghi chú chi tiết {reasonType === 'OTHER' && <span className="text-red-500">* (Bắt buộc tối thiểu 10 ký tự)</span>}
                    </label>
                    <Input
                      type="text"
                      placeholder="Ví dụ: Kẹt giấy máy in lô #2 làm rách 15 tem"
                      value={reasonNote}
                      onChange={(e) => setReasonNote(e.target.value)}
                      className="bg-white text-xs"
                    />
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="flex items-center justify-end gap-3 border-t border-slate-100 pt-4">
                  <Button
                    type="button"
                    variant="outline"
                    disabled={submitting}
                    onClick={() => navigate(backUrl)}
                    className="text-xs"
                  >
                    Hủy bỏ & Quay lại
                  </Button>
                  <Button
                    type="submit"
                    disabled={submitting}
                    className="bg-red-600 hover:bg-red-700 text-white font-medium text-xs px-5 py-2 shadow-sm"
                  >
                    {submitting ? (
                      <>
                        <Loader2 className="mr-1.5 size-3.5 animate-spin" />
                        Đang xử lý hủy tem...
                      </>
                    ) : (
                      'Xác nhận hủy & Hoàn hạn mức'
                    )}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>

        {/* Right Column (1/3 width): Rules & Guidelines Cards */}
        <div className="space-y-6 lg:col-span-1">
          {/* Business Rules Info Card */}
          <Card className="border-amber-200 bg-gradient-to-br from-amber-50/60 to-white shadow-sm">
            <CardHeader className="pb-3">
              <CardTitle className="text-sm font-bold text-amber-900 flex items-center gap-2">
                <ShieldAlert className="size-4 text-amber-600" />
                Quy tắc nghiệp vụ hủy tem
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-xs text-amber-900/90">
              <div className="flex items-start gap-2">
                <CheckCircle2 className="size-4 shrink-0 text-amber-600 mt-0.5" />
                <p>
                  <strong>Trạng thái áp dụng:</strong> Chỉ được đánh dấu hủy những mã tem ở trạng thái <em>Chưa kích hoạt (INACTIVE)</em>.
                </p>
              </div>
              <div className="flex items-start gap-2">
                <CheckCircle2 className="size-4 shrink-0 text-amber-600 mt-0.5" />
                <p>
                  <strong>Hoàn hạn mức nguyên tử:</strong> Ngay khi xác nhận hủy, hạn mức dải mã tương ứng sẽ được trả lại cho Hợp tác xã sử dụng cho các sản phẩm khác.
                </p>
              </div>
              <div className="flex items-start gap-2">
                <CheckCircle2 className="size-4 shrink-0 text-amber-600 mt-0.5" />
                <p>
                  <strong>Nhật ký Audit:</strong> Thao tác này sẽ ghi lại thời gian, tài khoản thực hiện và lý do vào nhật ký kiểm soát công khai.
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Quick Info & Tips Card */}
          <Card className="border-slate-200 shadow-sm">
            <CardHeader className="pb-3">
              <CardTitle className="text-sm font-bold text-slate-800 flex items-center gap-2">
                <Info className="size-4 text-emerald-600" />
                Thông tin lô hàng hiện tại
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2.5 text-xs text-slate-600">
              <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                <span className="text-slate-500">Tên lô hàng:</span>
                <span className="font-semibold text-slate-800">{shipment.name}</span>
              </div>
              <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                <span className="text-slate-500">Mã lô hàng:</span>
                <span className="font-mono font-medium text-slate-700">{maskId(shipment.id)}</span>
              </div>
              <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                <span className="text-slate-500">Tổng số tem lô:</span>
                <span className="font-semibold text-emerald-700">
                  {shipment.traceCodes?.length?.toLocaleString('vi-VN') || 0} tem
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Người tạo:</span>
                <span className="font-medium text-slate-700">{shipment.createdByName || 'Tài khoản hệ thống'}</span>
              </div>
            </CardContent>
          </Card>

          {/* Help Tip Card */}
          <div className="rounded-xl border border-blue-100 bg-blue-50/50 p-4 text-xs text-blue-800 space-y-2">
            <div className="flex items-center gap-1.5 font-semibold text-blue-900">
              <HelpCircle className="size-4 text-blue-600" />
              Hướng dẫn thao tác nhanh
            </div>
            <p className="text-blue-800/90 leading-relaxed">
              Bạn có thể sử dụng súng quét mã vạch chuyên dụng cắm cổng USB. Đặt con trỏ vào ô nhập danh sách mã lẻ và bấm quét liên tục, các mã sẽ được xuống dòng tự động.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
