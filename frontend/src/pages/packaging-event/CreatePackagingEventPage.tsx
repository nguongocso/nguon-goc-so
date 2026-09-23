import { CreatePackagingForm } from './components/CreatePackagingForm';
import { HelpButton } from '@/components/help/HelpButton';

/**
 * Trang ghi nhận sự kiện đóng gói lô sản xuất
 */
export default function CreatePackagingEventPage() {
  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <HelpButton screenKey="packaging-event-create" />
      </div>
      <CreatePackagingForm />
    </div>
  );
}
