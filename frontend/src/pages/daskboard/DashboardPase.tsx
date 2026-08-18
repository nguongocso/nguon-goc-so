import { DashboardContent } from '@/components/dashboard/DashboardContent';
import { HelpButton } from '@/components/help/HelpButton';

export function DashboardPage() {
  return (
    <>
      <div className="flex justify-end">
        <HelpButton screenKey="dashboard" />
      </div>
      <DashboardContent />
    </>
  );
}
