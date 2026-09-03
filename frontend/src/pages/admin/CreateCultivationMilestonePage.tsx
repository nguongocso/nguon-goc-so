import React from "react";
import { useNavigate } from "react-router-dom";
import { CalendarCheck } from "lucide-react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { HelpButton } from "@/components/help/HelpButton";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import { CultivationMilestoneFormContent } from "@/components/admin/cultivation-milestone/CultivationMilestoneFormContent";

export const CreateCultivationMilestonePage: React.FC = () => {
  const navigate = useNavigate();

  useSetBreadcrumb([
    { label: "Dashboard", href: "/dashboard" },
    { label: "Mốc canh tác", href: "/admin/cultivation-milestones" },
    { label: "Thêm mới" },
  ]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <CalendarCheck className="size-6 text-emerald-600" />
            Thêm mới mốc canh tác
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Khai báo thông tin mốc canh tác bắt buộc theo loại hoạt động.
          </p>
        </div>
        <HelpButton screenKey="admin-cultivation-milestones" />
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <CardTitle className="text-lg font-semibold text-slate-900">
            Thông tin mốc canh tác
          </CardTitle>
          <CardDescription>
            Chọn loại hoạt động, nhập tên mốc, mô tả và số ngày dự kiến tính
            từ ngày gieo trồng.
          </CardDescription>
        </CardHeader>
        <CardContent className="pt-6">
          <CultivationMilestoneFormContent
            milestone={null}
            onSuccess={() => navigate("/admin/cultivation-milestones")}
            onCancel={() => navigate("/admin/cultivation-milestones")}
          />
        </CardContent>
      </Card>
    </div>
  );
};

export default CreateCultivationMilestonePage;
