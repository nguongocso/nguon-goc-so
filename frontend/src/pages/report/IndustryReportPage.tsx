import { FileBarChart } from 'lucide-react';
import { IndustryReportPanel } from '@/components/report/IndustryReportPanel';

export default function IndustryReportPage() {
  return (
    <div className="container mx-auto space-y-6 py-6">
      <div>
        <h1 className="flex items-center gap-2 text-2xl font-bold text-slate-900">
          <FileBarChart className="size-6 text-emerald-700" />
          Báo cáo tổng hợp ngành
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Tổng hợp sản lượng và lô hàng theo địa bàn và khoảng thời gian.
        </p>
      </div>

      <IndustryReportPanel />
    </div>
  );
}