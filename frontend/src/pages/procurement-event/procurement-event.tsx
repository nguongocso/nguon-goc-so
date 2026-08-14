import { ProcurementEventForm } from '@/components/event-validation/ProcurementEventForm';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Truck } from 'lucide-react';

export default function ProcurementEventPage() {
  return (
    <div className="mx-auto max-w-2xl space-y-6 p-4">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Ghi sự kiện thu mua</h1>
        <p className="mt-1 text-sm text-slate-500">
          Ghi nhận sự kiện nhận hàng cho lô hàng đã kích hoạt tem.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Truck className="size-5 text-emerald-700" />
            Thông tin thu mua
          </CardTitle>
          <CardDescription>
            Nhập thông tin nhận hàng từ lô sản xuất. Chỉ dành cho Doanh nghiệp thu mua (VT-04).
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ProcurementEventForm />
        </CardContent>
      </Card>
    </div>
  );
}