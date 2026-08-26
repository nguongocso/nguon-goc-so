import { CreateOrganizationForm } from "@/components/organization/CreateOrganizationForm";
import { HelpButton } from "@/components/help/HelpButton";
import { Building2 } from "lucide-react";

export function CreateOrganizationPage() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Building2 className="size-6 text-emerald-600" />
            Tạo tổ chức mới
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Thiết lập tổ chức và tài khoản quản trị đầu tiên trong hệ thống.
          </p>
        </div>
        <HelpButton screenKey="organization-create" />
      </div>

      <CreateOrganizationForm />
    </div>
  );
}

export default CreateOrganizationPage;