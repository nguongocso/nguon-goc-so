import { CreateFarmAreaForm } from "@/components/farm-area/CreateFarmAreaForm";
import { HelpButton } from "@/components/help/HelpButton";
import { useNavigate } from "react-router-dom";

export const CreateFarmAreaPage: React.FC = () => {
  const navigate = useNavigate();

  const handleSuccess = () => {
    navigate('/farm-areas'); // quay lại danh sách
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Tạo vùng trồng mới</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Khai báo thông tin vị trí bản đồ, diện tích và loại cây trồng của vùng canh tác.
          </p>
        </div>
        <HelpButton screenKey="farm-area-create" />
      </div>
      <CreateFarmAreaForm
        onSuccess={handleSuccess}
        onCancel={() => navigate('/farm-areas')}
      />
    </div>
  );
};