import { useEffect, useState, type FormEvent } from "react";
import { Info, Loader2, RotateCcw, Search } from "lucide-react";
import { getFarmAreas } from "@/api/farmAreaApi";
import { getOrganizations } from "@/api/organizationApi";
import { getProductCategories } from "@/api/productCategoryApi";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { FarmArea } from "@/types/farmArea";
import type { Organization } from "@/types/organization";
import type { ProductCategory } from "@/types/productCategory";
import type { SeasonYieldComparisonParams } from "@/types/seasonYieldComparison";
import { cn } from "@/lib/utils";

const ALL_VALUE = "__all__";
const CURRENT_YEAR = new Date().getFullYear();
const YEAR_OPTIONS = Array.from(
  { length: 7 },
  (_, index) => CURRENT_YEAR + 1 - index,
);

interface SeasonYieldComparisonFilterProps {
  defaultYears: number[];
  loading?: boolean;
  currentUserRole?: string;
  onCompare: (params: SeasonYieldComparisonParams) => void;
}

export function SeasonYieldComparisonFilter({
  defaultYears,
  loading = false,
  currentUserRole,
  onCompare,
}: SeasonYieldComparisonFilterProps) {
  const [years, setYears] = useState<number[]>(defaultYears);
  const [farmAreaId, setFarmAreaId] = useState(ALL_VALUE);
  const [productCategoryId, setProductCategoryId] = useState(ALL_VALUE);
  const [organizationId, setOrganizationId] = useState(ALL_VALUE);
  const [validationMessage, setValidationMessage] = useState("");
  const [loadingOptions, setLoadingOptions] = useState(true);
  const [hasUnavailableOptions, setHasUnavailableOptions] = useState(false);

  const [farmAreas, setFarmAreas] = useState<FarmArea[]>([]);
  const [productCategories, setProductCategories] = useState<ProductCategory[]>(
    [],
  );
  const [organizations, setOrganizations] = useState<Organization[]>([]);

  const selectedProductCategoryLabel =
    productCategoryId === ALL_VALUE
      ? "Tất cả loại nông sản"
      : (productCategories.find((category) => category.id === productCategoryId)
          ?.name ?? "Không xác định");

  const selectedFarmAreaLabel =
    farmAreaId === ALL_VALUE
      ? "Tất cả vùng trồng"
      : (farmAreas.find((area) => area.id === farmAreaId)?.name ??
        "Không xác định");

  const selectedOrganizationLabel =
    organizationId === ALL_VALUE
      ? "Tất cả tổ chức"
      : (organizations.find(
          (organization) => organization.id === organizationId,
        )?.name ?? "Không xác định");

  useEffect(() => {
    let active = true;

    const loadOptions = async () => {
      setLoadingOptions(true);
      try {
        const [categories, areas, orgs] = await Promise.all([
          getProductCategories({ isActive: true }).catch(() => {
            return [];
          }),
          getFarmAreas().catch(() => {
            return [];
          }),
          currentUserRole === "VT-01"
            ? getOrganizations()
                .then((data) =>
                  data.map((item: any) => ({
                    id: item.organizationID || item.id,
                    name: item.organizationName || item.name,
                    code: item.organizationCode || item.code,
                    type: item.organizationType || item.type,
                    status: item.status,
                    createdAt: item.createdAt,
                    updatedAt: item.updatedAt,
                  }))
                )
                .catch(() => {
                  return [];
                })
            : Promise.resolve([]),
        ]);

        if (!active) return;

        setProductCategories(categories);
        setFarmAreas(areas);
        setOrganizations(orgs);
        setHasUnavailableOptions(false);
      } catch (err) {
        if (!active) return;
        setHasUnavailableOptions(true);
      } finally {
        if (active) {
          setLoadingOptions(false);
        }
      }
    };

    void loadOptions();
    return () => {
      active = false;
    };
  }, [currentUserRole]);

  const toggleYear = (year: number) => {
    setValidationMessage("");
    setYears((current) =>
      current.includes(year)
        ? current.filter((item) => item !== year)
        : [...current, year].sort((a, b) => a - b),
    );
  };

  const buildParams = (): SeasonYieldComparisonParams => ({
    years: [...years].sort((a, b) => a - b),
    ...(farmAreaId !== ALL_VALUE ? { farmAreaId } : {}),
    ...(productCategoryId !== ALL_VALUE ? { productCategoryId } : {}),
    ...(organizationId !== ALL_VALUE ? { organizationId } : {}),
  });

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (years.length === 0) {
      setValidationMessage("Hãy chọn ít nhất một năm để so sánh.");
      return;
    }
    onCompare(buildParams());
  };

  const handleReset = () => {
    setYears(defaultYears);
    setFarmAreaId(ALL_VALUE);
    setProductCategoryId(ALL_VALUE);
    setOrganizationId(ALL_VALUE);
    setValidationMessage("");
    onCompare({ years: [...defaultYears] });
  };

  return (
    <Card>
      <CardContent>
        <form className="space-y-5" onSubmit={handleSubmit}>
          <div>
            <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
              <div>
                <Label>Năm cần so sánh</Label>
                <p className="mt-1 text-xs text-muted-foreground">
                  Chọn các năm để so sánh sản lượng nông sản theo mùa vụ.
                </p>
              </div>
              <span className="rounded-full bg-primary/10 px-2.5 py-1 text-xs font-medium text-primary">
                Đã chọn {years.length} năm
              </span>
            </div>
            <div className="flex flex-wrap gap-2">
              {YEAR_OPTIONS.map((year) => {
                const selected = years.includes(year);
                return (
                  <Button
                    key={year}
                    type="button"
                    variant={selected ? "default" : "outline"}
                    aria-pressed={selected}
                    onClick={() => toggleYear(year)}
                    className={cn("min-w-16", selected && "shadow-sm")}
                  >
                    {year}
                  </Button>
                );
              })}
            </div>
            {validationMessage && (
              <p className="mt-2 text-sm font-medium text-destructive">
                {validationMessage}
              </p>
            )}
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            <div className="space-y-2">
              <Label htmlFor="season-yield-product-category">
                Loại nông sản
              </Label>
              <Select
                value={productCategoryId}
                onValueChange={(value) =>
                  setProductCategoryId(value ?? ALL_VALUE)
                }
              >
                <SelectTrigger
                  id="season-yield-product-category"
                  className="w-full"
                  disabled={loadingOptions}
                >
                  <SelectValue placeholder="Tất cả loại nông sản">
                    {selectedProductCategoryLabel}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL_VALUE}>
                    Tất cả loại nông sản
                  </SelectItem>
                  {productCategories.map((category) => (
                    <SelectItem key={category.id} value={category.id}>
                      {category.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="season-yield-farm-area">Vùng trồng</Label>
              <Select
                value={farmAreaId}
                onValueChange={(value) => setFarmAreaId(value ?? ALL_VALUE)}
              >
                <SelectTrigger
                  id="season-yield-farm-area"
                  className="w-full"
                  disabled={loadingOptions}
                >
                  <SelectValue placeholder="Tất cả vùng trồng">
                    {selectedFarmAreaLabel}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL_VALUE}>Tất cả vùng trồng</SelectItem>
                  {farmAreas.map((area) => (
                    <SelectItem key={area.id} value={area.id}>
                      {area.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {currentUserRole === "VT-01" && (
              <div className="space-y-2">
                <Label htmlFor="season-yield-organization">Tổ chức</Label>
                <Select
                  value={organizationId}
                  onValueChange={(value) => {
                    setOrganizationId(value ?? ALL_VALUE);
                    setFarmAreaId(ALL_VALUE);
                  }}
                >
                  <SelectTrigger
                    id="season-yield-organization"
                    className="w-full"
                    disabled={loadingOptions}
                  >
                    <SelectValue placeholder="Tất cả tổ chức">
                      {selectedOrganizationLabel}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={ALL_VALUE}>Tất cả tổ chức</SelectItem>
                    {organizations.map((organization) => (
                      <SelectItem key={organization.id} value={organization.id}>
                        {organization.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
          </div>

          {hasUnavailableOptions && (
            <div className="flex items-start gap-2 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-800 ring-1 ring-amber-200">
              <Info className="mt-0.5 h-4 w-4 shrink-0" />
              Một số danh sách bộ lọc không tải được theo quyền hiện tại. Bạn
              vẫn có thể so sánh theo năm.
            </div>
          )}

          <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
            <Button
              type="button"
              variant="outline"
              onClick={handleReset}
              disabled={loading}
            >
              <RotateCcw />
              Đặt lại
            </Button>
            <Button type="submit" disabled={loading || loadingOptions}>
              {loading ? <Loader2 className="animate-spin" /> : <Search />}
              So sánh sản lượng
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}