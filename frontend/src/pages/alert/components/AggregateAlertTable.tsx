import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  Clock,
  ExternalLink,
  CheckCircle2,
  Building2,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import type { AggregateAlertItem, AggregateAlertType } from '@/types/aggregateAlert';

interface AggregateAlertTableProps {
  items: AggregateAlertItem[];
  isAdmin?: boolean;
}

const TYPE_STYLE_MAP: Record<AggregateAlertType, { bg: string; text: string; border: string }> = {
  SCAN_ANOMALY: { bg: 'bg-rose-50', text: 'text-rose-700', border: 'border-rose-200' },
  CERT_EXPIRING: { bg: 'bg-amber-50', text: 'text-amber-700', border: 'border-amber-200' },
  CERT_EXPIRED: { bg: 'bg-red-50', text: 'text-red-700', border: 'border-red-200' },
  INSPECTION_EXPIRING: { bg: 'bg-orange-50', text: 'text-orange-700', border: 'border-orange-200' },
  INSPECTION_EXPIRED: { bg: 'bg-red-50', text: 'text-red-700', border: 'border-red-200' },
  UNPROCESSED_FEEDBACK: { bg: 'bg-blue-50', text: 'text-blue-700', border: 'border-blue-200' },
  CODE_RANGE_QUOTA: { bg: 'bg-yellow-50', text: 'text-yellow-800', border: 'border-yellow-200' },
  OVERDUE_MILESTONE: { bg: 'bg-purple-50', text: 'text-purple-700', border: 'border-purple-200' },
  OPEN_RECALL_CASE: { bg: 'bg-red-100', text: 'text-red-800', border: 'border-red-300' },
};

export const AggregateAlertTable: React.FC<AggregateAlertTableProps> = ({
  items,
  isAdmin = false,
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
    <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white shadow-sm">
      <table className="w-full text-left text-sm text-gray-600">
        <thead className="border-b border-gray-200 bg-gray-50/75 text-xs font-semibold uppercase tracking-wider text-gray-500">
          <tr>
            <th scope="col" className="px-4 py-3.5 whitespace-nowrap w-28">
              Mức độ
            </th>
            <th scope="col" className="px-4 py-3.5 whitespace-nowrap w-44">
              Nguồn cảnh báo
            </th>
            <th scope="col" className="px-4 py-3.5 min-w-[240px]">
              Nội dung cảnh báo
            </th>
            <th scope="col" className="px-4 py-3.5 whitespace-nowrap w-48">
              Đối tượng liên quan
            </th>
            {isAdmin && (
              <th scope="col" className="px-4 py-3.5 whitespace-nowrap w-44">
                Tổ chức
              </th>
            )}
            <th scope="col" className="px-4 py-3.5 whitespace-nowrap w-36">
              Thời gian
            </th>
            <th scope="col" className="px-4 py-3.5 text-right whitespace-nowrap w-32">
              Thao tác
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200">
          {items.map((item) => {
            const isHigh = item.severity === 'HIGH';
            const typeStyle = TYPE_STYLE_MAP[item.type] || {
              bg: 'bg-gray-50',
              text: 'text-gray-700',
              border: 'border-gray-200',
            };

            return (
              <tr
                key={item.id}
                className="transition-colors hover:bg-gray-50/80"
              >
                {/* Cột 1: Mức độ khẩn cấp */}
                <td className="px-4 py-3.5 whitespace-nowrap">
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
                </td>

                {/* Cột 2: Loại nguồn cảnh báo */}
                <td className="px-4 py-3.5 whitespace-nowrap">
                  <span
                    className={`inline-block rounded-md border px-2 py-0.5 text-xs font-medium ${typeStyle.bg} ${typeStyle.text} ${typeStyle.border}`}
                  >
                    {item.typeName}
                  </span>
                </td>

                {/* Cột 3: Nội dung cảnh báo */}
                <td className="px-4 py-3.5">
                  <div className="font-medium text-gray-900 line-clamp-1">
                    {item.title}
                  </div>
                  <div className="text-xs text-gray-500 line-clamp-2 mt-0.5">
                    {item.message}
                  </div>
                </td>

                {/* Cột 4: Đối tượng liên quan */}
                <td className="px-4 py-3.5 whitespace-nowrap">
                  <div className="font-medium text-gray-800 text-xs truncate max-w-[180px]" title={item.relatedEntityName || '—'}>
                    {item.relatedEntityName || '—'}
                  </div>
                  <div className="text-[11px] text-gray-400 mt-0.5">
                    {item.relatedEntityType}
                  </div>
                </td>

                {/* Cột 5: Tổ chức (chỉ hiển thị cho VT-01) */}
                {isAdmin && (
                  <td className="px-4 py-3.5 whitespace-nowrap">
                    <div className="flex items-center gap-1.5 text-xs text-gray-700 font-medium truncate max-w-[160px]" title={item.organizationName}>
                      <Building2 className="h-3.5 w-3.5 text-gray-400 shrink-0" />
                      <span className="truncate">{item.organizationName || '—'}</span>
                    </div>
                  </td>
                )}

                {/* Cột 6: Thời gian */}
                <td className="px-4 py-3.5 whitespace-nowrap text-xs text-gray-500">
                  {formatDate(item.createdAt)}
                </td>

                {/* Cột 7: Nút lối tắt xử lý trực tiếp */}
                <td className="px-4 py-3.5 text-right whitespace-nowrap">
                  {item.status === 'RESOLVED' ? (
                    <span className="inline-flex items-center gap-1 text-xs text-emerald-600 font-medium">
                      <CheckCircle2 className="h-3.5 w-3.5" />
                      Đã xử lý
                    </span>
                  ) : (
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => handleActionClick(item.actionUrl)}
                      className="h-8 gap-1.5 text-xs font-medium text-emerald-700 hover:text-emerald-800 hover:bg-emerald-50 border-emerald-300"
                    >
                      <span>Xử lý ngay</span>
                      <ExternalLink className="h-3.5 w-3.5 text-emerald-600" />
                    </Button>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};
