import { FileBarChart } from 'lucide-react';
import { IndustryReportPanel } from '@/components/report/IndustryReportPanel';
import { HelpButton } from '@/components/help/HelpButton';

export default function IndustryReportPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-2 text-2xl font-bold text-slate-900">
            <FileBarChart className="size-6 text-emerald-700" />
            Báo cáo tổng hợp ngành
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Tổng hợp sản lượng và lô hàng theo địa bàn và khoảng thời gian.
          </p>
        </div>
        <HelpButton screenKey="report-industry" />
      </div>

      <IndustryReportPanel />
    </div>
  );
}