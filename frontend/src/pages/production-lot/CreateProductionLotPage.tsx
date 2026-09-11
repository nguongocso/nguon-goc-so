import {
  cloneProductionLot,
  createProductionLot,
  getCloneProductionLotPreview,
  getFarmAreaOptions,
  getProductionLots,
  getProductCategoryOptions,
} from '@/api/productionLotApi';
import CreateProductionLotForm from '@/components/production-lot/CreateProductionLotForm';

import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import type {
  CloneProductionLotPreview,
  CreateProductionLotRequest,
  FarmAreaOption,
  ProductCategoryOption,
  ProductionLot,
} from '@/types/productionLot';
import { HelpButton } from '@/components/help/HelpButton';
import axios from 'axios';
import {
  AlertTriangle,
  BadgeCheck,
  Copy,
  LoaderCircle,
  RefreshCw,
  Sprout,
} from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';

const selectClassName =
  'h-10 w-full rounded-lg border border-input bg-white px-3 text-sm outline-none transition focus:border-emerald-600 focus:ring-3 focus:ring-emerald-100';

/**
 * Chuyển dữ liệu preview của lô mẫu thành giá trị khởi tạo của form.
 * (NCL-02-CN-007: tạo lô từ mẫu vụ trước)
 */
const incrementSeasonName = (name: string): string => {
  const baseName = name.replace(/\s*[vV]ụ\s*\d+$/i, '').trim();
  const match = name.match(/\s*[vV]ụ\s*(\d+)$/i);
  const nextNum = match ? (parseInt(match[1], 10) + 1).toString() : '2';
  return `${baseName} vụ ${nextNum}`;
};

const previewToInitialValues = (
  preview: CloneProductionLotPreview,
): CreateProductionLotRequest => ({
  name: incrementSeasonName(preview.name),
  farmAreaId: preview.farmAreaId,
  productCategoryId: preview.productCategoryId,
  expectedQuantity: preview.expectedQuantity,
  expectedQuantityUnit: preview.expectedQuantityUnit,
  plantingDate: preview.plantingDate,
});

