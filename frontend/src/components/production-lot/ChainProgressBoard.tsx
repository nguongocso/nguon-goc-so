import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ChainProgressBoardData, ChainProgressItem, ChainProgressStageGroup } from '@/types/productionLot';
import { StatusBadge, type StatusTone } from '@/components/common/StatusBadge';
import { Button } from '@/components/ui/button';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface ChainProgressBoardProps {
  data: ChainProgressBoardData;
  loading?: boolean;
}

const STAGE_TONES: Record<string, StatusTone> = {
  DRAFT: 'neutral',
  PENDING: 'warning',
  APPROVED: 'info',
  HARVESTED: 'success',
  PREPROCESSED: 'info',
  WAITING_TEST_RESULT: 'warning',
  PACKAGED: 'info',
  TAG_ACTIVATED: 'success',
  IN_CIRCULATION: 'success',
};

const STAGE_NAME_MAP: Record<string, string> = {
  DRAFT: 'Nháp',
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  HARVESTED: 'Đã thu hoạch',
  PREPROCESSED: 'Đã sơ chế',
  WAITING_TEST_RESULT: 'Chờ KQ kiểm nghiệm',
  PACKAGED: 'Đã đóng gói',
  TAG_ACTIVATED: 'Kích hoạt tem',
  IN_CIRCULATION: 'Đang lưu thông',
};

const ITEMS_PER_PAGE = 8;

