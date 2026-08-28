import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Search, X } from "lucide-react";

interface Props {
  onFilter: (params: {
    result: string;
    startDate: string;
    endDate: string;
  }) => void;
  onReset: () => void;
  loading?: boolean;
}

export const LoginHistoryFilter = ({ onFilter, onReset, loading }: Props) => {
  const [result, setResult] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onFilter({ result, startDate, endDate });
  };

  const handleReset = () => {
    setResult("");
    setStartDate("");
    setEndDate("");
    onReset();
  };

  return (
    <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
      <CardContent className="p-5">
        <form onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
            <div className="space-y-1.5">
              <Label
                htmlFor="login-result"
                className="text-sm font-medium text-emerald-800"
              >
                Kết quả
              </Label>
              <Select value={result} onValueChange={(value) => setResult(value ?? "")}
                items={[
                  { value: '', label: 'Tất cả' },
                  { value: 'SUCCESS', label: 'Thành công' },
                  { value: 'FAILED', label: 'Thất bại' },
                ]}
              >
                <SelectTrigger id="login-result" className="border-emerald-200 focus:ring-emerald-100">
                  <SelectValue placeholder="Tất cả" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">Tất cả</SelectItem>
                  <SelectItem value="SUCCESS">Thành công</SelectItem>
                  <SelectItem value="FAILED">Thất bại</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="login-start-date" className="text-sm font-medium text-emerald-800">
                Từ ngày
              </Label>
              <Input
                id="login-start-date"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="border-emerald-200 focus-visible:ring-emerald-100"
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="login-end-date" className="text-sm font-medium text-emerald-800">
                Đến ngày
              </Label>
              <Input
                id="login-end-date"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="border-emerald-200 focus-visible:ring-emerald-100"
              />
            </div>
          </div>

          <div className="flex justify-end gap-2 mt-5 pt-4 border-t border-emerald-100">
            <Button type="button" variant="delete" size="sm" onClick={handleReset} disabled={loading} className="gap-2">
              <X className="h-4 w-4" />
              Xóa bộ lọc
            </Button>
            <Button type="submit" variant="search" size="sm" disabled={loading} className="gap-2">
              <Search className="h-4 w-4" />
              Tìm kiếm
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};
