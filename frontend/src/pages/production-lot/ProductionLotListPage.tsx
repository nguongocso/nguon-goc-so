import { FileUp, Plus, Sprout } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { ProductionLotBoard } from '@/components/production-lot/ProductionLotBoard';
import { HelpButton } from '@/components/help/HelpButton';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { useAuth } from '@/hooks/useAuth';
import { usePermission } from '@/hooks/usePermission';
import { ROLE_ACCESS } from '@/config/roleAccess';

const ProductionLotListPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const canCreate = usePermission(ROLE_ACCESS.productionLotEdit);
  const canImport = user?.roleCode === 'VT-02'; // quyền nhập lô hàng loạt

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Lô sản xuất' },
  ]);

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={Sprout}
        title="Lô sản xuất"
        description="Quản lý các lô sản xuất thuộc phạm vi tổ chức của bạn."
        actions={
          <>
            <HelpButton screenKey="production-lot-list" />
            {canCreate && (
              <Button
                type="button"
                variant="create"
                onClick={() => navigate('/production-lots/create')}
              >
                <Plus className="size-4" />
                Tạo lô sản xuất
              </Button>
            )}
            {canImport && (
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate('/production-lots/import')}
              >
                <FileUp className="size-4" />
                Nhập lô hàng loạt
              </Button>
            )}
          </>
        }
      />

      <ProductionLotBoard hideCardHeader />
    </div>
  );
};

export default ProductionLotListPage;