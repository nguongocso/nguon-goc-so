import { useEffect, useRef, useState } from 'react';
import { isAxiosError } from 'axios';
import { useForm } from 'react-hook-form';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import { zodResolver } from '@hookform/resolvers/zod';

import { useAuth } from '@/hooks/useAuth';
import { useAutoGeolocation } from '@/hooks/useAutoGeolocation';
import { useLotValidation } from '@/hooks/useLotValidation';
import type { FarmLogEligibilityStatus } from './FarmLogEligibilityAlert';

import { getPackagingEligibility } from '@/api/cultivationMilestoneApi';
import { recordPackagingEvent } from '@/api/packagingApi';
import { getProductionLotById } from '@/api/productionLotApi';
import type { ProductionLot } from '@/types/productionLot';
import { getLocalDateString } from '@/utils/dateTime';
import {
  recordPackagingSchema,
  type RecordPackagingFormValues,
} from '@/utils/validators/packagingEventSchema';

const getPackagingError = (error: unknown) => {
  if (!isAxiosError<{ message?: string }>(error)) {
    return {
      message: 'Có lỗi xảy ra khi ghi sự kiện đóng gói',
      isNetworkError: true,
    };
  }

  return {
    message:
      error.response?.data?.message ??
      'Có lỗi xảy ra khi ghi sự kiện đóng gói',
    isNetworkError: !error.response,
  };
};

/** Quản lý dữ liệu và hành vi của biểu mẫu đóng gói. */
export function useCreatePackagingForm() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { user } = useAuth();
  const sourceLotId =
    (location.state as { productionLotId?: string } | null)?.productionLotId ??
    searchParams.get('productionLotId') ??
    '';
  const [lot, setLot] = useState<ProductionLot | null>(null);
  const [loadingLot, setLoadingLot] = useState(Boolean(sourceLotId));
  const [lotLoadError, setLotLoadError] = useState<string | null>(null);
  const [eligibilityStatus, setEligibilityStatus] =
    useState<FarmLogEligibilityStatus>('unselected');
  const [eligibilityMessage, setEligibilityMessage] = useState('');
  const [missingMilestones, setMissingMilestones] = useState<string[]>([]);
  const eligibilityRequestRef = useRef(0);
  const { validation, loading } = useLotValidation(sourceLotId, 'PACKAGING');
  const form = useForm<RecordPackagingFormValues>({
    resolver: zodResolver(recordPackagingSchema),
    defaultValues: {
      productionLotId: sourceLotId,
      packagingSpecification: '',
      packagingDate: getLocalDateString(),
      latitude: 0,
      longitude: 0,
    },
  });
  const latitude = form.watch('latitude');
  const longitude = form.watch('longitude');
  const currentPosition =
    typeof latitude === 'number' &&
    Number.isFinite(latitude) &&
    typeof longitude === 'number' &&
    Number.isFinite(longitude) &&
    !(latitude === 0 && longitude === 0)
      ? { lat: latitude, lng: longitude }
      : undefined;

  const checkFarmLogEligibility = async (productionLotId: string) => {
    const requestId = ++eligibilityRequestRef.current;
    setEligibilityStatus('checking');
    setEligibilityMessage('');
    setMissingMilestones([]);

    try {
      const eligibility = await getPackagingEligibility(productionLotId);
      if (requestId !== eligibilityRequestRef.current) return;
      if (!eligibility.eligible) {
        setEligibilityStatus('ineligible');
        setEligibilityMessage(
          'Lô sản xuất chưa đủ mốc canh tác bắt buộc. Vui lòng bổ sung nhật ký trước khi đóng gói.',
        );
        setMissingMilestones(
          eligibility.missingMilestones.map((milestone) => milestone.name),
        );
        return;
      }
      setEligibilityStatus('eligible');
      setEligibilityMessage(
        'Lô đã đáp ứng đầy đủ mốc canh tác bắt buộc theo tiêu chuẩn và loại nông sản.',
      );
    } catch (error: unknown) {
      if (requestId !== eligibilityRequestRef.current) return;
      const details = getPackagingError(error);
      setEligibilityStatus('error');
      setEligibilityMessage(
        details.isNetworkError
          ? 'Không thể kết nối để kiểm tra mốc canh tác. Vui lòng thử lại.'
          : details.message,
      );
    }
  };

  useEffect(() => {
    if (!sourceLotId) return;
    const fetchLot = async () => {
      setLoadingLot(true);
      setLotLoadError(null);
      setEligibilityStatus('checking');
      try {
        setLot(await getProductionLotById(sourceLotId));
        eligibilityRequestRef.current += 1;
        void checkFarmLogEligibility(sourceLotId);
      } catch {
        setLot(null);
        setLotLoadError(
          'Không thể tải thông tin lô sản xuất đã chọn. Vui lòng quay lại và thử lại.',
        );
      } finally {
        setLoadingLot(false);
      }
    };
    void fetchLot();
    // Hàm kiểm tra dùng ref để loại bỏ phản hồi cũ; chỉ chạy lại khi đổi lô.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sourceLotId]);

  const handleLocationSelect = (latitude: number, longitude: number) => {
    form.setValue('latitude', latitude, {
      shouldValidate: true,
      shouldDirty: true,
    });
    form.setValue('longitude', longitude, {
      shouldValidate: true,
      shouldDirty: true,
    });
  };

  useAutoGeolocation({
    onLocation: (latitude, longitude) => {
      handleLocationSelect(latitude, longitude);
      toast.success('Đã lấy vị trí hiện tại');
    },
    onError: (message) => toast.error(`Không thể lấy vị trí: ${message}`),
  });

  const onSubmit = async (values: RecordPackagingFormValues) => {
    if (eligibilityStatus !== 'eligible') {
      toast.error('Cần kiểm tra đủ mốc canh tác trước khi đóng gói');
      await checkFarmLogEligibility(values.productionLotId);
      return;
    }
    try {
      await recordPackagingEvent({
        productionLotId: values.productionLotId,
        packagingSpecification: values.packagingSpecification,
        packagingDate: values.packagingDate,
        latitude: values.latitude || undefined,
        longitude: values.longitude || undefined,
      });
      setEligibilityStatus('eligible');
      toast.success('Ghi sự kiện đóng gói thành công');
      navigate('/production-lots');
    } catch (error: unknown) {
      const details = getPackagingError(error);
      if (/chưa đủ mốc canh tác/i.test(details.message)) {
        setEligibilityStatus('ineligible');
        setEligibilityMessage(details.message);
        try {
          const eligibility = await getPackagingEligibility(
            values.productionLotId,
          );
          setMissingMilestones(
            eligibility.missingMilestones.map((milestone) => milestone.name),
          );
        } catch {
          setMissingMilestones([]);
        }
      } else {
        setEligibilityStatus('error');
        setEligibilityMessage(
          details.isNetworkError
            ? 'Không thể kết nối để ghi sự kiện đóng gói. Vui lòng thử lại.'
            : details.message,
        );
      }
      toast.error(details.message);
    }
  };

  return {
    ...form,
    checkFarmLogEligibility,
    currentPosition,
    eligibilityMessage,
    eligibilityStatus,
    handleLocationSelect,
    loading,
    loadingLot,
    lot,
    lotLoadError,
    missingMilestones,
    navigate,
    onSubmit,
    sourceLotId,
    user,
    validation,
  };
}
