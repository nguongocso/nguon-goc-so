import { CreateFarmAreaForm } from "@/components/farm-area/CreateFarmAreaForm";
import { useNavigate } from "react-router-dom";

export const CreateFarmAreaPage: React.FC = () => {
  const navigate = useNavigate();

  const handleSuccess = () => {
    navigate('/farm-areas'); // quay lại danh sách
  };

  return (
    <div className="container mx-auto py-8 max-w-4xl">
      <h1 className="text-2xl font-bold mb-6">Tạo vùng trồng mới</h1>
      <CreateFarmAreaForm
        onSuccess={handleSuccess}
        onCancel={() => navigate('/farm-areas')}
      />
    </div>
  );
};