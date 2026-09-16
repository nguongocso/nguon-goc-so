import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  Calendar,
  Download,
  LoaderCircle,
  MapPinOff,
  RefreshCw,
  Search,
  ShieldAlert,
  X,
} from 'lucide-react';
import { toast } from 'sonner';

import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListCard } from '@/components/common/ListCard';
import { DataTableShell } from '@/components/common/DataTableShell';
import { Pagination } from '@/components/common/Pagination';
import { FilterSelect } from '@/components/common/FilterSelect';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';

import {
  getAlertLots,
  exportAlertLots,
} from '@/api/territoryAlertLotApi';
import { NO_ASSIGNED_AREA_MESSAGE } from '@/constants/reportMessages';
import { formatDateTime } from '@/utils/dateTime';
import type { PageResponse } from '@/types/common';
import {
  ALERT_TYPE_CONFIG_MAP,
  type AlertBadgeSummary,
  type AlertLotQueryParams,
  type AlertLotSummaryResponse,
  type LotAlertType,
} from '@/types/territoryAlertLot';

// Danh sách các tùy chọn loại cảnh báo chính thức cho dropdown
const ALERT_TYPE_OPTIONS: Array<{ value: string; label: string }> = [
  { value: 'ALL', label: 'Tất cả loại cảnh báo' },
  { value: 'RECALLING', label: 'Đang thu hồi' },
  { value: 'LOCKED_LABEL', label: 'Tem bị khóa' },
  { value: 'INSPECTION_FAILED', label: 'Kết quả kiểm nghiệm không đạt' },
  { value: 'QUARANTINE_OVERWRITTEN', label: 'Ghi đè thời gian cách ly' },
  { value: 'SERIOUS_FEEDBACK_OPEN', label: 'Phản ánh mức độ nghiêm trọng' },
  { value: 'INSPECTION_EXPIRED', label: 'Kiểm nghiệm hết hiệu lực' },
];

