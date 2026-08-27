import { useState, useEffect } from "react";
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
import { getProductCategories } from "@/api/productCategoryApi";
import { getFarmAreas } from "@/api/farmAreaApi";
import { getOrganizations } from "@/api/organizationApi";
import { ProvinceUnitMultiSelect } from "@/components/common/ProvinceUnitMultiSelect";
import type { ProductCategory } from "@/types/productCategory";
import type { FarmArea } from "@/types/farmArea";
import type { Organization } from "@/types/organization";

interface Props {
  onFilter: (params: any) => void;
  onReset: () => void;
  loading?: boolean;
  currentUserRole?: string;
}

export const CropAreaFilter = ({
  onFilter,
  onReset,
  loading,
  currentUserRole,
}: Props) => {
  const [year, setYear] = useState(new Date().getFullYear());
  const [farmAreaId, setFarmAreaId] = useState("");
  const [productCategoryId, setProductCategoryId] = useState("");
  const [organizationId, setOrganizationId] = useState("");
  const [unitIds, setUnitIds] = useState<string[]>([]);

  // NCL-742 §8: bộ lọc địa bàn chỉ dành cho VT-01/VT-05.
  const canFilterByUnit =
    currentUserRole === "VT-01" || currentUserRole === "VT-05";

  const [productCategories, setProductCategories] = useState<ProductCategory[]>(
    [],
  );
  const [farmAreas, setFarmAreas] = useState<FarmArea[]>([]);
  const [organizations, setOrganizations] = useState<Organization[]>([]);

  useEffect(() => {
    Promise.all([
      getProductCategories(),
      getFarmAreas(),
      currentUserRole === "VT-01"
        ? getOrganizations().then((data) =>
            data.map((item: any) => ({
              id: item.organizationID || item.id,
              name: item.organizationName || item.name,
              code: item.organizationCode || item.code,
              type: item.organizationType || item.type,
              status: item.status,
              createdAt: item.createdAt,
              updatedAt: item.updatedAt,
            })),
          )
        : Promise.resolve([]),
    ]).then(([cats, areas, orgs]) => {
      setProductCategories(cats);
      setFarmAreas(areas);
      if (currentUserRole === "VT-01") setOrganizations(orgs);
    });
  }, [currentUserRole]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const params: any = {};
    if (year) params.year = year;
    if (farmAreaId) params.farmAreaId = farmAreaId;
    if (productCategoryId) params.productCategoryId = productCategoryId;
    if (organizationId) params.organizationId = organizationId;
    if (canFilterByUnit && unitIds.length > 0) params.unitIds = unitIds;
    onFilter(params);
  };

  const handleReset = () => {
    setYear(new Date().getFullYear());
    setFarmAreaId("");
    setProductCategoryId("");
    setOrganizationId("");
    setUnitIds([]);
    onReset();
  };

  const getFarmAreaName = (id: string) => {
    const area = farmAreas.find((a) => a.id === id);
    return area ? area.name : "Tất cả";
  };

  const getCategoryName = (id: string) => {
    const cat = productCategories.find((c) => c.id === id);
    return cat ? cat.name : "Tất cả";
  };

  const getOrganizationName = (id: string) => {
    const org = organizations.find((o) => o.id === id);
    return org ? org.name : "Tất cả";
  };

  return (
    <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
      <CardContent className="p-5">
        <form onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
            <div className="space-y-1.5">
              <Label htmlFor="year" className="text-sm font-medium text-emerald-800">
                Năm
              </Label>
              <Input
                id="year"
                type="number"
                value={year}
                onChange={(e) => setYear(Number(e.target.value))}
                placeholder="VD: 2026"
                className="h-9 border-emerald-200 focus-visible:ring-emerald-100"
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="productCategory" className="text-sm font-medium text-emerald-800">
                Loại nông sản
              </Label>
              <Select
                value={productCategoryId}
                onValueChange={(value) => setProductCategoryId(value || "")}
              >
                <SelectTrigger size="sm" className="w-full border-emerald-200 focus:ring-emerald-100">
                  <SelectValue placeholder="Tất cả">
                    {productCategoryId
                      ? getCategoryName(productCategoryId)
                      : "Tất cả"}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">Tất cả</SelectItem>
                  {productCategories.map((cat) => (
                    <SelectItem key={cat.id} value={cat.id}>
                      {cat.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="farmArea" className="text-sm font-medium text-emerald-800">
                Vùng trồng
              </Label>
              <Select
                value={farmAreaId}
                onValueChange={(value) => setFarmAreaId(value || "")}
              >
                <SelectTrigger size="sm" className="w-full border-emerald-200 focus:ring-emerald-100">
                  <SelectValue placeholder="Tất cả">
                    {farmAreaId ? getFarmAreaName(farmAreaId) : "Tất cả"}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">Tất cả</SelectItem>
                  {farmAreas.map((area) => (
                    <SelectItem key={area.id} value={area.id}>
                      {area.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {currentUserRole === "VT-01" && (
              <div className="space-y-1.5">
                <Label htmlFor="organization" className="text-sm font-medium text-emerald-800">
                  Tổ chức
                </Label>
                <Select
                  value={organizationId}
                  onValueChange={(value) => setOrganizationId(value || "")}
                >
                  <SelectTrigger size="sm" className="w-full border-emerald-200 focus:ring-emerald-100">
                    <SelectValue placeholder="Tất cả">
                      {organizationId
                        ? getOrganizationName(organizationId)
                        : "Tất cả"}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">Tất cả</SelectItem>
                    {organizations.map((org) => (
                      <SelectItem key={org.id} value={org.id}>
                        {org.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            {canFilterByUnit && (
              <div className="space-y-1.5">
                <Label className="text-sm font-medium text-emerald-800">Địa bàn</Label>
                <ProvinceUnitMultiSelect value={unitIds} onChange={setUnitIds} />
              </div>
            )}

            <div className="space-y-1.5">
              <Label className="invisible text-sm font-medium">Thao tác</Label>
              <div className="flex gap-2">
                <Button type="submit" variant="search" disabled={loading}>
                  <Search className="h-4 w-4 mr-1" /> Lọc
                </Button>
                <Button
                  type="button"
                  variant="delete"
                  onClick={handleReset}
                  disabled={loading}
                >
                  <X className="h-4 w-4 mr-1" /> Xóa
                </Button>
              </div>
            </div>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};