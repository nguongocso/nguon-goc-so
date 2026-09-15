import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Activity,
  ArrowDown,
  ArrowUp,
  Building2,
  Download,
  Inbox,
  Minus,
  PhoneCall,
  ShieldAlert,
} from 'lucide-react';
import { toast } from 'sonner';

import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListCard } from '@/components/common/ListCard';
import { ListToolbar } from '@/components/common/ListToolbar';
import { DataTableShell } from '@/components/common/DataTableShell';
import { RefreshButton } from '@/components/common/RefreshButton';
import { SearchInput } from '@/components/common/SearchInput';
import { FilterSelect } from '@/components/common/FilterSelect';
import { StatusBadge } from '@/components/common/StatusBadge';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import { HelpButton } from '@/components/help/HelpButton';

import {
  exportOrganizationUsage,
  getOrganizationUsage,
  OrganizationUsageApiError,
} from '@/api/organizationUsageApi';
import { formatDateTime, getLocalDateString } from '@/utils/dateTime';
import {
  ORGANIZATION_USAGE_METRICS,
  type MetricComparison,
  type OrganizationUsageDashboard,
  type OrganizationUsageItem,
  type OrganizationUsageSortKey,
} from '@/types/organizationUsage';

type StatusFilter = 'ALL' | 'NEEDS_SUPPORT' | 'NO_DATA';

const STATUS_OPTIONS: Array<{ value: StatusFilter; label: string }> = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'NEEDS_SUPPORT', label: 'Cần liên hệ hỗ trợ' },
  { value: 'NO_DATA', label: 'Chưa có dữ liệu' },
];

function defaultRange(): { from: string; to: string } {
  const today = new Date();
  const from = new Date(today);
  from.setDate(from.getDate() - 29);
  return { from: getLocalDateString(from), to: getLocalDateString(today) };
}

/** Ngày `YYYY-MM-DD` sang `DD/MM/YYYY` để hiển thị. */
function toDisplayDate(iso: string): string {
  const [y, m, d] = iso.split('-');
  return y && m && d ? `${d}/${m}/${y}` : iso;
}

/**
 * Mức độ ưu tiên trạng thái hiển thị của một tổ chức:
 * 0 = Cần liên hệ hỗ trợ, 1 = Đang hoạt động, 2 = Chưa có dữ liệu.
 * Dùng làm thứ tự sắp xếp mặc định, đếm thẻ tổng hợp và lọc trạng thái.
 */
function getStatusRank(item: OrganizationUsageItem): number {
  if (item.needsSupport && item.lastActivityAt != null) return 0;
  if (item.hasData) return 1;
  return 2;
}

/**
 * Ô hiển thị một chỉ số trên cùng một hàng: giá trị kỳ hiện tại
 * kèm phần trăm thay đổi so với kỳ trước (mũi tên + ±%).
 * Quy ước hiển thị: previous = 0, current > 0 → "+100.0%" (tăng từ con số 0);
 * không tăng không giảm → "0.0%"; chưa có dữ liệu → "—".
 * Màu dùng token ngữ nghĩa (success/destructive) theo AI_DESIGN_SYSTEM.md,
 * luôn kèm ký hiệu thay vì chỉ tô màu.
 */
function MetricCell({ metric, hasData }: { metric: MetricComparison; hasData: boolean }) {
  // Phương án A: một cột dùng một kiểu căn (trái) để không bị ziczac dọc;
  // cả số liệu lẫn dấu "—" đều bám lề trái, "—" chỉ khác màu muted.
  if (!hasData) {
    return <div className="text-left text-muted-foreground">—</div>;
  }
  const changePercent =
    metric.changePercent ?? (metric.change > 0 ? 100 : metric.change < 0 ? -100 : 0);
  const positive = metric.change > 0;
  const negative = metric.change < 0;
  const percentText = `${metric.change > 0 ? '+' : ''}${changePercent.toFixed(1)}%`;
  return (
    <div className="flex items-baseline justify-start gap-1.5 text-left">
      <span className="font-semibold tabular-nums">
        {metric.current.toLocaleString('vi-VN')}
      </span>
      <span
        className={`inline-flex items-center gap-0.5 text-xs font-medium tabular-nums ${
          positive
            ? 'text-success'
            : negative
              ? 'text-destructive'
              : 'text-muted-foreground'
        }`}
        aria-label={`Thay đổi ${metric.change > 0 ? '+' : ''}${percentText} so với kỳ trước ${metric.previous}`}
      >
        {positive ? (
          <ArrowUp className="size-3" aria-hidden />
        ) : negative ? (
          <ArrowDown className="size-3" aria-hidden />
        ) : (
          <Minus className="size-3" aria-hidden />
        )}
        {percentText}
      </span>
    </div>
  );
}

