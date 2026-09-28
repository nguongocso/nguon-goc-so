import { AlertTriangle, LoaderCircle, PackageSearch } from 'lucide-react';

import { LotValidationStatus } from '@/components/event-validation/LotValidationStatus';
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
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { FarmLogEligibilityAlert } from './FarmLogEligibilityAlert';
import { LocationPicker } from './LocationPicker';
import { useCreatePackagingForm } from './useCreatePackagingForm';

import { getLocalDateString } from '@/utils/dateTime';

/** Biểu mẫu ghi nhận sự kiện đóng gói cho lô sản xuất. */
export function CreatePackagingForm() {
  const controller = useCreatePackagingForm();
  const {
    checkFarmLogEligibility,
    currentPosition,
    eligibilityMessage,
    eligibilityStatus,
    formState: { errors, isSubmitting },
    handleLocationSelect,
    handleSubmit,
    loading,
    loadingLot,
    lot,
    lotLoadError,
    missingMilestones,
    navigate,
    onSubmit,
    register,
    sourceLotId,
    user,
    validation,
  } = controller;

  if (!sourceLotId) {
    return (
      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardContent className="flex flex-col items-center justify-center py-16 text-center">
          <span className="grid size-14 place-items-center rounded-full bg-slate-100 text-slate-500">
            <PackageSearch className="size-7" />
          </span>
          <h3 className="mt-4 text-lg font-bold text-slate-900">
            Chưa có lô sản xuất được chọn
          </h3>
          <p className="mt-1 max-w-md text-sm text-slate-500">
            Ghi sự kiện đóng gói là chức năng gắn với một lô sản xuất cụ thể.
            Vui lòng mở từ trang chi tiết lô hoặc quét mã truy xuất để chọn lô
            cần đóng gói.
          </p>
        </CardContent>
      </Card>
    );
  }
  if (loadingLot) {
    return (
      <div className="flex items-center justify-center gap-2 py-16 text-slate-500">
        <LoaderCircle className="size-4 animate-spin" />
        Đang tải lô sản xuất...
      </div>
    );
  }
  if (lotLoadError || !lot) {
    return (
      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardContent className="py-10">
          <Alert variant="destructive">
            <AlertTriangle className="size-4" />
            <AlertDescription>
              {lotLoadError ?? 'Không tìm thấy lô sản xuất đã chọn.'}
            </AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  const lotLabel = `${lot.name}${
    lot.productCategoryName ? ` - ${lot.productCategoryName}` : ''
  }`;
  return (
    <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
      <CardHeader>
        <CardTitle>Ghi sự kiện đóng gói</CardTitle>
        <CardDescription>Nhập thông tin đóng gói cho lô “{lotLabel}”.</CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-6">
          <div className="space-y-2 rounded-lg border border-emerald-100 bg-emerald-50/20 p-4">
            <Label className="text-sm text-muted-foreground">
              Lô sản xuất đang đóng gói
            </Label>
            <div className="text-lg font-bold text-emerald-900">{lotLabel}</div>
          </div>
          {loading || (validation && !validation.valid) ? (
            <LotValidationStatus
              isValid={loading ? null : (validation?.valid ?? null)}
              message={loading ? '' : (validation?.message ?? '')}
              loading={loading}
              className="mt-2"
            />
          ) : (
            <FarmLogEligibilityAlert
              status={eligibilityStatus}
              productionLotName={lot.name}
              missingMilestones={missingMilestones}
              message={eligibilityMessage || undefined}
              actionLabel={user?.roleCode === 'VT-02' ? 'Xem lịch sử nhật ký' : 'Ghi bổ sung nhật ký'}
              onAction={eligibilityStatus === 'ineligible' ? () => navigate(
                user?.roleCode === 'VT-02'
                  ? `/production-lots/${sourceLotId}/farm-logs`
                  : `/farm-logs/create?productionLotId=${encodeURIComponent(sourceLotId)}`,
              ) : undefined}
              onRetry={eligibilityStatus === 'error' || eligibilityStatus === 'ineligible'
                ? () => void checkFarmLogEligibility(sourceLotId)
                : undefined}
            />
          )}
          <div className="space-y-2">
            <Label htmlFor="packagingSpecification">Quy cách đóng gói *</Label>
            <Input
              id="packagingSpecification"
              placeholder="VD: Bao 60kg, Túi 500g x 20 túi/thùng..."
              {...register('packagingSpecification')}
            />
            {errors.packagingSpecification && (
              <p className="text-sm text-red-500">{errors.packagingSpecification.message}</p>
            )}
          </div>
          <div className="space-y-2">
            <Label htmlFor="packagingDate">Ngày đóng gói *</Label>
            <Input
              id="packagingDate"
              type="date"
              max={getLocalDateString()}
              {...register('packagingDate')}
            />
            {errors.packagingDate && (
              <p className="text-sm text-red-500">{errors.packagingDate.message}</p>
            )}
          </div>
          <div className="space-y-2">
            <Label>Vị trí đóng gói (click trên bản đồ)</Label>
            <LocationPicker
              onLocationSelect={handleLocationSelect}
              initialPosition={currentPosition}
              height="300px"
            />
          </div>
        </CardContent>
        <CardFooter className="flex justify-end gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate(-1)}
          >
            Hủy
          </Button>
          <Button
            type="submit"
            variant="create"
            disabled={isSubmitting || eligibilityStatus !== 'eligible' || !validation?.valid}
          >
            {isSubmitting ? 'Đang ghi...' : 'Ghi sự kiện'}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}
