import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  FileText,
  PlusCircle,
  Pencil,
  Trash2,
} from 'lucide-react';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
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

  console.log('[ProfileTemplateListPage] Render:', {
    username: user?.username,
    roleCode: user?.roleCode,
    orgId,
    templatesCount: safeTemplates.length,
    loading,
  });

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
      {/* Header trang */}
      <ListPageHeader
        icon={FileText}
        iconBoxClassName="bg-primary/10 text-primary"
        title="Mẫu hồ sơ truy xuất theo đối tác"
        description="Cấu hình danh mục các trường dữ liệu đưa vào hồ sơ kết xuất theo yêu cầu của từng đối tác hoặc siêu thị thu mua."
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

      {/* Danh sách mẫu hồ sơ */}
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
            <TableRow key={template.id} className="hover:bg-table-hover transition-colors">
              <TableCell className="text-center font-medium text-muted-foreground text-sm">
                {index + 1}
              </TableCell>

              <TableCell className="font-medium">
                <div className="flex items-center gap-2">
                  <span className="text-foreground">{template.name}</span>
                  {(template.isDefault || template.default) && (
                    <Badge variant="success" className="text-[11px] px-2 py-0.5">
                      Mặc định
                    </Badge>
                  )}
                </div>
              </TableCell>

              <TableCell>
                {template.partnerName ? (
                  <span className="text-sm text-foreground">{template.partnerName}</span>
                ) : (
                  <span className="text-xs text-muted-foreground italic">Dùng chung (Nhiều đối tác)</span>
                )}
              </TableCell>

              <TableCell>
                <Badge variant="outline" className="text-xs">
                  {template.fields?.length || 0} trường
                </Badge>
              </TableCell>

              <TableCell className="text-sm text-muted-foreground">
                {template.createdAt
                  ? new Date(template.createdAt).toLocaleDateString('vi-VN')
                  : '—'}
              </TableCell>

              <TableCell className="text-right">
                <div className="flex items-center justify-end gap-1">
                  {isManager && (
                    <>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() =>
                          navigate(`/export/profile-templates/${template.id}/edit`)
                        }
                        title="Chỉnh sửa mẫu hồ sơ"
                        aria-label={`Chỉnh sửa ${template.name}`}
                      >
                        <Pencil className="size-4 text-muted-foreground hover:text-foreground" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => setDeleteTarget(template)}
                        disabled={Boolean(template.isDefault || template.default)}
                        title={
                          (template.isDefault || template.default)
                            ? 'Không thể xóa mẫu mặc định'
                            : 'Xóa mẫu hồ sơ'
                        }
                        aria-label={`Xóa ${template.name}`}
                      >
                        <Trash2
                          className={`size-4 ${
                            (template.isDefault || template.default)
                              ? 'text-disabled cursor-not-allowed'
                              : 'text-destructive hover:opacity-80'
                          }`}
                        />
                      </Button>
                    </>
                  )}
                </div>
              </TableCell>
            </TableRow>
          ))}
        />
      </ListCard>

      {/* Dialog xác nhận xóa */}
      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title="Xác nhận xóa mẫu hồ sơ"
        description={`Bạn có chắc chắn muốn xóa mẫu hồ sơ "${deleteTarget?.name}"? Các lần xuất dữ liệu sau này sẽ không thể sử dụng mẫu này.`}
        confirmLabel={deleting ? 'Đang xóa...' : 'Xóa mẫu'}
        variant="destructive"
        onConfirm={handleDeleteConfirm}
        loading={deleting}
      />
    </div>
  );
};

export default ProfileTemplateListPage;
