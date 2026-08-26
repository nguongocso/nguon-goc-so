import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { toast } from 'sonner';
import {
  Clock,
  ShieldCheck,
  FileText,
  CheckCircle2,
  Leaf,
  FlaskConical,
  Boxes,
  Tag,
  Calendar,
  User,
  AlertTriangle,
  Info,
  PackageCheck,
  Image as ImageIcon,
  ZoomIn,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { getInputMaterialById, toggleInputMaterialStatus } from '@/api/inputMaterialApi';
import { MaterialGroup, MATERIAL_GROUP_VARIANTS } from '@/enums/materialGroup';
import { InputMaterialDeleteDialog } from '@/components/admin/input-material/InputMaterialDeleteDialog';
import type { InputMaterial } from '@/types/inputMaterial';

export const InputMaterialDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [material, setMaterial] = useState<InputMaterial | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  // Delete Dialog state
  const [showDeleteDialog, setShowDeleteDialog] = useState<boolean>(false);

  // Lightbox Zoom Modal state
  const [selectedImageIndex, setSelectedImageIndex] = useState<number | null>(null);

  useEffect(() => {
    if (id) {
      fetchDetail(id);
    }
  }, [id]);

  const fetchDetail = async (materialId: string) => {
    try {
      setLoading(true);
      const data = await getInputMaterialById(materialId);
      setMaterial(data);
    } catch {
      toast.error('Không thể tải thông tin chi tiết vật tư đầu vào');
      navigate('/admin/input-materials');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStatus = async () => {
    if (!material) return;
    try {
      const updated = await toggleInputMaterialStatus(material.id, !material.isActive);
      setMaterial(updated);
      toast.success(
        updated.isActive
          ? 'Kích hoạt vật tư thành công'
          : 'Đã chuyển vật tư sang trạng thái Ngừng sử dụng'
      );
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Có lỗi xảy ra khi đổi trạng thái');
    }
  };

  if (loading) {
    return (
      <div className="p-6 w-full space-y-6">
        <Card className="shadow-sm">
          <CardContent className="p-12 text-center text-muted-foreground">
            <div className="flex flex-col items-center justify-center gap-3">
              <div className="h-8 w-8 animate-spin rounded-full border-3 border-emerald-600 border-t-transparent" />
              <span>Đang tải thông tin chi tiết vật tư...</span>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!material) return null;

  const variant = MATERIAL_GROUP_VARIANTS[material.materialGroup];

  const getGroupIcon = (group: MaterialGroup) => {
    switch (group) {
      case MaterialGroup.PESTICIDE:
        return <FlaskConical className="h-8 w-8 text-red-600 dark:text-red-400" />;
      case MaterialGroup.FERTILIZER:
        return <Boxes className="h-8 w-8 text-emerald-600 dark:text-emerald-400" />;
      case MaterialGroup.BIOLOGICAL:
        return <Leaf className="h-8 w-8 text-teal-600 dark:text-teal-400" />;
      default:
        return <Tag className="h-8 w-8 text-blue-600 dark:text-blue-400" />;
    }
  };

  return (
    <div className="p-4 sm:p-6 w-full space-y-6">
      {/* Top Header Bar without ID */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-lg bg-emerald-500/10 text-emerald-700 dark:text-emerald-400">
            <PackageCheck className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100">
              Chi tiết vật tư: {material.name}
            </h1>
            <p className="text-xs sm:text-sm text-muted-foreground">
              Thông tin chi tiết quy định sử dụng và thời gian cách ly an toàn (PHI)
            </p>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate(`/admin/input-materials/${material.id}/edit`)}
            className="h-9 px-3 text-blue-600 hover:text-blue-700 hover:bg-blue-50 border-blue-200"
          >
            Chỉnh sửa
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setShowDeleteDialog(true)}
            className="h-9 px-3 text-red-600 hover:text-red-700 hover:bg-red-50 border-red-200"
          >
            Xóa
          </Button>
        </div>
      </div>

      {/* Hero Card Banner */}
      <Card className="shadow-sm border-gray-200 dark:border-gray-800 overflow-hidden">
        <CardContent className="p-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div className="flex items-start gap-4">
              <div className={`p-4 rounded-2xl ${variant.bgClass} border ${variant.borderClass} flex items-center justify-center shrink-0`}>
                {getGroupIcon(material.materialGroup)}
              </div>
              <div className="space-y-1.5">
                <div className="flex items-center gap-2 flex-wrap">
                  <Badge
                    variant="outline"
                    className={`${variant.bgClass} ${variant.textClass} ${variant.borderClass} font-semibold`}
                  >
                    {variant.label}
                  </Badge>
                  <Badge variant={material.isActive ? 'default' : 'secondary'} className="font-medium">
                    {material.isActive ? 'Đang sử dụng' : 'Ngừng sử dụng'}
                  </Badge>
                </div>
                <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
                  {material.name}
                </h2>
                {material.activeIngredient && (
                  <p className="text-sm text-muted-foreground">
                    Hoạt chất chính: <span className="font-semibold text-gray-800 dark:text-gray-200 italic">{material.activeIngredient}</span>
                  </p>
                )}
              </div>
            </div>

            {/* PHI Highlight Box */}
            <div className="flex items-center gap-4 bg-amber-500/10 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800 p-4 rounded-xl shrink-0">
              <div className="p-3 bg-amber-500/20 rounded-lg text-amber-700 dark:text-amber-400">
                <Clock className="h-6 w-6" />
              </div>
              <div>
                <span className="text-xs font-semibold uppercase tracking-wider text-amber-800 dark:text-amber-300">
                  Thời gian cách ly (PHI)
                </span>
                <div className="text-2xl font-extrabold text-amber-700 dark:text-amber-400">
                  {material.quarantineDays} <span className="text-base font-medium">ngày</span>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Single Column Stacked Cards Layout */}
      <div className="space-y-6 w-full">
        {/* Card 1: Quy định An toàn & Kiểm chứng Thu hoạch (QTN-25) */}
        <Card className="shadow-sm border-gray-200 dark:border-gray-800">
          <CardHeader className="pb-3 border-b border-gray-100 dark:border-gray-800">
            <CardTitle className="text-base font-semibold flex items-center gap-2">
              <ShieldCheck className="h-5 w-5 text-emerald-600" />
              Quy định An toàn & Kiểm chứng Thu hoạch (QTN-25)
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-4 space-y-4">
            <div className="p-4 rounded-lg bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800 flex items-start gap-3">
              <Info className="h-5 w-5 text-emerald-700 dark:text-emerald-400 shrink-0 mt-0.5" />
              <div className="text-xs sm:text-sm text-emerald-900 dark:text-emerald-200 leading-relaxed">
                <p className="font-semibold mb-1">Cơ chế tự động kiểm soát điều kiện an toàn:</p>
                Khi nhật ký canh tác ghi nhận sử dụng vật tư <span className="font-bold">{material.name}</span>, hệ thống sẽ tự động tính toán mốc thời gian cách ly an toàn là <span className="font-bold underline">{material.quarantineDays} ngày</span>. Sự kiện thu hoạch nông sản trước khoảng thời gian này sẽ bị cảnh báo vi phạm ATTP.
              </div>
            </div>

            <div className="space-y-3">
              <div className="flex justify-between items-center text-sm py-2 border-b border-gray-100 dark:border-gray-800">
                <span className="text-muted-foreground">Nhóm quản lý vật tư:</span>
                <span className="font-medium text-gray-900 dark:text-gray-100">{material.materialGroupDisplayName || variant.label}</span>
              </div>
              <div className="flex justify-between items-center text-sm py-2 border-b border-gray-100 dark:border-gray-800">
                <span className="text-muted-foreground">Đơn vị đo lường:</span>
                <span className="font-medium text-gray-900 dark:text-gray-100">{material.unit}</span>
              </div>
              <div className="flex justify-between items-center text-sm py-2 border-b border-gray-100 dark:border-gray-800">
                <span className="text-muted-foreground">Trạng thái kích hoạt:</span>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-medium">{material.isActive ? 'Đang dùng' : 'Ngừng dùng'}</span>
                  <Switch checked={material.isActive} onCheckedChange={handleToggleStatus} />
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Card 2: Hình ảnh sản phẩm & Vật tư */}
        <Card className="shadow-sm border-gray-200 dark:border-gray-800">
          <CardHeader className="pb-3 border-b border-gray-100 dark:border-gray-800 flex flex-row items-center justify-between">
            <CardTitle className="text-base font-semibold flex items-center gap-2">
              <ImageIcon className="h-5 w-5 text-blue-600" />
              Hình ảnh sản phẩm & Vật tư
            </CardTitle>
            {material.imageUrls && material.imageUrls.length > 0 && (
              <Badge variant="secondary" className="text-xs font-normal">
                {material.imageUrls.length} hình ảnh (bấm để xem to)
              </Badge>
            )}
          </CardHeader>
          <CardContent className="pt-4">
            {material.imageUrls && material.imageUrls.length > 0 ? (
              <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-3">
                {material.imageUrls.map((imgSrc, idx) => (
                  <div
                    key={idx}
                    onClick={() => setSelectedImageIndex(idx)}
                    className="group relative aspect-square rounded-xl overflow-hidden border border-gray-200 dark:border-gray-800 shadow-sm bg-gray-50 dark:bg-gray-900 cursor-pointer"
                    title="Bấm để phóng to xem ảnh"
                  >
                    <img
                      src={imgSrc}
                      alt={`${material.name} - Ảnh ${idx + 1}`}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                    />
                    <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white">
                      <ZoomIn className="h-6 w-6" />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="p-4 bg-gray-50 dark:bg-gray-900 rounded-xl border border-gray-100 dark:border-gray-800 flex flex-col items-center justify-center text-center">
                <div className={`p-5 rounded-2xl ${variant.bgClass} mb-3`}>
                  {getGroupIcon(material.materialGroup)}
                </div>
                <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                  {material.name}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  Chưa tải lên hình ảnh chụp từ thiết bị.
                </p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Card 3: Phạm vi nông sản áp dụng */}
        <Card className="shadow-sm border-gray-200 dark:border-gray-800">
          <CardHeader className="pb-3 border-b border-gray-100 dark:border-gray-800">
            <CardTitle className="text-base font-semibold flex items-center gap-2">
              <Leaf className="h-5 w-5 text-emerald-600" />
              Phạm vi nông sản áp dụng
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-4">
            {material.applyToAllCrops ? (
              <div className="p-4 rounded-lg bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800 flex items-center gap-3">
                <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0" />
                <div>
                  <p className="text-sm font-semibold text-emerald-900 dark:text-emerald-200">
                    Áp dụng cho tất cả nông sản
                  </p>
                  <p className="text-xs text-emerald-700 dark:text-emerald-400">
                    Vật tư này có thể ghi nhận cho mọi loại cây trồng và sản phẩm trong hệ thống.
                  </p>
                </div>
              </div>
            ) : material.applicableCropTypes && material.applicableCropTypes.length > 0 ? (
              <div className="space-y-2">
                <p className="text-xs text-muted-foreground font-medium mb-2">
                  Danh mục nông sản được phép sử dụng ({material.applicableCropTypes.length}):
                </p>
                <div className="flex flex-wrap gap-2">
                  {material.applicableCropTypes.map((crop) => (
                    <Badge key={crop.id} variant="secondary" className="px-3 py-1 text-xs">
                      {crop.name} {crop.group ? `(${crop.group})` : ''}
                    </Badge>
                  ))}
                </div>
              </div>
            ) : (
              <div className="p-3 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 rounded-lg flex items-center gap-2 text-xs text-amber-800 dark:text-amber-300">
                <AlertTriangle className="h-4 w-4 shrink-0" />
                <span>Chưa chỉ định danh mục nông sản cụ thể.</span>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Card 4: Nguồn quy định tham chiếu */}
        <Card className="shadow-sm border-gray-200 dark:border-gray-800">
          <CardHeader className="pb-3 border-b border-gray-100 dark:border-gray-800">
            <CardTitle className="text-base font-semibold flex items-center gap-2">
              <FileText className="h-5 w-5 text-blue-600" />
              Nguồn quy định tham chiếu
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-4">
            {material.referenceSource ? (
              <div className="p-3 bg-gray-50 dark:bg-gray-900 rounded-lg text-sm text-gray-800 dark:text-gray-200 leading-relaxed">
                {material.referenceSource}
              </div>
            ) : (
              <p className="text-sm text-muted-foreground italic">Chưa cập nhật nguồn quy định tham chiếu.</p>
            )}
          </CardContent>
        </Card>

        {/* Card 5: Nhật ký khởi tạo & Hệ thống */}
        <Card className="shadow-sm border-gray-200 dark:border-gray-800">
          <CardHeader className="pb-3 border-b border-gray-100 dark:border-gray-800">
            <CardTitle className="text-base font-semibold flex items-center gap-2">
              <Calendar className="h-5 w-5 text-purple-600" />
              Nhật ký khởi tạo & Hệ thống
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-4 space-y-3">
            <div className="flex justify-between items-center text-sm py-2 border-b border-gray-100 dark:border-gray-800">
              <span className="text-muted-foreground flex items-center gap-1.5">
                <User className="h-4 w-4" /> Người tạo:
              </span>
              <span className="font-medium text-gray-900 dark:text-gray-100">
                Quản trị viên nền tảng (VT-01)
              </span>
            </div>
            <div className="flex justify-between items-center text-sm py-2 border-b border-gray-100 dark:border-gray-800">
              <span className="text-muted-foreground flex items-center gap-1.5">
                <Calendar className="h-4 w-4" /> Ngày khai báo:
              </span>
              <span className="font-medium text-gray-900 dark:text-gray-100">
                {material.createdAt ? new Date(material.createdAt).toLocaleString('vi-VN') : '—'}
              </span>
            </div>
            <div className="flex justify-between items-center text-sm py-2">
              <span className="text-muted-foreground flex items-center gap-1.5">
                <Clock className="h-4 w-4" /> Cập nhật gần nhất:
              </span>
              <span className="font-medium text-gray-900 dark:text-gray-100">
                {material.updatedAt ? new Date(material.updatedAt).toLocaleString('vi-VN') : 'Chưa cập nhật'}
              </span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Delete Dialog */}
      <InputMaterialDeleteDialog
        open={showDeleteDialog}
        onClose={() => setShowDeleteDialog(false)}
        onSuccess={() => {
          toast.success('Đã xóa vật tư thành công');
          navigate('/admin/input-materials');
        }}
        material={material}
      />

      {/* Image Lightbox Modal (Phóng to xem ảnh) */}
      <Dialog open={selectedImageIndex !== null} onOpenChange={(v) => !v && setSelectedImageIndex(null)}>
        <DialogContent className="max-w-4xl max-h-[92vh] p-4 flex flex-col items-center justify-center bg-gray-950/95 border-gray-800 text-white">
          <DialogHeader className="w-full flex flex-row items-center justify-between border-b border-gray-800 pb-3 mb-2">
            <DialogTitle className="text-sm font-semibold text-gray-200 flex items-center gap-2">
              <ImageIcon className="h-4 w-4 text-emerald-400" />
              <span>{material.name}</span>
              {material.imageUrls && material.imageUrls.length > 1 && (
                <span className="text-xs text-gray-400 font-normal">
                  (Ảnh {selectedImageIndex !== null ? selectedImageIndex + 1 : 0} / {material.imageUrls.length})
                </span>
              )}
            </DialogTitle>
          </DialogHeader>

          {selectedImageIndex !== null && material.imageUrls && material.imageUrls[selectedImageIndex] && (
            <div className="relative w-full flex items-center justify-center overflow-hidden my-auto py-2">
              <img
                src={material.imageUrls[selectedImageIndex]}
                alt={`${material.name} - Phóng to`}
                className="max-h-[75vh] w-auto object-contain rounded-lg shadow-2xl transition-all"
              />

              {/* Button xem ảnh trước */}
              {material.imageUrls.length > 1 && (
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() =>
                    setSelectedImageIndex((prev) =>
                      prev !== null && prev > 0 ? prev - 1 : material.imageUrls!.length - 1
                    )
                  }
                  className="absolute left-2 top-1/2 -translate-y-1/2 bg-black/60 hover:bg-black/80 text-white rounded-full h-10 w-10 shadow-md"
                  title="Xem ảnh trước"
                >
                  <ChevronLeft className="h-6 w-6" />
                </Button>
              )}

              {/* Button xem ảnh sau */}
              {material.imageUrls.length > 1 && (
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() =>
                    setSelectedImageIndex((prev) =>
                      prev !== null && prev < material.imageUrls!.length - 1 ? prev + 1 : 0
                    )
                  }
                  className="absolute right-2 top-1/2 -translate-y-1/2 bg-black/60 hover:bg-black/80 text-white rounded-full h-10 w-10 shadow-md"
                  title="Xem ảnh kế tiếp"
                >
                  <ChevronRight className="h-6 w-6" />
                </Button>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
};
