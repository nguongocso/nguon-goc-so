import React from 'react';
import { CreateCertificationForm } from '@/components/certification/CreateCertificationForm';
import { HelpButton } from '@/components/help/HelpButton';
import { Award } from 'lucide-react';

const CreateCertificationPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Award className="size-6 text-emerald-600" />
            Tạo mới chứng nhận cho tổ chức
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Khai báo chứng nhận dựa trên tiêu chuẩn chất lượng có sẵn của tổ chức.
          </p>
        </div>
        <HelpButton screenKey="certification-create" />
      </div>
      <CreateCertificationForm />
    </div>
  );
};

export default CreateCertificationPage;