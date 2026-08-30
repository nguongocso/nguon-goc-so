import React, { useEffect, useState } from 'react';
import { SlidersHorizontal, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { toast } from 'sonner';
import { estimateImpact, getAllThresholds } from '@/api/anomalyThresholdApi';
import { GlobalThresholdCard } from '@/components/admin/anomaly-threshold/GlobalThresholdCard';
import { CategoryOverridesTable } from '@/components/admin/anomaly-threshold/CategoryOverridesTable';
import { CategoryOverrideDialog } from '@/components/admin/anomaly-threshold/CategoryOverrideDialog';
import { ImpactEstimationCard } from '@/components/admin/anomaly-threshold/ImpactEstimationCard';
import type {
  AllThresholdsResponse,
  AnomalyThresholdConfig,
  ImpactEstimationRequest,
  ImpactEstimationResult,
  UpdateGlobalThresholdRequest,
} from '@/types/anomalyThreshold';

export const AnomalyThresholdPage: React.FC = () => {
  useSetBreadcrumb([
    { label: 'Cấu hình ngưỡng quét bất thường', href: '/admin/anomaly-thresholds' },
  ]);

  const [data, setData] = useState<AllThresholdsResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [dialogOpen, setDialogOpen] = useState<boolean>(false);
  const [editingItem, setEditingItem] = useState<AnomalyThresholdConfig | null>(null);
  const [globalImpactResult, setGlobalImpactResult] = useState<ImpactEstimationResult | null>(null);
  const [estimatingGlobal, setEstimatingGlobal] = useState<boolean>(false);

  const loadData = async () => {
    try {
      setLoading(true);
      const res = await getAllThresholds();
      setData(res);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải cấu hình ngưỡng quét bất thường');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleOpenAddDialog = () => {
    setEditingItem(null);
    setDialogOpen(true);
  };

  const handleOpenEditDialog = (item: AnomalyThresholdConfig) => {
    setEditingItem(item);
    setDialogOpen(true);
  };

  const handleEstimateGlobalImpact = async (draft: UpdateGlobalThresholdRequest) => {
    try {
      setEstimatingGlobal(true);
      const payload: ImpactEstimationRequest = {
        productCategoryId: null,
        maxScansPerHour: draft.maxScansPerHour,
        maxScansPerDay: draft.maxScansPerDay,
        maxDistanceKmPer30Min: draft.maxDistanceKmPer30Min,
        minTimeBetweenScansMinutes: draft.minTimeBetweenScansMinutes,
        activationAgeDays: draft.activationAgeDays,
      };
      const result = await estimateImpact(payload);
      setGlobalImpactResult(result);
      toast.info('Đã hoàn thành ước lượng tác động (30 ngày)');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể ước lượng tác động');
    } finally {
      setEstimatingGlobal(false);
    }
  };

  return (
    <div className="space-y-6 p-4 md:p-6 max-w-7xl mx-auto">
      <ListPageHeader
        icon={SlidersHorizontal}
        title="Cấu hình ngưỡng quét bất thường"
        description="Điều chỉnh độ nhạy của bộ máy phát hiện quét bất thường cho toàn hệ thống và theo từng loại nông sản"
        actions={
          <Button
            variant="outline"
            size="sm"
            onClick={loadData}
            disabled={loading}
            className="flex items-center gap-1.5"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        }
      />

      {/* Global Threshold Configuration Card */}
      <GlobalThresholdCard
        initialData={data?.global || null}
        onSuccess={(updated) => {
          setData((prev) => (prev ? { ...prev, global: updated } : null));
        }}
        onEstimateImpact={handleEstimateGlobalImpact}
        estimating={estimatingGlobal}
      />

      {/* Impact Estimation Preview Section */}
      <ImpactEstimationCard result={globalImpactResult} loading={estimatingGlobal} />

      {/* Per-Category Overrides Section */}
      <CategoryOverridesTable
        overrides={data?.categoryOverrides || []}
        onAddClick={handleOpenAddDialog}
        onEditClick={handleOpenEditDialog}
        onRefresh={loadData}
        loading={loading}
      />

      {/* Add / Edit Category Override Dialog */}
      <CategoryOverrideDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        initialData={editingItem}
        onSuccess={loadData}
        defaultGlobalValues={data?.global || null}
      />
    </div>
  );
};

export default AnomalyThresholdPage;
