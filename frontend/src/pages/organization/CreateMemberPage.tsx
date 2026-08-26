import { CreateMemberForm } from '@/components/organization/CreateMemberForm';
import { HelpButton } from '@/components/help/HelpButton';

export function CreateMemberPage() {
  return (
    <div className="max-w-2xl mx-auto space-y-4">
      <div className="flex justify-end">
        <HelpButton screenKey="member-create" />
      </div>
      <CreateMemberForm />
    </div>
  );
}

export default CreateMemberPage;