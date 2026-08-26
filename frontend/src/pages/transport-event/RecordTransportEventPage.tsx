import { TransportEventForm } from "./components/TransportEventForm";
import { HelpButton } from "@/components/help/HelpButton";
import { Truck } from "lucide-react";

export default function RecordTransportEventPage() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Truck className="size-6 text-emerald-600" />
            Ghi sự kiện vận chuyển
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Quét mã truy xuất của lô hàng, sau đó nhập thông tin chuyến vận chuyển thực tế.
          </p>
        </div>
        <HelpButton screenKey="transport-event-record" />
      </div>
      <TransportEventForm />
    </div>
  );
}
