import { TransportEventForm } from "./components/TransportEventForm";
import { HelpButton } from "@/components/help/HelpButton";

export default function RecordTransportEventPage() {
  return (
    <>
      
      <div className="flex justify-end">
        <HelpButton screenKey="transport-event-record" />
      </div>
      <TransportEventForm />
    </>
  );
}