const CreateProductionLotPage = () => {
  const navigate = useNavigate();
  const [farmAreas, setFarmAreas] = useState<FarmAreaOption[]>([]);
  const [productCategories, setProductCategories] = useState<
    ProductCategoryOption[]
  >([]);
  const [isLoadingOptions, setIsLoadingOptions] = useState(true);
  const [loadError, setLoadError] = useState('');

  const [lots, setLots] = useState<ProductionLot[]>([]);
  const [isLoadingLots, setIsLoadingLots] = useState(true);
  const [lotsError, setLotsError] = useState('');

  // NCL-02-CN-007: chọn lô mẫu vụ trước để tạo nhanh vụ mới (không bắt buộc).
  const [sourceLotId, setSourceLotId] = useState('');
  const [preview, setPreview] = useState<CloneProductionLotPreview | null>(null);
  const [isLoadingPreview, setIsLoadingPreview] = useState(false);
  const [previewError, setPreviewError] = useState('');
  /**
   * Key của form. Khi chọn lô mẫu mới, thay đổi key để form remount và
   * prefill lại. Khi bỏ chọn lô mẫu, giữ nguyên key để form giữ giá trị đã
   * điền, chỉ mở khóa vùng trồng + loại nông sản.
   */
  const [formKey, setFormKey] = useState('fresh');

  const loadOptions = useCallback(async () => {
    setIsLoadingOptions(true);
    setLoadError('');

    try {
      const [farmAreaData, productCategoryData] = await Promise.all([
        getFarmAreaOptions(),
        getProductCategoryOptions(),
      ]);

      setFarmAreas(farmAreaData);
      setProductCategories(productCategoryData);
    } catch {
      setLoadError(
        'Không thể tải vùng trồng hoặc loại nông sản. Vui lòng thử lại.',
      );
    } finally {
      setIsLoadingOptions(false);
    }
  }, []);

  const loadLots = useCallback(async () => {
    setIsLoadingLots(true);
    setLotsError('');
    try {
      const data = await getProductionLots();
      setLots(data);
    } catch {
      setLotsError('Không thể tải danh sách lô sản xuất. Vui lòng thử lại.');
    } finally {
      setIsLoadingLots(false);
    }
  }, []);

  useEffect(() => {
    void loadOptions();
    void loadLots();
  }, [loadOptions, loadLots]);

  const loadPreview = useCallback(async (lotId: string) => {
    setIsLoadingPreview(true);
    setPreviewError('');
    setPreview(null);
    try {
      const data = await getCloneProductionLotPreview(lotId);
      setPreview(data);
      // Hiển thị thông báo khi sao chép thông tin từ lô mẫu.
      toast.success(`Đã sao chép thông tin từ lô “${data.sourceLotName}”.`);
      // Remount form với giá trị prefill từ lô mẫu, khóa vùng trồng + nông sản.
      setFormKey(data.sourceLotId);
    } catch (error: unknown) {
      if (axios.isAxiosError<{ message?: string }>(error)) {
        setPreviewError(
          error.response?.data?.message ||
            'Không thể tải dữ liệu lô mẫu. Vui lòng chọn một lô mẫu khác.',
        );
      } else {
        setPreviewError('Không thể kết nối đến máy chủ. Vui lòng thử lại.');
      }
    } finally {
      setIsLoadingPreview(false);
    }
  }, []);

  const handleSubmit = async (payload: CreateProductionLotRequest) => {
    if (preview) {
      const result = await cloneProductionLot(preview.sourceLotId, {
        name: payload.name,
        expectedQuantity: payload.expectedQuantity,
        expectedQuantityUnit: payload.expectedQuantityUnit,
        plantingDate: payload.plantingDate,
      });
      toast.success('Đã tạo lô sản xuất mới từ vụ trước ở trạng thái Nháp.');
      if (result.warnings.length > 0) {
        toast.warning(
          `Đã bỏ qua ${result.skippedCertifications.length} chứng nhận hết hạn.`,
        );
      }
      navigate(`/production-lots?highlightId=${result.lot.id}`);
    } else {
      await createProductionLot(payload);
    }
  };

  return (
    <div className="space-y-6">
      <header className="flex flex-col justify-between gap-5 md:flex-row md:items-end">
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-emerald-700">
              Quản lý sản xuất
            </p>
            <h1 className="text-3xl font-bold tracking-tight text-slate-900">Tạo lô sản xuất mới</h1>
            <p className="mt-1 max-w-2xl text-xs leading-5 text-slate-500">
              Khai báo thông tin ban đầu cho một lô sản xuất mới. Lô sau khi tạo sẽ được lưu ở trạng thái Nháp.
            </p>
          </div>
          <span className="inline-flex items-center rounded-full bg-amber-100 px-3 py-1 text-xs font-bold text-amber-700">
            Nháp
          </span>
        </div>

        <div className="flex items-center gap-3">
          <HelpButton screenKey="production-lot-create" />
        </div>
      </header>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <div className="space-y-6">
          {isLoadingPreview && (
            <Card className="border-slate-200 bg-white shadow-sm">
              <CardContent className="grid min-h-40 place-items-center p-8 text-center">
                <div>
                  <LoaderCircle className="mx-auto size-8 animate-spin text-emerald-700" />
                  <p className="mt-4 font-semibold">Đang tải dữ liệu lô mẫu...</p>
                </div>
              </CardContent>
            </Card>
          )}

          {previewError && (
            <div
              role="alert"
              className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"
            >
              <p className="font-bold">Không thể tạo lô từ mẫu này</p>
              <p className="mt-1">{previewError}</p>
              <p className="mt-1">Vui lòng quay lại và chọn một lô mẫu hợp lệ khác.</p>
            </div>
          )}

          {isLoadingOptions ? (
            <Card className="border-slate-200 bg-white shadow-sm">
              <CardContent className="grid min-h-80 place-items-center p-8 text-center">
                <div>
                  <LoaderCircle className="mx-auto size-8 animate-spin text-emerald-700" />
                  <p className="mt-4 font-semibold">Đang tải dữ liệu biểu mẫu...</p>
                </div>
              </CardContent>
            </Card>
          ) : loadError ? (
            <Card className="border-red-200 bg-white shadow-sm">
              <CardContent className="grid min-h-80 place-items-center p-8 text-center">
                <div>
                  <p className="font-semibold text-red-700">{loadError}</p>
                  <Button
                    type="button"
                    variant="outline"
                    className="mt-4"
                    onClick={() => void loadOptions()}
                  >
                    <RefreshCw className="size-4" />
                    Thử lại
                  </Button>
                </div>
              </CardContent>
            </Card>
          ) : (
            <CreateProductionLotForm
              key={formKey}
              farmAreas={farmAreas}
              productCategories={productCategories}
              previousLots={lots}
              isLoadingPreviousLots={isLoadingLots}
              initialValues={
                preview ? previewToInitialValues(preview) : undefined
              }
              lockFarmAreaAndCategory={Boolean(preview)}
              submitLabel="Tạo"
              infoBanner={undefined}
              onSelectPreviousLot={(lotId) => {
                setSourceLotId(lotId);
                if (lotId) {
                  void loadPreview(lotId);
                } else {
                  setPreview(null);
                  setPreviewError('');
                  setIsLoadingPreview(false);
                }
              }}
              onCancelCopy={() => {
                setSourceLotId('');
                setPreview(null);
                setPreviewError('');
                setIsLoadingPreview(false);
              }}
              onCancel={() => navigate('/production-lots')}
              onSubmit={handleSubmit}
            />
          )}
        </div>

        <aside className="space-y-4">
          <Card className="border-slate-200 bg-white shadow-sm">
            <CardContent className="space-y-4 p-5">
              <div>
                <p className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Thông tin bổ sung
                </p>
                <div className="mt-3 flex items-center gap-3">
                  <span className="rounded-full bg-amber-100 px-3 py-1.5 text-xs font-bold text-amber-700">
                    Nháp
                  </span>
                  <span className="text-sm text-slate-600">
                    {preview ? 'Nghiệp vụ hoàn toàn mới' : 'Có thể cập nhật'}
                  </span>
                </div>
              </div>

              <div className="text-sm leading-6 text-slate-500">
                {preview ? (
                  <>
                    <p>Nhật ký canh tác, sự kiện chuỗi, lô hàng, mã truy xuất và lịch sử phê duyệt của lô mẫu không được sao chép.</p>
                    <p className="mt-2">Vùng trồng và loại nông sản được giữ nguyên từ lô mẫu.</p>
                  </>
                ) : (
                  <p>Chọn vùng trồng đầy đủ trước khi gửi lô sang bước chờ duyệt.</p>
                )}
              </div>

              {!preview && (
                <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
                  <p className="font-bold">Thông tin được bảo vệ</p>
                  <p className="mt-1">Tổ chức và người tạo được xác định từ tài khoản đăng nhập, không cần nhập lại trên biểu mẫu.</p>
                </div>
              )}
            </CardContent>
          </Card>
        </aside>
      </div>
    </div>
  );
};

export default CreateProductionLotPage;