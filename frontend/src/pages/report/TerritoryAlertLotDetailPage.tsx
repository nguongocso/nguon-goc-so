import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  Building2,
  Clock,
  GitBranch,
  Lock,
  MapPin,
  Package,
  ShieldAlert,
  User,
  XCircle,
} from 'lucide-react';

import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import {
  ProductionLotStatusBadge,
  type ProductionLotStatus,
} from '@/components/production-lot/ProductionLotStatusBadge';

import { getAlertLotDetail } from '@/api/territoryAlertLotApi';
import { formatDateTime } from '@/utils/dateTime';
import {
  ALERT_TYPE_CONFIG_MAP,
  type AlertLotDetailResponse,
  type LotAlertEvidenceDetail,
  type ReadonlyChainEventItem,
} from '@/types/territoryAlertLot';

/**
 * Việt hóa mức độ cảnh báo kỹ thuật (CRITICAL, HIGH, MEDIUM, LOW) sang tiếng Việt trực quan.
 */
function getSeverityBadge(severity?: string) {
  switch (severity?.toUpperCase()) {
    case 'CRITICAL':
      return (
        <Badge variant="destructive" className="text-xs font-bold bg-red-600 hover:bg-red-700 text-white border-red-700 shadow-2xs">
          Rất nghiêm trọng
        </Badge>
      );
    case 'HIGH':
      return (
        <Badge className="text-xs font-semibold bg-amber-600 hover:bg-amber-700 text-white border-amber-700 shadow-2xs">
          Nghiêm trọng
        </Badge>
      );
    case 'MEDIUM':
      return (
        <Badge variant="secondary" className="text-xs font-medium bg-yellow-100 text-yellow-800 border-yellow-300">
          Trung bình
        </Badge>
      );
    case 'LOW':
      return (
        <Badge variant="outline" className="text-xs text-slate-700 border-slate-300">
          Thấp
        </Badge>
      );
    default:
      return (
        <Badge variant="secondary" className="text-xs">
          {severity || 'Chưa rõ'}
        </Badge>
      );
  }
}

/**
 * Component hiển thị thông tin bằng chứng cảnh báo dưới dạng văn bản và nhãn trực quan,
 * giúp người dùng và cán bộ quản lý đọc hiểu nhanh chóng thay vì xem định dạng JSON thô.
 */
