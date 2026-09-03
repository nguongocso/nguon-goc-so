import CreateProductionLotForm from '@/components/production-lot/CreateProductionLotForm';
import {
  createProductionLot,
  getFarmAreaOptions,
  getProductCategoryOptions,
} from '@/api/productionLotApi';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import type {
  CreateProductionLotRequest,
  FarmAreaOption,
  ProductCategoryOption,
} from '@/types/productionLot';
import { HelpButton } from '@/components/help/HelpButton';
import { Info, LoaderCircle, RefreshCw, ShieldCheck } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const CreateProductionLotPage = () => {
  const navigate = useNavigate();
  const [farmAreas, setFarmAreas] = useState<FarmAreaOption[]>([]);
  const [productCategories, setProductCategories] = useState<
    ProductCategoryOption[]
  >([]);
  const [isLoadingOptions, setIsLoadingOptions] = useState(true);
  const [loadError, setLoadError] = useState('');

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

  useEffect(() => {
    void loadOptions();
  }, [loadOptions]);

  const handleSubmit = async (payload: CreateProductionLotRequest) => {
    await createProductionLot(payload);
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
            farmAreas={farmAreas}
            productCategories={productCategories}
            onCancel={() => navigate(-1)}
            onSubmit={handleSubmit}
          />
        )}

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
                <span className="text-sm text-slate-600">Có thể cập nhật</span>
              </div>
              <p className="mt-4 text-sm leading-6 text-slate-500">
                Chọn vùng trồng đầy đủ trước khi gửi lô sang bước chờ duyệt.
              </p>
            </CardContent>
          </Card>

          <div className="flex gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-5 text-emerald-900">
            <Info className="mt-0.5 size-5 shrink-0" />
            <div>
              <p className="text-sm font-bold">Thông tin được bảo vệ</p>
              <p className="mt-2 text-sm leading-6 text-emerald-800">
                Tổ chức và người tạo được xác định từ tài khoản đăng nhập,
                không cần nhập lại trên biểu mẫu.
              </p>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
};

export default CreateProductionLotPage;
