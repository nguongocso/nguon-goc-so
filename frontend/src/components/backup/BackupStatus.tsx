import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

interface Props {
  isBackupInProgress: boolean;
  isRestoreInProgress: boolean;
}

export const BackupStatus = ({ isBackupInProgress, isRestoreInProgress }: Props) => {
  let statusText = 'Hệ thống hoạt động bình thường';
  let dotColor = 'bg-green-500';

  if (isRestoreInProgress) {
    statusText = '⛔ Hệ thống đang phục hồi dữ liệu (Bảo trì)';
    dotColor = 'bg-red-500';
  } else if (isBackupInProgress) {
    statusText = '⏳ Đang sao lưu dữ liệu...';
    dotColor = 'bg-amber-500';
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">📊 Trạng thái hệ thống</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex items-center gap-3">
          <div className={`h-3 w-3 rounded-full ${dotColor}`} />
          <span className="font-medium">{statusText}</span>
          {isRestoreInProgress && (
            <Badge variant="destructive" className="ml-2">
              Bảo trì
            </Badge>
          )}
        </div>
      </CardContent>
    </Card>
  );
};