import { Eye } from 'lucide-react';
import { DataTableShell } from '@/components/common/DataTableShell';
import { StatusBadge } from '@/components/common/StatusBadge';
import { Button } from '@/components/ui/button';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import type {
  CertificateVerification,
  CertificateVerificationStatus,
} from '@/types/certificateVerification';

const STATUS_LABEL: Record<CertificateVerificationStatus, string> = {
  PENDING: 'Đang chờ xác thực',
  VERIFIED: 'Đã xác thực',
  REJECTED: 'Đã từ chối',
};

const STATUS_TONE = {
  PENDING: 'warning',
  VERIFIED: 'success',
  REJECTED: 'danger',
} as const;

/** Định dạng ngày giờ chuẩn dd/MM/yyyy HH:mm dùng chung cho bảng xác thực. */
export const formatCertificateDateTime = (value?: string | null): string => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (part: number): string => String(part).padStart(2, '0');
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

interface CertificateVerificationTableProps {
  items: CertificateVerification[];
  loading: boolean;
  /** Số thứ tự bắt đầu của trang hiện tại (dùng cho phân trang). */
  startIndex: number;
  onViewDetail: (item: CertificateVerification) => void;
}

export const CertificateVerificationTable = ({
  items,
  loading,
  startIndex,
  onViewDetail,
}: CertificateVerificationTableProps) => {
  const header = (
    <>
      <TableHead className="w-14 text-center">STT</TableHead>
      <TableHead>Tiêu chuẩn</TableHead>
      <TableHead>Số hiệu</TableHead>
      <TableHead>Tổ chức</TableHead>
      <TableHead>Cơ quan cấp</TableHead>
      <TableHead>Ngày nộp</TableHead>
      <TableHead className="text-center">Trạng thái</TableHead>
      <TableHead className="text-center">Thao tác</TableHead>
    </>
  );

  const body = items.map((item, index) => (
    <TableRow key={item.id} className="transition-colors hover:bg-muted/40">
      <TableCell className="text-center font-medium text-muted-foreground">
        {startIndex + index + 1}
      </TableCell>
      <TableCell className="font-medium">{item.standardName}</TableCell>
      <TableCell className="font-mono text-xs">{item.code}</TableCell>
      <TableCell className="max-w-64 truncate" title={item.organizationName}>
        {item.organizationName}
      </TableCell>
      <TableCell className="max-w-64 truncate" title={item.issuedBy || undefined}>
        {item.issuedBy || '—'}
      </TableCell>
      <TableCell className="tabular-nums">{formatCertificateDateTime(item.createdAt)}</TableCell>
      <TableCell>
        <div className="flex justify-center">
          <StatusBadge
            label={STATUS_LABEL[item.verificationStatus]}
            tone={STATUS_TONE[item.verificationStatus]}
            className={item.verificationStatus === 'PENDING' ? 'text-amber-800' : undefined}
          />
        </div>
      </TableCell>
      <TableCell className="text-center">
        <Button variant="outline" size="sm" onClick={() => onViewDetail(item)}>
          <Eye /> Chi tiết
        </Button>
      </TableCell>
    </TableRow>
  ));

  return (
    <DataTableShell
      header={header}
      body={body}
      loading={loading}
      empty={!loading && items.length === 0}
      colSpan={8}
      loadingMessage="Đang tải chứng nhận..."
      emptyMessage="Không có chứng nhận phù hợp."
    />
  );
};

export default CertificateVerificationTable;
