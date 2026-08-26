import { TransportEventForm } from "./components/TransportEventForm";
import { HelpButton } from "@/components/help/HelpButton";

export default function RecordTransportEventPage() {
  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <HelpButton screenKey="transport-event-record" />
      </div>
      <TransportEventForm />
    </div>
  );
}
