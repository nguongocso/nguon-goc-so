import { useEffect, useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { toast } from 'sonner';
import { getProductCategories } from '@/api/productCategoryApi';
import { createInputMaterial, updateInputMaterial } from '@/api/inputMaterialApi';
import { MaterialGroup, MATERIAL_GROUP_LABELS } from '@/enums/materialGroup';
import type { InputMaterial } from '@/types/inputMaterial';
import type { ProductCategory } from '@/types/productCategory';

interface Props {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
  material?: InputMaterial | null;
}

export const InputMaterialFormModal = ({ open, onClose, onSuccess, material }: Props) => {
  const [submitting, setSubmitting] = useState(false);
  const [categories, setCategories] = useState<ProductCategory[]>([]);

  // Form states
  const [name, setName] = useState('');
  const [materialGroup, setMaterialGroup] = useState<MaterialGroup>(MaterialGroup.PESTICIDE);
  const [activeIngredient, setActiveIngredient] = useState('');
  const [unit, setUnit] = useState('');
  const [quarantineDays, setQuarantineDays] = useState<string>('7');
  const [applyToAllCrops, setApplyToAllCrops] = useState<boolean>(true);
  const [selectedCropIds, setSelectedCropIds] = useState<string[]>([]);
  const [referenceSource, setReferenceSource] = useState('');

  // Errors
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (open) {
      loadCropCategories();
      if (material) {
        setName(material.name || '');
        setMaterialGroup(material.materialGroup || MaterialGroup.PESTICIDE);
        setActiveIngredient(material.activeIngredient || '');
        setUnit(material.unit || '');
        setQuarantineDays(material.quarantineDays !== undefined ? String(material.quarantineDays) : '0');
        setApplyToAllCrops(material.applyToAllCrops ?? true);
        setSelectedCropIds(material.applicableCropTypes ? material.applicableCropTypes.map((c) => c.id) : []);
        setReferenceSource(material.referenceSource || '');
      } else {
        // Default new
        setName('');
        setMaterialGroup(MaterialGroup.PESTICIDE);
        setActiveIngredient('');
        setUnit('');
        setQuarantineDays('7');
        setApplyToAllCrops(true);
        setSelectedCropIds([]);
        setReferenceSource('');
      }
      setErrors({});
    }
  }, [open, material]);

  const loadCropCategories = async () => {
    try {
      const data = await getProductCategories({ isActive: true });
      setCategories(data);
    } catch {
      // Ignore if load fails
    }
  };

  const validate = () => {
    const errs: Record<string, string> = {};

    if (!name.trim()) {
      errs.name = 'Tên vật tư không được để trống';
    }

    if (!unit.trim()) {
      errs.unit = 'Đơn vị tính không được để trống';
    }

    // TC-02: validation thời gian cách ly
    if (materialGroup === MaterialGroup.PESTICIDE) {
      if (quarantineDays === '' || quarantineDays === null || quarantineDays === undefined) {
        errs.quarantineDays = 'Nhóm thuốc bảo vệ thực vật bắt buộc phải có thời gian cách ly';
      }
    }

    if (quarantineDays !== '') {
      const days = parseInt(quarantineDays, 10);
      if (isNaN(days) || days < 0) {
        errs.quarantineDays = 'Thời gian cách ly phải là số nguyên không âm';
      }
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    try {
      setSubmitting(true);
      const parsedDays = quarantineDays !== '' ? parseInt(quarantineDays, 10) : 0;

      const payload = {
        name: name.trim(),
        materialGroup,
        activeIngredient: activeIngredient.trim() || undefined,
        unit: unit.trim(),
        quarantineDays: parsedDays,
        applyToAllCrops,
        applicableCropTypeIds: !applyToAllCrops ? selectedCropIds : undefined,
        referenceSource: referenceSource.trim() || undefined,
      };

      if (material) {
        await updateInputMaterial(material.id, payload);
        toast.success('Cập nhật thông tin vật tư thành công');
      } else {
        await createInputMaterial(payload);
        toast.success('Thêm mới vật tư đầu vào thành công');
      }

      onSuccess();
      onClose();
    } catch (error: any) {
      const message = error.response?.data?.message || 'Có lỗi xảy ra khi lưu thông tin vật tư';
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const toggleCropSelection = (cropId: string) => {
    setSelectedCropIds((prev) =>
      prev.includes(cropId) ? prev.filter((id) => id !== cropId) : [...prev, cropId]
    );
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold text-gray-900 dark:text-gray-100">
            {material ? 'Chỉnh sửa vật tư đầu vào' : 'Khai báo vật tư đầu vào mới'}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4 py-2">
          {/* Tên vật tư */}
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="material-name" className="text-sm font-semibold">
              Tên vật tư <span className="text-red-500">*</span>
            </Label>
            <Input
              id="material-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="VD: Brightin 4.0EC, Phân bón NPK 16-16-8..."
            />
            {errors.name && <p className="text-xs text-red-500 font-medium">{errors.name}</p>}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Nhóm vật tư */}
            <div className="flex flex-col gap-1.5">
              <Label className="text-sm font-semibold">
                Nhóm vật tư <span className="text-red-500">*</span>
              </Label>
              <Select
                value={materialGroup}
                onValueChange={(val: string | null) => {
                  if (!val) return;
                  const grp = val as MaterialGroup;
                  setMaterialGroup(grp);
                  if (grp === MaterialGroup.PESTICIDE && (quarantineDays === '0' || quarantineDays === '')) {
                    setQuarantineDays('7');
                  } else if (grp === MaterialGroup.FERTILIZER && quarantineDays === '') {
                    setQuarantineDays('0');
                  }
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Chọn nhóm vật tư">
                    {MATERIAL_GROUP_LABELS[materialGroup]}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {Object.values(MaterialGroup).map((grp) => (
                    <SelectItem key={grp} value={grp} label={MATERIAL_GROUP_LABELS[grp]}>
                      {MATERIAL_GROUP_LABELS[grp]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Đơn vị tính */}
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="material-unit" className="text-sm font-semibold">
                Đơn vị tính <span className="text-red-500">*</span>
              </Label>
              <Input
                id="material-unit"
                value={unit}
                onChange={(e) => setUnit(e.target.value)}
                placeholder="ml, g, kg, lít, chai..."
              />
              {errors.unit && <p className="text-xs text-red-500 font-medium">{errors.unit}</p>}
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Hoạt chất */}
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="active-ingredient" className="text-sm font-semibold">
                Hoạt chất
              </Label>
              <Input
                id="active-ingredient"
                value={activeIngredient}
                onChange={(e) => setActiveIngredient(e.target.value)}
                placeholder="VD: Abamectin, Mancozeb..."
              />
            </div>

            {/* Thời gian cách ly (PHI) */}
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="quarantine-days" className="text-sm font-semibold">
                Thời gian cách ly (Số ngày PHI){' '}
                {materialGroup === MaterialGroup.PESTICIDE && <span className="text-red-500">*</span>}
              </Label>
              <Input
                id="quarantine-days"
                type="number"
                min={0}
                value={quarantineDays}
                onChange={(e) => setQuarantineDays(e.target.value)}
                placeholder="0"
              />
              {errors.quarantineDays && (
                <p className="text-xs text-red-500 font-medium">{errors.quarantineDays}</p>
              )}
            </div>
          </div>

          {/* Nông sản áp dụng */}
          <div className="flex flex-col gap-2 pt-2 border-t border-gray-100 dark:border-gray-800">
            <Label className="text-sm font-semibold">Loại nông sản áp dụng</Label>
            <div className="flex gap-6">
              <label className="flex items-center space-x-2 cursor-pointer text-sm">
                <input
                  type="radio"
                  name="applyToAllCrops"
                  checked={applyToAllCrops}
                  onChange={() => setApplyToAllCrops(true)}
                  className="h-4 w-4 text-emerald-600 focus:ring-emerald-500 border-gray-300"
                />
                <span>Tất cả loại nông sản</span>
              </label>
              <label className="flex items-center space-x-2 cursor-pointer text-sm">
                <input
                  type="radio"
                  name="applyToAllCrops"
                  checked={!applyToAllCrops}
                  onChange={() => setApplyToAllCrops(false)}
                  className="h-4 w-4 text-emerald-600 focus:ring-emerald-500 border-gray-300"
                />
                <span>Chỉ định danh mục nông sản</span>
              </label>
            </div>

            {!applyToAllCrops && (
              <div className="mt-2 p-3 bg-gray-50 dark:bg-gray-900 rounded-md border max-h-36 overflow-y-auto space-y-2">
                {categories.length === 0 ? (
                  <p className="text-xs text-muted-foreground">Chưa có danh mục nông sản nào</p>
                ) : (
                  categories.map((cat) => (
                    <div key={cat.id} className="flex items-center space-x-2">
                      <Checkbox
                        id={`cat-${cat.id}`}
                        checked={selectedCropIds.includes(cat.id)}
                        onCheckedChange={() => toggleCropSelection(cat.id)}
                      />
                      <Label htmlFor={`cat-${cat.id}`} className="text-xs cursor-pointer">
                        {cat.name} ({cat.group})
                      </Label>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>

          {/* Nguồn quy định tham chiếu */}
          <div className="flex flex-col gap-1.5 pt-2 border-t border-gray-100 dark:border-gray-800">
            <Label htmlFor="reference-source" className="text-sm font-semibold">
              Nguồn quy định tham chiếu
            </Label>
            <Textarea
              id="reference-source"
              value={referenceSource}
              onChange={(e) => setReferenceSource(e.target.value)}
              placeholder="VD: Thông tư 10/2020/TT-BNNPTNT, Tiêu chuẩn VietGAP TCVN 11892-1:2017..."
              rows={2}
            />
          </div>

          <DialogFooter className="pt-4 border-t border-gray-100 dark:border-gray-800">
            <Button type="button" variant="outline" onClick={onClose} disabled={submitting}>
              Hủy bỏ
            </Button>
            <Button type="submit" variant="create" disabled={submitting}>
              {submitting ? 'Đang lưu...' : material ? 'Cập nhật' : 'Thêm mới'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};
