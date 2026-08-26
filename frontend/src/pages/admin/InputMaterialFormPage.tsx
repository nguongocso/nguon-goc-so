import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
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
import {
  Upload,
  Image as ImageIcon,
  X,
  PackageCheck,
  Clock,
  ShieldCheck,
} from 'lucide-react';
import { getProductCategories } from '@/api/productCategoryApi';
import {
  createInputMaterial,
  getInputMaterialById,
  updateInputMaterial,
} from '@/api/inputMaterialApi';
import { MaterialGroup, MATERIAL_GROUP_LABELS } from '@/enums/materialGroup';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import type { ProductCategory } from '@/types/productCategory';
import type { InputMaterial } from '@/types/inputMaterial';

export const InputMaterialFormPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditMode = Boolean(id);

  const [loading, setLoading] = useState<boolean>(isEditMode);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [material, setMaterial] = useState<InputMaterial | null>(null);

  useSetBreadcrumb([
    { label: 'Dashboard', href: '/dashboard' },
    { label: 'Danh mục vật tư', href: '/admin/input-materials' },
    { label: isEditMode ? (material ? `Chỉnh sửa: ${material.name}` : 'Chỉnh sửa vật tư') : 'Khai báo vật tư mới' },
  ]);

  // Form states
  const [name, setName] = useState<string>('');
  const [materialGroup, setMaterialGroup] = useState<MaterialGroup>(MaterialGroup.PESTICIDE);
  const [activeIngredient, setActiveIngredient] = useState<string>('');
  const [unit, setUnit] = useState<string>('');
  const [quarantineDays, setQuarantineDays] = useState<string>('7');
  const [applyToAllCrops, setApplyToAllCrops] = useState<boolean>(true);
  const [selectedCropIds, setSelectedCropIds] = useState<string[]>([]);
  const [referenceSource, setReferenceSource] = useState<string>('');

  // Multiple images state (base64 or image URLs)
  const [images, setImages] = useState<string[]>([]);

  // Form errors
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    fetchCropCategories();
    if (id) {
      loadMaterialDetail(id);
    }
  }, [id]);

  const fetchCropCategories = async () => {
    try {
      const res = await getProductCategories();
      setCategories(res);
    } catch {
      toast.error('Không thể tải danh sách loại nông sản');
    }
  };

  const loadMaterialDetail = async (materialId: string) => {
    try {
      setLoading(true);
      const data = await getInputMaterialById(materialId);
      setMaterial(data);
      setName(data.name || '');
      setMaterialGroup(data.materialGroup || MaterialGroup.PESTICIDE);
      setActiveIngredient(data.activeIngredient || '');
      setUnit(data.unit || '');
      setQuarantineDays(data.quarantineDays !== undefined ? String(data.quarantineDays) : '0');
      setApplyToAllCrops(data.applyToAllCrops ?? true);
      setSelectedCropIds(data.applicableCropTypes ? data.applicableCropTypes.map((c) => c.id) : []);
      setReferenceSource(data.referenceSource || '');
      if (data.imageUrls && data.imageUrls.length > 0) {
        setImages(data.imageUrls);
      }
    } catch {
      toast.error('Không thể tải thông tin vật tư cần chỉnh sửa');
      navigate('/admin/input-materials');
    } finally {
      setLoading(false);
    }
  };

  const handleImagesChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    const newImages: string[] = [];
    const fileList = Array.from(files);

    let processedCount = 0;
    fileList.forEach((file) => {
      if (file.size > 5 * 1024 * 1024) {
        toast.error(`Ảnh ${file.name} vượt quá dung lượng tối đa 5MB`);
        processedCount++;
        return;
      }
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          newImages.push(event.target.result as string);
        }
        processedCount++;
        if (processedCount === fileList.length) {
          setImages((prev) => [...prev, ...newImages]);
          toast.success(`Đã thêm ${newImages.length} hình ảnh từ thiết bị`);
        }
      };
      reader.readAsDataURL(file);
    });
  };

  const handleRemoveImage = (indexToRemove: number) => {
    setImages((prev) => prev.filter((_, idx) => idx !== indexToRemove));
  };

  const validate = (): boolean => {
    const errs: Record<string, string> = {};

    if (!name.trim()) {
      errs.name = 'Tên vật tư không được để trống';
    }

    if (!unit.trim()) {
      errs.unit = 'Đơn vị tính không được để trống';
    }

    if (materialGroup === MaterialGroup.PESTICIDE && quarantineDays === '') {
      errs.quarantineDays = 'Thuốc BVTV bắt buộc phải có thời gian cách ly';
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
        imageUrls: images,
      };

      if (isEditMode && id) {
        await updateInputMaterial(id, payload);
        toast.success('Cập nhật thông tin vật tư thành công');
        navigate(`/admin/input-materials/${id}`);
      } else {
        const created = await createInputMaterial(payload);
        toast.success('Thêm mới vật tư đầu vào thành công');
        navigate(`/admin/input-materials/${created.id}`);
      }
    } catch (error: any) {
      const message = error.response?.data?.message || 'Có lỗi xảy ra khi lưu thông tin vật tư';
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const toggleCropSelection = (cropId: string) => {
    setSelectedCropIds((prev) =>
      prev.includes(cropId) ? prev.filter((i) => i !== cropId) : [...prev, cropId]
    );
  };

  if (loading) {
    return (
      <div className="p-6 w-full space-y-6">
        <div className="h-8 w-8 animate-spin rounded-full border-3 border-emerald-600 border-t-transparent mx-auto" />
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 w-full space-y-6">
      {/* Top Header Single Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-lg bg-emerald-500/10 text-emerald-700 dark:text-emerald-400">
            <PackageCheck className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100">
              {isEditMode ? `Chỉnh sửa vật tư: ${material?.name || ''}` : 'Khai báo vật tư đầu vào mới'}
            </h1>
            <p className="text-xs sm:text-sm text-muted-foreground">
              {isEditMode
                ? 'Cập nhật thông tin chi tiết và quy định cách ly cho vật tư'
                : 'Khai báo thông tin vật tư và thiết lập thời gian cách ly (PHI) an toàn'}
            </p>
          </div>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6 w-full">
        {/* Single Column Stacked Cards Layout */}
        <div className="space-y-6">
          {/* Card 1: Thông tin cơ bản vật tư (Dạng 1 cột kéo dài) */}
          <Card className="shadow-sm border-gray-200 dark:border-gray-800">
            <CardHeader className="pb-3 border-b border-gray-100 dark:border-gray-800">
              <CardTitle className="text-base font-semibold flex items-center gap-2">
                <ShieldCheck className="h-5 w-5 text-emerald-600" />
                1. Thông tin cơ bản vật tư
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-4 space-y-4">
              {/* Tên vật tư */}
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="material-name" className="text-sm font-semibold">
                  Tên thương mại vật tư <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="material-name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="VD: Brightin 4.0EC, Phân bón NPK 16-16-8..."
                  className="h-10 w-full"
                />
                {errors.name && <p className="text-xs text-red-500 font-medium">{errors.name}</p>}
              </div>

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
                  <SelectTrigger className="h-10 w-full">
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
                  className="h-10 w-full"
                />
                {errors.unit && <p className="text-xs text-red-500 font-medium">{errors.unit}</p>}
              </div>

              {/* Tên hoạt chất chính */}
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="active-ingredient" className="text-sm font-semibold">
                  Tên hoạt chất chính
                </Label>
                <Input
                  id="active-ingredient"
                  value={activeIngredient}
                  onChange={(e) => setActiveIngredient(e.target.value)}
                  placeholder="VD: Abamectin, Mancozeb..."
                  className="h-10 w-full"
                />
              </div>

              {/* Thời gian cách ly */}
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="quarantine-days" className="text-sm font-semibold flex items-center gap-1">
                  <Clock className="h-4 w-4 text-amber-600" />
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
                  className="h-10 w-full"
                />
                {errors.quarantineDays && (
                  <p className="text-xs text-red-500 font-medium">{errors.quarantineDays}</p>
                )}
              </div>
            </CardContent>
          </Card>

          {/* Card 2: Phạm vi nông sản áp dụng & Nguồn quy định */}
          <Card className="shadow-sm border-gray-200 dark:border-gray-800">
            <CardHeader className="pb-3 border-b border-gray-100 dark:border-gray-800">
              <CardTitle className="text-base font-semibold flex items-center gap-2">
                <ShieldCheck className="h-5 w-5 text-emerald-600" />
                2. Phạm vi nông sản áp dụng & Nguồn quy định
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-4 space-y-4">
              {/* Nông sản áp dụng */}
              <div className="flex flex-col gap-2">
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
                    <span className="font-medium">Tất cả loại nông sản</span>
                  </label>
                  <label className="flex items-center space-x-2 cursor-pointer text-sm">
                    <input
                      type="radio"
                      name="applyToAllCrops"
                      checked={!applyToAllCrops}
                      onChange={() => setApplyToAllCrops(false)}
                      className="h-4 w-4 text-emerald-600 focus:ring-emerald-500 border-gray-300"
                    />
                    <span className="font-medium">Chỉ định danh mục nông sản</span>
                  </label>
                </div>

                {!applyToAllCrops && (
                  <div className="mt-2 p-4 bg-gray-50 dark:bg-gray-900 rounded-lg border max-h-48 overflow-y-auto space-y-2">
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
                          <Label htmlFor={`cat-${cat.id}`} className="text-xs sm:text-sm cursor-pointer">
                            {cat.name} {cat.group ? `(${cat.group})` : ''}
                          </Label>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </div>

              {/* Nguồn quy định */}
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="reference-source" className="text-sm font-semibold">
                  Nguồn quy định tham chiếu
                </Label>
                <Textarea
                  id="reference-source"
                  value={referenceSource}
                  onChange={(e) => setReferenceSource(e.target.value)}
                  placeholder="VD: Thông tư 10/2020/TT-BNNPTNT, Tiêu chuẩn VietGAP TCVN 11892-1:2017..."
                  rows={3}
                  className="w-full"
                />
              </div>
            </CardContent>
          </Card>

          {/* Card 3: Hình ảnh vật tư từ thiết bị */}
          <Card className="shadow-sm border-gray-200 dark:border-gray-800">
            <CardHeader className="pb-3 border-b border-gray-100 dark:border-gray-800 flex flex-row items-center justify-between">
              <CardTitle className="text-base font-semibold flex items-center gap-2">
                <ImageIcon className="h-5 w-5 text-blue-600" />
                3. Hình ảnh vật tư từ thiết bị
              </CardTitle>
              <span className="text-xs text-muted-foreground font-normal">
                ({images.length} ảnh)
              </span>
            </CardHeader>
            <CardContent className="pt-4 space-y-4">
              {/* Upload Button Zone */}
              <label className="flex flex-col items-center justify-center w-full h-32 border-2 border-dashed border-gray-300 dark:border-gray-700 rounded-xl cursor-pointer bg-gray-50/50 dark:bg-gray-900/50 hover:bg-gray-100 dark:hover:bg-gray-800/50 transition-colors p-4 text-center">
                <div className="p-2 bg-emerald-500/10 text-emerald-600 rounded-full mb-1">
                  <Upload className="h-6 w-6" />
                </div>
                <span className="text-xs font-semibold text-gray-800 dark:text-gray-200">
                  Tải nhiều ảnh từ thiết bị
                </span>
                <span className="text-[11px] text-muted-foreground">
                  Bấm hoặc kéo thả ảnh (tối đa 5MB/ảnh)
                </span>
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  onChange={handleImagesChange}
                  className="hidden"
                />
              </label>

              {/* Multiple Images Gallery Grid */}
              {images.length > 0 && (
                <div className="space-y-2">
                  <Label className="text-xs font-semibold text-gray-700 dark:text-gray-300">
                    Danh sách ảnh đã thêm:
                  </Label>
                  <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-3 max-h-72 overflow-y-auto p-1">
                    {images.map((imgSrc, idx) => (
                      <div
                        key={idx}
                        className="relative aspect-square rounded-lg overflow-hidden border border-gray-200 dark:border-gray-800 shadow-sm group"
                      >
                        <img
                          src={imgSrc}
                          alt={`Vật tư ${idx + 1}`}
                          className="w-full h-full object-cover"
                        />
                        <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                          <Button
                            type="button"
                            variant="destructive"
                            size="icon"
                            onClick={() => handleRemoveImage(idx)}
                            className="h-7 w-7 rounded-full"
                            title="Xóa ảnh này"
                          >
                            <X className="h-4 w-4" />
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Action Bar Footer */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-200 dark:border-gray-800">
          <Link to="/admin/input-materials">
            <Button type="button" variant="outline" disabled={submitting}>
              Hủy bỏ
            </Button>
          </Link>
          <Button type="submit" variant="create" disabled={submitting} className="px-6">
            {submitting ? 'Đang lưu...' : isEditMode ? 'Lưu thay đổi' : 'Thêm vật tư mới'}
          </Button>
        </div>
      </form>
    </div>
  );
};
