import { useEffect, useMemo, useState } from 'react';
import { z } from 'zod';
import { getLocalDateString } from '@/utils/dateTime';
import {
  AlertCircle,
  Download,
  FileBarChart,
  LoaderCircle,
  MapPin,
  MapPinOff,
  SearchCheck,
  CalendarDays,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useExportIndustryReport } from '@/hooks/useExportIndustryReport';
import { useAuth } from '@/hooks/useAuth';
import { useAdministrativeUnits } from '@/hooks/useAdministrativeUnits';
import { getMyAreas } from '@/api/areaAssignmentApi';
import { AdministrativeUnitCascadeSelect } from '@/components/common/AdministrativeUnitCascadeSelect';
import { Badge } from '@/components/ui/badge';
import {
  NO_ASSIGNED_AREA_MESSAGE,
  NO_ASSIGNED_AREA_HINT,
} from '@/constants/reportMessages';
import type { AssignedArea } from '@/types/areaAssignment';
import { cn } from '@/lib/utils';

const filterSchema = z
  .object({
    unitIds: z.array(z.string()),
    fromDate: z.string().min(1, 'Vui lòng chọn ngày bắt đầu'),
    toDate: z.string().min(1, 'Vui lòng chọn ngày kết thúc'),
  })
  .refine((data) => data.fromDate <= data.toDate, {
    message: 'Ngày bắt đầu phải trước hoặc bằng ngày kết thúc',
    path: ['toDate'],
  });

type QuickRange = 'today' | 'week' | 'month' | 'year' | null;

function getQuickRangeDates(range: QuickRange): { from: string; to: string } {
  const now = new Date();
  const to = getLocalDateString(now);
  let from = to;

  if (range === 'today') {
    // giữ nguyên
  } else if (range === 'week') {
    const d = new Date(now);
    d.setDate(d.getDate() - 7);
    from = getLocalDateString(d);
  } else if (range === 'month') {
    const d = new Date(now);
    d.setDate(d.getDate() - 30);
    from = getLocalDateString(d);
  } else if (range === 'year') {
    const d = new Date(now);
    d.setFullYear(d.getFullYear() - 1);
    from = getLocalDateString(d);
  }

  return { from, to };
}