export default function TerritoryAlertLotListPage() {
  const navigate = useNavigate();

  // === State filters ===
  const [alertType, setAlertType] = useState<LotAlertType | 'ALL'>('ALL');
  const [searchKeyword, setSearchKeyword] = useState<string>('');
  const [fromDate, setFromDate] = useState<string>('');
  const [toDate, setToDate] = useState<string>('');

  // === State pagination & data ===
  const [page, setPage] = useState<number>(0);
  const [pageSize] = useState<number>(10);
  const [data, setData] = useState<PageResponse<AlertLotSummaryResponse> | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isUnassignedArea, setIsUnassignedArea] = useState<boolean>(false);

  // Load danh sách lô có cảnh báo
  const fetchAlertLots = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);

    const queryParams: AlertLotQueryParams = {
      page,
      size: pageSize,
      sortBy: 'latestAlertTriggeredAt',
      sortDir: 'desc',
    };

    if (alertType !== 'ALL') {
      queryParams.alertType = alertType;
    }
    if (fromDate) {
      queryParams.fromDate = fromDate;
    }
    if (toDate) {
      queryParams.toDate = toDate;
    }

    try {
      const response = await getAlertLots(queryParams);

      // Nhận diện trạng thái chưa gán địa bàn:
      // Chỉ khi người dùng không chọn bộ lọc nào và Backend trả message: "Bạn chưa được phân công địa bàn quản lý nào."
      const hasFilter = alertType !== 'ALL' || Boolean(searchKeyword.trim()) || Boolean(fromDate) || Boolean(toDate);
      if (!hasFilter && response.message === NO_ASSIGNED_AREA_MESSAGE) {
        setIsUnassignedArea(true);
        setData({
          items: [],
          page: 0,
          size: pageSize,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true,
        });
      } else {
        setIsUnassignedArea(false);
        setData(response.data);
      }
    } catch (err: unknown) {
      const errObj = err as { status?: number; message?: string };
      if (errObj.status === 403) {
        setErrorMessage('Bạn không có quyền truy cập báo cáo lô cảnh báo theo địa bàn.');
      } else {
        setErrorMessage(errObj.message || 'Không thể tải danh sách lô có cảnh báo. Vui lòng thử lại.');
      }
      setData(null);
    } finally {
      setIsLoading(false);
    }
  }, [alertType, fromDate, toDate, page, pageSize]);

  // Fetch dữ liệu mỗi khi tham số filter hoặc page thay đổi
  useEffect(() => {
    void fetchAlertLots();
  }, [fetchAlertLots]);

  // Khi thay đổi bất kỳ filter nào -> reset page về 0
  const handleAlertTypeChange = (value: LotAlertType | 'ALL' | null) => {
    if (value) {
      setAlertType(value);
      setPage(0);
    }
  };

  const handleFromDateChange = (value: string) => {
    setFromDate(value);
    setPage(0);
  };

  const handleToDateChange = (value: string) => {
    setToDate(value);
    setPage(0);
  };

  const handleResetFilter = () => {
    setAlertType('ALL');
    setSearchKeyword('');
    setFromDate('');
    setToDate('');
    setPage(0);
  };

  // Lọc client-side cho trường tìm kiếm: Lô sản xuất / Loại nông sản / Vùng trồng (và mã lô)
  const displayedItems = useMemo(() => {
    if (!data?.items) return [];
    if (!searchKeyword.trim()) return data.items;
    const kw = searchKeyword.trim().toLowerCase();
    return data.items.filter((item) =>
      item.lotName?.toLowerCase().includes(kw) ||
      item.productCategoryName?.toLowerCase().includes(kw) ||
      item.farmAreaName?.toLowerCase().includes(kw) ||
      item.lotCode?.toLowerCase().includes(kw) ||
      item.organizationName?.toLowerCase().includes(kw)
    );
  }, [data?.items, searchKeyword]);

  // Thao tác xuất báo cáo PDF
  const handleExport = async () => {
    if (isUnassignedArea) {
      toast.warning('Bạn chưa được phân công địa bàn nên không thể xuất báo cáo.');
      return;
    }

    setIsExporting(true);
    const queryParams: AlertLotQueryParams = {};
    if (alertType !== 'ALL') {
      queryParams.alertType = alertType;
    }
    if (fromDate) {
      queryParams.fromDate = fromDate;
    }
    if (toDate) {
      queryParams.toDate = toDate;
    }

    try {
      const { blob, fileName } = await exportAlertLots(queryParams);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName || 'Danh_sach_lo_canh_bao.pdf';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success('Xuất báo cáo PDF thành công.');
    } catch (err: unknown) {
      const errObj = err as { message?: string };
      toast.error(errObj.message || 'Có lỗi xảy ra khi xuất báo cáo.');
    } finally {
      setIsExporting(false);
    }
  };

  // Render các badge cảnh báo cho 1 dòng
  const renderAlertBadges = (item: AlertLotSummaryResponse) => {
    const summaries: AlertBadgeSummary[] = item.alertSummaries && item.alertSummaries.length > 0
      ? item.alertSummaries
      : item.alertTypes?.map((t) => ({
          alertType: t,
          alertName: ALERT_TYPE_CONFIG_MAP[t]?.label || t,
          severity: 'HIGH',
          triggeredAt: item.latestAlertTriggeredAt,
        })) || [];

    if (summaries.length === 0) {
      return <span className="text-xs text-muted-foreground">Không có</span>;
    }

    return (
      <div className="flex flex-wrap gap-1.5">
        {summaries.map((summary, idx) => {
          const config = ALERT_TYPE_CONFIG_MAP[summary.alertType];
          const label = config?.label || summary.alertName || summary.alertType;
          const badgeClass = config?.badgeClass || 'bg-slate-100 text-slate-800 border-slate-200';

          return (
            <Badge
              key={`${summary.alertType}-${idx}`}
              variant="outline"
              className={`text-xs font-medium border px-2 py-0.5 ${badgeClass}`}
              title={config?.description || label}
            >
              {label}
            </Badge>
          );
        })}
      </div>
    );
  };

  return (
    <div className="space-y-6">
      {/* Header trang danh sách */}
      <ListPageHeader
        icon={ShieldAlert}
        title="Theo dõi lô có cảnh báo"
        description="Theo dõi các lô sản xuất phát sinh cảnh báo vi phạm, tem nghi vấn hoặc kiểm nghiệm không đạt trong địa bàn phụ trách."
        actions={
          <Button
            onClick={handleExport}
            disabled={isExporting || isLoading || isUnassignedArea}
            variant="outline"
            className="flex items-center gap-2 border-emerald-600 text-emerald-700 hover:bg-emerald-50"
          >
            {isExporting ? (
              <LoaderCircle className="size-4 animate-spin text-emerald-700" />
            ) : (
              <Download className="size-4 text-emerald-700" />
            )}
            <span>{isExporting ? 'Đang xuất...' : 'Xuất báo cáo'}</span>
          </Button>
        }
      />

      {/* Thông báo lỗi khi tải API thất bại */}
      {errorMessage && (
        <Alert variant="destructive">
          <AlertCircle className="size-4" />
          <AlertTitle>Lỗi hệ thống</AlertTitle>
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
      )}

      {/* Case A: Không được gán địa bàn */}
      {!isLoading && isUnassignedArea ? (
        <Card className="border-amber-200 bg-amber-50/40">
          <CardContent className="flex flex-col items-center py-14 text-center">
            <div className="rounded-full bg-amber-100 p-4 text-amber-700 mb-4 shadow-sm ring-4 ring-amber-50">
              <MapPinOff className="size-8" />
            </div>
            <h3 className="text-lg font-bold text-amber-900">
              Bạn chưa được phân công địa bàn nên chưa có dữ liệu lô cảnh báo để xem.
            </h3>
            <p className="mt-2 text-sm text-amber-700 max-w-lg">
              Tài khoản cán bộ quản lý ngành của bạn hiện tại chưa được liên kết với địa bàn hành chính nào. Vui lòng liên hệ Quản trị viên hệ thống để được gán địa bàn quản lý.
            </p>
          </CardContent>
        </Card>
      ) : (
        /* Toàn bộ Bộ lọc và Bảng danh sách nằm gọn trong khung trắng ListCard chuẩn của hệ thống */
        <ListCard>
          {/* Thanh công cụ / Bộ lọc tìm kiếm: Giãn đều vừa trọn vẹn khung trắng */}
          <div className="flex flex-col xl:flex-row items-stretch xl:items-center justify-between gap-3 pb-3 border-b border-slate-100 w-full">
            {/* Cụm 4 bộ lọc: Bộ lọc Loại cảnh báo có kích thước cố định vừa vặn (~260px), 3 bộ lọc (tìm kiếm, từ ngày, đến ngày) được chia đều BẰNG NHAU */}
            <div className="flex flex-col sm:flex-row flex-wrap xl:flex-nowrap items-stretch sm:items-center gap-3 flex-1 min-w-0">
              {/* 1. Lọc theo Loại cảnh báo: Cố định 260px vừa khít nhãn 'Kết quả kiểm nghiệm không đạt', không bị dư thừa và không bị ... */}
              <div className="w-full sm:w-[260px] shrink-0">
                <FilterSelect
                  value={alertType}
                  onValueChange={(val) => handleAlertTypeChange((val as LotAlertType | 'ALL') || 'ALL')}
                  options={ALERT_TYPE_OPTIONS}
                  ariaLabel="Lọc theo loại cảnh báo"
                  placeholder="Chọn loại cảnh báo"
                  className="w-full h-10 min-w-0"
                />
              </div>

              {/* Nhóm 3 bộ lọc: Tìm kiếm, Từ ngày, Đến ngày - Chia đều BẰNG NHAU theo grid 3 cột */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 flex-1 min-w-0">
                {/* 2. Lọc theo Lô sản xuất / Loại nông sản / Vùng trồng */}
                <div className="min-w-0 relative">
                  <Search className="absolute left-2.5 top-3 size-4 text-muted-foreground pointer-events-none" />
                  <Input
                    id="searchFilter"
                    type="text"
                    placeholder="Nhập tên lô, loại nông sản hoặc vùng trồng..."
                    value={searchKeyword}
                    onChange={(e) => setSearchKeyword(e.target.value)}
                    className="w-full h-10 pl-9 pr-2.5 text-sm"
                    title="Nhập tên lô, loại nông sản hoặc vùng trồng"
                  />
                </div>

                {/* 3. Lọc theo Khoảng thời gian: Từ ngày */}
                <div className="min-w-0 relative">
                  <Calendar className="absolute left-2.5 top-3 size-4 text-muted-foreground pointer-events-none" />
                  <Input
                    id="fromDate"
                    type="date"
                    value={fromDate}
                    onChange={(e) => handleFromDateChange(e.target.value)}
                    className="w-full h-10 pl-9 pr-1 text-sm"
                    title="Từ ngày"
                  />
                </div>

                {/* 4. Lọc theo Khoảng thời gian: Đến ngày */}
                <div className="min-w-0 relative">
                  <Calendar className="absolute left-2.5 top-3 size-4 text-muted-foreground pointer-events-none" />
                  <Input
                    id="toDate"
                    type="date"
                    value={toDate}
                    onChange={(e) => handleToDateChange(e.target.value)}
                    className="w-full h-10 pl-9 pr-1 text-sm"
                    title="Đến ngày"
                  />
                </div>
              </div>
            </div>

            {/* Cụm nút hành động: Xóa bộ lọc (màu đỏ) & Làm mới (chuẩn hệ thống) */}
            <div className="flex items-center gap-2 shrink-0">
              <Button
                type="button"
                variant="delete"
                size="sm"
                onClick={handleResetFilter}
                className="h-10 px-3 flex items-center gap-1.5"
              >
                <X className="size-4" />
                <span>Xóa bộ lọc</span>
              </Button>

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => void fetchAlertLots()}
                disabled={isLoading}
                className="h-10 px-3 flex items-center gap-1.5 border-emerald-600 text-emerald-700 hover:bg-emerald-50"
                title="Làm mới dữ liệu"
              >
                <RefreshCw className={`size-4 ${isLoading ? 'animate-spin text-emerald-600' : ''}`} />
                <span>Làm mới</span>
              </Button>
            </div>
          </div>

          {/* Bảng dữ liệu chuẩn hệ thống qua DataTableShell */}
          <DataTableShell
            colSpan={8}
            loading={isLoading}
            empty={!isLoading && displayedItems.length === 0}
            loadingMessage="Đang tải danh sách lô có cảnh báo..."
            emptyMessage="Không tìm thấy lô sản xuất phù hợp với bộ lọc."
            header={
              <>
                <TableHead className="w-12 text-center font-semibold">STT</TableHead>
                <TableHead className="font-semibold min-w-[180px]">Lô sản xuất</TableHead>
                <TableHead className="font-semibold min-w-[160px]">Vùng trồng</TableHead>
                <TableHead className="font-semibold min-w-[140px]">Loại nông sản</TableHead>
                <TableHead className="font-semibold min-w-[180px]">Tổ chức sở hữu</TableHead>
                <TableHead className="font-semibold min-w-[200px]">Loại cảnh báo</TableHead>
                <TableHead className="font-semibold min-w-[150px]">Thời gian cảnh báo</TableHead>
                <TableHead className="w-24 text-center font-semibold">Thao tác</TableHead>
              </>
            }
            body={displayedItems.map((lot, idx) => {
              const stt = page * pageSize + idx + 1;
              return (
                <TableRow key={lot.lotId} className="hover:bg-slate-50/70 transition-colors">
                  <TableCell className="text-center font-medium text-muted-foreground">
                    {stt}
                  </TableCell>
                  <TableCell>
                    <p className="font-medium text-slate-900 line-clamp-1">{lot.lotName}</p>
                  </TableCell>
                  <TableCell className="text-sm text-slate-700">
                    {lot.farmAreaName || '—'}
                  </TableCell>
                  <TableCell className="text-sm text-slate-700">
                    {lot.productCategoryName || '—'}
                  </TableCell>
                  <TableCell>
                    <p className="font-medium text-slate-800 line-clamp-1">{lot.organizationName}</p>
                    {(lot.communeName || lot.provinceName) && (
                      <p className="text-xs text-muted-foreground line-clamp-1 mt-0.5">
                        {[lot.communeName, lot.provinceName].filter(Boolean).join(', ')}
                      </p>
                    )}
                  </TableCell>
                  <TableCell>
                    {renderAlertBadges(lot)}
                  </TableCell>
                  <TableCell className="text-sm text-slate-600">
                    {lot.latestAlertTriggeredAt ? (
                      formatDateTime(lot.latestAlertTriggeredAt)
                    ) : (
                      <span className="text-muted-foreground">Chưa có</span>
                    )}
                  </TableCell>
                  <TableCell className="text-center">
                    <Button
                      size="sm"
                      variant="outline"
                      className="h-8 text-xs"
                      onClick={() => navigate(`/reports/alert-lots/${lot.lotId}`)}
                    >
                      Chi tiết
                    </Button>
                  </TableCell>
                </TableRow>
              );
            })}
          />

          {/* Phân trang chuẩn hệ thống: Tự động ẩn khi totalPages <= 1 */}
          {data && data.totalElements > 0 && (
            <Pagination
              currentPage={page}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              pageSize={pageSize}
              loading={isLoading}
              itemLabel="lô cảnh báo"
              onPageChange={setPage}
            />
          )}
        </ListCard>
      )}
    </div>
  );
}
