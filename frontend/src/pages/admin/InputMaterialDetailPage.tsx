import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { toast } from 'sonner';
import {
  ArrowLeft,
  Clock,
  ShieldCheck,
  Edit2,
  Trash2,
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
} from 'lucide-react';
import { getInputMaterialById, toggleInputMaterialStatus } from '@/api/inputMaterialApi';
import { MaterialGroup, MATERIAL_GROUP_VARIANTS } from '@/enums/materialGroup';
import { InputMaterialFormModal } from '@/components/admin/input-material/InputMaterialFormModal';
import { InputMaterialDeleteDialog } from '@/components/admin/input-material/InputMaterialDeleteDialog';
import type { InputMaterial } from '@/types/inputMaterial';

export const InputMaterialDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [material, setMaterial] = useState<InputMaterial | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  // Modals
  const [showEditModal, setShowEditModal] = useState<boolean>(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState<boolean>(false);

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
    } catch (error: any) {
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
      <div className="p-6 space-y-6">
        <div className="flex items-center gap-3">
          <Button variant="outline" size="sm" onClick={() => navigate('/admin/input-materials')}>
            <ArrowLeft className="h-4 w-4 mr-1.5" /> Quay lại
          </Button>
        </div>
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
    <div className="p-4 sm:p-6 max-w-6xl mx-auto space-y-6">
      {/* Top Header & Breadcrumb */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link to="/admin/input-materials">
            <Button variant="outline" size="sm" className="h-9 px-3">
              <ArrowLeft className="h-4 w-4 mr-1.5" /> Quay lại danh sách
            </Button>
          </Link>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100">
              Chi tiết vật tư đầu vào
            </h1>
            <p className="text-xs sm:text-sm text-muted-foreground">
              Mã vật tư: <span className="font-mono">{material.id}</span>
            </p>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setShowEditModal(true)}
            className="h-9 px-3 text-blue-600 hover:text-blue-700 hover:bg-blue-50 border-blue-200"
          >
            <Edit2 className="h-4 w-4 mr-1.5" /> Chỉnh sửa
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setShowDeleteDialog(true)}
            className="h-9 px-3 text-red-600 hover:text-red-700 hover:bg-red-50 border-red-200"
          >
            <Trash2 className="h-4 w-4 mr-1.5" /> Xóa
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

      {/* Main Info Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Column 1: An toàn & Quy định */}
        <div className="space-y-6">
          {/* Card QTN-25 Pre-harvest Safety Alert */}
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

          {/* Nguồn quy định tham chiếu */}
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
        </div>

        {/* Column 2: Phạm vi áp dụng & Nhật ký */}
        <div className="space-y-6">
          {/* Loại nông sản áp dụng */}
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

          {/* Nhật ký & Khởi tạo */}
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
                  {material.createdBy ? `ID: ${material.createdBy}` : 'Quản trị viên nền tảng (VT-01)'}
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
      </div>

      {/* Form Modal Edit */}
      <InputMaterialFormModal
        open={showEditModal}
        onClose={() => setShowEditModal(false)}
        onSuccess={() => fetchDetail(material.id)}
        material={material}
      />

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
    </div>
  );
};
