import { CorrectPreprocessingForm } from './components/CorrectPreprocessingForm';
import { HelpButton } from '@/components/help/HelpButton';

/**
 * Trang đính chính sự kiện sơ chế nông sản
 */
export default function CorrectPreprocessingEventPage() {
  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <HelpButton screenKey="preprocessing-event-correct" />
      </div>
      <CorrectPreprocessingForm />
    </div>
  );
}
