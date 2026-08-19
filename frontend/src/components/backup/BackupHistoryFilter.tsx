// frontend/src/components/backup/BackupHistoryFilter.tsx
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Search, X } from 'lucide-react';
import type { BackupOperationType, BackupStatus } from '@/types/backup';

const operationOptions: { value: BackupOperationType; label: string }[] = [
  { value: 'BACKUP', label: 'Sao lưu' },
  { value: 'RESTORE', label: 'Phục hồi' },
];

const statusOptions: { value: BackupStatus; label: string }[] = [
  { value: 'IN_PROGRESS', label: 'Đang xử lý' },
  { value: 'SUCCESS', label: 'Thành công' },
  { value: 'FAILED', label: 'Thất bại' },
];

interface Props {
  operationType?: BackupOperationType;
  status?: BackupStatus;
  onOperationTypeChange: (val?: BackupOperationType) => void;
  onStatusChange: (val?: BackupStatus) => void;
  onApply: () => void;
  onReset: () => void;
}

export const BackupHistoryFilter = ({
  operationType,
  status,
  onOperationTypeChange,
  onStatusChange,
  onApply,
  onReset,
}: Props) => {
  // Helper lấy nhãn hiển thị
  const getOperationLabel = (value?: BackupOperationType) => {
    if (!value) return 'Tất cả';
    const option = operationOptions.find((opt) => opt.value === value);
    return option ? option.label : value;
  };

  const getStatusLabel = (value?: BackupStatus) => {
    if (!value) return 'Tất cả';
    const option = statusOptions.find((opt) => opt.value === value);
    return option ? option.label : value;
  };

  return (
    <div className="flex flex-wrap items-end gap-3 p-3 bg-muted/30 rounded-lg border">
      {/* Loại thao tác */}
      <div className="flex-1 min-w-[150px]">
        <label className="text-xs font-medium text-muted-foreground">Loại</label>
        <Select
          value={operationType || ''}
          onValueChange={(val) =>
            onOperationTypeChange(val ? (val as BackupOperationType) : undefined)
          }
        >
          <SelectTrigger className="h-9">
            <SelectValue placeholder="Tất cả">
              {getOperationLabel(operationType)}
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">Tất cả</SelectItem>
            {operationOptions.map((opt) => (
              <SelectItem key={opt.value} value={opt.value}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Trạng thái */}
      <div className="flex-1 min-w-[150px]">
        <label className="text-xs font-medium text-muted-foreground">Trạng thái</label>
        <Select
          value={status || ''}
          onValueChange={(val) =>
            onStatusChange(val ? (val as BackupStatus) : undefined)
          }
        >
          <SelectTrigger className="h-9">
            <SelectValue placeholder="Tất cả">
              {getStatusLabel(status)}
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">Tất cả</SelectItem>
            {statusOptions.map((opt) => (
              <SelectItem key={opt.value} value={opt.value}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Button size="sm" onClick={onApply} className="gap-1 h-9">
        <Search className="h-3.5 w-3.5" /> Lọc
      </Button>
      <Button size="sm" variant="outline" onClick={onReset} className="gap-1 h-9">
        <X className="h-3.5 w-3.5" /> Xóa
      </Button>
    </div>
  );
};