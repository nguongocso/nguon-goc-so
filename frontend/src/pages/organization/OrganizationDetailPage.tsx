import { HelpButton } from '@/components/help/HelpButton';
import { OrganizationDetail } from '@/components/organization/OrganizationDetail';

/**
 * Trang chi tiết thông tin tổ chức.
 */
export function OrganizationDetailPage() {
  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <HelpButton screenKey="organization-detail" />
      </div>
      <OrganizationDetail />
    </div>
  );
}

export default OrganizationDetailPage;
