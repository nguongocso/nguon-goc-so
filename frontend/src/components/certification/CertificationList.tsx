import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Trash2 } from 'lucide-react';
import type { ProductionLotCertification } from '@/types/certification';

interface Props {
  certifications: ProductionLotCertification[];
  onDetach: (certificationId: string) => void;
  canManage: boolean;
  loading?: boolean;
}

const formatDate = (dateStr: string) => {
  try {
    return new Date(dateStr).toLocaleDateString('vi-VN');
  } catch {
    return dateStr;
  }
};

export const CertificationList = ({ certifications, onDetach, canManage, loading }: Props) => {
  if (loading) return <div className="text-center py-4">Đang tải...</div>;

  if (!certifications || certifications.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        <p>Chưa có chứng nhận nào được gắn cho lô này.</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Tên chứng nhận</TableHead>
            <TableHead>Mã</TableHead>
            <TableHead>Tổ chức cấp</TableHead>
            <TableHead>Ngày cấp</TableHead>
            <TableHead>Ngày hết hạn</TableHead>
            <TableHead>Trạng thái</TableHead>
            <TableHead>Ghi chú</TableHead>
            {canManage && <TableHead className="text-right">Thao tác</TableHead>}
          </TableRow>
        </TableHeader>
        <TableBody>
          {certifications.map((cert) => (
            <TableRow key={cert.id}>
              <TableCell className="font-medium">{cert.certificationName}</TableCell>
              <TableCell>{cert.certificationCode}</TableCell>
              <TableCell>{cert.issuedBy || '—'}</TableCell>
              <TableCell>{formatDate(cert.issueDate)}</TableCell>
              <TableCell>{formatDate(cert.expiryDate)}</TableCell>
              <TableCell>
                {cert.isValid ? (
                  <Badge variant="success">
                    Còn hiệu lực
                  </Badge>
                ) : (
                  <Badge variant="destructive">Hết hạn</Badge>
                )}
              </TableCell>
              <TableCell className="max-w-[150px] truncate">{cert.note || '—'}</TableCell>
              {canManage && (
                <TableCell className="text-right">
                  <Button
                    variant="delete"
                    size="sm"
                    onClick={() => onDetach(cert.certificationId)}
                    title="Gỡ chứng nhận"
                  >
                    <Trash2 className="h-4 w-4 text-destructive" />
                  </Button>
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
};