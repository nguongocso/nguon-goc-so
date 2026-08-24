import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Card, CardContent } from "@/components/ui/card";
import { Search, X } from "lucide-react";
import { formatActionType } from "@/utils/activityLogFormatter";

const FILTER_ACTIONS = [
  "CREATE",
  "UPDATE",
  "DELETE",
  "APPROVE",
  "REJECT",
  "SUBMIT",
  "ACTIVATE",
  "LOCK",
  "UNLOCK",
  "LOGIN",
  "LOGOUT",
  "RECORD_EVENT",
  "EXPORT",
  "EXPORT_DOSSIER",
  "GS1_DOSSIER_EXPORT",
  "ATTACH_CERTIFICATION",
  "CREATE_CERTIFICATION",
  "UPDATE_PRODUCT_CATEGORY",
  "RECALL",
];

const ACTION_OPTIONS = FILTER_ACTIONS.map((value) => ({
  value,
  label: formatActionType(value),
}));

interface Props {
  onFilter: (params: any) => void;
  onReset: () => void;
  loading?: boolean;
}

export const ActivityLogFilter = ({ onFilter, onReset, loading }: Props) => {
  const [action, setAction] = useState("");
  const [actorName, setActorName] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onFilter({ action, actorName, startDate, endDate });
  };

  const handleReset = () => {
    setAction("");
    setActorName("");
    setStartDate("");
    setEndDate("");
    onReset();
  };

  // Helper để lấy label hiển thị
  const getActionLabel = (value: string) => {
    if (!value) return "Tất cả";
    const option = ACTION_OPTIONS.find((opt) => opt.value === value);
    return option ? option.label : value;
  };

  return (
    <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
      <CardContent className="p-5">
        <form onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Loại thao tác */}
            <div className="space-y-1.5">
              <Label
                htmlFor="action"
                className="text-sm font-medium text-emerald-800"
              >
                Loại thao tác
              </Label>
              <Select
                value={action}
                onValueChange={(value) => setAction(value ?? "")}
              >
                <SelectTrigger
                  id="action"
                  className="border-emerald-200 focus:ring-emerald-100"
                >
                  <SelectValue placeholder="Tất cả">
                    {getActionLabel(action)}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">Tất cả</SelectItem>
                  {ACTION_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Người thực hiện */}
            <div className="space-y-1.5">
              <Label
                htmlFor="actorName"
                className="text-sm font-medium text-emerald-800"
              >
                Người thực hiện
              </Label>
              <Input
                id="actorName"
                value={actorName}
                onChange={(e) => setActorName(e.target.value)}
                placeholder="Tên hoặc username..."
                className="border-emerald-200 focus-visible:ring-emerald-100"
              />
            </div>

            {/* Từ ngày */}
            <div className="space-y-1.5">
              <Label
                htmlFor="startDate"
                className="text-sm font-medium text-emerald-800"
              >
                Từ ngày
              </Label>
              <Input
                id="startDate"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="border-emerald-200 focus-visible:ring-emerald-100"
              />
            </div>

            {/* Đến ngày */}
            <div className="space-y-1.5">
              <Label
                htmlFor="endDate"
                className="text-sm font-medium text-emerald-800"
              >
                Đến ngày
              </Label>
              <Input
                id="endDate"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="border-emerald-200 focus-visible:ring-emerald-100"
              />
            </div>
          </div>

          <div className="flex justify-end gap-2 mt-5 pt-4 border-t border-emerald-100">
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
