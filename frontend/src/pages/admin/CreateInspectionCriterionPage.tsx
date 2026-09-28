import React from "react";
import { useNavigate } from "react-router-dom";
import { FlaskConical } from "lucide-react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { HelpButton } from "@/components/help/HelpButton";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import { InspectionCriterionFormContent } from "@/components/admin/inspection-criterion/InspectionCriterionFormContent";

export const CreateInspectionCriterionPage: React.FC = () => {
  const navigate = useNavigate();

  // Breadcrumb: bỏ mức "Quản trị" (/admin không phải trang chức năng độc lập)
  useSetBreadcrumb([
    { label: "Tổng quan", href: "/dashboard" },
    { label: "Chỉ tiêu kiểm nghiệm", href: "/admin/inspection-criteria" },
    { label: "Thêm mới" },
  ]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <FlaskConical className="size-6 text-emerald-600" />
            Thêm mới chỉ tiêu kiểm nghiệm
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Khai báo thông tin chỉ tiêu kiểm nghiệm và ngưỡng tối đa cho phép.
          </p>
        </div>
        <HelpButton screenKey="admin-inspection-criteria" />
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <CardTitle className="text-lg font-semibold text-slate-900">
            Thông tin chỉ tiêu kiểm nghiểm
          </CardTitle>
          <CardDescription>
            Nhập tên chỉ tiêu, đơn vị đo, ngưỡng tối đa và chọn tiêu chuẩn tham
            chiếu. Kết quả kiểm nghiệm vượt ngưỡng tối đa sẽ được coi là không
            đạt.
          </CardDescription>
        </CardHeader>
        <CardContent className="pt-6">
          <InspectionCriterionFormContent
            criterion={null}
            onSuccess={() => navigate("/admin/inspection-criteria")}
            onCancel={() => navigate("/admin/inspection-criteria")}
          />
        </CardContent>
      </Card>
    </div>
  );
};

export default CreateInspectionCriterionPage;
