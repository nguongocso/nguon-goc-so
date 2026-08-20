import { useAuth } from '@/hooks/useAuth';
import { AdminDashboard } from './AdminDashboard';
import { CooperativeDashboard } from './CooperativeDashboard';
import { EventRecorderDashboard } from './EventRecorderDashboard';
import { ProcurementDashboard } from './ProcurementDashboard';
import { ManagementDashboard } from './ManagementDashboard';

export function DashboardContent() {
  const { user } = useAuth();

  // VT-06 (Người tiêu dùng tra cứu) không có dashboard nội bộ — chỉ truy cập /public/trace/:code
  switch (user?.roleCode) {
    case 'VT-01':
      return <AdminDashboard />;
    case 'VT-02':
      return <CooperativeDashboard />;
    case 'VT-03':
      return <EventRecorderDashboard />;
    case 'VT-04':
      return <ProcurementDashboard />;
    case 'VT-05':
      return <ManagementDashboard />;
    default:
      return (
        <div className="rounded-lg border bg-white p-6 text-muted-foreground">
          Không có Dashboard phù hợp với vai trò hiện tại.
        </div>
      );
  }
}