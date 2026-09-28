import { useCallback } from 'react';
import { LogOut } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { zodResolver } from '@hookform/resolvers/zod';

import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { HelpButton } from '@/components/help/HelpButton';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter } from '@/components/ui/card';
import { CoopWarehouseExitFields } from './CoopWarehouseExitFields';
import {
  useCoopWarehouseLocation,
  useCoopWarehouseShipmentState,
} from './useCoopWarehousePageState';

import {
  recordWarehouseExitSchema,
  type RecordWarehouseExitFormValues,
} from '@/utils/validators/coopWarehouseEventSchema';

import { submitCoopWarehouseExit } from './coopWarehouseSubmitters';
import { getCurrentDatetimeString } from './coopWarehouseUtils';

/** Trang ghi nhận sự kiện xuất kho hợp tác xã cho các lô hàng. */
export default function CreateCoopWarehouseExitPage() {
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
  } = useForm<RecordWarehouseExitFormValues>({
    resolver: zodResolver(recordWarehouseExitSchema),
    defaultValues: {
      shipmentId: initialShipmentIds[0] || '',
      exitTime: getCurrentDatetimeString(),
      destination: '',
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
    invalidStatus: 'NOT_IN_WAREHOUSE',
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
    { label: 'Ghi sự kiện xuất kho HTX' },
  ]);

  const onSubmit = (values: RecordWarehouseExitFormValues) => submitCoopWarehouseExit(values, {
    items: shipmentState.selectedItems,
    hasValidationError: shipmentState.hasValidationError,
    setServerError: shipmentState.setServerError,
    onSuccess: () => navigate(-1),
  });

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={LogOut}
        title="Ghi sự kiện xuất kho HTX"
        description={
          'Ghi nhận thời điểm các lô hàng rời kho hợp tác xã để chuyển đi thu mua hoặc vận chuyển.'
        }
        iconBoxClassName="bg-amber-50 text-amber-600"
        actions={<HelpButton screenKey="coop-warehouse-exit" />}
      />
      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-6 pt-6">
            <CoopWarehouseExitFields
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
              className="bg-amber-600 text-white hover:bg-amber-700"
            >
              <LogOut className="mr-1.5 h-4 w-4" />
              {isSubmitting ? 'Đang xử lý...' : `Ghi xuất kho (${shipmentState.selectedItems.length} lô)`}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
