import { OrganizationDetail } from "@/components/organization/OrganizationDetail";
import { HelpButton } from "@/components/help/HelpButton";

const OrganizationDetailPage = () => {
  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <HelpButton screenKey="organization-detail" />
      </div>
      <OrganizationDetail />
    </div>
  );
};

export default OrganizationDetailPage;