import React from 'react';
import { CertificateVerificationList } from '@/components/admin/certificate-verification/CertificateVerificationList';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';

export const CertificateVerificationPage: React.FC = () => {
  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Xác thực chứng nhận' },
  ]);

  return (
    <div className="space-y-6">
      <CertificateVerificationList />
    </div>
  );
};

export default CertificateVerificationPage;
