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
  onFilter: (params: any) => void;
  onReset: () => void;
  loading?: boolean;
}

const STATUS_OPTIONS = [
  { value: "", label: "Tất cả" },
  { value: "true", label: "Đang hoạt động" },
  { value: "false", label: "Đã ẩn" },
];

export const ProductCategoryFilter = ({
  onFilter,
  onReset,
  loading,
}: Props) => {
  const [name, setName] = useState("");
  const [group, setGroup] = useState("");
  const [isActive, setIsActive] = useState<boolean | undefined>(undefined);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const params: any = {};
    if (name) params.name = name;
    if (group) params.categoryGroup = group;
    if (isActive !== undefined) params.isActive = isActive;
    onFilter(params);
  };

  const handleReset = () => {
    setName("");
    setGroup("");
    setIsActive(undefined);
    onReset();
  };

  return (
    <Card className="shadow-sm">
      <CardContent className="p-5">
        <form onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 gap-5 md:grid-cols-4">
            {/* Tên loại nông sản */}
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="name" className="text-sm font-medium">
                Tên loại nông sản
              </Label>
              <Input
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Tìm theo tên..."
                className="h-9"
              />
            </div>

            {/* Nhóm hàng */}
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="group" className="text-sm font-medium">
                Nhóm hàng
              </Label>
              <Input
                id="group"
                value={group}
                onChange={(e) => setGroup(e.target.value)}
                placeholder="VD: Cây ăn quả"
                className="h-9"
              />
            </div>

            {/* Trạng thái */}
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="isActive" className="text-sm font-medium">
                Trạng thái
              </Label>
              <Select
                value={isActive !== undefined ? String(isActive) : ""}
                onValueChange={(val) =>
                  setIsActive(val === "" ? undefined : val === "true")
                }
              >
                <SelectTrigger className="h-9">
                  <SelectValue placeholder="Tất cả" />
                </SelectTrigger>
                <SelectContent>
                  {STATUS_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Thao tác */}
            <div className="flex flex-col gap-1.5">
              {/* Label ẩn để căn chỉnh */}
              <Label className="invisible text-sm font-medium">Thao tác</Label>
              <div className="flex gap-2">
                <Button type="submit" disabled={loading} className="flex-1">
                  <Search className="mr-1 h-4 w-4" />
                  Tìm kiếm
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleReset}
                  disabled={loading}
                  className="flex-1"
                >
                  <X className="mr-1 h-4 w-4" />
                  Xóa
                </Button>
              </div>
            </div>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};