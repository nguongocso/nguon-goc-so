import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  Clock,
  ExternalLink,
  CheckCircle2,
  Building2,
  Eye,
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { DataTableShell } from '@/components/common/DataTableShell';
import { TableHead, TableRow, TableCell } from '@/components/ui/table';
import type { AggregateAlertItem, AggregateAlertType } from '@/types/aggregateAlert';

interface AggregateAlertTableProps {
  items: AggregateAlertItem[];
  isAdmin?: boolean;
  page?: number;
  pageSize?: number;
  loading?: boolean;
  hasActiveFilters?: boolean;
  onResetFilters?: () => void;
}

const TYPE_STYLE_MAP: Record<AggregateAlertType, { bg: string; text: string; border: string }> = {
  SCAN_ANOMALY: { bg: 'bg-rose-50', text: 'text-rose-700', border: 'border-rose-200' },
  CERT_EXPIRING: { bg: 'bg-emerald-50', text: 'text-emerald-700', border: 'border-emerald-200' },
  CERT_EXPIRED: { bg: 'bg-teal-50', text: 'text-teal-800', border: 'border-teal-200' },
  INSPECTION_EXPIRING: { bg: 'bg-sky-50', text: 'text-sky-700', border: 'border-sky-200' },
  INSPECTION_EXPIRED: { bg: 'bg-cyan-50', text: 'text-cyan-800', border: 'border-cyan-200' },
  UNPROCESSED_FEEDBACK: { bg: 'bg-blue-50', text: 'text-blue-700', border: 'border-blue-200' },
  CODE_RANGE_QUOTA: { bg: 'bg-amber-50', text: 'text-amber-800', border: 'border-amber-200' },
  OVERDUE_MILESTONE: { bg: 'bg-purple-50', text: 'text-purple-700', border: 'border-purple-200' },
  OPEN_RECALL_CASE: { bg: 'bg-red-100', text: 'text-red-800', border: 'border-red-300' },
  API_KEY_EXPIRING: { bg: 'bg-orange-50', text: 'text-orange-700', border: 'border-orange-200' },
  API_KEY_QUOTA_WARNING: { bg: 'bg-yellow-50', text: 'text-yellow-800', border: 'border-yellow-200' },
};

const RELATED_ENTITY_TYPE_LABELS: Record<string, string> = {
  TRACE_CODE: 'Mã tem truy xuất',
  ProductionLot: 'Lô sản xuất',
  PRODUCTION_LOT: 'Lô sản xuất',
  Certification: 'Chứng nhận chất lượng',
  CERTIFICATION: 'Chứng nhận chất lượng',
  PRODUCT_FEEDBACK: 'Phản ánh người tiêu dùng',
  CODE_RANGE: 'Dải mã truy xuất',
  MILESTONE_REMINDER: 'Mốc canh tác bắt buộc',
  RECALL_CASE: 'Vụ việc thu hồi',
  PARTNER_API_KEY: 'Khóa API đối tác',
};