export const ChainProgressBoard: React.FC<ChainProgressBoardProps> = ({ data, loading }) => {
  const navigate = useNavigate();

  // Tab đang được chọn: 'ALL' | 'STAGNANT' | StageKey (VD: 'HARVESTED')
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [currentPage, setCurrentPage] = useState<number>(1);

  // Đổi tab sẽ reset về trang 1
  const handleTabChange = (tabKey: string) => {
    setActiveTab(tabKey);
    setCurrentPage(1);
  };

  // Gom toàn bộ danh sách lô từ các giai đoạn
  const allItemsWithStage = useMemo(() => {
    if (!data?.stages) return [];
    const items: (ChainProgressItem & { stageKey: string; stageName: string })[] = [];
    data.stages.forEach((group: ChainProgressStageGroup) => {
      group.items.forEach((item: ChainProgressItem) => {
        items.push({
          ...item,
          stageKey: group.stage,
          stageName: group.stageName,
        });
      });
    });
    return items;
  }, [data]);

  // Lấy các lô thuộc Tab đang chọn
  const filteredItems = useMemo(() => {
    if (activeTab === 'ALL') {
      return allItemsWithStage;
    }
    if (activeTab === 'STAGNANT') {
      return allItemsWithStage.filter((item) => item.isStagnant);
    }
    return allItemsWithStage.filter((item) => item.stageKey === activeTab);
  }, [activeTab, allItemsWithStage]);

  if (loading) {
    return (
      <div className="p-8 text-center text-slate-500 text-sm">
        Đang tải dữ liệu tiến độ chuỗi...
      </div>
    );
  }

  if (!data || !data.stages || data.stages.length === 0) {
    return (
      <div className="p-8 text-center text-slate-500 text-sm border rounded-lg bg-white shadow-sm">
        Không tìm thấy dữ liệu tiến độ chuỗi sản xuất.
      </div>
    );
  }

  const totalItems = filteredItems.length;
  const totalPages = Math.ceil(totalItems / ITEMS_PER_PAGE) || 1;
  const safeCurrentPage = Math.min(currentPage, totalPages);

  const startIndex = (safeCurrentPage - 1) * ITEMS_PER_PAGE;
  const visibleItems = filteredItems.slice(startIndex, startIndex + ITEMS_PER_PAGE);

  const handleCardClick = (item: ChainProgressItem) => {
    if (item.targetScreen) {
      navigate(item.targetScreen);
    }
  };

  return (
    <div className="space-y-5">
      {/* 1. Dải Tabs Navigation chính: Tất cả | Tồn đọng + 9 Giai đoạn */}
      <div className="bg-slate-50 border border-slate-200 rounded-lg p-2.5 space-y-2">
        <div className="text-xs font-semibold text-slate-500 px-1">
          Lọc theo giai đoạn & trạng thái:
        </div>

        <div className="flex flex-wrap gap-1.5 items-center">
          {/* Tab Tất cả */}
          <button
            type="button"
            onClick={() => handleTabChange('ALL')}
            className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all flex items-center gap-1.5 ${
              activeTab === 'ALL'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'bg-white text-slate-700 hover:bg-slate-100 border border-slate-200'
            }`}
          >
            <span>Tất cả lô đang mở</span>
            <span
              className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
                activeTab === 'ALL'
                  ? 'bg-emerald-700 text-white'
                  : 'bg-slate-100 text-slate-700'
              }`}
            >
              {data.totalOpenLots}
            </span>
          </button>

          {/* Tab Lô tồn đọng (Cần xử lý ngay) */}
          <button
            type="button"
            onClick={() => handleTabChange('STAGNANT')}
            className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all flex items-center gap-1.5 ${
              activeTab === 'STAGNANT'
                ? 'bg-rose-600 text-white shadow-sm'
                : data.stagnantLotsCount > 0
                ? 'bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200 font-semibold'
                : 'bg-white text-slate-700 hover:bg-slate-100 border border-slate-200'
            }`}
          >
            <span>Cần xử lý / Tồn đọng</span>
            <span
              className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
                activeTab === 'STAGNANT'
                  ? 'bg-rose-700 text-white'
                  : data.stagnantLotsCount > 0
                  ? 'bg-rose-200 text-rose-800'
                  : 'bg-slate-100 text-slate-700'
              }`}
            >
              {data.stagnantLotsCount}
            </span>
          </button>

          <div className="h-4 w-px bg-slate-300 mx-1 hidden sm:block" />

          {/* 9 Tab giai đoạn quy chuẩn */}
          {data.stages.map((group: ChainProgressStageGroup) => {
            const isSelected = activeTab === group.stage;
            const stagnantInStage = group.items.filter((i) => i.isStagnant).length;
            const displayName = STAGE_NAME_MAP[group.stage] || group.stageName;

            return (
              <button
                key={group.stage}
                type="button"
                onClick={() => handleTabChange(group.stage)}
                className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-all flex items-center gap-1.5 ${
                  isSelected
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : 'bg-white text-slate-700 hover:bg-slate-100 border border-slate-200'
                }`}
              >
                <span>{displayName}</span>
                <span
                  className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
                    isSelected
                      ? 'bg-emerald-700 text-white'
                      : 'bg-slate-100 text-slate-700'
                  }`}
                >
                  {group.count}
                </span>

                {/* Badge đốm đỏ nếu trong stage này có lô tồn đọng */}
                {!isSelected && stagnantInStage > 0 && (
                  <span
                    className="w-2 h-2 rounded-full bg-rose-500 inline-block"
                    title={`Có ${stagnantInStage} lô tồn đọng trong giai đoạn này`}
                  />
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* 2. Tiêu đề Khung hiển thị & Phân trang */}
      <div className="flex items-center justify-between border-b border-slate-200 pb-3 pt-1">
        <div className="flex items-center gap-2">
          <h3 className="font-semibold text-base text-slate-900">
            {activeTab === 'ALL' && 'Tất cả các lô sản xuất đang mở'}
            {activeTab === 'STAGNANT' && 'Danh sách các lô cần xử lý ngay / Tồn đọng'}
            {activeTab !== 'ALL' &&
              activeTab !== 'STAGNANT' &&
              `Giai đoạn: ${
                data.stages.find((s) => s.stage === activeTab)?.stageName || activeTab
              }`}
          </h3>
          <StatusBadge
            label={`${filteredItems.length} lô`}
            tone={activeTab === 'STAGNANT' && filteredItems.length > 0 ? 'danger' : 'info'}
          />
        </div>

        {/* Control phân trang nếu có nhiều lô */}
        {totalPages > 1 && (
          <div className="flex items-center gap-3 text-xs text-slate-600">
            <span>
              Trang <strong className="text-slate-900">{safeCurrentPage}</strong> / {totalPages}
            </span>
            <div className="flex items-center gap-1">
              <Button
                type="button"
                variant="outline"
                size="icon"
                className="h-7 w-7"
                disabled={safeCurrentPage <= 1}
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button
                type="button"
                variant="outline"
                size="icon"
                className="h-7 w-7"
                disabled={safeCurrentPage >= totalPages}
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* 3. Danh sách thẻ lô hiển thị ở dạng Grid 4 cột rộng rãi */}
      {filteredItems.length === 0 ? (
        <div className="py-12 text-center text-sm text-slate-500 bg-slate-50/60 rounded-lg border border-dashed border-slate-200 space-y-1">
          <p className="font-medium text-slate-700">Không có lô nào ở danh mục này</p>
          <p className="text-xs text-slate-400">
            Vui lòng chọn tab giai đoạn khác hoặc thay đổi bộ lọc tìm kiếm.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {visibleItems.map((item) => {
            const tone = STAGE_TONES[item.stageKey] || 'neutral';
            return (
              <div
                key={item.id}
                onClick={() => handleCardClick(item)}
                className={`flex flex-col justify-between border rounded-lg p-3.5 bg-white transition-all cursor-pointer hover:border-emerald-500 hover:shadow-md ${
                  item.isStagnant
                    ? 'border-rose-300 bg-rose-50/20'
                    : 'border-slate-200'
                }`}
              >
                <div className="space-y-2.5">
                  {/* Badge Giai đoạn hiện tại (Hiển thị khi ở Tab Tất cả hoặc Tồn đọng) */}
                  {(activeTab === 'ALL' || activeTab === 'STAGNANT') && (
                    <div className="flex items-center justify-between gap-2 border-b border-slate-100 pb-2">
                      <span className="text-[11px] font-semibold text-slate-500 truncate">
                        Giai đoạn:
                      </span>
                      <StatusBadge label={item.stageName} tone={tone} />
                    </div>
                  )}

                  {/* Tên lô */}
                  <div className="font-semibold text-sm text-slate-900 line-clamp-1">
                    {item.name}
                  </div>

                  {/* Vùng trồng & Loại nông sản */}
                  <div className="text-xs text-slate-600 space-y-1">
                    <div className="truncate">
                      Vùng trồng: <span className="font-medium text-slate-800">{item.farmAreaName}</span>
                    </div>
                    <div className="truncate">
                      Nông sản: <span className="font-medium text-slate-800">{item.productCategoryName}</span>
                    </div>
                  </div>

                  {/* Thời gian ở giai đoạn & Cảnh báo tồn đọng */}
                  <div className="flex items-center justify-between gap-2 pt-1 border-t border-slate-100/80">
                    <span className="text-[11px] text-slate-500">
                      Thời gian lưu: <strong className="text-slate-700">{item.daysInStage} ngày</strong>
                    </span>
                    {item.isStagnant && (
                      <StatusBadge label="Tồn đọng" tone="danger" />
                    )}
                  </div>
                </div>

                {/* Việc cần làm tiếp theo */}
                <div className="mt-3 pt-2.5 border-t border-slate-100 flex flex-col gap-1.5">
                  <span className="text-[11px] text-slate-500 font-medium">
                    Việc cần làm tiếp theo:
                  </span>
                  <Button
                    type="button"
                    variant={item.isStagnant ? 'destructive' : 'outline'}
                    size="sm"
                    className="w-full text-xs justify-start min-h-8 h-auto py-1 px-2.5 leading-snug whitespace-normal text-left"
                    title={item.nextActionRequired}
                  >
                    <span className="w-full text-left line-clamp-2">{item.nextActionRequired}</span>
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

