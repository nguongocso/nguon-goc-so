import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  BarChart3,
  CheckCircle2,
  Lock,
  RefreshCw,
  ShieldAlert,
  Unlock,
} from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { toast } from 'sonner';

import {
  getLoginAnomalies,
  getStatusLabel,
  getSuspiciousCases,
  lockAccount,
  resolveUserAnomalies,
  unlockAccount,
} from '@/api/loginAnomalyApi';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import type { PageResponse } from '@/types/common';
import type { LoginAnomalyItem, LoginAnomalyStatus, SuspiciousCaseItem } from '@/types/loginAnomaly';

const PAGE_SIZE = 10;
const STATUS_OPTIONS: Array<{ value: 'ALL' | 'OPEN' | 'DISMISSED'; label: string }> = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'OPEN', label: 'Chưa giải quyết' },
  { value: 'DISMISSED', label: 'Đã giải quyết' },
];

const STATUS_COLORS: Record<LoginAnomalyStatus, string> = {
  OPEN: '#f59e0b',
  DISMISSED: '#22c55e',
};

const REASON_LABEL: Record<string, string> = {
  REPEATED_FAILED_LOGIN: 'Sai mật khẩu liên tiếp',
  UNUSUAL_COUNTRY: 'Đăng nhập từ quốc gia bất thường',
};

const EMPTY_PAGE: PageResponse<LoginAnomalyItem> = {
  items: [],
  page: 0,
  size: PAGE_SIZE,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

const EMPTY_SUSPICIOUS_PAGE: PageResponse<SuspiciousCaseItem> = {
  items: [],
  page: 0,
  size: PAGE_SIZE,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

const formatDateTime = (value: string) =>
  new Date(value).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

const formatReason = (reason: string) => REASON_LABEL[reason] ?? reason;

const formatLockDuration = (durationMs: number) => {
  const totalMinutes = Math.max(0, Math.ceil(durationMs / (60 * 1000)));
  const days = Math.floor(totalMinutes / (60 * 24));
  const hours = Math.floor((totalMinutes % (60 * 24)) / 60);
  const minutes = totalMinutes % 60;

  const parts: string[] = [];
  if (days > 0) parts.push(`${days} ngày`);
  if (hours > 0) parts.push(`${hours} giờ`);
  if (minutes > 0 || parts.length === 0) parts.push(`${minutes} phút`);

  return parts.join(' ');
};

const getStatusBadgeClass = (status: LoginAnomalyStatus) => {
  if (status === 'OPEN') return 'bg-amber-100 text-amber-800';
  return 'bg-emerald-100 text-emerald-800';
};

const getTimelineData = (items: LoginAnomalyItem[]) => {
  const map = new Map<string, number>();

  items.forEach((item) => {
    const dateKey = new Date(item.detectedAt).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
    });
    map.set(dateKey, (map.get(dateKey) ?? 0) + 1);
  });

  return Array.from(map.entries()).map(([label, value]) => ({ label, value }));
};

const getReasonBreakdown = (items: LoginAnomalyItem[]) => {
  const map = new Map<string, number>();

  items.forEach((item) => {
    const label = formatReason(item.reasonCode);
    map.set(label, (map.get(label) ?? 0) + 1);
  });

  return Array.from(map.entries()).map(([name, value]) => ({ name, value }));
};

