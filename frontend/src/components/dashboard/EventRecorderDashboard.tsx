import { ProductionLotBoard } from '@/components/production-lot/ProductionLotBoard';
import { HelpButton } from '@/components/help/HelpButton';

export function EventRecorderDashboard() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            Ghi nhật ký canh tác
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Chọn lô sản xuất để ghi nhật ký canh tác hoặc xem thông tin chi tiết.
          </p>
        </div>
        <HelpButton screenKey="dashboard" />
      </div>

      <ProductionLotBoard
        canCreate={false}
        canEdit={false}
        canSubmitForApproval={false}
        canApprove={false}
        canRecordFarmLog={true}
      />
    </div>
  );
}