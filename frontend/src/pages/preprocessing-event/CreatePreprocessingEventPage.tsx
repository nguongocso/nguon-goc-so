import { CreatePreprocessingForm } from "./components/CreatePreprocessingForm";
import { HelpButton } from "@/components/help/HelpButton";

export default function CreatePreprocessingEventPage() {
  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <HelpButton screenKey="preprocessing-event-create" />
      </div>
      <CreatePreprocessingForm />
    </div>
  );
}