export default function LoginAnomalyTrackingPage() {
  const [page, setPage] = useState(0);
  const [suspiciousPage, setSuspiciousPage] = useState(0);
  const [items, setItems] = useState<PageResponse<LoginAnomalyItem>>(EMPTY_PAGE);
  const [suspiciousCases, setSuspiciousCases] = useState<PageResponse<SuspiciousCaseItem>>(EMPTY_SUSPICIOUS_PAGE);
  const [allItems, setAllItems] = useState<LoginAnomalyItem[]>([]);
  const [allSuspiciousCases, setAllSuspiciousCases] = useState<SuspiciousCaseItem[]>([]);
  const [eventsLoading, setEventsLoading] = useState(true);
  const [suspiciousLoading, setSuspiciousLoading] = useState(true);
  const [statsLoading, setStatsLoading] = useState(true);
  const [viewMode, setViewMode] = useState<'events' | 'suspiciousAccounts'>('events');
  const [eventStatusFilter, setEventStatusFilter] = useState<LoginAnomalyStatus | 'ALL'>('ALL');
  const [eventAccountFilter, setEventAccountFilter] = useState('');
  const [suspiciousAccountFilter, setSuspiciousAccountFilter] = useState('');
  const [suspiciousStatusFilter, setSuspiciousStatusFilter] = useState<LoginAnomalyStatus | 'ALL'>('ALL');
  const [lockDialogOpen, setLockDialogOpen] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState<{
    id?: string;
    userId: string;
    username: string;
    fullName?: string;
    reasonCode?: string;
  } | null>(null);
  const [lockMode, setLockMode] = useState<'temporary' | 'permanent'>('temporary');
  const [lockDays, setLockDays] = useState('0');
  const [lockHours, setLockHours] = useState('1');
  const [lockMinutes, setLockMinutes] = useState('0');
  const [lockSubmitting, setLockSubmitting] = useState(false);

  const normalizeDurationInput = (value: string) => {
    const digitsOnly = value.replace(/\D/g, '');
    if (digitsOnly === '') return '';
    return digitsOnly.replace(/^0+(?=\d)/, '');
  };

  const refreshAllSuspiciousCases = useCallback(async (options: { silent?: boolean } = {}) => {
    const { silent = false } = options;

    try {
      const data = await getSuspiciousCases({
        page: 0,
        size: 500,
      });
      setAllSuspiciousCases(data.items);
    } catch (error: any) {
      if (!silent) {
        toast.error(error.response?.data?.message || 'Không thể tải thống kê tài khoản nghi vấn');
      }
      setAllSuspiciousCases([]);
    }
  }, []);

  const refreshStats = useCallback(async (options: { silent?: boolean } = {}) => {
    const { silent = false } = options;

    try {
      if (!silent) {
        setStatsLoading(true);
      }

      const responses = await Promise.all([
        getLoginAnomalies({ status: 'OPEN', size: 100 }),
        getLoginAnomalies({ status: 'DISMISSED', size: 100 }),
        getLoginAnomalies({ size: 200 }),
      ]);

      setAllItems(responses[2].items);
      await refreshAllSuspiciousCases({ silent });
    } catch (error: any) {
      if (!silent) {
        toast.error(error.response?.data?.message || 'Không thể tải thống kê đăng nhập bất thường');
      }
    } finally {
      if (!silent) {
        setStatsLoading(false);
      }
    }
  }, [refreshAllSuspiciousCases]);

  const refreshItems = useCallback(async (overrides?: {
    page?: number;
    status?: LoginAnomalyStatus | 'ALL';
    username?: string;
    silent?: boolean;
  }) => {
    const nextPage = overrides?.page ?? page;
    const nextStatus = overrides?.status ?? eventStatusFilter;
    const nextUsername = overrides?.username ?? eventAccountFilter;
    const silent = overrides?.silent ?? false;

    try {
      if (!silent) {
        setEventsLoading(true);
      }

      const data = await getLoginAnomalies({
        page: nextPage,
        size: PAGE_SIZE,
        status: nextStatus === 'ALL' ? undefined : nextStatus,
        username: nextUsername,
      });
      setItems(data);
    } catch (error: any) {
      if (!silent) {
        toast.error(error.response?.data?.message || 'Không thể tải danh sách đăng nhập bất thường');
      }
      setItems(EMPTY_PAGE);
    } finally {
      if (!silent) {
        setEventsLoading(false);
      }
    }
  }, [page, eventStatusFilter, eventAccountFilter]);

  const refreshSuspiciousCases = useCallback(async (overrides?: {
    page?: number;
    status?: LoginAnomalyStatus | 'ALL';
    username?: string;
    silent?: boolean;
  }) => {
    const nextPage = overrides?.page ?? suspiciousPage;
    const nextStatus = overrides?.status ?? suspiciousStatusFilter;
    const nextUsername = overrides?.username ?? suspiciousAccountFilter;
    const silent = overrides?.silent ?? false;

    try {
      if (!silent) {
        setSuspiciousLoading(true);
      }

      const data = await getSuspiciousCases({
        page: nextPage,
        size: PAGE_SIZE,
        status: nextStatus === 'ALL' ? undefined : nextStatus,
        username: nextUsername,
      });
      setSuspiciousCases(data);
    } catch (error: any) {
      if (!silent) {
        toast.error(error.response?.data?.message || 'Không thể tải danh sách tài khoản nghi vấn');
      }
      setSuspiciousCases(EMPTY_SUSPICIOUS_PAGE);
    } finally {
      if (!silent) {
        setSuspiciousLoading(false);
      }
    }
  }, [suspiciousPage, suspiciousStatusFilter, suspiciousAccountFilter]);

  useEffect(() => {
    void refreshStats();
  }, [refreshStats]);

  useEffect(() => {
    void refreshAllSuspiciousCases();
  }, [refreshAllSuspiciousCases]);

  useEffect(() => {
    setPage(0);
  }, [eventStatusFilter, eventAccountFilter]);

  useEffect(() => {
    setSuspiciousPage(0);
  }, [suspiciousStatusFilter, suspiciousAccountFilter]);

  useEffect(() => {
    void refreshItems();
  }, [refreshItems]);

  useEffect(() => {
    void refreshSuspiciousCases();
  }, [refreshSuspiciousCases]);

  const handleRefresh = useCallback(async (options: { silent?: boolean } = {}) => {
    const { silent = false } = options;

    await Promise.all([
      refreshStats({ silent }),
      refreshItems({ silent }),
      refreshSuspiciousCases({ silent }),
      refreshAllSuspiciousCases({ silent }),
    ]);
  }, [refreshItems, refreshStats, refreshSuspiciousCases, refreshAllSuspiciousCases]);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      if (document.visibilityState === 'hidden') {
        return;
      }

      void handleRefresh({ silent: true });
    }, 15000);

    return () => window.clearInterval(intervalId);
  }, [handleRefresh]);

  const stats = useMemo(() => {
    const openCount = allItems.filter((item) => item.status === 'OPEN').length;
    const dismissedCount = allItems.filter((item) => item.status === 'DISMISSED').length;

    return {
      total: allItems.length,
      open: openCount,
      dismissed: dismissedCount,
    };
  }, [allItems]);

  const suspiciousStats = useMemo(() => {
    // Get unique accounts by userId to avoid duplicates
    const uniqueUserMap = new Map<string, SuspiciousCaseItem>();
    allSuspiciousCases.forEach((item) => {
      if (!uniqueUserMap.has(item.userId)) {
        uniqueUserMap.set(item.userId, item);
      }
    });

    const uniqueAccounts = Array.from(uniqueUserMap.values());
    const totalCount = uniqueAccounts.length;

    // Count unique accounts by status
    const openUserIds = new Set(
      uniqueAccounts
        .filter((item) => item.status === 'OPEN')
        .map((item) => item.userId),
    );
    const openCount = openUserIds.size;

    const dismissedUserIds = new Set(
      uniqueAccounts
        .filter((item) => item.status === 'DISMISSED')
        .map((item) => item.userId),
    );
    const dismissedCount = dismissedUserIds.size;

    // Count unique locked accounts from anomaly events (allItems has lock info)
    const lockedUserIds = new Set(
      allItems.filter((item) => item.accountLocked).map((item) => item.userId),
    );
    const lockedCount = uniqueAccounts.filter((account) => lockedUserIds.has(account.userId)).length;

    return {
      total: totalCount,
      open: openCount,
      locked: lockedCount,
      dismissed: dismissedCount,
    };
  }, [allSuspiciousCases, allItems]);

  const reasonChartData = useMemo(() => getReasonBreakdown(allItems), [allItems]);
  const timelineChartData = useMemo(() => getTimelineData(allItems), [allItems]);

  const suspiciousAccounts = useMemo(() => suspiciousCases.items, [suspiciousCases.items]);
  const suspiciousPageSize = PAGE_SIZE;
  const suspiciousTotalPages = Math.max(1, suspiciousCases.totalPages || 1);
  const paginatedSuspiciousAccounts = suspiciousAccounts;

  const resetLockForm = () => {
    setSelectedAccount(null);
    setLockMode('temporary');
    setLockDays('0');
    setLockHours('1');
    setLockMinutes('0');
    setLockSubmitting(false);
  };

  const isAccountLocked = (userId: string) => {
    const item = allItems.find((entry) => entry.userId === userId);
    return Boolean(item?.accountLocked);
  };

  const getSuspiciousAccountStateLabel = (userId: string) => {
    const item = allItems.find((entry) => entry.userId === userId);

    if (!item?.accountLocked) {
      return 'Hoạt động bình thường';
    }

    if (item.permanentLock) {
      return 'Đã khóa tài khoản (vĩnh viễn)';
    }

    if (item.lockUntil) {
      const remainingMs = new Date(item.lockUntil).getTime() - Date.now();
      if (remainingMs > 0) {
        return `Đã khóa tài khoản (${formatLockDuration(remainingMs)})`;
      }
    }

    return 'Đã khóa tài khoản';
  };

  const handleAccountAction = async (account: { userId: string; username: string; fullName: string; status?: LoginAnomalyStatus }) => {
    try {
      const locked = isAccountLocked(account.userId);

      if (locked) {
        await unlockAccount(account.userId);
        setViewMode('suspiciousAccounts');
        setSuspiciousStatusFilter('OPEN');
        setSuspiciousPage(0);
        toast.success('Đã mở khóa tài khoản thành công');
      } else {
        setSelectedAccount({
          userId: account.userId,
          username: account.username,
          fullName: account.fullName,
          reasonCode: 'REPEATED_FAILED_LOGIN',
        });
        setLockDialogOpen(true);
        return;
      }

      await refreshStats();
      await refreshItems();
      await refreshSuspiciousCases({ page: 0, status: 'OPEN', username: suspiciousAccountFilter });
      await refreshAllSuspiciousCases();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể xử lý tài khoản');
    }
  };

  const handleMarkResolved = async (account: { userId: string; username: string; fullName: string }) => {
    try {
      const confirmed = window.confirm(`Bạn có muốn đánh dấu tất cả sự kiện bất thường của tài khoản ${account.username} là đã giải quyết?`);
      if (!confirmed) return;

      await resolveUserAnomalies(account.userId);
      setViewMode('suspiciousAccounts');
      setSuspiciousStatusFilter('DISMISSED');
      setSuspiciousPage(0);
      toast.success('Đã đánh dấu tất cả sự kiện bất thường của tài khoản là đã giải quyết');
      await refreshStats();
      await refreshItems();
      await refreshSuspiciousCases({ page: 0, status: 'DISMISSED', username: suspiciousAccountFilter });
      await refreshAllSuspiciousCases();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể đánh dấu đã giải quyết');
    }
  };

  const handleLockSubmit = async () => {
    if (!selectedAccount) return;

    try {
      setLockSubmitting(true);
      const permanent = lockMode === 'permanent';
      const reasonLabel = selectedAccount.reasonCode ? formatReason(selectedAccount.reasonCode) : 'đăng nhập bất thường';
      const parsedDays = Number(lockDays || 0);
      const parsedHours = Number(lockHours || 0);
      const parsedMinutes = Number(lockMinutes || 0);

      await lockAccount(
        selectedAccount.userId,
        selectedAccount.id,
        `Khóa ${permanent ? 'vĩnh viễn' : 'tạm thời'} do phát hiện đăng nhập bất thường: ${reasonLabel}`,
        { days: parsedDays, hours: parsedHours, minutes: parsedMinutes, permanent },
      );

      setSuspiciousStatusFilter('OPEN');
      setSuspiciousPage(0);
      setViewMode('suspiciousAccounts');
      toast.success(
        permanent ? 'Đã khóa vĩnh viễn tài khoản thành công' : 'Đã khóa tài khoản thành công',
      );
      setLockDialogOpen(false);
      resetLockForm();
      await refreshStats();
      await refreshItems();
      await refreshSuspiciousCases({ page: 0, status: 'OPEN', username: suspiciousAccountFilter });
      await refreshAllSuspiciousCases();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể khóa tài khoản');
    } finally {
      setLockSubmitting(false);
    }
  };

  const applyEventStatusFilter = async (nextStatus: LoginAnomalyStatus | 'ALL') => {
    const nextUsername = eventAccountFilter;
    setEventStatusFilter(nextStatus);
    setPage(0);
    await refreshItems({
      page: 0,
      status: nextStatus,
      username: nextUsername,
    });
  };

  const applySuspiciousStatusFilter = async (nextStatus: LoginAnomalyStatus | 'ALL') => {
    const nextUsername = suspiciousAccountFilter;
    setSuspiciousStatusFilter(nextStatus);
    setSuspiciousPage(0);
    await refreshSuspiciousCases({
      page: 0,
      status: nextStatus,
      username: nextUsername,
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3">
          <div className="rounded-xl bg-emerald-100 p-2.5 text-emerald-700">
            <ShieldAlert className="h-6 w-6" />
          </div>
          <div>
            <p className="mb-1 text-xs font-semibold uppercase tracking-[0.18em] text-emerald-700">
              Giám sát bảo mật
            </p>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Theo dõi đăng nhập bất thường</h1>
            <p className="text-sm text-muted-foreground">
              Tổng quan các sự kiện phát hiện bất thường, trạng thái xử lý và hành động khóa/mở khóa tài khoản.
            </p>
          </div>
        </div>

        <Button variant="outline" onClick={() => void handleRefresh()} disabled={eventsLoading || suspiciousLoading || statsLoading}>
          <RefreshCw className={`h-4 w-4 ${eventsLoading || suspiciousLoading || statsLoading ? 'animate-spin' : ''}`} />
          Làm mới
        </Button>
      </div>

      <div className="space-y-5">
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <div className="h-2.5 w-2.5 rounded-full bg-amber-500" />
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-700">Sự kiện bất thường</h2>
          </div>
          <div className="grid gap-4 md:grid-cols-3">
            <Card>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <p className="text-sm text-muted-foreground">Tổng sự kiện</p>
                  <p className="mt-1 text-2xl font-bold">{stats.total}</p>
                </div>
                <AlertTriangle className="h-8 w-8 text-amber-500" />
              </CardContent>
            </Card>

            <Card>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <p className="text-sm text-muted-foreground">Chưa giải quyết</p>
                  <p className="mt-1 text-2xl font-bold">{stats.open}</p>
                </div>
                <BarChart3 className="h-8 w-8 text-amber-500" />
              </CardContent>
            </Card>

            <Card>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <p className="text-sm text-muted-foreground">Đã giải quyết</p>
                  <p className="mt-1 text-2xl font-bold">{stats.dismissed}</p>
                </div>
                <CheckCircle2 className="h-8 w-8 text-emerald-500" />
              </CardContent>
            </Card>
          </div>
        </div>

        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <div className="h-2.5 w-2.5 rounded-full bg-emerald-500" />
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-700">Tài khoản nghi vấn</h2>
          </div>
          <div className="grid gap-4 md:grid-cols-4">
            <Card>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <p className="text-sm text-muted-foreground">Tổng số tài khoản nghi vấn</p>
                  <p className="mt-1 text-2xl font-bold">{suspiciousStats.total}</p>
                </div>
                <ShieldAlert className="h-8 w-8 text-emerald-500" />
              </CardContent>
            </Card>

            <Card>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <p className="text-sm text-muted-foreground">Chưa giải quyết</p>
                  <p className="mt-1 text-2xl font-bold">{suspiciousStats.open}</p>
                </div>
                <AlertTriangle className="h-8 w-8 text-amber-500" />
              </CardContent>
            </Card>

            <Card>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <p className="text-sm text-muted-foreground">Đã khóa</p>
                  <p className="mt-1 text-2xl font-bold">{suspiciousStats.locked}</p>
                </div>
                <Lock className="h-8 w-8 text-red-500" />
              </CardContent>
            </Card>

            <Card>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <p className="text-sm text-muted-foreground">Đã giải quyết</p>
                  <p className="mt-1 text-2xl font-bold">{suspiciousStats.dismissed}</p>
                </div>
                <CheckCircle2 className="h-8 w-8 text-emerald-500" />
              </CardContent>
            </Card>
          </div>
        </div>
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Phân bố theo nguyên nhân</CardTitle>
          </CardHeader>
          <CardContent className="h-72">
            {reasonChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={reasonChartData}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Bar dataKey="value" radius={[8, 8, 0, 0]}>
                    {reasonChartData.map((entry, index) => (
                      <Cell key={`${entry.name}-${index}`} fill={index % 2 === 0 ? '#10b981' : '#f59e0b'} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                Chưa có dữ liệu
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Xu hướng phát hiện</CardTitle>
          </CardHeader>
          <CardContent className="h-72">
            {timelineChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={timelineChartData}
                    dataKey="value"
                    nameKey="label"
                    cx="50%"
                    cy="50%"
                    outerRadius={80}
                  >
                    {timelineChartData.map((entry, index) => (
                      <Cell key={`${entry.label}-${index}`} fill={Object.values(STATUS_COLORS)[index % Object.values(STATUS_COLORS).length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                Chưa có dữ liệu
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <Dialog open={lockDialogOpen} onOpenChange={(open) => {
        setLockDialogOpen(open);
        if (!open) resetLockForm();
      }}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Khóa tài khoản</DialogTitle>
          </DialogHeader>

          {selectedAccount && (
            <div className="space-y-4">
              <div className="rounded-lg border bg-muted/30 p-3 text-sm">
                <div className="font-medium text-foreground">{selectedAccount.fullName || selectedAccount.username}</div>
                <div className="text-muted-foreground">{selectedAccount.username}</div>
              </div>

              <div className="space-y-2">
                <Label>Chọn kiểu khóa</Label>
                <div className="grid grid-cols-2 gap-2">
                  <Button
                    type="button"
                    variant={lockMode === 'temporary' ? 'default' : 'outline'}
                    onClick={() => setLockMode('temporary')}
                  >
                    Khóa theo thời gian
                  </Button>
                  <Button
                    type="button"
                    variant={lockMode === 'permanent' ? 'default' : 'outline'}
                    onClick={() => setLockMode('permanent')}
                  >
                    Khóa vĩnh viễn
                  </Button>
                </div>
              </div>

              {lockMode === 'temporary' && (
                <div className="grid grid-cols-3 gap-3">
                  <div className="space-y-2">
                    <Label htmlFor="lock-days">Ngày</Label>
                    <Input
                      id="lock-days"
                      type="text"
                      inputMode="numeric"
                      min={0}
                      value={lockDays}
                      onChange={(event) => setLockDays(normalizeDurationInput(event.target.value))}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="lock-hours">Giờ</Label>
                    <Input
                      id="lock-hours"
                      type="text"
                      inputMode="numeric"
                      min={0}
                      max={23}
                      value={lockHours}
                      onChange={(event) => setLockHours(normalizeDurationInput(event.target.value))}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="lock-minutes">Phút</Label>
                    <Input
                      id="lock-minutes"
                      type="text"
                      inputMode="numeric"
                      min={0}
                      max={59}
                      value={lockMinutes}
                      onChange={(event) => setLockMinutes(normalizeDurationInput(event.target.value))}
                    />
                  </div>
                </div>
              )}

              {lockMode === 'permanent' && (
                <div className="rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
                  Khi chọn khóa vĩnh viễn, tài khoản sẽ chỉ được mở khóa khi admin nhấn nút mở khóa thủ công. Các trường ngày/giờ/phút sẽ bị bỏ qua.
                </div>
              )}
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setLockDialogOpen(false)} disabled={lockSubmitting}>
              Hủy
            </Button>
            <Button onClick={() => void handleLockSubmit()} disabled={lockSubmitting}>
              {lockSubmitting ? 'Đang xử lý...' : (lockMode === 'permanent' ? 'Khóa vĩnh viễn' : 'Xác nhận khóa')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Card>
        <CardHeader className="pb-0">
          <div className="space-y-3">
            <CardTitle className="text-base">Theo dõi đăng nhập bất thường</CardTitle>
            <Tabs value={viewMode} onValueChange={(value) => setViewMode(value as 'events' | 'suspiciousAccounts')} className="w-full">
              <TabsList className="w-full justify-start rounded-lg bg-slate-100 p-1">
                <TabsTrigger
                  value="events"
                  className="rounded-md px-3 py-2 text-sm h-9 data-[state=active]:bg-white data-[state=active]:text-emerald-700"
                >
                  Sự kiện bất thường
                </TabsTrigger>
                <TabsTrigger
                  value="suspiciousAccounts"
                  className="rounded-md px-3 py-2 text-sm h-9 data-[state=active]:bg-white data-[state=active]:text-emerald-700"
                >
                  Tài khoản nghi vấn
                </TabsTrigger>
              </TabsList>
            </Tabs>
          </div>
        </CardHeader>

        {viewMode === 'events' ? (
          <div className="border-t bg-slate-50 px-4 py-3">
            <div className="flex flex-col gap-3 md:flex-row md:items-center">
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium text-muted-foreground">Trạng thái:</span>
                <div className="flex flex-wrap gap-2">
                  <Button
                    type="button"
                    variant={eventStatusFilter === 'ALL' ? 'default' : 'outline'}
                    size="sm"
                    onClick={() => void applyEventStatusFilter('ALL')}
                  >
                    Tất cả
                  </Button>
                  <Button
                    type="button"
                    variant={eventStatusFilter === 'OPEN' ? 'default' : 'outline'}
                    size="sm"
                    onClick={() => void applyEventStatusFilter('OPEN')}
                  >
                    Chưa giải quyết
                  </Button>
                  <Button
                    type="button"
                    variant={eventStatusFilter === 'DISMISSED' ? 'default' : 'outline'}
                    size="sm"
                    onClick={() => void applyEventStatusFilter('DISMISSED')}
                  >
                    Đã giải quyết
                  </Button>
                </div>
              </div>

              <div className="flex-1 md:max-w-xs">
                <Input
                  value={eventAccountFilter}
                  onChange={(event) => {
                    const nextValue = event.target.value;
                    setEventAccountFilter(nextValue);
                    setPage(0);
                    void refreshItems({
                      page: 0,
                      status: eventStatusFilter,
                      username: nextValue,
                    });
                  }}
                  placeholder="Tìm theo tài khoản / tên"
                  className="h-9"
                />
              </div>
            </div>
          </div>
        ) : (
          <div className="border-t bg-slate-50 px-4 py-3">
            <div className="flex flex-col gap-3 md:flex-row md:items-center">
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium text-muted-foreground">Trạng thái:</span>
                <div className="flex flex-wrap gap-2">
                  {STATUS_OPTIONS.map((option) => (
                    <Button
                      key={option.value}
                      type="button"
                      variant={suspiciousStatusFilter === option.value ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => void applySuspiciousStatusFilter(option.value)}
                    >
                      {option.label}
                    </Button>
                  ))}
                </div>
              </div>

              <div className="flex-1 md:max-w-xs">
                <Input
                  value={suspiciousAccountFilter}
                  onChange={(event) => {
                    const nextValue = event.target.value;
                    setSuspiciousAccountFilter(nextValue);
                    setSuspiciousPage(0);
                    void refreshSuspiciousCases({
                      page: 0,
                      status: suspiciousStatusFilter,
                      username: nextValue,
                    });
                  }}
                  placeholder="Tìm theo tài khoản / tên"
                  className="h-9"
                />
              </div>
            </div>
          </div>
        )}

        <CardContent className="p-0">
          <div className="relative min-h-[420px] overflow-hidden">
            <div
              className={[
                'transition-all duration-200 ease-out',
                viewMode === 'events' ? 'block opacity-100' : 'hidden opacity-0',
              ].join(' ')}
            >
              {eventsLoading ? (
                <div className="flex justify-center py-12">
                  <div className="h-6 w-6 animate-spin rounded-full border-b-2 border-primary" />
                </div>
              ) : items.items.length === 0 ? (
                <div className="px-4 py-16 text-center text-muted-foreground">
                  <AlertTriangle className="mx-auto mb-3 h-10 w-10 text-muted-foreground/50" />
                  <p className="font-medium">Không có sự kiện nào trong trạng thái này</p>
                </div>
              ) : (
                <>
                  <div className="overflow-x-auto">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Tài khoản</TableHead>
                          <TableHead>Nguyên nhân</TableHead>
                          <TableHead>IP</TableHead>
                          <TableHead>Quốc gia</TableHead>
                          <TableHead>Trạng thái</TableHead>
                          <TableHead>Thời gian</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {items.items.map((item) => (
                          <TableRow key={item.id}>
                            <TableCell>
                              <div>
                                <div className="font-medium">{item.fullName || item.username}</div>
                                <div className="text-xs text-muted-foreground">{item.username}</div>
                              </div>
                            </TableCell>
                            <TableCell>
                              <div className="font-medium">{formatReason(item.reasonCode)}</div>
                              {item.attemptCount ? (
                                <div className="text-xs text-muted-foreground">{item.attemptCount} lần thất bại</div>
                              ) : null}
                            </TableCell>
                            <TableCell className="font-mono text-xs">{item.ipAddress}</TableCell>
                            <TableCell>{item.countryCode || '—'}</TableCell>
                            <TableCell>
                              <Badge className={getStatusBadgeClass(item.status)}>{getStatusLabel(item.status)}</Badge>
                            </TableCell>
                            <TableCell>{formatDateTime(item.detectedAt)}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>

                  {items.totalPages > 1 && (
                    <div className="flex items-center justify-between border-t px-4 py-3">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={page === 0}
                        onClick={() => setPage((current) => Math.max(current - 1, 0))}
                      >
                        Trước
                      </Button>
                      <span className="text-sm text-muted-foreground">
                        Trang {items.page + 1} / {items.totalPages}
                      </span>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={page >= items.totalPages - 1}
                        onClick={() => setPage((current) => current + 1)}
                      >
                        Tiếp
                      </Button>
                    </div>
                  )}
                </>
              )}
            </div>

            <div
              className={[
                'transition-all duration-200 ease-out',
                viewMode === 'suspiciousAccounts' ? 'block opacity-100' : 'hidden opacity-0',
              ].join(' ')}
            >
              {suspiciousLoading ? (
                <div className="flex justify-center py-12">
                  <div className="h-6 w-6 animate-spin rounded-full border-b-2 border-primary" />
                </div>
              ) : suspiciousAccounts.length === 0 ? (
                <div className="px-4 py-16 text-center text-muted-foreground">
                  <ShieldAlert className="mx-auto mb-3 h-10 w-10 text-muted-foreground/50" />
                  <p className="font-medium">Không có tài khoản nào trong vòng 24h đạt từ 5 sự kiện bất thường trở lên</p>
                </div>
              ) : (
                <>
                  <div className="overflow-x-auto">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Tài khoản nghi vấn</TableHead>
                          <TableHead>Số sự kiện / 24h</TableHead>
                          <TableHead>Nguyên nhân</TableHead>
                          <TableHead>Trạng thái</TableHead>
                          <TableHead>Thời gian gần nhất</TableHead>
                          <TableHead className="text-center">Thao tác</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {paginatedSuspiciousAccounts.map((account) => (
                          <TableRow key={account.userId}>
                            <TableCell>
                              <div>
                                <div className="font-medium">{account.fullName || account.username}</div>
                                <div className="text-xs text-muted-foreground">{account.username}</div>
                              </div>
                            </TableCell>
                            <TableCell>
                              <span className="font-semibold text-red-600">{account.anomalyCount}</span>
                            </TableCell>
                            <TableCell>Đăng nhập bất thường</TableCell>
                            <TableCell>
                              <Badge className={getStatusBadgeClass(account.status)}>{getStatusLabel(account.status)}</Badge>
                            </TableCell>
                            <TableCell>{formatDateTime(account.lastDetectedAt)}</TableCell>
                            <TableCell>
                              {account.status === 'DISMISSED' || suspiciousStatusFilter === 'DISMISSED' ? (
                                <div className="flex justify-center">
                                  <div className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-left">
                                    <div className="mt-1 text-sm font-medium text-slate-700">
                                      {getSuspiciousAccountStateLabel(account.userId)}
                                    </div>
                                  </div>
                                </div>
                              ) : (
                                <div className="flex justify-center gap-1">
                                  {isAccountLocked(account.userId) ? (
                                    <Button
                                      size="icon-sm"
                                      variant="ghost"
                                      title="Mở khóa tài khoản"
                                      onClick={() => void handleAccountAction(account)}
                                    >
                                      <Unlock className="size-4" />
                                    </Button>
                                  ) : (
                                    <Button
                                      size="icon-sm"
                                      variant="ghost"
                                      title="Khóa tài khoản"
                                      onClick={() => void handleAccountAction(account)}
                                    >
                                      <Lock className="size-4" />
                                    </Button>
                                  )}
                                  <Button
                                    size="icon-sm"
                                    variant="ghost"
                                    title="Đánh dấu đã giải quyết"
                                    onClick={() => void handleMarkResolved(account)}
                                  >
                                    <CheckCircle2 className="size-4" />
                                  </Button>
                                </div>
                              )}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>

                  {suspiciousAccounts.length > suspiciousPageSize && (
                    <div className="flex items-center justify-between border-t px-4 py-3">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={suspiciousPage === 0}
                        onClick={() => setSuspiciousPage((current) => Math.max(current - 1, 0))}
                      >
                        Trước
                      </Button>
                      <span className="text-sm text-muted-foreground">
                        Trang {suspiciousPage + 1} / {suspiciousTotalPages}
                      </span>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={suspiciousPage >= suspiciousTotalPages - 1}
                        onClick={() => setSuspiciousPage((current) => Math.min(current + 1, suspiciousTotalPages - 1))}
                      >
                        Tiếp
                      </Button>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
