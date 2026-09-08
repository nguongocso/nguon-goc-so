import React, { useState } from 'react';
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

const CARDS_PER_PAGE = 4;

export const ChainProgressBoard: React.FC<ChainProgressBoardProps> = ({ data, loading }) => {
  const navigate = useNavigate();
  // Quản lý trang hiện tại cho từng giai đoạn { stageKey: pageIndex }
  const [pageMap, setPageMap] = useState<Record<string, number>>({});

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

  const handlePageChange = (stageKey: string, newPage: number) => {
    setPageMap((prev) => ({ ...prev, [stageKey]: newPage }));
  };

  const handleCardClick = (item: ChainProgressItem) => {
    if (item.targetScreen) {
      navigate(item.targetScreen);
    }
  };

  return (
    <div className="space-y-6">
      {data.stages.map((group: ChainProgressStageGroup, index: number) => {
        const totalItems = group.items.length;
        const totalPages = Math.ceil(totalItems / CARDS_PER_PAGE) || 1;
        const currentPage = pageMap[group.stage] || 1;

        const startIndex = (currentPage - 1) * CARDS_PER_PAGE;
        const visibleItems = group.items.slice(startIndex, startIndex + CARDS_PER_PAGE);

        const tone = STAGE_TONES[group.stage] || 'neutral';

        return (
          <div
            key={group.stage}
            className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm space-y-4"
          >
            {/* Header giai đoạn (nằm ngang trên từng đề mục) */}
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-3">
                <span className="text-xs font-bold text-slate-400">0{index + 1}</span>
                <h3 className="font-semibold text-base text-slate-900">
                  {group.stageName}
                </h3>
                <StatusBadge label={`${group.count} lô`} tone={tone} />
              </div>

              {/* Phân trang ngắn gọn cho từng đề mục nếu danh sách dài */}
              {totalPages > 1 && (
                <div className="flex items-center gap-2 text-xs text-slate-600">
                  <span>
                    Trang {currentPage} / {totalPages}
                  </span>
                  <div className="flex items-center gap-1">
                    <Button
                      type="button"
                      variant="outline"
                      size="icon"
                      className="h-7 w-7"
                      disabled={currentPage <= 1}
                      onClick={() => handlePageChange(group.stage, currentPage - 1)}
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="icon"
                      className="h-7 w-7"
                      disabled={currentPage >= totalPages}
                      onClick={() => handlePageChange(group.stage, currentPage + 1)}
                    >
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              )}
            </div>

            {/* Danh sách các lô xếp nằm ngang (Horizontal row / Grid) */}
            {group.items.length === 0 ? (
              <div className="py-4 text-center text-xs text-slate-400 italic bg-slate-50/50 rounded border border-dashed border-slate-200">
                Chưa có lô nào ở giai đoạn này
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                {visibleItems.map((item: ChainProgressItem) => (
                  <div
                    key={item.id}
                    onClick={() => handleCardClick(item)}
                    className={`flex flex-col justify-between border rounded-lg p-3.5 bg-white transition-all cursor-pointer hover:border-emerald-500 hover:shadow-md ${
                      item.isStagnant
                        ? 'border-rose-300 bg-rose-50/20'
                        : 'border-slate-200'
                    }`}
                  >
                    <div className="space-y-2">
                      {/* Tên lô */}
                      <div className="font-medium text-sm text-slate-900 line-clamp-1">
                        {item.name}
                      </div>

                      {/* Vùng trồng & Loại nông sản */}
                      <div className="text-xs text-slate-600 space-y-0.5">
                        <div className="truncate">Vùng trồng: <span className="font-medium text-slate-800">{item.farmAreaName}</span></div>
                        <div className="truncate">Nông sản: <span className="font-medium text-slate-800">{item.productCategoryName}</span></div>
                      </div>

                      {/* Thời gian ở giai đoạn & Cảnh báo tồn đọng */}
                      <div className="flex items-center gap-2 pt-1">
                        <span className="text-[11px] text-slate-500">
                          Thời gian: {item.daysInStage} ngày
                        </span>
                        {item.isStagnant && (
                          <StatusBadge
                            label={`Tồn đọng (${item.daysInStage} ngày)`}
                            tone="danger"
                          />
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
                        variant={item.isStagnant ? 'danger' : 'outline'}
                        size="sm"
                        className="w-full text-xs justify-start truncate h-8"
                      >
                        {item.nextActionRequired}
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};
