import { HelpButton } from '@/components/help/HelpButton';
import { StorageConditionForm } from './StorageConditionForm';
import { StorageConditionResult } from './StorageConditionResult';
import { useStorageCondition } from './useStorageCondition';

/** Trang ghi nhận và giám sát điều kiện bảo quản cho lô hàng. */
export default function StorageConditionPage() {
  const controller = useStorageCondition();

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Bảo quản</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Ghi nhận nhiệt độ và độ ẩm trong quá trình vận chuyển. Dữ liệu mô phỏng, nhập tay.
          </p>
        </div>
        <HelpButton screenKey="storage-condition" />
      </div>

      <StorageConditionForm controller={controller} />
      {controller.result && <StorageConditionResult result={controller.result} />}
    </div>
  );
}
