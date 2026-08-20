import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle,
} from '@/components/ui/card';
import { correctPackagingSchema, type CorrectPackagingFormValues } from '@/utils/validators/packagingEventSchema';
import { correctPackagingEvent } from '@/api/packagingApi';
import { Label } from '../../../components/ui/label';
import { Input } from '../../../components/ui/input';
import { LocationPicker } from '@/pages/packaging-event/components/LocationPicker';
import { Button } from '../../../components/ui/button';
import { getLocalDateString } from '@/utils/dateTime';

export function CorrectPackagingForm() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<CorrectPackagingFormValues>({
    resolver: zodResolver(correctPackagingSchema),
    defaultValues: {
      packagingSpecification: '',
      packagingDate: getLocalDateString(),
      correctionReason: '',
      latitude: 0,
      longitude: 0,
    },
  });

  const handleLocationSelect = (lat: number, lng: number) => {
    setValue('latitude', lat);
    setValue('longitude', lng);
  };

  const onSubmit = async (values: CorrectPackagingFormValues) => {
    if (!id) {
      toast.error('Thiếu ID sự kiện gốc');
      return;
    }
    try {
      await correctPackagingEvent(id, {
        packagingSpecification: values.packagingSpecification,
        packagingDate: values.packagingDate,
        correctionReason: values.correctionReason,
        latitude: values.latitude || undefined,
        longitude: values.longitude || undefined,
      });
      toast.success('Đính chính sự kiện thành công');
      navigate('/production-lots');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Có lỗi xảy ra');
    }
  };

  return (
    <Card className="max-w-4xl mx-auto">
      <CardHeader>
        <CardTitle>Đính chính sự kiện đóng gói</CardTitle>
        <CardDescription>Cập nhật thông tin đóng gói (sự kiện gốc sẽ được giữ nguyên).</CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-6">
          <div className="space-y-2">
            <Label htmlFor="packagingSpecification">Quy cách đóng gói mới *</Label>
            <Input id="packagingSpecification" {...register('packagingSpecification')} />
            {errors.packagingSpecification && <p className="text-sm text-red-500">{errors.packagingSpecification.message}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="packagingDate">Ngày đóng gói mới *</Label>
            <Input id="packagingDate" type="date" {...register('packagingDate')} max={getLocalDateString()} />
            {errors.packagingDate && <p className="text-sm text-red-500">{errors.packagingDate.message}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="correctionReason">Lý do đính chính *</Label>
            <Input id="correctionReason" {...register('correctionReason')} placeholder="VD: Nhập sai quy cách..." />
            {errors.correctionReason && <p className="text-sm text-red-500">{errors.correctionReason.message}</p>}
          </div>

          <div className="space-y-2">
            <Label>Vị trí (click trên bản đồ)</Label>
            <LocationPicker onLocationSelect={handleLocationSelect} height="300px" />
          </div>
        </CardContent>
        <CardFooter className="flex justify-end gap-2">
          <Button type="button" variant="outline" onClick={() => navigate(-1)}>Hủy</Button>
          {/* CHANGED: thêm variant="edit" */}
          <Button type="submit" disabled={isSubmitting} variant="edit">
            {isSubmitting ? 'Đang xử lý...' : 'Đính chính'}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}