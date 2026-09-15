import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { EditFarmAreaForm } from "@/components/farm-area/EditFarmAreaForm";
import { FarmAreaBoundaryEditor } from "@/components/farm-area/FarmAreaBoundaryEditor";
import { HelpButton } from "@/components/help/HelpButton";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import { getFarmAreaById } from "@/api/farmAreaApi";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import type { FarmArea } from "@/types/farmArea";
import { toast } from "sonner";
import { FileText, MapPinned, RefreshCw } from "lucide-react";

export const EditFarmAreaPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [farmArea, setFarmArea] = useState<FarmArea | null>(null);
  const [loading, setLoading] = useState(true);

  useSetBreadcrumb(
    farmArea
      ? [
        { label: "Tổng quan", href: "/dashboard" },
        { label: "Vùng trồng", href: "/farm-areas" },
        { label: `Chỉnh sửa ${farmArea.name}` },
      ]
      : [
        { label: "Tổng quan", href: "/dashboard" },
        { label: "Vùng trồng", href: "/farm-areas" },
        { label: "Chỉnh sửa vùng trồng" },
      ]
  );

  useEffect(() => {
    if (!id) return;
    const fetchArea = async () => {
      try {
        setLoading(true);
        const data = await getFarmAreaById(id);
        setFarmArea(data);
      } catch (error: any) {
        toast.error(error.response?.data?.message || "Không thể tải thông tin vùng trồng");
        navigate("/farm-areas");
      } finally {
        setLoading(false);
      }
    };
    void fetchArea();
  }, [id, navigate]);

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20 text-muted-foreground">
        <RefreshCw className="h-5 w-5 animate-spin mr-2 text-emerald-500" />
        Đang tải thông tin vùng trồng...
      </div>
    );
  }

  if (!farmArea) return null;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-foreground">
            Chỉnh sửa vùng trồng
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Cập nhật thông tin vị trí bản đồ, diện tích và ranh giới đa giác của vùng canh tác.
          </p>
        </div>
        <HelpButton screenKey="farm-area-edit" />
      </div>

      <Tabs defaultValue="general" className="w-full">
        <TabsList className="mb-4">
          <TabsTrigger value="general" className="flex items-center gap-2">
            <FileText className="size-4" />
            Thông tin chung
          </TabsTrigger>
          <TabsTrigger value="boundary" className="flex items-center gap-2">
            <MapPinned className="size-4" />
            Ranh giới trên bản đồ
          </TabsTrigger>
        </TabsList>

        <TabsContent value="general" className="mt-2">
          <EditFarmAreaForm
            farmArea={farmArea}
            onSuccess={() => navigate("/farm-areas")}
            onCancel={() => navigate("/farm-areas")}
          />
        </TabsContent>

        <TabsContent value="boundary" className="mt-2">
          <FarmAreaBoundaryEditor
            farmArea={farmArea}
            onSaveSuccess={() => {
              // Tải lại thông tin vùng trồng khi cần đồng bộ diện tích tính toán
              if (id) {
                void getFarmAreaById(id).then(setFarmArea);
              }
            }}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default EditFarmAreaPage;