function SortButton({
  label,
  sortKey,
  activeKey,
  direction,
  onSort,
}: {
  label: string;
  sortKey: OrganizationUsageSortKey;
  activeKey: OrganizationUsageSortKey;
  direction: 'asc' | 'desc';
  onSort: (key: OrganizationUsageSortKey) => void;
}) {
  const active = activeKey === sortKey;
  return (
    <button
      type="button"
      onClick={() => onSort(sortKey)}
      aria-sort={active ? (direction === 'asc' ? 'ascending' : 'descending') : 'none'}
      className="inline-flex items-center gap-1 font-medium hover:text-foreground"
    >
      {label}
      {active ? (
        direction === 'asc' ? (
          <ArrowUp className="size-3 text-primary" aria-hidden />
        ) : (
          <ArrowDown className="size-3 text-primary" aria-hidden />
        )
      ) : (
        <span className="size-3" aria-hidden />
      )}
    </button>
  );
}

/**
 * Nội dung trang Mức độ sử dụng nền tảng (NCL-07-CN-008):
 * thẻ tổng hợp và bảng dashboard mức độ sử dụng theo tổ chức,
 * kèm thanh bộ lọc đặt ngay phía trên bảng trong cùng một thẻ.
 * Đổi kỳ/từ khóa/trạng thái được áp dụng ngay, không cần nút xác nhận.
 * File page chỉ gọi component này, không chứa nghiệp vụ hiển thị.
 */
