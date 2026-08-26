import { CreateMemberForm } from '@/components/organization/CreateMemberForm';
import { HelpButton } from '@/components/help/HelpButton';
import { UserPlus } from 'lucide-react';

export function CreateMemberPage() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <UserPlus className="size-6 text-emerald-600" />
            Thêm thành viên mới
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Nhập thông tin tài khoản để tạo và thêm thành viên vào tổ chức.
          </p>
        </div>
        <HelpButton screenKey="member-create" />
      </div>
      <CreateMemberForm />
    </div>
  );
}

export default CreateMemberPage;