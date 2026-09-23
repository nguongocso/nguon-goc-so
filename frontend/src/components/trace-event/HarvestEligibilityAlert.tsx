import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertTriangle, CheckCircle2, Info, LoaderCircle } from 'lucide-react';
import type { HarvestEligibilityResponse } from '@/types/farmLog';

/** Thuộc tính của component HarvestEligibilityAlert */
interface HarvestEligibilityAlertProps {
  loading: boolean;
  eligibility: HarvestEligibilityResponse | null;
  isEarlyHarvest: boolean;
  isOverrideBlocked: boolean;
  selectedHarvestDate: string;
}

/** Hiển thị cảnh báo và trạng thái kiểm tra thời gian cách ly thuốc BVTV trước khi thu hoạch */
export const HarvestEligibilityAlert = ({
  loading,
  eligibility,
  isEarlyHarvest,
  isOverrideBlocked,
  selectedHarvestDate,
}: HarvestEligibilityAlertProps) => {
  if (loading) {
    return (
      <div className="flex items-center gap-2 text-xs text-muted-foreground p-2.5 rounded-lg bg-slate-50 border">
        <LoaderCircle className="h-3.5 w-3.5 animate-spin text-slate-500" />
        <span>Đang kiểm tra thời gian cách ly thuốc bảo vệ thực vật...</span>
      </div>
    );
  }

  if (eligibility?.determined && eligibility.eligibleHarvestDate) {
    if (isEarlyHarvest) {
      if (isOverrideBlocked) {
        return (
          <Alert variant="destructive" className="border-red-300 bg-red-50 text-red-900">
            <AlertTriangle className="h-4 w-4 text-red-600 shrink-0" />
            <AlertDescription className="space-y-1 text-sm">
              <p className="font-semibold">⚠️ Chưa hết thời gian cách ly thuốc BVTV!</p>
              <p>
                Lô có thời gian cách ly đến ngày <strong>{eligibility.eligibleHarvestDate}</strong>.
                Bạn đang chọn ngày thu hoạch <strong>{selectedHarvestDate}</strong> (thu hoạch sớm).
              </p>
              <p className="pt-1 text-xs font-medium text-red-700">
                Chỉ Quản lý hợp tác xã (VT-02) mới có quyền ghi đè thu hoạch
                sớm kèm lý do bắt buộc. Vui lòng liên hệ Quản lý HTX hoặc
                chọn ngày thu hoạch sau thời hạn cách ly.
              </p>
            </AlertDescription>
          </Alert>
        );
      }

      return (
        <Alert className="border-amber-300 bg-amber-50 text-amber-900">
          <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0" />
          <AlertDescription className="space-y-1 text-sm">
            <p className="font-semibold">⚠️ Cảnh báo thu hoạch trước thời gian cách ly</p>
            <p>
              Lô sản xuất có thời gian cách ly thuốc BVTV đến ngày <strong>{eligibility.eligibleHarvestDate}</strong>.
              Bạn đang chọn ngày thu hoạch sớm: <strong>{selectedHarvestDate}</strong>.
            </p>
            <p className="pt-1 text-xs text-amber-800">
              Quản lý có thể ghi đè nhưng{' '}
              <strong>bắt buộc phải nhập lý do</strong>. Dữ liệu này sẽ được
              lưu vết vào lịch sử audit và hồ sơ truy xuất nguồn gốc.
            </p>
          </AlertDescription>
        </Alert>
      );
    }

    return (
      <div
        className={
          'flex items-center gap-2 rounded-lg border border-emerald-200 ' +
          'bg-emerald-50 p-2.5 text-xs text-emerald-800'
        }
      >
        <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
        <span>
          Đã đảm bảo thời gian cách ly thuốc BVTV (Đủ điều kiện thu hoạch từ
          ngày <strong>{eligibility.eligibleHarvestDate}</strong>).
        </span>
      </div>
    );
  }

  if (eligibility && !eligibility.determined) {
    return (
      <Alert className="border-blue-200 bg-blue-50 text-blue-900">
        <Info className="h-4 w-4 text-blue-600 shrink-0" />
        <AlertDescription className="space-y-1 text-sm">
          <p className="font-semibold">ℹ️ Thông báo vật tư canh tác</p>
          <p>
            Lô có vật tư chưa xác định được thời gian cách ly tự động:{' '}
            <span className="font-medium">
              {eligibility.unmatchedMaterials?.join(', ') ||
                'Vật tư ngoài danh mục'}
            </span>.
          </p>
          <p className="text-xs text-blue-700">
            Hệ thống cho phép ghi nhận thu hoạch bình thường và sẽ lưu ghi chú vào hồ sơ truy xuất nguồn gốc.
          </p>
        </AlertDescription>
      </Alert>
    );
  }

  return null;
};
