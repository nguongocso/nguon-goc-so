import { useParams } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { AttachmentManager } from "@/components/farm-log/AttachmentManager";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";

export default function AttachmentManagementPage() {
  const { logId } = useParams<{ logId: string }>();

  useSetBreadcrumb([
    { label: "Tổng quan", href: "/dashboard" },
    ...(logId
      ? [{ label: "Chi tiết nhật ký", href: `/farm-logs/${logId}` }]
      : []),
    { label: "Quản lý chứng từ" },
  ]);

  if (!logId) {
    return (
      <div className="container mx-auto py-6">
        <p className="text-muted-foreground">Không tìm thấy ID nhật ký</p>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      <h1 className="text-2xl font-bold">Quản lý chứng từ</h1>

      <Card>
        <CardHeader>
          <CardTitle>Chứng từ đính kèm</CardTitle>
        </CardHeader>
        <CardContent>
          <AttachmentManager
            logId={logId}
            onUpdate={() => {
              // Optional: refresh parent data if needed
            }}
          />
        </CardContent>
      </Card>
    </div>
  );
}
