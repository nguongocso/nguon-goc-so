import React from 'react';
import { CreateInvitationForm } from '@/components/invitation/CreateInvitationForm';
import { MailPlus } from 'lucide-react';

const CreateInvitationPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
          <MailPlus className="size-6 text-emerald-600" />
          Mời thành viên mới
        </h1>
        <p className="text-sm text-muted-foreground mt-1">
          Gửi thư mời qua email để thành viên mới đăng ký và tham gia tổ chức.
        </p>
      </div>
      <CreateInvitationForm />
    </div>
  );
};

export default CreateInvitationPage;