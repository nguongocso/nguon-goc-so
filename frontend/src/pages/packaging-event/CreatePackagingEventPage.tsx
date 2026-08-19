import { CreatePackagingForm } from "./components/CreatePackagingForm";
import { HelpButton } from "@/components/help/HelpButton";

export default function CreatePackagingEventPage() {
  return (
    <>
      <div className="flex justify-end">
        <HelpButton screenKey="packaging-event-create" />
      </div>
      <CreatePackagingForm />
    </>
  );
}
