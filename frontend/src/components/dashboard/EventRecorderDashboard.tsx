import { ProductionLotBoard } from '@/components/production-lot/ProductionLotBoard';
import { HelpButton } from '@/components/help/HelpButton';
import { MilestoneReminderCard } from '@/components/farm-log/MilestoneReminderCard';

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

      {/* NCL-03-CN-007: Thẻ nhắc lịch ghi nhật ký theo mốc canh tác bắt buộc quá hạn */}
      <MilestoneReminderCard userOnly={true} />

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