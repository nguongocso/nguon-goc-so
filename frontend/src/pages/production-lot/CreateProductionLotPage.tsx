import {
  cloneProductionLot,
  createProductionLot,
  getCloneProductionLotPreview,
  getFarmAreaOptions,
  getProductionLots,
  getProductCategoryOptions,
} from '@/api/productionLotApi';
import CreateProductionLotForm from '@/components/production-lot/CreateProductionLotForm';
import { PRODUCTION_LOT_STATUS_LABELS } from '@/components/production-lot/ProductionLotStatusBadge';
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
  Info,
  LoaderCircle,
  RefreshCw,
  ShieldCheck,
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
const previewToInitialValues = (
  preview: CloneProductionLotPreview,
): CreateProductionLotRequest => ({
  name: preview.name,
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

  const handleSelectSource = (lotId: string) => {
    setSourceLotId(lotId);
    if (lotId) {
      void loadPreview(lotId);
    } else {
      // Bỏ chọn: giữ nguyên formKey để form giữ giá trị đã prefill,
      // chỉ mở khóa vùng trồng + loại nông sản.
      setPreview(null);
      setPreviewError('');
      setIsLoadingPreview(false);
    }
  };

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
        <div>
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-emerald-700">
            Quản lý sản xuất
          </p>
          <h1 className="text-3xl font-bold tracking-tight text-slate-900">Tạo lô sản xuất</h1>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600">
            Khai báo thông tin ban đầu cho một lô sản xuất mới. Lô sau khi tạo
            sẽ được lưu ở trạng thái Nháp.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <HelpButton screenKey="production-lot-create" />
          <div className="flex items-center gap-3 rounded-xl border border-blue-200 bg-blue-50 px-4 py-3">
            <ShieldCheck className="size-5 text-blue-700" />
            <div>
              <p className="text-xs font-bold text-blue-950">Phạm vi tổ chức</p>
              <p className="mt-0.5 text-xs text-blue-700">
                Quản lý hợp tác xã · VT-02
              </p>
            </div>
          </div>
        </div>
      </header>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <div className="space-y-6">
          <Card className="border-slate-200 bg-white shadow-sm">
            <CardContent className="space-y-2 p-5 sm:p-6">
              <Label htmlFor="sourceLotSelect" className="flex items-center gap-2">
                <Copy className="size-4 text-emerald-700" />
                Sao chép từ lô vụ trước (không bắt buộc)
              </Label>
              {isLoadingLots ? (
                <div className="flex items-center gap-2 py-2 text-sm text-slate-600">
                  <LoaderCircle className="size-4 animate-spin text-emerald-700" />
                  Đang tải danh sách lô sản xuất...
                </div>
              ) : lotsError ? (
                <div className="space-y-3">
                  <p className="text-sm font-semibold text-red-700">{lotsError}</p>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => void loadLots()}
                  >
                    <RefreshCw className="size-4" />
                    Thử lại
                  </Button>
                </div>
              ) : (
                <select
                  id="sourceLotSelect"
                  className={selectClassName}
                  value={sourceLotId}
                  onChange={(event) => handleSelectSource(event.target.value)}
                >
                  <option value="">Không sao chép — tạo lô hoàn toàn mới</option>
                  {lots.map((lot) => (
                    <option key={lot.id} value={lot.id}>
                      {lot.name} · {lot.farmAreaName ?? 'Chưa có vùng trồng'} ·{' '}
                      {PRODUCTION_LOT_STATUS_LABELS[lot.status] ?? lot.status}
                    </option>
                  ))}
                </select>
              )}
              <p className="text-xs text-slate-500">
                Chọn một lô sản xuất trước đó để tự điền sẵn vùng trồng, loại
                nông sản và thông tin vụ mới. Bỏ trống để khai báo từ đầu.
              </p>
            </CardContent>
          </Card>

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

          {preview && (
            <Card className="border-slate-200 bg-white shadow-sm">
              <CardContent className="space-y-4 p-5 sm:p-6">
                <h2 className="text-base font-bold text-slate-900">
                  Dữ liệu kế thừa từ lô “{preview.sourceLotName}”
                </h2>
                <dl className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <dt className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                      Vùng trồng
                    </dt>
                    <dd className="mt-1 text-sm font-semibold text-slate-800">
                      {preview.farmAreaName}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                      Loại nông sản
                    </dt>
                    <dd className="mt-1 text-sm font-semibold text-slate-800">
                      {preview.productCategoryName}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                      Sản lượng dự kiến
                    </dt>
                    <dd className="mt-1 text-sm font-semibold text-slate-800">
                      {preview.expectedQuantity} {preview.expectedQuantityUnit}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                      Ngày gieo trồng
                    </dt>
                    <dd className="mt-1 text-sm font-semibold text-slate-800">
                      {preview.plantingDate ?? '—'}
                    </dd>
                  </div>
                </dl>

                {preview.activeCertifications.length > 0 && (
                  <div className="flex gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
                    <BadgeCheck className="mt-0.5 size-5 shrink-0" />
                    <div>
                      <p className="font-bold">
                        {preview.activeCertifications.length} chứng nhận còn
                        hiệu lực sẽ được sao chép:
                      </p>
                      <ul className="mt-1 list-inside list-disc">
                        {preview.activeCertifications.map((cert) => (
                          <li key={cert.id}>
                            {cert.name} ({cert.code})
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>
                )}

                {preview.warnings.length > 0 && (
                  <div
                    role="alert"
                    className="flex gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800"
                  >
                    <AlertTriangle className="mt-0.5 size-5 shrink-0" />
                    <div>
                      <p className="font-bold">
                        {preview.skippedCertifications.length} chứng nhận hết
                        hạn sẽ không được sao chép:
                      </p>
                      <ul className="mt-1 list-inside list-disc">
                        {preview.skippedCertifications.map((cert) => (
                          <li key={cert.id}>
                            {cert.name} ({cert.code})
                          </li>
                        ))}
                      </ul>
                      {preview.warnings.map((warning) => (
                        <p key={warning} className="mt-1">
                          {warning}
                        </p>
                      ))}
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
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
              initialValues={
                preview ? previewToInitialValues(preview) : undefined
              }
              lockFarmAreaAndCategory={Boolean(preview)}
              submitLabel={preview ? 'Tạo lô từ mẫu' : 'Tạo lô sản xuất'}
              infoBanner={
                preview ? (
                  <div className="flex gap-3 rounded-xl border border-blue-200 bg-blue-50 p-4 text-sm text-blue-800">
                    <Info className="mt-0.5 size-5 shrink-0" />
                    <p>
                      <strong>
                        Lô này được tạo từ mẫu vụ trước “{preview.sourceLotName}
                        ”.
                      </strong>{' '}
                      Vùng trồng và loại nông sản được giữ nguyên; bạn có thể
                      chỉnh tên lô, sản lượng dự kiến và ngày gieo trồng của vụ
                      mới.
                    </p>
                  </div>
                ) : undefined
              }
              onCancel={() => navigate('/production-lots')}
              onSubmit={handleSubmit}
            />
          )}
        </div>

        <aside className="space-y-4">
          <Card className="border-slate-200 bg-white shadow-sm">
            <CardContent className="p-5">
              <p className="text-xs font-bold uppercase tracking-wider text-slate-400">
                Trạng thái sau khi tạo
              </p>
              <div className="mt-4 flex items-center gap-3">
                <span className="rounded-full bg-amber-100 px-3 py-1.5 text-xs font-bold text-amber-700">
                  Nháp
                </span>
                <span className="text-sm text-slate-600">
                  {preview ? 'Nghiệp vụ hoàn toàn mới' : 'Có thể cập nhật'}
                </span>
              </div>
              {preview ? (
                <p className="mt-4 text-sm leading-6 text-slate-500">
                  Nhật ký canh tác, sự kiện chuỗi, lô hàng, mã truy xuất và lịch
                  sử phê duyệt của lô mẫu không được sao chép.
                </p>
              ) : (
                <p className="mt-4 text-sm leading-6 text-slate-500">
                  Chọn vùng trồng đầy đủ trước khi gửi lô sang bước chờ duyệt.
                </p>
              )}
            </CardContent>
          </Card>

          {preview ? (
            <div className="flex gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-5 text-emerald-900">
              <ShieldCheck className="mt-0.5 size-5 shrink-0" />
              <div>
                <p className="text-sm font-bold">Phạm vi tổ chức</p>
                <p className="mt-2 text-sm leading-6 text-emerald-800">
                  Chỉ sao chép lô thuộc tổ chức của bạn. Tổ chức và người tạo
                  được xác định từ tài khoản đăng nhập.
                </p>
              </div>
            </div>
          ) : (
            <div className="flex gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-5 text-emerald-900">
              <Sprout className="mt-0.5 size-5 shrink-0" />
              <div>
                <p className="text-sm font-bold">Thông tin được bảo vệ</p>
                <p className="mt-2 text-sm leading-6 text-emerald-800">
                  Tổ chức và người tạo được xác định từ tài khoản đăng nhập,
                  không cần nhập lại trên biểu mẫu.
                </p>
              </div>
            </div>
          )}
        </aside>
      </div>
    </div>
  );
};

export default CreateProductionLotPage;