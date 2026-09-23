import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { FileText, PlusCircle } from 'lucide-react';
import { TableHead } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListCard } from '@/components/common/ListCard';
import { ListToolbar } from '@/components/common/ListToolbar';
import { SearchInput } from '@/components/common/SearchInput';
import { RefreshButton } from '@/components/common/RefreshButton';
import { DataTableShell } from '@/components/common/DataTableShell';
import { ConfirmDialog } from '@/components/common/ConfirmDialog';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { useAuth } from '@/hooks/useAuth';
import { usePermission } from '@/hooks/usePermission';
import { useProfileTemplates } from '@/hooks/useProfileTemplates';
import type { ProfileTemplate } from '@/types/profileTemplate';
import { toast } from 'sonner';
import { ProfileTemplateTableRow } from './ProfileTemplateTableRow';

/**
 * Trang danh sách các mẫu hồ sơ truy xuất nguồn gốc theo đối tác của tổ chức HTX.
 * Cho phép tìm kiếm, xem chi tiết, điều hướng tạo mới hoặc xóa mẫu.
 */
export const ProfileTemplateListPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const orgId = user?.organizationId;
  const isManager = usePermission(['VT-02']);

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Mẫu hồ sơ truy xuất' },
  ]);

  const {
    templates,
    loading,
    refresh,
    deleteTemplate,
  } = useProfileTemplates(orgId);

  const safeTemplates = useMemo(() => (Array.isArray(templates) ? templates : []), [templates]);

  const [search, setSearch] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<ProfileTemplate | null>(null);
  const [deleting, setDeleting] = useState(false);

  // Lọc client side theo tên mẫu hoặc tên đối tác
  const filteredTemplates = useMemo(() => {
    if (!search.trim()) return safeTemplates;
    const q = search.toLowerCase().trim();
    return safeTemplates.filter(
      (t) =>
        (t.name && t.name.toLowerCase().includes(q)) ||
        (t.partnerName && t.partnerName.toLowerCase().includes(q))
    );
  }, [safeTemplates, search]);

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    if (deleteTarget.isDefault || deleteTarget.default) {
      toast.error('Không thể xóa mẫu hồ sơ đang được đặt làm mặc định');
      setDeleteTarget(null);
      return;
    }

    setDeleting(true);
    try {
      await deleteTemplate(deleteTarget.id);
      setDeleteTarget(null);
    } catch {
      // Error handled by hook
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={FileText}
        iconBoxClassName="bg-primary/10 text-primary"
        title="Mẫu hồ sơ truy xuất theo đối tác"
        description={
          'Cấu hình danh mục các trường dữ liệu đưa vào hồ sơ kết xuất theo ' +
          'yêu cầu của từng đối tác hoặc siêu thị thu mua.'
        }
        actions={
          isManager && (
            <Button
              variant="create"
              onClick={() => navigate('/export/profile-templates/new')}
              className="gap-2"
            >
              <PlusCircle className="size-4" />
              <span>Tạo mẫu mới</span>
            </Button>
          )
        }
      />

      <ListCard>
        <ListToolbar
          left={
            <div className="w-full sm:w-80">
              <SearchInput
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Tìm theo tên mẫu hoặc đối tác..."
              />
            </div>
          }
          right={<RefreshButton onClick={refresh} loading={loading} />}
        />

        <DataTableShell
          loading={loading}
          empty={filteredTemplates.length === 0}
          colSpan={6}
          loadingMessage="Đang tải danh sách mẫu hồ sơ..."
          emptyMessage={
            search
              ? 'Không tìm thấy mẫu hồ sơ phù hợp với từ khóa tìm kiếm.'
              : 'Chưa có mẫu hồ sơ nào. Hãy tạo mẫu đầu tiên để tùy biến trường dữ liệu.'
          }
          emptyAction={
            isManager && !search ? (
              <Button
                variant="create"
                onClick={() => navigate('/export/profile-templates/new')}
                className="gap-2 mt-2"
              >
                <PlusCircle className="size-4" />
                <span>Tạo mẫu mới</span>
              </Button>
            ) : undefined
          }
          header={
            <>
              <TableHead className="font-medium text-label w-[60px] text-center">STT</TableHead>
              <TableHead className="font-medium text-label w-[30%]">Tên mẫu hồ sơ</TableHead>
              <TableHead className="font-medium text-label w-[25%]">Đối tác áp dụng</TableHead>
              <TableHead className="font-medium text-label w-[15%]">Số trường chọn</TableHead>
              <TableHead className="font-medium text-label w-[15%]">Thời gian tạo</TableHead>
              <TableHead className="font-medium text-label text-right w-[100px]">Thao tác</TableHead>
            </>
          }
          body={filteredTemplates.map((template, index) => (
            <ProfileTemplateTableRow
              key={template.id}
              template={template}
              index={index}
              isManager={isManager}
              onEdit={(id) => navigate(`/export/profile-templates/${id}/edit`)}
              onDelete={(target) => setDeleteTarget(target)}
            />
          ))}
        />
      </ListCard>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title="Xác nhận xóa mẫu hồ sơ"
        description={
          `Bạn có chắc chắn muốn xóa mẫu hồ sơ "${deleteTarget?.name}"? ` +
          'Các lần xuất dữ liệu sau này sẽ không thể sử dụng mẫu này.'
        }
        confirmLabel={deleting ? 'Đang xóa...' : 'Xóa mẫu'}
        variant="destructive"
        onConfirm={handleDeleteConfirm}
        loading={deleting}
      />
    </div>
  );
};

export default ProfileTemplateListPage;
