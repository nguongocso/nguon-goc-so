import React from 'react';
import { CreateInvitationForm } from '@/components/invitation/CreateInvitationForm';
import { HelpButton } from '@/components/help/HelpButton';

const CreateInvitationPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <HelpButton screenKey="invitation-create" />
      </div>
      <CreateInvitationForm />
    </div>
  );
};

export default CreateInvitationPage;