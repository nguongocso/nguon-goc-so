import { ProductionLotBoard } from '@/components/production-lot/ProductionLotBoard';

const ProductionLotListPage = () => {
  return (
    <main className="min-h-screen bg-slate-50 px-4 py-8 text-slate-900 md:px-8">
      <div className="mx-auto max-w-7xl space-y-6">
        <header>
          <p className="mb-2 text-xs font-semibold uppercase tracking-[0.18em] text-emerald-700">
            Quản lý sản xuất
          </p>
          <h1 className="text-2xl font-bold tracking-tight md:text-3xl">
            Lô sản xuất
          </h1>
          <p className="mt-2 text-sm text-slate-500">
            Quản lý các lô sản xuất thuộc phạm vi tổ chức của bạn.
          </p>
        </header>

        <ProductionLotBoard />
      </div>
    </main>
  );
};

export default ProductionLotListPage;