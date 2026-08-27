import { useParams } from 'react-router-dom';
import { FarmLogList } from '@/components/farm-log/FarmLogList';
import { HelpButton } from '@/components/help/HelpButton';

export default function FarmLogHistoryPage() {
  const { productionLotId } = useParams<{ productionLotId: string }>();

  if (!productionLotId) return <div>Không tìm thấy ID lô sản xuất</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-end">
        <HelpButton screenKey="farm-log-history" />
      </div>
      <FarmLogList productionLotId={productionLotId} />
    </div>
  );
}