import { CreateCodeRangeForm } from "@/components/admin/CreateCodeRangeForm";
import { HelpButton } from "@/components/help/HelpButton";

const CreateCodeRangePage: React.FC = () => {
  return (
    <div className="space-y-6">
      
      <div className="mb-6 flex justify-end">
        <HelpButton screenKey="admin-code-range-create" />
      </div>
      <CreateCodeRangeForm />
    </div>
  );
}; 

export default CreateCodeRangePage;