export default function OrganizationUsageContent() {
  const initial = useMemo(() => defaultRange(), []);
  const [fromDate, setFromDate] = useState<string>(initial.from);
  const [toDate, setToDate] = useState<string>(initial.to);

  const [searchKeyword, setSearchKeyword] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [sortKey, setSortKey] = useState<OrganizationUsageSortKey>('statusPriority');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');

  const [data, setData] = useState<OrganizationUsageDashboard | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [isForbidden, setIsForbidden] = useState<boolean>(false);

  const fetchData = useCallback(async (from: string, to: string) => {
    setIsLoading(true);
    setIsForbidden(false);
    try {
      const result = await getOrganizationUsage({
        startDate: from || undefined,
        endDate: to || undefined,
      });
      setData(result);
    } catch (err: unknown) {
      if (err instanceof OrganizationUsageApiError && err.status === 403) {
        setIsForbidden(true);
        setData(null);
      } else {
        toast.error(
          err instanceof Error ? err.message : 'Không thể tải dữ liệu mức độ sử dụng.'
        );
        setData(null);
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Đổi kỳ được áp dụng ngay: chỉ gọi API khi cả hai ô đều rỗng hoặc đủ ngày hợp lệ.
  useEffect(() => {
    const isComplete = (value: string) => value === '' || /^\d{4}-\d{2}-\d{2}$/.test(value);
    if (!isComplete(fromDate) || !isComplete(toDate)) return;
    if (fromDate && toDate && fromDate > toDate) {
      toast.error('Từ ngày phải trước hoặc bằng đến ngày.');
      return;
    }
    void fetchData(fromDate, toDate);
  }, [fromDate, toDate, fetchData]);

  const handleSort = (key: OrganizationUsageSortKey) => {
    if (key === sortKey) {
      setSortDir((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir(key === 'organizationName' || key === 'statusPriority' ? 'asc' : 'desc');
    }
  };

  const handleExport = async () => {
    setIsExporting(true);
    try {
      const { blob, fileName } = await exportOrganizationUsage({
        startDate: fromDate || undefined,
        endDate: toDate || undefined,
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName || 'Bao_cao_muc_do_su_dung.csv';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success('Xuất báo cáo thành công.');
    } catch (err: unknown) {
      if (err instanceof OrganizationUsageApiError && err.status === 403) {
        toast.error('Bạn không có quyền xuất báo cáo này.');
      } else {
        toast.error(err instanceof Error ? err.message : 'Không thể xuất báo cáo.');
      }
    } finally {
      setIsExporting(false);
    }
  };

  // Ba nhóm loại trừ nhau theo đúng trạng thái hiển thị, cộng lại bằng tổng số.
  const summary = useMemo(() => {
    const items = data?.items ?? [];
    const countByRank = (rank: number) =>
      items.filter((item) => getStatusRank(item) === rank).length;
    const total = items.length;
    const share = (count: number) => (total > 0 ? Math.round((count / total) * 100) : 0);
    const active = countByRank(1);
    const needsSupport = countByRank(0);
    const noData = countByRank(2);
    return {
      total,
      active,
      needsSupport,
      noData,
      activeShare: share(active),
      needsSupportShare: share(needsSupport),
      noDataShare: share(noData),
    };
  }, [data]);

  const periodLabel =
    fromDate && toDate
      ? `Kỳ ${toDisplayDate(fromDate)} → ${toDisplayDate(toDate)}`
      : 'Kỳ 30 ngày gần nhất';

  const displayedItems = useMemo(() => {
    let items: OrganizationUsageItem[] = data?.items ?? [];
    const kw = searchKeyword.trim().toLowerCase();
    if (kw) {
      items = items.filter((item) =>
        item.organizationName?.toLowerCase().includes(kw)
      );
    }
    if (statusFilter === 'NEEDS_SUPPORT') {
      items = items.filter((item) => getStatusRank(item) === 0);
    } else if (statusFilter === 'NO_DATA') {
      items = items.filter((item) => !item.hasData);
    }
    const dir = sortDir === 'asc' ? 1 : -1;
    const byName = (a: OrganizationUsageItem, b: OrganizationUsageItem) =>
      (a.organizationName ?? '').localeCompare(b.organizationName ?? '', 'vi');
    return [...items].sort((a, b) => {
      switch (sortKey) {
        case 'organizationName':
          return byName(a, b) * dir;
        case 'lastActivityAt':
          return (
            (a.lastActivityAt ?? '').localeCompare(b.lastActivityAt ?? '') * dir
          );
        case 'statusPriority':
          return (getStatusRank(a) - getStatusRank(b)) * dir || byName(a, b);
        default:
          return (a[sortKey].current - b[sortKey].current) * dir;
      }
    });
  }, [data, searchKeyword, statusFilter, sortKey, sortDir]);

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={Activity}
        title="Mức độ sử dụng nền tảng"
        description="Theo dõi mức độ sử dụng của từng tổ chức theo kỳ, so sánh với kỳ trước"
        actions={
          <>
            <HelpButton screenKey="report-organization-usage" />
            <Button
              variant="outline"
              onClick={() => void handleExport()}
              disabled={isExporting || isLoading || isForbidden}
            >
              <Download className="size-4" />
              {isExporting ? 'Đang xuất...' : 'Xuất báo cáo'}
            </Button>
          </>
        }
      />

      {isForbidden ? (
        <Alert variant="destructive">
          <ShieldAlert className="size-4" />
          <AlertTitle>Bạn không có quyền truy cập</AlertTitle>
          <AlertDescription>
            Mức độ sử dụng nền tảng chỉ dành cho Quản trị viên
            hệ thống (VT-01).
          </AlertDescription>
        </Alert>
      ) : (
        <>
          {/* Thẻ tổng hợp: dùng Card size="sm" và CardContent mặc định (chỉ padding ngang)
              để tránh cộng dồn padding dọc; bố cục hàng ngang icon + số liệu lấp đầy
              khoảng trắng, viền trái màu và thanh tỉ lệ giúp phân biệt nhanh trạng thái. */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Card size="sm" className="border-l-4 border-l-primary">
              <CardContent>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="truncate text-sm font-medium text-muted-foreground">
                      Tổng số tổ chức
                    </div>
                    <div className="mt-1 text-3xl font-bold tabular-nums">
                      {summary.total}
                    </div>
                    <div className="mt-1 truncate text-xs text-muted-foreground">
                      {periodLabel}
                    </div>
                  </div>
                  <div className="rounded-full bg-primary-light p-2.5 text-primary">
                    <Building2 className="size-5" aria-hidden />
                  </div>
                </div>
              </CardContent>
            </Card>
            <Card size="sm" className="border-l-4 border-l-success">
              <CardContent>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-sm font-medium text-muted-foreground">
                      Đang hoạt động
                    </div>
                    <div className="mt-1 flex items-baseline gap-2">
                      <span className="text-3xl font-bold tabular-nums text-success">
                        {summary.active}
                      </span>
                      <span className="text-xs font-medium text-muted-foreground tabular-nums">
                        {summary.activeShare}% tổng số
                      </span>
                    </div>
                    <div
                      className="mt-2 h-1.5 overflow-hidden rounded-full bg-muted"
                      role="presentation"
                      aria-hidden="true"
                    >
                      <div
                        className="h-full rounded-full bg-success"
                        style={{ width: `${summary.activeShare}%` }}
                      />
                    </div>
                  </div>
                  <div className="rounded-full bg-success-bg p-2.5 text-success">
                    <Activity className="size-5" aria-hidden />
                  </div>
                </div>
              </CardContent>
            </Card>
            <Card size="sm" className="border-l-4 border-l-warning">
              <CardContent>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-sm font-medium text-muted-foreground">
                      Cần liên hệ hỗ trợ
                    </div>
                    <div className="mt-1 flex items-baseline gap-2">
                      <span className="text-3xl font-bold tabular-nums text-warning">
                        {summary.needsSupport}
                      </span>
                      <span className="text-xs font-medium text-muted-foreground tabular-nums">
                        {summary.needsSupportShare}% tổng số
                      </span>
                    </div>
                    <div
                      className="mt-2 h-1.5 overflow-hidden rounded-full bg-muted"
                      role="presentation"
                      aria-hidden="true"
                    >
                      <div
                        className="h-full rounded-full bg-warning"
                        style={{ width: `${summary.needsSupportShare}%` }}
                      />
                    </div>
                  </div>
                  <div className="rounded-full bg-warning-bg p-2.5 text-warning">
                    <PhoneCall className="size-5" aria-hidden />
                  </div>
                </div>
              </CardContent>
            </Card>
            <Card size="sm" className="border-l-4 border-l-border">
              <CardContent>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-sm font-medium text-muted-foreground">
                      Chưa có dữ liệu
                    </div>
                    <div className="mt-1 flex items-baseline gap-2">
                      <span className="text-3xl font-bold tabular-nums text-muted-foreground">
                        {summary.noData}
                      </span>
                      <span className="text-xs font-medium text-muted-foreground tabular-nums">
                        {summary.noDataShare}% tổng số
                      </span>
                    </div>
                    <div
                      className="mt-2 h-1.5 overflow-hidden rounded-full bg-muted"
                      role="presentation"
                      aria-hidden="true"
                    >
                      <div
                        className="h-full rounded-full bg-muted-foreground"
                        style={{ width: `${summary.noDataShare}%` }}
                      />
                    </div>
                  </div>
                  <div className="rounded-full bg-muted p-2.5 text-muted-foreground">
                    <Inbox className="size-5" aria-hidden />
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          <ListCard>
            <ListToolbar
              left={
                <>
                  <div className="flex flex-col gap-1">
                    <Label htmlFor="usage-from-date">Từ ngày</Label>
                    <Input
                      id="usage-from-date"
                      type="date"
                      value={fromDate}
                      max={toDate || undefined}
                      onChange={(event) => setFromDate(event.target.value)}
                      className="w-44"
                    />
                  </div>
                  <div className="flex flex-col gap-1">
                    <Label htmlFor="usage-to-date">Đến ngày</Label>
                    <Input
                      id="usage-to-date"
                      type="date"
                      value={toDate}
                      min={fromDate || undefined}
                      onChange={(event) => setToDate(event.target.value)}
                      className="w-44"
                    />
                  </div>
                  <div className="flex flex-col gap-1 lg:w-64">
                    <Label htmlFor="usage-search-org">Tìm tổ chức</Label>
                    <SearchInput
                      id="usage-search-org"
                      value={searchKeyword}
                      onChange={(event) => setSearchKeyword(event.target.value)}
                      placeholder="Tên tổ chức..."
                    />
                  </div>
                  <div className="flex flex-col gap-1 lg:w-56">
                    <Label>Trạng thái</Label>
                    <FilterSelect
                      value={statusFilter}
                      onValueChange={(value) => setStatusFilter((value as StatusFilter) ?? 'ALL')}
                      options={STATUS_OPTIONS}
                      placeholder="Tất cả trạng thái"
                      ariaLabel="Lọc theo trạng thái"
                    />
                  </div>
                </>
              }
              right={
                <div className="flex shrink-0 flex-col gap-1">
                  {/* Nhãn ẩn giữ chỗ để nút Làm mới căn giữa theo hàng ô nhập liệu. */}
                  <span
                    aria-hidden="true"
                    className="invisible hidden text-sm leading-none font-medium select-none sm:block"
                  >
                    &nbsp;
                  </span>
                  <RefreshButton
                    onClick={() => void fetchData(fromDate, toDate)}
                    loading={isLoading}
                  />
                </div>
              }
            />

            <DataTableShell
              loading={isLoading}
              loadingMessage="Đang tải dữ liệu mức độ sử dụng..."
              empty={!isLoading && displayedItems.length === 0}
              emptyMessage={
                data && data.items.length === 0
                  ? 'Chưa có tổ chức nào trong hệ thống.'
                  : 'Không tìm thấy tổ chức phù hợp với bộ lọc.'
              }
              colSpan={9}
              header={
                <>
                  <TableHead>
                    <SortButton
                      label="Tổ chức"
                      sortKey="organizationName"
                      activeKey={sortKey}
                      direction={sortDir}
                      onSort={handleSort}
                    />
                  </TableHead>
                  {ORGANIZATION_USAGE_METRICS.map((metric) => (
                    <TableHead key={metric.key} className="text-left">
                      <span className="inline-flex justify-start">
                        <SortButton
                          label={metric.label}
                          sortKey={metric.key}
                          activeKey={sortKey}
                          direction={sortDir}
                          onSort={handleSort}
                        />
                      </span>
                    </TableHead>
                  ))}
                  <TableHead>
                    <SortButton
                      label="Hoạt động gần nhất"
                      sortKey="lastActivityAt"
                      activeKey={sortKey}
                      direction={sortDir}
                      onSort={handleSort}
                    />
                  </TableHead>
                  <TableHead>
                    <SortButton
                      label="Trạng thái"
                      sortKey="statusPriority"
                      activeKey={sortKey}
                      direction={sortDir}
                      onSort={handleSort}
                    />
                  </TableHead>
                </>
              }
              body={
                <>
                  {displayedItems.map((item) => (
                    <TableRow key={item.organizationId}>
                      <TableCell>
                        <div className="font-medium">{item.organizationName}</div>
                      </TableCell>
                      <TableCell className="text-left">
                        <MetricCell metric={item.productionLots} hasData={item.hasData} />
                      </TableCell>
                      <TableCell className="text-left">
                        <MetricCell metric={item.farmLogs} hasData={item.hasData} />
                      </TableCell>
                      <TableCell className="text-left">
                        <MetricCell metric={item.chainEvents} hasData={item.hasData} />
                      </TableCell>
                      <TableCell className="text-left">
                        <MetricCell metric={item.activatedLabels} hasData={item.hasData} />
                      </TableCell>
                      <TableCell className="text-left">
                        <MetricCell metric={item.publicLookups} hasData={item.hasData} />
                      </TableCell>
                      <TableCell className="text-left">
                        <MetricCell metric={item.activeUsers} hasData={item.hasData} />
                      </TableCell>
                      {/* Phương án A: ngày và dấu "—" cùng căn trái để thẳng cột. */}
                      <TableCell className="whitespace-nowrap text-left text-sm">
                        {item.lastActivityAt ? (
                          formatDateTime(item.lastActivityAt)
                        ) : (
                          <span className="text-muted-foreground">—</span>
                        )}
                      </TableCell>
                      <TableCell>
                        {item.needsSupport && item.lastActivityAt != null ? (
                          <StatusBadge
                            label="Cần liên hệ hỗ trợ"
                            tone="warning"
                            icon={PhoneCall}
                          />
                        ) : !item.hasData ? (
                          <StatusBadge label="Chưa có dữ liệu" tone="neutral" />
                        ) : (
                          <StatusBadge label="Đang hoạt động" tone="success" />
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </>
              }
            />
          </ListCard>
        </>
      )}
    </div>
  );
}
