import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent } from '@/components/ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Search, RotateCcw } from 'lucide-react';
import { MaterialGroup, MATERIAL_GROUP_LABELS } from '@/enums/materialGroup';
import type { InputMaterialQueryParams } from '@/types/inputMaterial';

interface Props {
  onFilter: (params: InputMaterialQueryParams) => void;
  onReset: () => void;
  loading?: boolean;
}

const GROUP_OPTIONS = [
  { value: 'ALL', label: 'Tất cả nhóm vật tư' },
  { value: MaterialGroup.PESTICIDE, label: MATERIAL_GROUP_LABELS[MaterialGroup.PESTICIDE] },
  { value: MaterialGroup.FERTILIZER, label: MATERIAL_GROUP_LABELS[MaterialGroup.FERTILIZER] },
  { value: MaterialGroup.BIOLOGICAL, label: MATERIAL_GROUP_LABELS[MaterialGroup.BIOLOGICAL] },
  { value: MaterialGroup.OTHER, label: MATERIAL_GROUP_LABELS[MaterialGroup.OTHER] },
];

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'true', label: 'Đang sử dụng' },
  { value: 'false', label: 'Ngừng sử dụng' },
];

export const InputMaterialFilter = ({ onFilter, onReset, loading }: Props) => {
  const [keyword, setKeyword] = useState('');
  const [group, setGroup] = useState<string>('ALL');
  const [status, setStatus] = useState<string>('ALL');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const params: InputMaterialQueryParams = {
      page: 0,
    };
    if (keyword.trim()) params.keyword = keyword.trim();
    if (group !== 'ALL') params.group = group as MaterialGroup;
    if (status !== 'ALL') params.isActive = status === 'true';
    onFilter(params);
  };

  const handleReset = () => {
    setKeyword('');
    setGroup('ALL');
    setStatus('ALL');
    onReset();
  };

  return (
    <Card className="shadow-sm border-emerald-100 dark:border-emerald-950">
      <CardContent className="p-4 sm:p-5">
        <form onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-4 items-end">
            {/* Từ khóa */}
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="keyword" className="text-xs font-semibold text-gray-700 dark:text-gray-300">
                Tìm theo tên / hoạt chất
              </Label>
              <Input
                id="keyword"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="VD: Brightin, Abamectin..."
                className="h-9"
              />
            </div>

            {/* Nhóm vật tư */}
            <div className="flex flex-col gap-1.5">
              <Label className="text-xs font-semibold text-gray-700 dark:text-gray-300">
                Nhóm vật tư
              </Label>
              <Select value={group} onValueChange={(val: string | null) => setGroup(val || 'ALL')}>
                <SelectTrigger size="sm" className="w-full h-9">
                  <SelectValue placeholder="Tất cả nhóm vật tư" />
                </SelectTrigger>
                <SelectContent>
                  {GROUP_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Trạng thái */}
            <div className="flex flex-col gap-1.5">
              <Label className="text-xs font-semibold text-gray-700 dark:text-gray-300">
                Trạng thái
              </Label>
              <Select value={status} onValueChange={(val: string | null) => setStatus(val || 'ALL')}>
                <SelectTrigger size="sm" className="w-full h-9">
                  <SelectValue placeholder="Tất cả trạng thái" />
                </SelectTrigger>
                <SelectContent>
                  {STATUS_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Buttons */}
            <div className="flex gap-2">
              <Button type="submit" variant="search" disabled={loading} className="flex-1 h-9">
                <Search className="mr-1.5 h-4 w-4" />
                Lọc dữ liệu
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={handleReset}
                disabled={loading}
                className="h-9 px-3"
                title="Đặt lại bộ lọc"
              >
                <RotateCcw className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};
