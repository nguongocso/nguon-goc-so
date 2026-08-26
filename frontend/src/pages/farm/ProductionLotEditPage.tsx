import React from 'react';
import { ProductionLotEditForm } from "@/components/farm/ProductionLotEditForm";
import { HelpButton } from "@/components/help/HelpButton";
import { Edit } from "lucide-react";

const ProductionLotEditPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Edit className="size-6 text-emerald-600" />
            Chỉnh sửa lô sản xuất
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Cập nhật thông tin chi tiết của lô sản xuất khi đang ở trạng thái bản nháp.
          </p>
        </div>
        <HelpButton screenKey="production-lot-edit" />
      </div>
      <ProductionLotEditForm />
    </div>
  );
};

export default ProductionLotEditPage;