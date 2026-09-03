import { CorrectPackagingForm } from './components/CorrectPackagingForm';
import { HelpButton } from "@/components/help/HelpButton";

export default function CorrectPackagingEventPage() {
  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <HelpButton screenKey="packaging-event-correct" />
      </div>
      <CorrectPackagingForm />
    </div>
  );
}
