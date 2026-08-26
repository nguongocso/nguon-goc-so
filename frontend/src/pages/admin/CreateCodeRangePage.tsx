import React from 'react';
import { CreateCodeRangeForm } from "@/components/admin/CreateCodeRangeForm";
import { HelpButton } from "@/components/help/HelpButton";
import { Hash } from "lucide-react";

const CreateCodeRangePage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Hash className="size-6 text-emerald-600" />
            Cấp dải mã truy xuất
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Cấp một dải mã mới cho tổ chức để sử dụng trong việc sinh tem truy xuất nguồn gốc.
          </p>
        </div>
        <HelpButton screenKey="admin-code-range-create" />
      </div>
      <CreateCodeRangeForm />
    </div>
  );
}; 

export default CreateCodeRangePage;