function AlertEvidenceViewer({
  evidenceData,
}: {
  evidenceData: Record<string, unknown>;
}) {
  if (!evidenceData || Object.keys(evidenceData).length === 0) {
    return null;
  }

  const renderItem = (key: string, value: unknown) => {
    if (value === null || value === undefined || value === '') return null;

    let label = key;
    let formattedValue: React.ReactNode = String(value);

    switch (key) {
      // Bỏ qua vì đã hiển thị nổi bật ở thanh truy vết đầu khung
      case 'affectedStage':
      case 'traceRule':
      case 'failedCriteriaCount':
      case 'totalCriteriaCount':
      case 'passedCriteriaCount':
      case 'rawEventData':
        return null;

      case 'lotStatus':
        label = 'Trạng thái lô sản xuất';
        formattedValue = (
          <ProductionLotStatusBadge
            status={String(value) as ProductionLotStatus}
            className="text-xs font-medium"
          />
        );
        break;

      case 'recallScope':
        label = 'Phạm vi thu hồi';
        formattedValue = <span className="font-bold text-red-900">{String(value)}</span>;
        break;

      case 'hasRecalledShipment':
        // Nếu đã có danh sách lô hàng cụ thể thì không hiển thị dòng boolean trùng lặp
        if (evidenceData.recalledShipmentList || evidenceData.recalledShipmentNames) {
          return null;
        }
        label = 'Lô hàng xuất kho bị thu hồi';
        formattedValue = value ? (
          <Badge variant="destructive" className="text-xs font-medium">
            Có lô hàng bị thu hồi
          </Badge>
        ) : (
          <span className="text-slate-600">Không có</span>
        );
        break;

      case 'recalledShipmentList':
        label = 'Danh sách lô hàng bị thu hồi';
        if (Array.isArray(value) && value.length > 0) {
          formattedValue = (
            <div className="mt-1 space-y-1.5 w-full">
              {value.map((item: any, idx) => (
                <div
                  key={idx}
                  className="flex flex-wrap items-center justify-between gap-2 p-2 rounded-md bg-red-50 border border-red-200 text-xs"
                >
                  <div className="flex items-center gap-1.5">
                    <Package className="size-3.5 text-red-600" />
                    <span className="font-bold text-red-950">
                      Lô hàng: {item.name || item.id}
                    </span>
                    {item.quantity != null && (
                      <span className="text-slate-600 font-medium">
                        ({Number(item.quantity).toLocaleString('vi-VN')} kg)
                      </span>
                    )}
                  </div>
                  <Badge variant="destructive" className="text-2xs font-semibold px-2 py-0.5">
                    {item.status === 'RECALLED'
                      ? 'Đã thu hồi'
                      : item.status === 'RECALLING'
                      ? 'Đang thu hồi'
                      : item.status || 'Thu hồi'}
                  </Badge>
                </div>
              ))}
            </div>
          );
        }
        break;

      case 'recalledShipmentNames':
        if (evidenceData.recalledShipmentList) return null;
        label = 'Lô hàng bị thu hồi';
        if (Array.isArray(value)) {
          formattedValue = (
            <div className="flex flex-wrap gap-1.5 justify-end">
              {value.map((sName, idx) => (
                <Badge key={idx} variant="destructive" className="text-xs font-semibold">
                  {String(sName)}
                </Badge>
              ))}
            </div>
          );
        }
        break;

      case 'targetShipmentName':
        label = 'Lô hàng chỉ định thu hồi';
        formattedValue = <span className="font-bold text-red-900">{String(value)}</span>;
        break;

      case 'recallReason':
        label = 'Lý do thu hồi';
        formattedValue = <span className="text-slate-800 font-medium">{String(value)}</span>;
        break;

      case 'recallStatus':
        label = 'Trạng thái yêu cầu thu hồi';
        formattedValue = (
          <Badge variant="outline" className="text-xs font-medium border-amber-300 bg-amber-50 text-amber-800">
            {value === 'APPROVED'
              ? 'Đã phê duyệt thu hồi'
              : value === 'PENDING'
              ? 'Đang chờ duyệt'
              : String(value)}
          </Badge>
        );
        break;

      case 'recallRequestId':
        label = 'Mã yêu cầu thu hồi';
        formattedValue = <span className="font-mono text-xs text-slate-700">{String(value)}</span>;
        break;

      case 'requestedAt':
        label = 'Thời điểm yêu cầu';
        formattedValue = <span className="text-slate-700">{formatDateTime(String(value))}</span>;
        break;

      case 'approvedAt':
        label = 'Thời điểm phê duyệt';
        formattedValue = <span className="text-slate-700">{formatDateTime(String(value))}</span>;
        break;

      case 'failedRatio':
        label = 'Tỷ lệ chỉ tiêu không đạt';
        formattedValue = (
          <Badge variant="destructive" className="text-xs font-bold bg-red-600 text-white border-red-700">
            {String(value)}
          </Badge>
        );
        break;

      case 'failedCriteriaNames':
        label = 'Chỉ tiêu không đạt';
        if (Array.isArray(value)) {
          formattedValue = (
            <div className="flex flex-wrap gap-1.5 justify-end">
              {value.map((critName, idx) => (
                <span
                  key={idx}
                  className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-red-100 text-red-900 text-xs font-bold border border-red-300"
                >
                  <XCircle className="size-3 text-red-600" />
                  {String(critName)}
                </span>
              ))}
            </div>
          );
        }
        break;

      case 'criterionName':
        label = 'Chỉ tiêu không đạt';
        formattedValue = <span className="font-semibold text-red-800">{String(value)}</span>;
        break;

      case 'measuredValue':
        label = 'Hàm lượng đo được';
        formattedValue = <span className="font-bold text-red-700">{String(value)}</span>;
        break;

      case 'maxAllowedLimit':
        label = 'Ngưỡng tối đa cho phép (MRL)';
        formattedValue = <span className="font-medium text-slate-700">{String(value)}</span>;
        break;

      case 'testUnitName':
        label = 'Đơn vị phân tích';
        formattedValue = <span className="text-slate-700">{String(value)}</span>;
        break;

      case 'inspectionUnit':
        label = 'Đơn vị kiểm nghiệm';
        formattedValue = <span className="font-medium text-slate-800">{String(value)}</span>;
        break;

      case 'sampleSentDate':
        label = 'Ngày gửi mẫu';
        formattedValue = <span className="text-slate-700">{formatDateTime(String(value))}</span>;
        break;

      case 'scopeWarningDetails':
        label = 'Chi tiết kiểm nghiệm';
        formattedValue = <span className="text-red-700 font-medium">{String(value)}</span>;
        break;

      case 'lockedCount':
        label = 'Số lượng tem bị khóa';
        formattedValue = <span className="font-bold text-red-700">{String(value)} tem</span>;
        break;

      case 'sampleCodes':
        label = 'Mã tem nghi vấn';
        if (Array.isArray(value)) {
          formattedValue = (
            <div className="flex flex-wrap gap-1 justify-end">
              {value.map((code, idx) => (
                <Badge key={idx} variant="outline" className="font-mono text-xs bg-slate-50 text-slate-700">
                  {String(code)}
                </Badge>
              ))}
            </div>
          );
        }
        break;

      case 'openFeedbackCount':
        label = 'Số phản ánh chưa xử lý';
        formattedValue = <span className="font-bold text-red-700">{String(value)} phản ánh</span>;
        break;

      case 'feedbackStatus':
        label = 'Trạng thái phản ánh';
        formattedValue = (
          <Badge variant="outline" className="text-xs">
            {value === 'OPEN' ? 'Đang mở' : value === 'IN_PROGRESS' ? 'Đang xử lý' : String(value)}
          </Badge>
        );
        break;

      case 'severity':
        label = 'Mức độ vi phạm';
        formattedValue = (
          <Badge variant="destructive" className="text-xs font-medium">
            {value === 'QUALITY_SUSPECTED'
              ? 'Nghi vấn chất lượng'
              : value === 'COUNTERFEIT_SUSPECTED'
              ? 'Nghi vấn hàng giả'
              : String(value)}
          </Badge>
        );
        break;

      case 'content':
        label = 'Nội dung phản ánh';
        formattedValue = <span className="text-slate-800 italic">"{String(value)}"</span>;
        break;

      case 'recordedAt':
        label = 'Thời điểm ghi nhận';
        formattedValue = <span className="text-slate-700">{formatDateTime(String(value))}</span>;
        break;

      case 'eventId':
        label = 'Mã sự kiện';
        formattedValue = <span className="font-mono text-xs text-slate-600">{String(value)}</span>;
        break;

      case 'inspectionRequestId':
        label = 'Mã yêu cầu kiểm nghiệm';
        formattedValue = <span className="font-mono text-xs text-slate-600">{String(value)}</span>;
        break;

      case 'alertId':
        label = 'Mã cảnh báo';
        formattedValue = <span className="font-mono text-xs text-slate-600">{String(value)}</span>;
        break;

      case 'message':
        label = 'Nội dung chi tiết';
        formattedValue = <span className="text-slate-800">{String(value)}</span>;
        break;

      default:
        label = key.replace(/([A-Z])/g, ' $1').replace(/^./, (str) => str.toUpperCase());
        if (typeof value === 'boolean') {
          formattedValue = value ? 'Có' : 'Không';
        } else if (typeof value === 'object') {
          formattedValue = JSON.stringify(value);
        }
        break;
    }

    return (
      <div
        key={key}
        className="flex flex-col sm:flex-row sm:items-center sm:justify-between py-1.5 border-b border-dashed border-slate-200 last:border-b-0 gap-1 text-xs"
      >
        <span className="text-slate-600 font-medium">{label}:</span>
        <div className="text-slate-900 sm:text-right">{formattedValue}</div>
      </div>
    );
  };

  const renderedItems = Object.entries(evidenceData)
    .map(([k, v]) => renderItem(k, v))
    .filter(Boolean);

  const hasTraceContext = Boolean(evidenceData.affectedStage || evidenceData.traceRule);

  if (renderedItems.length === 0 && !hasTraceContext) {
    return null;
  }

  return (
    <div className="mt-3 rounded-lg border border-slate-200 bg-white p-3.5 shadow-2xs">
      {/* Khung phân đoạn chuỗi cung ứng & Quy tắc truy vết vi phạm */}
      {hasTraceContext && (
        <div className="flex items-start gap-2 rounded-md bg-red-50/70 border border-red-200 p-2.5 text-xs mb-3">
          <GitBranch className="size-4 text-red-600 shrink-0 mt-0.5" />
          <div className="space-y-0.5">
            <div className="font-bold text-red-950">
              {String(evidenceData.affectedStage || 'Khâu phát sinh cảnh báo vi phạm')}
            </div>
            {Boolean(evidenceData.traceRule) && (
              <p className="text-2xs text-red-800 leading-snug">
                {String(evidenceData.traceRule)}
              </p>
            )}
          </div>
        </div>
      )}

      <p className="text-xs font-bold text-slate-800 mb-2 flex items-center gap-1.5">
        <span className="inline-block size-1.5 rounded-full bg-red-600" />
        Bằng chứng chi tiết:
      </p>
      <div className="space-y-0.5">{renderedItems}</div>
    </div>
  );
}

