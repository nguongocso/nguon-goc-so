import { CreatePackagingForm } from "./components/CreatePackagingForm";
import { HelpButton } from "@/components/help/HelpButton";

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
