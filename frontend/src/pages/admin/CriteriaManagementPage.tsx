import React from "react";
import { useParams } from "react-router-dom";
import { CriterionList } from "@/components/admin/CriterionList";

const CriteriaManagementPage: React.FC = () => {
  const { standardId } = useParams<{ standardId: string }>();

  return (
    <div className="container mx-auto py-6 space-y-6">
      <CriterionList standardId={standardId} />
    </div>
  );
};

export default CriteriaManagementPage;