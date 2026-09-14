import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  FileText,
  PlusCircle,
  Pencil,
  Trash2,
  CheckCircle2,
  Building,
  Calendar,
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
    { label: 'Xuất dữ liệu mở', href: '/export/open-data' },
    { label: 'Mẫu hồ sơ truy xuất' },
  ]);

  const {
    templates,
    loading,
    refresh,
    deleteTemplate,
  } = useProfileTemplates(orgId);

  const [search, setSearch] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<ProfileTemplate | null>(null);
  const [deleting, setDeleting] = useState(false);

  // Lọc client side theo tên mẫu hoặc tên đối tác
  const filteredTemplates = useMemo(() => {
    if (!search.trim()) return templates;
    const q = search.toLowerCase().trim();
    return templates.filter(
      (t) =>
        t.name.toLowerCase().includes(q) ||
        (t.partnerName && t.partnerName.toLowerCase().includes(q))
    );
  }, [templates, search]);

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    if (deleteTarget.isDefault) {
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
          colSpan={5}
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
              <TableHead className="font-medium text-label w-[30%]">Tên mẫu hồ sơ</TableHead>
              <TableHead className="font-medium text-label w-[25%]">Đối tác áp dụng</TableHead>
              <TableHead className="font-medium text-label w-[15%]">Số trường chọn</TableHead>
              <TableHead className="font-medium text-label w-[15%]">Trạng thái</TableHead>
              <TableHead className="font-medium text-label text-right w-[15%]">Thao tác</TableHead>
            </>
          }
          body={filteredTemplates.map((template) => (
            <TableRow key={template.id} className="hover:bg-table-hover transition-colors">
              <TableCell className="font-medium">
                <div className="flex items-center gap-2">
                  <span className="text-foreground">{template.name}</span>
                  {template.isDefault && (
                    <Badge variant="success" className="text-[11px] gap-1 px-2 py-0.5">
                      <CheckCircle2 className="size-3" />
                      Mặc định
                    </Badge>
                  )}
                </div>
                {template.createdAt && (
                  <div className="flex items-center gap-1 text-xs text-muted-foreground mt-0.5">
                    <Calendar className="size-3" />
                    <span>Tạo: {new Date(template.createdAt).toLocaleDateString('vi-VN')}</span>
                  </div>
                )}
              </TableCell>

              <TableCell>
                {template.partnerName ? (
                  <div className="flex items-center gap-1.5 text-sm text-foreground">
                    <Building className="size-3.5 text-muted-foreground shrink-0" />
                    <span>{template.partnerName}</span>
                  </div>
                ) : (
                  <span className="text-xs text-muted-foreground italic">Dùng chung (Nhiều đối tác)</span>
                )}
              </TableCell>

              <TableCell>
                <Badge variant="outline" className="text-xs">
                  {template.fields?.length || 0} trường
                </Badge>
              </TableCell>

              <TableCell>
                {template.isDefault ? (
                  <Badge variant="default" className="text-xs">
                    Áp dụng mặc định
                  </Badge>
                ) : (
                  <Badge variant="secondary" className="text-xs">
                    Theo đối tác
                  </Badge>
                )}
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
                        disabled={template.isDefault}
                        title={
                          template.isDefault
                            ? 'Không thể xóa mẫu mặc định'
                            : 'Xóa mẫu hồ sơ'
                        }
                        aria-label={`Xóa ${template.name}`}
                      >
                        <Trash2
                          className={`size-4 ${
                            template.isDefault
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
