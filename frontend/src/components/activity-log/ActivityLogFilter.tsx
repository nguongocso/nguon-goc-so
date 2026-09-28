import { useEffect, useState } from "react";
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
import { formatActionType, formatTargetType } from "@/utils/activityLogFormatter";
import type { ActivityLogExportFilterRequest } from "@/types/activityLog";

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
  "UPDATE_ORGANIZATION_PROFILE",
  "CREATE_API_KEY",
  "REVOKE_API_KEY",
  "CREATE_INSPECTION_REQUEST",
  "RECORD_INSPECTION_RESULT",
  "UPDATE_INSPECTION_RESULT",
  "RECORD_INSPECTION_RESULTS",
  "DELETE_INSPECTION_RESULT",
  "UPLOAD_INSPECTION_RESULT_FILE",
  "UPDATE_ROLE_PERMISSIONS",
  "CREATE_FARM_AREA",
  "DELETE_SHIPMENT_DRAFT",
  "RESOLVE_ALERT",
  "RECORD_EVENT",
  "EXPORT",
  "EXPORT_DOSSIER",
  "GS1_DOSSIER_EXPORT",
  "ATTACH_CERTIFICATION",
  "CREATE_CERTIFICATION",
  "VERIFY_CERTIFICATION",
  "REJECT_CERTIFICATION",
  "UPDATE_PRODUCT_CATEGORY",
  "RECALL",
];

const ACTION_OPTIONS = FILTER_ACTIONS.map((value) => ({
  value,
  label: formatActionType(value),
}));

const OBJECT_TYPES = [
  "PRODUCTION_LOT", "FARM_LOG", "FARM_AREA", "SHIPMENT", "CHAIN_EVENT",
  "CERTIFICATION", "USER", "ORGANIZATION", "INSPECTION_REQUEST", "RECALL_REQUEST",
  "ACTIVITY_LOG_EXPORT",
].map((value) => ({ value, label: formatTargetType(value) }));
const ALL_VALUE = "__ALL__";

interface Props {
  onFilter: (params: ActivityLogExportFilterRequest) => void;
  onReset: () => void;
  loading?: boolean;
  initialValues?: ActivityLogExportFilterRequest;
}

export const ActivityLogFilter = ({ onFilter, onReset, loading, initialValues }: Props) => {
  const [action, setAction] = useState(initialValues?.action ?? "");
  const [actorName, setActorName] = useState(initialValues?.actorName ?? "");
  const [startDate, setStartDate] = useState(initialValues?.startDate ?? "");
  const [endDate, setEndDate] = useState(initialValues?.endDate ?? "");
  const [objectType, setObjectType] = useState(initialValues?.objectType ?? "");

  useEffect(() => {
    setAction(initialValues?.action ?? "");
    setActorName(initialValues?.actorName ?? "");
    setStartDate(initialValues?.startDate ?? "");
    setEndDate(initialValues?.endDate ?? "");
    setObjectType(initialValues?.objectType ?? "");
  }, [initialValues?.action, initialValues?.actorName, initialValues?.startDate,
    initialValues?.endDate, initialValues?.objectType]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onFilter({ action, actorName, startDate, endDate, objectType });
  };

  const handleReset = () => {
    setAction("");
    setActorName("");
    setStartDate("");
    setEndDate("");
    setObjectType("");
    onReset();
  };

  // Helper để lấy label hiển thị
  const getActionLabel = (value: string) => {
    if (!value) return "Tất cả";
    const option = ACTION_OPTIONS.find((opt) => opt.value === value);
    return option ? option.label : value;
  };

  return (
    <Card>
      <CardContent className="p-5">
        <form onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
            {/* Loại thao tác */}
            <div className="space-y-1.5">
              <Label
                htmlFor="action"
                className="text-sm font-medium text-label"
              >
                Loại thao tác
              </Label>
              <Select
                value={action || ALL_VALUE}
                onValueChange={(value) => setAction(value && value !== ALL_VALUE ? value : "")}
              >
                <SelectTrigger id="action">
                  <SelectValue placeholder="Tất cả">
                    {getActionLabel(action)}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL_VALUE}>Tất cả</SelectItem>
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
                className="text-sm font-medium text-label"
              >
                Người thực hiện
              </Label>
              <Input
                id="actorName"
                value={actorName}
                onChange={(e) => setActorName(e.target.value)}
                placeholder="Tên hoặc username..."
              />
            </div>

            {/* Từ ngày */}
            <div className="space-y-1.5">
              <Label
                htmlFor="startDate"
                className="text-sm font-medium text-label"
              >
                Từ ngày
              </Label>
              <Input
                id="startDate"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>

            {/* Đến ngày */}
            <div className="space-y-1.5">
              <Label
                htmlFor="endDate"
                className="text-sm font-medium text-label"
              >
                Đến ngày
              </Label>
              <Input
                id="endDate"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>

            {/* Loại đối tượng */}
            <div className="space-y-1.5">
              <Label htmlFor="objectType" className="text-sm font-medium text-label">
                Loại đối tượng
              </Label>
              <Select
                value={objectType || ALL_VALUE}
                onValueChange={(value) => setObjectType(value && value !== ALL_VALUE ? value : "")}
              >
                <SelectTrigger id="objectType">
                  <SelectValue placeholder="Tất cả">
                    {objectType ? formatTargetType(objectType) : "Tất cả"}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL_VALUE}>Tất cả</SelectItem>
                  {OBJECT_TYPES.map((option) => (
                    <SelectItem key={option.value} value={option.value}>{option.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="mt-5 flex justify-end gap-2 border-t border-border pt-4">
            <Button
              type="button"
              variant="outline"
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
