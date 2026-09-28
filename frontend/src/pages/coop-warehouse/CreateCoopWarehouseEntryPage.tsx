import { useCallback } from 'react';
import { LogIn } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { zodResolver } from '@hookform/resolvers/zod';

import { ListPageHeader } from '@/components/common/ListPageHeader';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { HelpButton } from '@/components/help/HelpButton';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter } from '@/components/ui/card';
import { CoopWarehouseEntryFields } from './CoopWarehouseEntryFields';
import {
  useCoopWarehouseLocation,
  useCoopWarehouseShipmentState,
} from './useCoopWarehousePageState';

import {
  recordWarehouseEntrySchema,
  type RecordWarehouseEntryFormValues,
} from '@/utils/validators/coopWarehouseEventSchema';

import { submitCoopWarehouseEntry } from './coopWarehouseSubmitters';
import { getCurrentDatetimeString } from './coopWarehouseUtils';

/** Trang ghi nhận sự kiện nhập kho hợp tác xã cho các lô hàng. */
export default function CreateCoopWarehouseEntryPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const productionLotId = searchParams.get('productionLotId') || '';
  const queryParam = searchParams.get('shipmentIds') || searchParams.get('shipmentId') || '';
  const initialShipmentIds = queryParam ? queryParam.split(',').filter(Boolean) : [];
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RecordWarehouseEntryFormValues>({
    resolver: zodResolver(recordWarehouseEntrySchema),
    defaultValues: {
      shipmentId: initialShipmentIds[0] || '',
      entryTime: getCurrentDatetimeString(),
      warehouseName: '',
      storageCondition: '',
      notes: '',
      latitude: undefined,
      longitude: undefined,
    },
  });
  const onShipmentSelected = useCallback(
    (shipmentId: string) => setValue('shipmentId', shipmentId),
    [setValue],
  );
  const shipmentState = useCoopWarehouseShipmentState({
    productionLotId,
    queryParam,
    initialShipmentIds,
    invalidStatus: 'IN_WAREHOUSE',
    onShipmentSelected,
  });
  const handleLocationChange = useCallback(
    (latitude: number, longitude: number) => {
      setValue('latitude', latitude, { shouldDirty: true, shouldValidate: true });
      setValue('longitude', longitude, { shouldDirty: true, shouldValidate: true });
    },
    [setValue],
  );
  const location = useCoopWarehouseLocation({
    latitude: watch('latitude'),
    longitude: watch('longitude'),
    onLocationChange: handleLocationChange,
  });

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Lô sản xuất', href: '/production-lots' },
    ...(productionLotId
      ? [{
          label: shipmentState.lotName || 'Chi tiết lô sản xuất',
          href: `/production-lots/${productionLotId}`,
        }]
      : []),
    { label: 'Ghi sự kiện nhập kho HTX' },
  ]);

  const onSubmit = (values: RecordWarehouseEntryFormValues) => submitCoopWarehouseEntry(values, {
    items: shipmentState.selectedItems,
    hasValidationError: shipmentState.hasValidationError,
    setServerError: shipmentState.setServerError,
    onSuccess: () => navigate(-1),
  });

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={LogIn}
        title="Ghi sự kiện nhập kho HTX"
        description={
          'Ghi nhận thời điểm các lô hàng rời xưởng đóng gói và nhập vào kho lưu trữ hợp tác xã.'
        }
        actions={<HelpButton screenKey="coop-warehouse-entry" />}
      />
      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-6 pt-6">
            <CoopWarehouseEntryFields
              register={register}
              errors={errors}
              selectedItems={shipmentState.selectedItems}
              invalidItems={shipmentState.invalidItems}
              loadingShipments={shipmentState.loadingShipments}
              serverError={shipmentState.serverError}
              currentPosition={location.currentPosition}
              onLocationSelect={location.handleLocationSelect}
            />
          </CardContent>
          <CardFooter className="flex justify-end gap-3 rounded-b-xl border-t bg-slate-50/50 p-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate(-1)}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting || !shipmentState.selectedItems.length || shipmentState.hasValidationError}
              className="bg-emerald-600 text-white hover:bg-emerald-700"
            >
              <LogIn className="mr-1.5 h-4 w-4" />
              {isSubmitting ? 'Đang xử lý...' : `Ghi nhập kho (${shipmentState.selectedItems.length} lô)`}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
