import { useState } from 'react';
import { z } from 'zod';
import { LoaderCircle, Send, MapPin } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card, CardContent } from '@/components/ui/card';
import { useProcurementEvent } from '@/hooks/useProcurementEvent';

const formSchema = z.object({
  shipmentId: z.string().min(1, 'Vui lòng nhập mã lô hàng').uuid('Mã lô hàng không hợp lệ (UUID)'),
  receivedQuantity: z.coerce.number().positive('Số lượng thực nhận phải lớn hơn 0'),
  notes: z.string().max(500, 'Ghi chú tối đa 500 ký tự').optional(),
  latitude: z.coerce.number().min(-90).max(90).optional(),
  longitude: z.coerce.number().min(-180).max(180).optional(),
});

type FormValues = z.infer<typeof formSchema>;

export function ProcurementEventForm() {
  const [shipmentId, setShipmentId] = useState('');
  const [receivedQuantity, setReceivedQuantity] = useState('');
  const [notes, setNotes] = useState('');
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const { data, isLoading, error, submit, reset } = useProcurementEvent();

  const validate = (): FormValues | null => {
    const result = formSchema.safeParse({
      shipmentId,
      receivedQuantity,
      notes: notes || undefined,
      latitude: latitude || undefined,
      longitude: longitude || undefined,
    });
    if (!result.success) {
      setFormError(result.error.issues[0]?.message ?? 'Dữ liệu không hợp lệ');
      return null;
    }
    setFormError(null);
    return result.data;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const values = validate();
    if (!values) return;
    void submit({
      shipmentId: values.shipmentId,
      receivedQuantity: values.receivedQuantity,
      notes: values.notes,
      latitude: values.latitude,
      longitude: values.longitude,
    });
  };

  const handleReset = () => {
    setShipmentId('');
    setReceivedQuantity('');
    setNotes('');
    setLatitude('');
    setLongitude('');
    setFormError(null);
    reset();
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="shipmentId">Mã lô hàng (Shipment ID) *</Label>
        <Input
          id="shipmentId"
          value={shipmentId}
          onChange={(e) => setShipmentId(e.target.value)}
          placeholder="550e8400-e29b-41d4-a716-446655440000"
          disabled={isLoading}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="receivedQuantity">Số lượng thực nhận *</Label>
        <Input
          id="receivedQuantity"
          type="number"
          value={receivedQuantity}
          onChange={(e) => setReceivedQuantity(e.target.value)}
          placeholder="VD: 1000"
          disabled={isLoading}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="notes">Ghi chú</Label>
        <Textarea
          id="notes"
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="Mô tả tình trạng nhận hàng..."
          rows={3}
          disabled={isLoading}
        />
        <p className="text-xs text-muted-foreground">{notes.length}/500</p>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="latitude" className="flex items-center gap-1">
            <MapPin className="size-3" /> Vĩ độ
          </Label>
          <Input
            id="latitude"
            type="number"
            step="any"
            value={latitude}
            onChange={(e) => setLatitude(e.target.value)}
            placeholder="10.823"
            disabled={isLoading}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="longitude" className="flex items-center gap-1">
            <MapPin className="size-3" /> Kinh độ
          </Label>
          <Input
            id="longitude"
            type="number"
            step="any"
            value={longitude}
            onChange={(e) => setLongitude(e.target.value)}
            placeholder="106.629"
            disabled={isLoading}
          />
        </div>
      </div>

      {(formError || error) && (
        <Alert variant="destructive">
          <AlertDescription>{formError || error}</AlertDescription>
        </Alert>
      )}

      <div className="flex gap-2">
        <Button type="submit" variant="view" disabled={isLoading} className="flex-1">
          {isLoading ? (
            <LoaderCircle className="size-4 animate-spin" />
          ) : (
            <Send className="size-4" />
          )}
          {isLoading ? 'Đang ghi nhận...' : 'Ghi sự kiện thu mua'}
        </Button>
        <Button type="button" variant="outline" onClick={handleReset} disabled={isLoading}>
          Làm mới
        </Button>
      </div>

      {data && (
        <Card className="border-emerald-200 bg-emerald-50">
          <CardContent className="pt-6">
            <p className="font-semibold text-emerald-800">Ghi nhận thành công!</p>
            <div className="mt-2 space-y-1 text-sm text-emerald-700">
              <p>Mã sự kiện: <span className="font-mono">{data.id}</span></p>
              <p>Lô hàng: {data.eventData.shipmentName}</p>
              <p>Số lượng: {data.eventData.receivedQuantity.toLocaleString('vi-VN')}</p>
              <p>Người ghi: {data.recordedByName}</p>
              <p>Thời gian: {new Date(data.recordedAt).toLocaleString('vi-VN')}</p>
            </div>
          </CardContent>
        </Card>
      )}
    </form>
  );
}