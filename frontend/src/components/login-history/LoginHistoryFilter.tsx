import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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

  const getResultLabel = (value: string) => {
    if (value === "SUCCESS") return "Thành công";
    if (value === "FAILED") return "Thất bại";
    return "Tất cả";
  };

  return (
    <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
      <CardContent className="p-3">
        <form
          onSubmit={handleSubmit}
          className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2"
        >
          <Select value={result} onValueChange={(value) => setResult(value ?? "")}>
            <SelectTrigger
              size="sm"
              aria-label="Kết quả"
              className="w-full sm:w-44 border-emerald-200 focus:ring-emerald-100"
            >
              <SelectValue placeholder="Tất cả">
                {getResultLabel(result)}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">Tất cả</SelectItem>
              <SelectItem value="SUCCESS">Thành công</SelectItem>
              <SelectItem value="FAILED">Thất bại</SelectItem>
            </SelectContent>
          </Select>

          <Input
            type="date"
            aria-label="Từ ngày"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="h-8 w-full sm:w-40 border-emerald-200 focus-visible:ring-emerald-100"
          />

          <Input
            type="date"
            aria-label="Đến ngày"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="h-8 w-full sm:w-40 border-emerald-200 focus-visible:ring-emerald-100"
          />

          <div className="flex items-center gap-2 sm:ml-auto">
            <Button
              type="button"
              variant="delete"
              size="sm"
              onClick={handleReset}
              disabled={loading}
              className="gap-2"
            >
              <X className="h-4 w-4" />
              Xóa bộ lọc
            </Button>
            <Button
              type="submit"
              variant="search"
              size="sm"
              disabled={loading}
              className="gap-2"
            >
              <Search className="h-4 w-4" />
              Tìm kiếm
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};
