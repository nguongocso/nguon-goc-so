import { ProductionLotEditForm } from "@/components/farm/ProductionLotEditForm";
import { HelpButton } from "@/components/help/HelpButton";

const ProductionLotEditPage: React.FC = () => {
  return (
    <div className="container mx-auto py-8">
      <div className="mb-6 flex justify-end">
        <HelpButton screenKey="production-lot-edit" />
      </div>
      <ProductionLotEditForm />
    </div>
  );
}

export default ProductionLotEditPage;