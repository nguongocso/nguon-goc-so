import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { CalendarCheck, Loader2 } from "lucide-react";
import { toast } from "sonner";
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
import { getCultivationMilestone } from "@/api/cultivationMilestoneApi";
import type { CultivationMilestone } from "@/types/cultivationMilestone";

export const EditCultivationMilestonePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [milestone, setMilestone] = useState<CultivationMilestone | null>(null);

  useSetBreadcrumb([
    { label: "Dashboard", href: "/dashboard" },
    { label: "Mốc canh tác", href: "/admin/cultivation-milestones" },
    { label: "Cập nhật" },
  ]);

  useEffect(() => {
    const fetchMilestone = async () => {
      if (!id) return;
      try {
        setLoading(true);
        const data = await getCultivationMilestone(Number(id));
        setMilestone(data);
      } catch (error: any) {
        toast.error(
          error.response?.data?.message ||
            "Không thể tải thông tin mốc canh tác"
        );
        navigate("/admin/cultivation-milestones");
      } finally {
        setLoading(false);
      }
    };
    fetchMilestone();
  }, [id, navigate]);

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="h-6 w-6 animate-spin mr-2 text-emerald-600" />
        Đang tải thông tin mốc canh tác...
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <CalendarCheck className="size-6 text-emerald-600" />
            Cập nhật mốc canh tác
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Bộ mốc canh tác được áp dụng theo tiêu chuẩn và loại nông sản gắn
            cho lô.
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
            Cập nhật thông tin mốc canh tác: tên, loại hoạt động, phạm vi áp
            dụng (loại nông sản và tiêu chuẩn), mức bắt buộc và thời điểm dự
            kiến.
          </CardDescription>
        </CardHeader>
        <CardContent className="pt-6">
          <CultivationMilestoneFormContent
            open
            milestone={milestone}
            onSuccess={() => navigate("/admin/cultivation-milestones")}
            onCancel={() => navigate("/admin/cultivation-milestones")}
          />
        </CardContent>
      </Card>
    </div>
  );
};

export default EditCultivationMilestonePage;