export const AggregateAlertTable: React.FC<AggregateAlertTableProps> = ({
  items,
  isAdmin = false,
  page = 0,
  pageSize = 10,
  loading = false,
  hasActiveFilters = false,
  onResetFilters,
}) => {
  const navigate = useNavigate();

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '—';
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateStr;
    }
  };

  const handleActionClick = (actionUrl: string) => {
    if (actionUrl) {
      navigate(actionUrl);
    }
  };

  return (
    <DataTableShell
      header={
        <>
          <TableHead className="w-12 text-center">STT</TableHead>
          <TableHead className="w-28">Mức độ</TableHead>
          <TableHead className="w-48">Nguồn cảnh báo</TableHead>
          <TableHead>Nội dung cảnh báo</TableHead>
          <TableHead className="w-48">Đối tượng liên quan</TableHead>
          {isAdmin && <TableHead className="w-40">Tổ chức</TableHead>}
          <TableHead className="w-36">Thời gian</TableHead>
          <TableHead className="w-32 text-center">Thao tác</TableHead>
        </>
      }
      body={items.map((item, index) => {
        const isHigh = item.severity === 'HIGH';
        const typeStyle = TYPE_STYLE_MAP[item.type] || {
          bg: 'bg-gray-50',
          text: 'text-gray-700',
          border: 'border-gray-200',
        };

        return (
          <TableRow key={item.id} className="hover:bg-muted/40 transition-colors">
            {/* Cột 1: STT */}
            <TableCell className="text-center font-medium text-muted-foreground">
              {page * pageSize + index + 1}
            </TableCell>

            {/* Cột 2: Mức độ khẩn cấp */}
            <TableCell>
              {isHigh ? (
                <Badge
                  variant="outline"
                  className="gap-1 border-red-200 bg-red-50 text-red-700 font-semibold px-2 py-0.5"
                >
                  <AlertTriangle className="h-3.5 w-3.5 text-red-600 shrink-0" />
                  Mức cao
                </Badge>
              ) : (
                <Badge
                  variant="outline"
                  className="gap-1 border-amber-200 bg-amber-50 text-amber-700 font-medium px-2 py-0.5"
                >
                  <Clock className="h-3.5 w-3.5 text-amber-600 shrink-0" />
                  Trung bình
                </Badge>
              )}
            </TableCell>

            {/* Cột 3: Loại nguồn cảnh báo */}
            <TableCell>
              <span
                className={`inline-block rounded-md border px-2.5 py-1 text-xs font-medium whitespace-nowrap ${typeStyle.bg} ${typeStyle.text} ${typeStyle.border}`}
              >
                {item.typeName}
              </span>
            </TableCell>

            {/* Cột 4: Nội dung cảnh báo */}
            <TableCell className="whitespace-normal min-w-[280px]">
              <div className="font-medium text-foreground leading-snug break-words">
                {item.title}
              </div>
              <div className="text-xs text-muted-foreground mt-1 leading-relaxed break-words">
                {item.message}
              </div>
            </TableCell>

            {/* Cột 5: Đối tượng liên quan */}
            <TableCell className="whitespace-normal min-w-[160px] max-w-[220px]">
              <div className="font-medium text-foreground text-xs leading-snug break-words" title={item.relatedEntityName || '—'}>
                {item.relatedEntityName || '—'}
              </div>
              <div className="text-[11px] text-muted-foreground mt-0.5">
                {RELATED_ENTITY_TYPE_LABELS[item.relatedEntityType] || item.relatedEntityType}
              </div>
            </TableCell>

            {/* Cột 6: Tổ chức (chỉ hiển thị cho VT-01) */}
            {isAdmin && (
              <TableCell>
                <div className="flex items-center gap-1.5 text-xs text-muted-foreground font-medium truncate max-w-[160px]" title={item.organizationName}>
                  <Building2 className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                  <span className="truncate">{item.organizationName || '—'}</span>
                </div>
              </TableCell>
            )}

            {/* Cột 7: Thời gian */}
            <TableCell className="text-xs text-muted-foreground">
              {formatDate(item.createdAt)}
            </TableCell>

            {/* Cột 8: Nút lối tắt xử lý trực tiếp */}
            <TableCell className="text-center">
              {item.status === 'RESOLVED' ? (
                <span className="inline-flex items-center gap-1 text-xs text-emerald-600 font-medium">
                  <CheckCircle2 className="h-3.5 w-3.5" />
                  Đã xử lý
                </span>
              ) : (() => {
                let label = 'Xử lý ngay';
                let IconComponent = ExternalLink;
                let onClick = () => handleActionClick(item.actionUrl);

                if (isAdmin) {
                  if (item.type === 'OVERDUE_MILESTONE') {
                    label = 'Giám sát lô';
                    IconComponent = Eye;
                    onClick = () => {
                      toast.info('Cảnh báo mốc canh tác thuộc nghiệp vụ sản xuất của Quản lý HTX (VT-02). Bạn đang chuyển tới trang lô sản xuất để giám sát.');
                      handleActionClick(item.actionUrl);
                    };
                  } else if (item.type === 'INSPECTION_EXPIRING' || item.type === 'INSPECTION_EXPIRED') {
                    label = 'Giám sát lô';
                    IconComponent = Eye;
                    onClick = () => {
                      toast.info('Bạn đang chuyển tới trang lô sản xuất để giám sát kết quả kiểm nghiệm.');
                      handleActionClick(item.actionUrl);
                    };
                  } else if (item.type === 'CODE_RANGE_QUOTA') {
                    label = 'Duyệt cấp bù';
                  } else if (item.type === 'API_KEY_EXPIRING') {
                    label = 'Gia hạn';
                    onClick = () => {
                      const url = item.actionUrl ? `${item.actionUrl}${item.actionUrl.includes('?') ? '&' : '?'}keyId=${item.relatedEntityId}&action=renew` : `/integration/api-keys?keyId=${item.relatedEntityId}&action=renew`;
                      navigate(url);
                    };
                  } else if (item.type === 'API_KEY_QUOTA_WARNING') {
                    label = 'Nâng hạn mức';
                    onClick = () => {
                      const url = item.actionUrl ? `${item.actionUrl}${item.actionUrl.includes('?') ? '&' : '?'}keyId=${item.relatedEntityId}&action=quota` : `/integration/api-keys?keyId=${item.relatedEntityId}&action=quota`;
                      navigate(url);
                    };
                  } else if (item.type === 'CERT_EXPIRING' || item.type === 'CERT_EXPIRED') {
                    label = 'Thẩm định';
                  }
                }

                return (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={onClick}
                    className="h-8 gap-1.5 text-xs font-medium text-emerald-700 hover:text-emerald-800 hover:bg-emerald-50 border-emerald-300"
                  >
                    <span>{label}</span>
                    <IconComponent className="h-3.5 w-3.5 text-emerald-600" />
                  </Button>
                );
              })()}
            </TableCell>
          </TableRow>
        );
      })}
      loading={loading}
      empty={!loading && items.length === 0}
      colSpan={isAdmin ? 8 : 7}
      loadingMessage="Đang tổng hợp cảnh báo từ các nguồn..."
      emptyMessage={
        hasActiveFilters
          ? 'Không tìm thấy cảnh báo phù hợp'
          : 'Không có cảnh báo nào đang mở'
      }
      emptyAction={
        hasActiveFilters ? (
          <div className="flex flex-col items-center gap-2">
            <span className="text-xs text-muted-foreground max-w-md text-center">
              Không có cảnh báo nào khớp với các tiêu chí tìm kiếm hoặc bộ lọc hiện tại của bạn.
            </span>
            {onResetFilters && (
              <Button
                variant="link"
                size="sm"
                onClick={onResetFilters}
                className="text-xs text-emerald-600 hover:text-emerald-700 h-auto p-0"
              >
                Đặt lại bộ lọc
              </Button>
            )}
          </div>
        ) : (
          <span className="text-xs text-muted-foreground max-w-md text-center">
            Tất cả các nguồn cảnh báo của tổ chức hiện đang ở trạng thái an toàn hoặc đã được giải quyết dứt điểm.
          </span>
        )
      }
    />
  );
};

