import {
  approveProductionLot,
  getProductionLots,
  submitProductionLot,
} from '@/api/productionLotApi';
import { ProductionLotList } from '@/components/production-lot/ProductionLotList';
import { useAuth } from '@/hooks/useAuth';
import { usePermission } from '@/hooks/usePermission';
import { ROLE_ACCESS } from '@/config/roleAccess';
import type { ProductionLot } from '@/types/productionLot';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';

interface ProductionLotBoardProps {
  lots?: ProductionLot[];
  isLoading?: boolean;
  canCreate?: boolean;
  canEdit?: boolean;
  canSubmitForApproval?: boolean;
  canApprove?: boolean;
  canRecordFarmLog?: boolean;
  onRecordProcurement?: (lotId: string) => void;
  /** Ẩn khối tiêu đề của card (trang /production-lots đã có ListPageHeader riêng). */
  hideCardHeader?: boolean;
}

export const ProductionLotBoard = ({
  lots: propLots,
  isLoading: propIsLoading,
  canCreate: propCanCreate,
  canEdit: propCanEdit,
  canSubmitForApproval: propCanSubmitForApproval,
  canApprove: propCanApprove,
  canRecordFarmLog: propCanRecordFarmLog,
  onRecordProcurement,
  hideCardHeader = false,
}: ProductionLotBoardProps) => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [internalLots, setInternalLots] = useState<ProductionLot[]>([]);
  const [internalLoading, setInternalLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Nếu có prop lots thì dùng (chế độ controlled), ngược lại tự gọi API
  const refresh = useCallback(
    async (showTableLoading = false) => {
      if (propLots !== undefined) return;
      if (showTableLoading) setInternalLoading(true);
      setIsRefreshing(true);
      try {
        const data = await getProductionLots();
        setInternalLots(data);
      } catch {
        toast.error('Không thể tải danh sách lô sản xuất');
      } finally {
        setIsRefreshing(false);
        setInternalLoading(false);
      }
    },
    [propLots],
  );

  useEffect(() => {
    if (propLots !== undefined) {
      setInternalLots(propLots);
      setInternalLoading(propIsLoading || false);
      return;
    }

    void refresh(true);
  }, [propLots, propIsLoading, refresh]);

  const isLoading = propLots !== undefined ? propIsLoading || false : internalLoading;
  const lots = propLots !== undefined ? propLots : internalLots;

  const canCreateByRole = usePermission(ROLE_ACCESS.productionLotEdit);
  const canCreate = propCanCreate !== undefined ? propCanCreate : canCreateByRole;
  const canEdit = propCanEdit !== undefined ? propCanEdit : canCreateByRole;
  const canSubmitForApproval = propCanSubmitForApproval !== undefined
    ? propCanSubmitForApproval
    : (user?.roleCode === 'VT-01' || user?.roleCode === 'VT-02');
  const canApprove = propCanApprove !== undefined ? propCanApprove : user?.roleCode === 'VT-02';
  const canRecordFarmLog = propCanRecordFarmLog !== undefined
    ? propCanRecordFarmLog
    : user?.roleCode === 'VT-03';

  const handleSubmitForApproval = async (id: string) => {
    try {
      const updated = await submitProductionLot(id);
      setInternalLots((prev) => prev.map((lot) => (lot.id === id ? { ...lot, ...updated } : lot)));
      toast.success('Đã gửi yêu cầu duyệt lô!');
    } catch (error: any) {
      const message = error.response?.data?.message || 'Không thể gửi duyệt lô.';
      toast.error(message);
    }
  };

  const handleDecideApproval = async (id: string, approved: boolean, reason?: string) => {
    try {
      const result = await approveProductionLot(id, { approved, reason });
      setInternalLots((prev) =>
        prev.map((lot) => (lot.id === id ? { ...lot, ...result } : lot))
      );
      toast.success(approved ? 'Đã duyệt lô sản xuất!' : 'Đã trả lại lô sản xuất kèm lý do.');
    } catch (error: any) {
      const message = error.response?.data?.message || 'Không thể xử lý duyệt lô.';
      toast.error(message);
      throw error;
    }
  };

  return (
    <ProductionLotList
      lots={lots}
      isLoading={isLoading}
      canCreate={canCreate}
      canEdit={canEdit}
      canSubmitForApproval={canSubmitForApproval}
      canApprove={canApprove}
      canRecordFarmLog={canRecordFarmLog}
      onCreate={() => navigate('/production-lots/create')}
      onEdit={(id) => navigate(`/production-lots/${id}/edit`)}
      onSubmitForApproval={handleSubmitForApproval}
      onDecideApproval={handleDecideApproval}
      onRecordFarmLog={(id) =>
        navigate(`/farm-logs/create?productionLotId=${encodeURIComponent(id)}`)
      }
      onRecordProcurement={onRecordProcurement}
      hideCardHeader={hideCardHeader}
      onRefresh={propLots !== undefined ? undefined : () => void refresh()}
      isRefreshing={isRefreshing}
    />
  );
};