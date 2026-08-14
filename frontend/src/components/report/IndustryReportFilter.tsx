import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent } from '@/components/ui/card';
import { Search, X } from 'lucide-react';

interface Props {
  onSearch: (region: string, fromDate: string, toDate: string) => void;
  onReset: () => void;
  loading?: boolean;
}

export const IndustryReportFilter = ({ onSearch, onReset, loading }: Props) => {
  const [region, setRegion] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (region.trim() && fromDate && toDate) {
      onSearch(region.trim(), fromDate, toDate);
    }
  };

  const handleReset = () => {
    setRegion('');
    setFromDate('');
    setToDate('');
    onReset();
  };

  return (
    <Card>
      <CardContent className="p-4">
        <form onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <Label htmlFor="region">Địa bàn *</Label>
              <Input
                id="region"
                value={region}
                onChange={(e) => setRegion(e.target.value)}
                placeholder="VD: Phú Thọ, Hà Nội..."
                required
              />
            </div>
            <div>
              <Label htmlFor="fromDate">Từ ngày *</Label>
              <Input
                id="fromDate"
                type="date"
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
                required
              />
            </div>
            <div>
              <Label htmlFor="toDate">Đến ngày *</Label>
              <Input
                id="toDate"
                type="date"
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
                required
              />
            </div>
            <div className="flex items-end gap-2">
              {/* CHANGED: thêm variant="view" */}
              <Button
                type="submit"
                disabled={loading || !region || !fromDate || !toDate}
                variant="view"
              >
                <Search className="h-4 w-4 mr-1" /> Xem báo cáo
              </Button>
              <Button type="button" variant="outline" onClick={handleReset} disabled={loading}>
                <X className="h-4 w-4 mr-1" /> Xóa
              </Button>
            </div>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};