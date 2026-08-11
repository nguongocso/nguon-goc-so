import { WarehouseReceiptForm } from './components/WarehouseReceiptForm';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Warehouse } from 'lucide-react';

export default function WarehouseReceiptPage() {
  return (
    <div className="mx-auto max-w-2xl space-y-6 p-4">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Nhập kho & Đối chiếu số lượng</h1>
        <p className="mt-1 text-sm text-slate-500">
          Ghi nhận số lượng thực nhận khi hàng về kho. Hệ thống tự động đối chiếu với số lượng khai báo trên lô hàng.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Warehouse className="size-5 text-blue-700" />
            Thông tin nhập kho
          </CardTitle>
          <CardDescription>
            Quét hoặc nhập mã truy xuất, sau đó nhập số lượng thực nhận và tình trạng hàng hóa. Chỉ dành cho Doanh nghiệp thu mua (VT-04).
          </CardDescription>
        </CardHeader>
        <CardContent>
          <WarehouseReceiptForm />
        </CardContent>
      </Card>
    </div>
  );
}