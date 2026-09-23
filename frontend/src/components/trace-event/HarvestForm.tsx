import { LoaderCircle, Sprout } from 'lucide-react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { HarvestFormFields } from './HarvestFormFields';
import { useHarvestForm } from './useHarvestForm';

/** Thuộc tính của form ghi nhận sự kiện thu hoạch. */
export interface HarvestFormProps {
  productionLotId: string;
  productionLotName: string;
  onSuccess?: () => void;
  onCancel?: () => void;
}

/** Biểu mẫu ghi nhận sự kiện thu hoạch cho lô sản xuất. */
export function HarvestForm(props: HarvestFormProps) {
  const controller = useHarvestForm(props);
  const {
    error,
    handleSubmit,
    isOverrideBlocked,
    isSubmitting,
    onSubmit,
  } = controller;
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Sprout className="h-5 w-5 text-emerald-600" />
          Ghi nhận thu hoạch
        </CardTitle>
        <CardDescription>
          Ghi nhận sự kiện thu hoạch cho lô sản xuất{' '}
          <span className="font-semibold">{props.productionLotName}</span>
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-4">
          {error && (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          <HarvestFormFields controller={controller} />
          <div className="rounded-lg bg-amber-50 p-3 text-sm text-amber-800">
            <p className="font-medium">⚠️ Lưu ý:</p>
            <ul className="mt-1 list-disc space-y-1 pl-5">
              <li>Lô sản xuất phải ở trạng thái <strong>Đã duyệt (APPROVED)</strong></li>
              <li>
                Sau khi ghi nhận, trạng thái lô sẽ chuyển sang{' '}
                <strong>Đã thu hoạch (HARVESTED)</strong>
              </li>
              <li>Thao tác này không thể hoàn tác</li>
            </ul>
          </div>
        </CardContent>
        <CardFooter className="flex justify-end gap-3">
          {props.onCancel && (
            <Button type="button" variant="outline" onClick={props.onCancel}>
              Hủy
            </Button>
          )}
          <Button
            type="submit"
            variant="create"
            disabled={isSubmitting || isOverrideBlocked}
            title={isOverrideBlocked
              ? 'Chưa hết thời gian cách ly - Chỉ Quản lý HTX mới có quyền ghi đè'
              : undefined}
          >
            {isSubmitting && <LoaderCircle className="mr-2 h-4 w-4 animate-spin" />}
            {isSubmitting ? 'Đang ghi nhận...' : 'Ghi nhận thu hoạch'}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}
