import React from 'react';
import { OrganizationProfileForm } from "@/components/organization/OrganizationProfileForm";
import { HelpButton } from "@/components/help/HelpButton";

const OrganizationProfilePage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <HelpButton screenKey="organization-profile" />
      </div>
      <OrganizationProfileForm />
    </div>
  );
};

export default OrganizationProfilePage;