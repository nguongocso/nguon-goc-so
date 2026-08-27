import React from 'react';
import { CreateInvitationForm } from '@/components/invitation/CreateInvitationForm';
import { HelpButton } from '@/components/help/HelpButton';
import { MailPlus } from 'lucide-react';

const CreateInvitationPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <MailPlus className="size-6 text-emerald-600" />
            Mời thành viên mới
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Gửi thư mời qua email để thành viên mới đăng ký và tham gia tổ chức.
          </p>
        </div>
        <HelpButton screenKey="invitation-create" />
      </div>
      <CreateInvitationForm />
    </div>
  );
};

export default CreateInvitationPage;