export function IndustryReportPanel() {
  const { user } = useAuth();
  const [unitIds, setUnitIds] = useState<string[]>([]);
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [format, setFormat] = useState<'PDF' | 'EXCEL'>('PDF');
  const [formError, setFormError] = useState<string | null>(null);

  // State cho quick range đang active
  const [activeQuickRange, setActiveQuickRange] = useState<QuickRange>(null);

  const { report, isLoading, isExporting, fetchReport, exportReport, reset } =
    useExportIndustryReport();

  const trimmedParams = useMemo(
    () => ({ unitIds, fromDate, toDate }),
    [unitIds, fromDate, toDate],
  );

  const validate = () => {
    const result = filterSchema.safeParse(trimmedParams);
    if (!result.success) {
      setFormError(result.error.issues[0]?.message ?? 'Dữ liệu lọc không hợp lệ');
      return false;
    }
    setFormError(null);
    return true;
  };

  // VT-05: hiển thị địa bàn đang phụ trách (chỉ đọc) — NCL-742 §6.
  const isRegulator = user?.roleCode === 'VT-05';
  const [myAreas, setMyAreas] = useState<AssignedArea[]>([]);
  const { units, loading: unitsLoading } = useAdministrativeUnits();
  useEffect(() => {
    if (!isRegulator) return;
    let cancelled = false;
    getMyAreas()
      .then((areas) => {
        if (!cancelled) setMyAreas(areas);
      })
      .catch(() => {
        // Không chặn việc xem báo cáo nếu không tải được địa bàn của tôi.
      });
    return () => {
      cancelled = true;
    };
  }, [isRegulator]);

  // Hàm áp dụng quick range
  const applyQuickRange = (range: QuickRange) => {
    if (!range) {
      setActiveQuickRange(null);
      return;
    }
    const { from, to } = getQuickRangeDates(range);
    setFromDate(from);
    setToDate(to);
    setActiveQuickRange(range);
    // Tự động reset lỗi và báo cáo cũ
    setFormError(null);
    reset();
  };

  // Khi người dùng thay đổi thủ công, bỏ active
  const handleFromDateChange = (value: string) => {
    setFromDate(value);
    setActiveQuickRange(null);
  };
  const handleToDateChange = (value: string) => {
    setToDate(value);
    setActiveQuickRange(null);
  };

  const handleView = () => {
    reset();
    if (!validate()) return;
    void fetchReport(trimmedParams);
  };

  const handleExport = () => {
    if (!validate()) return;
    void exportReport(trimmedParams, format);
  };

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileBarChart className="size-5 text-emerald-700" />
            Báo cáo tổng hợp theo địa bàn
          </CardTitle>
          <CardDescription>
            Tổng hợp sản lượng và lô hàng của nhiều tổ chức trong cùng địa bàn, theo khoảng thời gian.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {isRegulator && (
            <div className="rounded-lg border border-emerald-200 bg-emerald-50/60 p-3">
              <p className="text-sm text-emerald-900">
                Bạn đang phụ trách <span className="font-semibold">{myAreas.length}</span> địa bàn
              </p>
              {myAreas.length > 0 && (
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {myAreas.map((area) => (
                    <Badge key={area.assignmentId} variant="outline" className="gap-1 bg-white">
                      <MapPin className="h-3 w-3 text-emerald-500" />
                      {area.unitName}
                      <span className="font-normal text-muted-foreground">({area.provinceName})</span>
                    </Badge>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Quick range buttons */}
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-sm font-medium text-muted-foreground mr-1">Chọn nhanh:</span>
            {[
              { label: 'Hôm nay', value: 'today' as const },
              { label: '7 ngày qua', value: 'week' as const },
              { label: '30 ngày qua', value: 'month' as const },
              { label: '1 năm qua', value: 'year' as const },
            ].map(({ label, value }) => (
              <Button
                key={value}
                variant="outline"
                size="sm"
                className={cn(
                  'h-8 px-3 text-xs font-normal',
                  activeQuickRange === value &&
                    'border-emerald-600 bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                )}
                onClick={() => applyQuickRange(value)}
              >
                {label}
              </Button>
            ))}
            {activeQuickRange && (
              <Button
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs text-muted-foreground"
                onClick={() => {
                  setActiveQuickRange(null);
                  setFromDate('');
                  setToDate('');
                  reset();
                }}
              >
                <CalendarDays className="mr-1 size-3" />
                Bỏ chọn
              </Button>
            )}
          </div>

          <div className="space-y-2">
            <Label>Địa bàn</Label>
            <div className="rounded-lg border p-3">
              <AdministrativeUnitCascadeSelect
                units={units}
                value={unitIds}
                onChange={setUnitIds}
                disabled={unitsLoading}
                loading={unitsLoading}
              />
            </div>
          </div>

          <div className="grid gap-4 md:grid-cols-3">
            <div className="space-y-2">
              <Label htmlFor="fromDate">Từ ngày *</Label>
              <Input
                id="fromDate"
                type="date"
                value={fromDate}
                onChange={(e) => handleFromDateChange(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="toDate">Đến ngày *</Label>
              <Input
                id="toDate"
                type="date"
                value={toDate}
                onChange={(e) => handleToDateChange(e.target.value)}
              />
            </div>
            <div className="flex items-end">
              <Button onClick={handleView} disabled={isLoading} className="w-full">
                {isLoading ? (
                  <LoaderCircle className="size-4 animate-spin" />
                ) : (
                  <SearchCheck className="size-4" />
                )}
                {isLoading ? 'Đang xem...' : 'Xem báo cáo'}
              </Button>
            </div>
          </div>

          {formError && (
            <Alert variant="destructive">
              <AlertCircle className="size-4" />
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}
        </CardContent>
      </Card>

      {isLoading && (
        <Card>
          <CardContent className="grid place-items-center py-12">
            <LoaderCircle className="size-8 animate-spin text-emerald-700" />
          </CardContent>
        </Card>
      )}

      {!isLoading && report && !report.hasData && (
        report.message === NO_ASSIGNED_AREA_MESSAGE ? (
          <Card>
            <CardContent className="flex flex-col items-center py-10 text-center">
              <MapPinOff className="mb-3 size-10 text-muted-foreground" />
              <p className="font-medium">{NO_ASSIGNED_AREA_MESSAGE}</p>
              <p className="mt-1 text-sm text-muted-foreground">{NO_ASSIGNED_AREA_HINT}</p>
            </CardContent>
          </Card>
        ) : (
          <Alert>
            <AlertCircle className="size-4" />
            <AlertDescription>
              {report.message || 'Chưa có dữ liệu cho địa bàn và khoảng thời gian đã chọn.'}
            </AlertDescription>
          </Alert>
        )
      )}

      {!isLoading && report?.hasData && (
        <Card>
          <CardHeader>
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <CardTitle>
                  Kết quả{report.region ? ` — ${report.region}` : ''}
                </CardTitle>
                <CardDescription>
                  {report.fromDate} → {report.toDate}
                </CardDescription>
              </div>
              <div className="flex items-center gap-2">
                <Select value={format} onValueChange={(v) => v && setFormat(v as 'PDF' | 'EXCEL')}
                  items={[
                    { value: 'PDF', label: 'PDF' },
                    { value: 'EXCEL', label: 'EXCEL' },
                  ]}
                >
                  <SelectTrigger className="w-28">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="PDF">PDF</SelectItem>
                    <SelectItem value="EXCEL">EXCEL</SelectItem>
                  </SelectContent>
                </Select>
                <Button onClick={handleExport} disabled={isExporting} variant="outline">
                  {isExporting ? (
                    <LoaderCircle className="size-4 animate-spin" />
                  ) : (
                    <Download className="size-4" />
                  )}
                  {isExporting ? 'Đang xuất...' : 'Xuất báo cáo'}
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="rounded-lg border bg-slate-50 p-4">
                <p className="text-sm text-muted-foreground">Số tổ chức</p>
                <p className="mt-1 text-2xl font-bold">{report.totalOrganizations}</p>
              </div>
              <div className="rounded-lg border bg-slate-50 p-4">
                <p className="text-sm text-muted-foreground">Số lô hàng</p>
                <p className="mt-1 text-2xl font-bold">{report.totalShipments}</p>
              </div>
              <div className="rounded-lg border bg-slate-50 p-4">
                <p className="text-sm text-muted-foreground">Tổng sản lượng</p>
                <p className="mt-1 text-2xl font-bold">
                  {report.totalQuantity.toLocaleString('vi-VN')}
                </p>
              </div>
            </div>

            {report.productBreakdown.length > 0 && (
              <div>
                <p className="mb-2 text-sm font-semibold">Chi tiết theo nhóm nông sản</p>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Loại nông sản</TableHead>
                      <TableHead className="text-right">Số lô hàng</TableHead>
                      <TableHead className="text-right">Sản lượng</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {report.productBreakdown.map((item) => (
                      <TableRow key={item.productCategoryName}>
                        <TableCell className="font-medium">{item.productCategoryName}</TableCell>
                        <TableCell className="text-right">{item.shipmentCount}</TableCell>
                        <TableCell className="text-right">
                          {item.totalQuantity.toLocaleString('vi-VN')}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}