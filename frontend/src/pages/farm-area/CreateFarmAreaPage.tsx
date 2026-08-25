import { CreateFarmAreaForm } from "@/components/farm-area/CreateFarmAreaForm";
import { HelpButton } from "@/components/help/HelpButton";
import { useNavigate } from "react-router-dom";

export const CreateFarmAreaPage: React.FC = () => {
  const navigate = useNavigate();

  const handleSuccess = () => {
    navigate('/farm-areas'); // quay lại danh sách
  };

  return (
    <div className="max-w-4xl space-y-6">
      
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Tạo vùng trồng mới</h1>
        <HelpButton screenKey="farm-area-create" />
      </div>
      <CreateFarmAreaForm
        onSuccess={handleSuccess}
        onCancel={() => navigate('/farm-areas')}
      />
    </div>
  );
};