/**
 * Chuẩn hóa hiển thị địa điểm/tọa độ của sự kiện chuỗi cung ứng.
 * Thay vì hiển thị chuỗi kỹ thuật POINT (X Y), chuẩn hóa về "Tọa độ: X, Y".
 */
function formatLocationDisplay(loc?: string | null) {
  if (!loc) return null;
  const trimmed = loc.trim();
  // Chuẩn hóa định dạng WKT: POINT (105.8865152 21.5482368)
  if (/^point\s*\(/i.test(trimmed)) {
    const coords = trimmed.replace(/^point\s*\(([^)]+)\)/i, '$1').trim().replace(/\s+/, ', ');
    return `Tọa độ: ${coords}`;
  }
  // Nếu đã bắt đầu bằng "Tọa độ:" hoặc "Tạo độ:"
  if (/^t[ạọ]o\s*độ:/i.test(trimmed)) {
    return trimmed.replace(/^t[ạọ]o\s*độ:\s*/i, 'Tọa độ: ');
  }
  // Trường hợp là chuỗi địa danh/địa chỉ hành chính thông thường
  return `Địa điểm: ${trimmed}`;
}

export default function TerritoryAlertLotDetailPage() {
  const { lotId } = useParams<{ lotId: string }>();

  const [detail, setDetail] = useState<AlertLotDetailResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isForbidden, setIsForbidden] = useState<boolean>(false);

  // Đăng ký breadcrumb động (chỉ hiển thị tên lô sản xuất, không có chữ "Chi tiết")
  useSetBreadcrumb(
    detail?.lotInfo
      ? [
          { label: 'Tổng quan', href: '/dashboard' },
          { label: 'Theo dõi lô có cảnh báo', href: '/reports/alert-lots' },
          {
            label: detail.lotInfo.lotName || 'Lô sản xuất',
          },
        ]
      : null,
  );

  useEffect(() => {
    if (!lotId) {
      setErrorMessage('Mã định danh lô sản xuất không hợp lệ.');
      setIsLoading(false);
      return;
    }

    let isMounted = true;
    setIsLoading(true);
    setErrorMessage(null);
    setIsForbidden(false);

    getAlertLotDetail(lotId)
      .then((res) => {
        if (!isMounted) return;
        setDetail(res);
      })
      .catch((err: unknown) => {
        if (!isMounted) return;
        const errObj = err as { status?: number; message?: string };
        if (errObj.status === 403) {
          setIsForbidden(true);
          setErrorMessage('Bạn không có quyền xem chi tiết lô này do nằm ngoài địa bàn phụ trách.');
        } else if (errObj.status === 404) {
          setErrorMessage('Không tìm thấy thông tin lô sản xuất hoặc lô đã bị xóa.');
        } else {
          setErrorMessage(errObj.message || 'Không thể tải chi tiết lô cảnh báo. Vui lòng thử lại sau.');
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [lotId]);

  // Loading state
  if (isLoading) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center space-y-4">
        <div className="size-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent" />
        <p className="text-sm font-medium text-muted-foreground">
          Đang tải chi tiết lô có cảnh báo...
        </p>
      </div>
    );
  }

  // 403 Forbidden State: Xử lý an toàn khi truy cập lô ngoài địa bàn
  if (isForbidden) {
    return (
      <div className="space-y-6 py-6">
        <Card className="border-red-200 bg-red-50/40">
          <CardContent className="flex flex-col items-center py-16 text-center">
            <div className="rounded-full bg-red-100 p-4 text-red-700 mb-4 shadow-sm ring-4 ring-red-50">
              <Lock className="size-8" />
            </div>
            <h2 className="text-xl font-bold text-red-950">
              Không có quyền truy cập lô sản xuất
            </h2>
            <p className="mt-2 text-sm text-red-800 max-w-md">
              Lô sản xuất này thuộc địa bàn quản lý khác ngoài phạm vi được phân công của bạn. Hệ thống tuân thủ nghiêm ngặt quy tắc bảo mật phân quyền theo địa bàn (NCL-670 / NCL-742).
            </p>
            <Link
              to="/reports/alert-lots"
              className="mt-6 inline-flex items-center justify-center rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-medium text-red-900 shadow-sm hover:bg-red-100 transition-colors"
            >
              <ArrowLeft className="size-4 mr-2" />
              Quay lại danh sách theo dõi lô có cảnh báo
            </Link>
          </CardContent>
        </Card>
      </div>
    );
  }

  // Error state chung (404 hoặc lỗi server)
  if (errorMessage || !detail) {
    return (
      <div className="space-y-6 py-6">
        <Alert variant="destructive">
          <AlertCircle className="size-4" />
          <AlertTitle>Không thể tải dữ liệu</AlertTitle>
          <AlertDescription>
            {errorMessage || 'Dữ liệu chi tiết lô không tồn tại hoặc đã bị xóa.'}
          </AlertDescription>
        </Alert>
        <div>
          <Link
            to="/reports/alert-lots"
            className="inline-flex items-center justify-center rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-800 shadow-sm hover:bg-slate-50 transition-colors"
          >
            <ArrowLeft className="size-4 mr-2" />
            Quay lại danh sách
          </Link>
        </div>
      </div>
    );
  }

  const { lotInfo, organization, activeAlerts, timelineEvents } = detail;

  return (
    <div className="space-y-6 pb-12">
      {/* Header trang chi tiết: Loại bỏ dòng <- Danh sách / LOT-..., loại bỏ Chế độ chỉ xem, Việt hóa trạng thái */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b pb-4">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
              {lotInfo.lotName || 'Chi tiết lô cảnh báo'}
            </h1>

            {lotInfo.status && (
              <ProductionLotStatusBadge
                status={lotInfo.status as ProductionLotStatus}
                className="text-xs font-medium"
              />
            )}
          </div>
        </div>
      </div>

      {/* Grid 2 cột: Thông tin lô & Thông tin tổ chức sở hữu */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Card 1: Thông tin công khai lô sản xuất */}
        <Card>
          <CardHeader className="pb-3 border-b bg-slate-50/50">
            <CardTitle className="flex items-center gap-2 text-base text-slate-800">
              <Package className="size-5 text-emerald-600" />
              Thông tin lô sản xuất
            </CardTitle>
            <CardDescription className="text-xs">
              Các thông số kỹ thuật và định danh công khai của lô
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 pt-4 text-sm">
            <div className="flex justify-between py-1 border-b border-dashed">
              <span className="text-muted-foreground">Tên lô sản phẩm:</span>
              <span className="font-medium text-slate-900">{lotInfo.lotName}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-dashed">
              <span className="text-muted-foreground">Loại nông sản:</span>
              <span className="font-medium text-slate-800">{lotInfo.productCategoryName || 'Chưa phân loại'}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-dashed">
              <span className="text-muted-foreground">Vùng trồng:</span>
              <span className="font-medium text-slate-800">{lotInfo.farmAreaName || 'Chưa xác định'}</span>
            </div>
            {lotInfo.farmAreaAddress && (
              <div className="flex justify-between py-1 border-b border-dashed">
                <span className="text-muted-foreground">Địa chỉ vùng trồng:</span>
                <span className="text-right text-slate-800 max-w-xs">{lotInfo.farmAreaAddress}</span>
              </div>
            )}
            <div className="flex justify-between py-1 border-b border-dashed">
              <span className="text-muted-foreground">Sản lượng dự kiến:</span>
              <span className="font-medium text-slate-800">
                {lotInfo.expectedQuantity != null
                  ? `${lotInfo.expectedQuantity.toLocaleString('vi-VN')} ${lotInfo.quantityUnit || 'kg'}`
                  : 'Chưa cập nhật'}
              </span>
            </div>
            <div className="flex justify-between py-1 border-b border-dashed">
              <span className="text-muted-foreground">Sản lượng thực tế:</span>
              <span className="font-medium text-slate-800">
                {lotInfo.actualQuantity != null
                  ? `${lotInfo.actualQuantity.toLocaleString('vi-VN')} ${lotInfo.quantityUnit || 'kg'}`
                  : 'Chưa cập nhật'}
              </span>
            </div>
            <div className="flex justify-between py-1 border-b border-dashed">
              <span className="text-muted-foreground">Ngày gieo trồng:</span>
              <span className="text-slate-800">{lotInfo.plantingDate || 'Chưa cập nhật'}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-dashed">
              <span className="text-muted-foreground">Ngày thu hoạch:</span>
              <span className="text-slate-800">{lotInfo.harvestDate || 'Chưa cập nhật'}</span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-muted-foreground">Thời điểm tạo lô:</span>
              <span className="text-slate-800">
                {lotInfo.createdAt ? formatDateTime(lotInfo.createdAt) : 'Chưa cập nhật'}
              </span>
            </div>
          </CardContent>
        </Card>

        {/* Card 2: Thông tin tổ chức sở hữu */}
        <Card>
          <CardHeader className="pb-3 border-b bg-slate-50/50">
            <CardTitle className="flex items-center gap-2 text-base text-slate-800">
              <Building2 className="size-5 text-emerald-600" />
              Tổ chức sở hữu
            </CardTitle>
            <CardDescription className="text-xs">
              Hợp tác xã hoặc doanh nghiệp quản lý lô sản xuất
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 pt-4 text-sm">
            <div className="flex justify-between py-1 border-b border-dashed">
              <span className="text-muted-foreground">Tên tổ chức:</span>
              <span className="font-bold text-slate-900 text-right">{organization.organizationName}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-dashed">
              <span className="text-muted-foreground">Mã định danh:</span>
              <span className="font-mono text-slate-800">{organization.taxCode || 'Chưa cập nhật'}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-dashed">
              <span className="text-muted-foreground">Số điện thoại liên hệ:</span>
              <span className="font-mono text-slate-800">{organization.contactPhone || 'Chưa cập nhật'}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-dashed">
              <span className="text-muted-foreground">Địa chỉ trụ sở:</span>
              <span className="text-right text-slate-800 max-w-xs">{organization.address || 'Chưa cập nhật'}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-dashed">
              <span className="text-muted-foreground">Xã / Phường:</span>
              <span className="text-slate-800">{organization.communeName || 'Chưa cập nhật'}</span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-muted-foreground">Tỉnh / Thành phố:</span>
              <span className="font-medium text-slate-800">{organization.provinceName || 'Chưa cập nhật'}</span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Card 3: Danh sách cảnh báo đang kích hoạt (Active Alerts) */}
      <Card className="border-red-200">
        <CardHeader className="pb-3 border-b bg-red-50/40">
          <CardTitle className="flex items-center gap-2 text-base text-red-950">
            <ShieldAlert className="size-5 text-red-600" />
            Cảnh báo đang kích hoạt ({activeAlerts?.length || 0})
          </CardTitle>
          <CardDescription className="text-xs text-red-800">
            Các vi phạm an toàn, tem nghi vấn hoặc kết quả kiểm nghiệm không đạt được ghi nhận
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4 pt-4">
          {!activeAlerts || activeAlerts.length === 0 ? (
            <div className="py-6 text-center text-sm text-muted-foreground">
              Không có cảnh báo nào đang kích hoạt trên lô sản xuất này.
            </div>
          ) : (
            <div className="space-y-4">
              {activeAlerts.map((alert: LotAlertEvidenceDetail, index: number) => {
                const config = ALERT_TYPE_CONFIG_MAP[alert.alertType];
                const badgeLabel = config?.label || alert.alertType;
                const badgeClass = config?.badgeClass || 'bg-red-100 text-red-800 border-red-200';

                return (
                  <div
                    key={`${alert.alertType}-${index}`}
                    className="rounded-lg border border-red-100 bg-red-50/20 p-4 space-y-2 transition-all"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <Badge
                          variant="outline"
                          className={`text-xs font-semibold px-2.5 py-0.5 border ${badgeClass}`}
                        >
                          {badgeLabel}
                        </Badge>
                        {getSeverityBadge(alert.severity)}
                      </div>
                      <span className="text-xs text-muted-foreground flex items-center gap-1">
                        <Clock className="size-3.5" />
                        Kích hoạt lúc: {formatDateTime(alert.triggeredAt)}
                      </span>
                    </div>

                    <h4 className="font-semibold text-slate-900 text-sm">{alert.title}</h4>
                    <p className="text-sm text-slate-700 leading-relaxed">{alert.message}</p>

                    {/* Dữ liệu bằng chứng hiển thị trực quan dạng text dễ đọc */}
                    {alert.evidenceData && Object.keys(alert.evidenceData).length > 0 && (
                      <AlertEvidenceViewer evidenceData={alert.evidenceData} />
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Card 4: Dòng sự kiện chuỗi cung ứng (Timeline Events) */}
      <Card>
        <CardHeader className="pb-3 border-b bg-slate-50/50">
          <CardTitle className="flex items-center gap-2 text-base text-slate-800">
            <Clock className="size-5 text-emerald-600" />
            Dòng sự kiện chuỗi cung ứng
          </CardTitle>
          <CardDescription className="text-xs">
            Lịch sử các bước sản xuất, sơ chế, đóng gói, vận chuyển và kiểm nghiệm đã ghi nhận
          </CardDescription>
        </CardHeader>
        <CardContent className="pt-6">
          {!timelineEvents || timelineEvents.length === 0 ? (
            <div className="py-8 text-center text-sm text-muted-foreground">
              Chưa có sự kiện chuỗi cung ứng nào được ghi nhận cho lô sản xuất này.
            </div>
          ) : (
            <div className="relative pl-6 space-y-6 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200">
              {timelineEvents.map((evt: ReadonlyChainEventItem) => {
                const isWarningEvent = evt.hasAlert || evt.earlyHarvest;

                return (
                  <div key={evt.eventId} className="relative group">
                    {/* Dot icon */}
                    <div
                      className={`absolute -left-6 top-1 size-4 rounded-full border-2 bg-white transition-all ${
                        isWarningEvent
                          ? 'border-red-600 bg-red-100 ring-4 ring-red-100'
                          : 'border-emerald-600'
                      }`}
                    />

                    <div
                      className={`rounded-lg border p-4 shadow-2xs space-y-1.5 transition-all ${
                        isWarningEvent ? 'border-red-300 bg-red-50/30' : 'bg-white'
                      }`}
                    >
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-slate-900 text-sm">
                            {evt.eventTypeName || evt.eventType}
                          </span>
                          {evt.earlyHarvest && (
                            <Badge variant="destructive" className="text-xs font-medium">
                              <AlertTriangle className="size-3 mr-1" />
                              Thu hoạch sớm trước thời gian cách ly
                            </Badge>
                          )}
                          {!evt.earlyHarvest && evt.alertWarning && (
                            <Badge variant="destructive" className="text-xs font-medium">
                              <AlertTriangle className="size-3 mr-1" />
                              {evt.alertWarning}
                            </Badge>
                          )}
                        </div>
                        <span className="text-xs text-muted-foreground">
                          {evt.recordedAt ? formatDateTime(evt.recordedAt) : 'Chưa rõ thời gian'}
                        </span>
                      </div>

                      {evt.description && (
                        <p className="text-sm text-slate-700">{evt.description}</p>
                      )}

                      <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground pt-1 border-t border-slate-100 mt-2">
                        {evt.shipmentName && (
                          <span className="flex items-center gap-1">
                            <Package className="size-3 text-slate-400" />
                            Lô hàng: <strong className="text-slate-800">{evt.shipmentName}</strong>
                          </span>
                        )}
                        {evt.recordedByName && (
                          <span className="flex items-center gap-1">
                            <User className="size-3 text-slate-400" />
                            Người ghi nhận: <strong className="text-slate-700">{evt.recordedByName}</strong>
                          </span>
                        )}
                        {evt.location && (
                          <span className="flex items-center gap-1">
                            <MapPin className="size-3 text-slate-400" />
                            <strong className="text-slate-700">{formatLocationDisplay(evt.location)}</strong>
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
