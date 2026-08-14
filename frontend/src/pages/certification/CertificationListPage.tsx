import { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { PlusCircle, RefreshCw, Search } from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Card,
  CardContent,
} from '@/components/ui/card';
import { getAllCertifications } from '@/api/certificationApi';
import type { CertificationResponse } from '@/types/certification';
import { CertificationStatusBadge } from '@/components/certification/CertificationStatusBadge';
import { CertificationDetailDialog } from '@/components/certification/CertificationDetailDialog';
import { usePermission } from '@/hooks/usePermission';

type SortField = 'name' | 'issueDate' | 'expiryDate' | 'status';
type SortDir = 'asc' | 'desc';

const ITEMS_PER_PAGE = 10;

// 👇 Thêm mapping trạng thái
const STATUS_LABELS: Record<string, string> = {
  all: 'Tất cả trạng thái',
  valid: 'Còn hiệu lực',
  expiring: 'Sắp hết hạn',
  expired: 'Hết hạn',
};

const getStatusLabel = (value: string): string => {
  return STATUS_LABELS[value] || value;
};

const CertificationListPage = () => {
  const navigate = useNavigate();
  const canCreate = usePermission(['VT-02']);

  const [certifications, setCertifications] = useState<CertificationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [sortField, setSortField] = useState<SortField>('issueDate');
  const [sortDir, setSortDir] = useState<SortDir>('desc');
  const [page, setPage] = useState(0);
  const [selectedCertId, setSelectedCertId] = useState<string | null>(null);

  const fetchCertifications = async () => {
    try {
      setLoading(true);
      const data = await getAllCertifications();
      setCertifications(data);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải danh sách chứng nhận');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCertifications();
  }, []);

  const filtered = useMemo(() => {
    let result = [...certifications];

    if (search.trim()) {
      const lower = search.toLowerCase();
      result = result.filter(
        (c) =>
          c.name.toLowerCase().includes(lower) ||
          c.code.toLowerCase().includes(lower) ||
          (c.issuedBy && c.issuedBy.toLowerCase().includes(lower))
      );
    }

    if (statusFilter === 'valid') {
      result = result.filter((c) => c.isValid);
    } else if (statusFilter === 'expired') {
      result = result.filter((c) => !c.isValid);
    } else if (statusFilter === 'expiring') {
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const threshold = new Date(today.getTime() + 30 * 24 * 60 * 60 * 1000);
      result = result.filter((c) => {
        const expiry = new Date(c.expiryDate + 'T00:00:00');
        return c.isValid && expiry <= threshold;
      });
    }

    result.sort((a, b) => {
      let cmp = 0;
      switch (sortField) {
        case 'name':
          cmp = a.name.localeCompare(b.name, 'vi');
          break;
        case 'issueDate':
          cmp = a.issueDate.localeCompare(b.issueDate);
          break;
        case 'expiryDate':
          cmp = a.expiryDate.localeCompare(b.expiryDate);
          break;
        case 'status':
          cmp = (a.isValid ? 1 : 0) - (b.isValid ? 1 : 0);
          break;
      }
      return sortDir === 'asc' ? cmp : -cmp;
    });

    return result;
  }, [certifications, search, statusFilter, sortField, sortDir]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / ITEMS_PER_PAGE));
  const paged = filtered.slice(page * ITEMS_PER_PAGE, (page + 1) * ITEMS_PER_PAGE);

  const toggleSortDir = (field: SortField) => {
    if (sortField === field) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDir('asc');
    }
    setPage(0);
  };

  const sortIndicator = (field: SortField) => {
    if (sortField !== field) return '';
    return sortDir === 'asc' ? ' ↑' : ' ↓';
  };

  const formatDate = (dateStr: string) => {
    try {
      return new Date(dateStr + 'T00:00:00').toLocaleDateString('vi-VN');
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="py-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-[#111827]">Quản lý chứng nhận</h1>
          <p className="text-sm text-[#6B7280]">
            Danh sách các chứng nhận của tổ chức bạn
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={fetchCertifications} disabled={loading}>
            <RefreshCw className={`h-4 w-4 mr-1 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
          {canCreate && (
            <Button variant="create" onClick={() => navigate('/certifications/create')}>
              <PlusCircle className="h-4 w-4 mr-1" />
              Tạo chứng nhận
            </Button>
          )}
        </div>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#9CA3AF]" />
              <Input
                placeholder="Tìm theo tên, mã hoặc cơ quan cấp..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
                className="pl-10"
              />
            </div>
            {/* 👇 Select đã sửa */}
            <Select
              value={statusFilter}
              onValueChange={(v) => {
                setStatusFilter(v ?? 'all');
                setPage(0);
              }}
            >
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="Trạng thái">
                  {getStatusLabel(statusFilter)}
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Tất cả trạng thái</SelectItem>
                <SelectItem value="valid">Còn hiệu lực</SelectItem>
                <SelectItem value="expiring">Sắp hết hạn</SelectItem>
                <SelectItem value="expired">Hết hạn</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Table */}
      <Card>
        <CardContent className="p-0">
          {loading ? (
            <div className="text-center py-12 text-[#6B7280]">Đang tải dữ liệu...</div>
          ) : filtered.length === 0 ? (
            <div className="text-center py-12 text-[#6B7280]">
              {search || statusFilter !== 'all'
                ? 'Không tìm thấy chứng nhận nào phù hợp với bộ lọc.'
                : 'Chưa có chứng nhận nào. Nhấn "Tạo chứng nhận" để thêm mới.'}
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead
                        className="cursor-pointer select-none"
                        onClick={() => toggleSortDir('name')}
                      >
                        Tên chứng nhận{sortIndicator('name')}
                      </TableHead>
                      <TableHead>Mã</TableHead>
                      <TableHead>Cơ quan cấp</TableHead>
                      <TableHead
                        className="cursor-pointer select-none"
                        onClick={() => toggleSortDir('issueDate')}
                      >
                        Ngày cấp{sortIndicator('issueDate')}
                      </TableHead>
                      <TableHead
                        className="cursor-pointer select-none"
                        onClick={() => toggleSortDir('expiryDate')}
                      >
                        Ngày hết hạn{sortIndicator('expiryDate')}
                      </TableHead>
                      <TableHead
                        className="cursor-pointer select-none"
                        onClick={() => toggleSortDir('status')}
                      >
                        Trạng thái{sortIndicator('status')}
                      </TableHead>
                      <TableHead className="text-right">Thao tác</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {paged.map((cert) => (
                      <TableRow key={cert.id}>
                        <TableCell className="font-medium">{cert.name}</TableCell>
                        <TableCell className="font-mono text-sm">{cert.code}</TableCell>
                        <TableCell>{cert.issuedBy || '—'}</TableCell>
                        <TableCell>{formatDate(cert.issueDate)}</TableCell>
                        <TableCell>{formatDate(cert.expiryDate)}</TableCell>
                        <TableCell>
                          <CertificationStatusBadge
                            isValid={cert.isValid}
                            expiryDate={cert.expiryDate}
                          />
                        </TableCell>
                        <TableCell className="text-right">
                          <Button
                            variant="view"
                            size="sm"
                            onClick={() => setSelectedCertId(cert.id)}
                          >
                            Xem
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {/* Pagination */}
              <div className="flex items-center justify-between px-6 py-4 border-t border-[#E5E7EB]">
                <div className="text-sm text-[#6B7280]">
                  Hiển thị {paged.length > 0 ? page * ITEMS_PER_PAGE + 1 : 0}–
                  {Math.min((page + 1) * ITEMS_PER_PAGE, filtered.length)} trong
                  tổng số {filtered.length} chứng nhận
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 0}
                    onClick={() => setPage((p) => p - 1)}
                  >
                    Trước
                  </Button>
                  {Array.from({ length: totalPages }, (_, i) => (
                    <Button
                      key={i}
                      variant={i === page ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => setPage(i)}
                    >
                      {i + 1}
                    </Button>
                  ))}
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Sau
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {/* Detail Dialog */}
      <CertificationDetailDialog
        certification={selectedCertId ? certifications.find((c) => c.id === selectedCertId) ?? null : null}
        open={!!selectedCertId}
        onClose={() => setSelectedCertId(null)}
      />
    </div>
  );
};

export default CertificationListPage;