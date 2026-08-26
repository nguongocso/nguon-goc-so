import { CreateMemberForm } from '@/components/organization/CreateMemberForm';
import { HelpButton } from '@/components/help/HelpButton';

export function CreateMemberPage() {
  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <HelpButton screenKey="member-create" />
      </div>
      <CreateMemberForm />
    </div>
  );
}

export default CreateMemberPage;