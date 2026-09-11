import React, { useCallback, useEffect, useState } from 'react';
import { Activity } from 'lucide-react';
import { getChainProgressBoard } from '@/api/productionLotApi';
import { ChainProgressBoard } from '@/components/production-lot/ChainProgressBoard';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { ListCard } from '@/components/common/ListCard';
import { ListToolbar } from '@/components/common/ListToolbar';
import { SearchInput } from '@/components/common/SearchInput';
import { FilterSelect } from '@/components/common/FilterSelect';
import { RefreshButton } from '@/components/common/RefreshButton';
import { StatusBadge } from '@/components/common/StatusBadge';
import type { ChainProgressBoardData } from '@/types/productionLot';

const STAGNANT_THRESHOLD_OPTIONS = [
  { value: '5', label: 'Ngưỡng 5 ngày' },
  { value: '7', label: 'Ngưỡng 7 ngày' },
  { value: '10', label: 'Ngưỡng 10 ngày (Mặc định)' },
  { value: '14', label: 'Ngưỡng 14 ngày' },
  { value: '30', label: 'Ngưỡng 30 ngày' },
];

export const ChainProgressPage: React.FC = () => {
  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Bảng tiến độ chuỗi' },
  ]);

  const [data, setData] = useState<ChainProgressBoardData | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const [search, setSearch] = useState<string>('');
  const [stagnantThresholdDays, setStagnantThresholdDays] = useState<string>('10');

  const fetchData = useCallback(async (showLoading = true) => {
    try {
      if (showLoading) setLoading(true);
      setIsRefreshing(true);
      setError(null);

      const result = await getChainProgressBoard({
        stagnantThresholdDays: Number(stagnantThresholdDays),
        search: search.trim() || undefined,
      });
      setData(result);
    } catch (err: any) {
      console.error('Lỗi khi tải bảng tiến độ chuỗi:', err);
      setError(err?.response?.data?.message || 'Không thể tải dữ liệu tiến độ chuỗi.');
    } finally {
      setLoading(false);
      setIsRefreshing(false);
    }
  }, [search, stagnantThresholdDays]);

  useEffect(() => {
    fetchData(true);
  }, [stagnantThresholdDays, fetchData]);

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearch(e.target.value);
  };

  const handleThresholdChange = (val: string | null) => {
    if (val) {
      setStagnantThresholdDays(val);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header trang chuẩn theo thiết kế ứng dụng */}
      <ListPageHeader
        icon={Activity}
        title="Bảng theo dõi tiến độ chuỗi của từng lô"
        description="Tổng hợp tất cả lô đang mở theo 9 giai đoạn của chuỗi sản xuất & lưu thông kèm việc cần làm tiếp theo."
      />

      {/* Card chứa toolbar và bảng dữ liệu */}
      <ListCard>
        <ListToolbar
          left={
            <SearchInput
              value={search}
              onChange={handleSearchChange}
              placeholder="Tìm tên lô, vùng trồng..."
            />
          }
          right={
            <>
              <FilterSelect
                value={stagnantThresholdDays}
                onValueChange={handleThresholdChange}
                options={STAGNANT_THRESHOLD_OPTIONS}
                className="w-auto min-w-[230px]"
              />
              <RefreshButton
                onClick={() => fetchData(false)}
                loading={isRefreshing}
              />
            </>
          }
        />

        {/* Thông báo số liệu tổng quan */}
        {data && (
          <div className="flex items-center gap-4 text-xs text-slate-600 bg-slate-50 p-3 rounded-lg border border-slate-100">
            <div>
              Tổ chức: <span className="font-semibold text-slate-800">{data.organizationName}</span>
            </div>
            <div>
              Tổng số lô đang mở: <span className="font-semibold text-slate-800">{data.totalOpenLots}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span>Số lô tồn đọng:</span>
              <StatusBadge
                label={`${data.stagnantLotsCount} lô`}
                tone={data.stagnantLotsCount > 0 ? 'danger' : 'neutral'}
              />
            </div>
          </div>
        )}

        {/* Hiển thị lỗi nếu có */}
        {error && (
          <div className="p-3 text-xs bg-rose-50 text-rose-700 border border-rose-200 rounded-lg">
            {error}
          </div>
        )}

        {/* Bảng tiến độ dạng đề mục dọc, các lô nằm ngang */}
        {data && <ChainProgressBoard data={data} loading={loading} />}
      </ListCard>
    </div>
  );
};
