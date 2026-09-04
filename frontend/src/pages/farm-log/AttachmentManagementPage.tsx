import { useParams, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ArrowLeft } from "lucide-react";
import { AttachmentManager } from "@/components/farm-log/AttachmentManager";

export default function AttachmentManagementPage() {
  const { logId } = useParams<{ logId: string }>();
  const navigate = useNavigate();

  if (!logId) {
    return (
      <div className="container mx-auto py-6">
        <p className="text-muted-foreground">Không tìm thấy ID nhật ký</p>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="outline" onClick={() => navigate(-1)}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          Quay lại
        </Button>
        <h1 className="text-2xl font-bold">Quản lý chứng từ</h1>
      </div>

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