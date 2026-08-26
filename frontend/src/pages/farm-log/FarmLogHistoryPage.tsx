import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { FarmLogTab } from '@/components/farm-log/FarmLogTab';
import { HelpButton } from '@/components/help/HelpButton';
import { getFarmLogs } from '@/api/farmLogApi';
import type { FarmLog } from '@/types/farmLog';

export default function FarmLogHistoryPage() {
  const { productionLotId } = useParams<{ productionLotId: string }>();
  const navigate = useNavigate();
  const [logs, setLogs] = useState<FarmLog[]>([]);
  const [loading, setLoading] = useState(true);

  const loadLogs = async () => {
    if (!productionLotId) return;
    try {
      setLoading(true);
      const response = await getFarmLogs({ productionLotId, page: 0, size: 100 });
      setLogs(response.items);
    } catch (error) {
      // console.error('Không thể tải nhật ký:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
  }, [productionLotId]);

  if (!productionLotId) return <div>Không tìm thấy ID lô sản xuất</div>;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Lịch sử nhật ký canh tác
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Theo dõi toàn bộ các hoạt động canh tác đã được ghi nhận cho lô sản xuất.
          </p>
        </div>
        <HelpButton screenKey="farm-log-history" />
      </div>
      {loading ? (
        <div>Đang tải...</div>
      ) : (
        <FarmLogTab
          logs={logs}
          onCreateLog={() => navigate(`/farm-logs/create?productionLotId=${productionLotId}`)}
          onLogUpdated={loadLogs}
        />
      )}
    </div>
  );
}