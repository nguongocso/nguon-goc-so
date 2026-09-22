import { Truck } from 'lucide-react';

import { HelpButton } from '@/components/help/HelpButton';

import { TransportEventForm } from './components/TransportEventForm';

/**
 * Trang ghi nhận sự kiện vận chuyển nông sản
 */
export default function RecordTransportEventPage() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="flex items-center gap-2 text-2xl font-bold tracking-tight text-slate-900">
            <Truck className="size-6 text-emerald-600" />
            Ghi sự kiện vận chuyển
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Quét mã truy xuất của lô hàng, sau đó nhập thông tin chuyến vận chuyển thực tế.
          </p>
        </div>
        <HelpButton screenKey="transport-event-record" />
      </div>
      <TransportEventForm />
    </div>
  );
}
