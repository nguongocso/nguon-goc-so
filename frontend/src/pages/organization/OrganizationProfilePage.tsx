import { Building2 } from 'lucide-react';

import { HelpButton } from '@/components/help/HelpButton';
import { OrganizationProfileForm } from '@/components/organization/OrganizationProfileForm';

/**
 * Trang quản lý hồ sơ thông tin của tổ chức hiện tại.
 */
export function OrganizationProfilePage() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Building2 className="size-6 text-emerald-600" />
            Hồ sơ tổ chức
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Thông tin định danh và liên hệ của hợp tác xã, doanh nghiệp.
          </p>
        </div>
        <HelpButton screenKey="organization-profile" />
      </div>
      <OrganizationProfileForm />
    </div>
  );
}

export default OrganizationProfilePage;
