// @vitest-environment jsdom

import { beforeEach, describe, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { CertificateVerificationPage } from '@/pages/admin/CertificateVerificationPage';
import type { CertificateVerification } from '@/types/certificateVerification';

const certificateApi = vi.hoisted(() => ({
  getCertificateVerifications: vi.fn(),
  getCertificateVerification: vi.fn(),
  getCertificateDocument: vi.fn(),
  verifyCertificate: vi.fn(),
  rejectCertificate: vi.fn(),
}));

vi.mock('@/api/certificateVerificationApi', () => certificateApi);

const MOCK_CERTIFICATE: CertificateVerification = {
  id: 'certificate-1',
  organizationId: 'organization-1',
  organizationName: 'HTX Nông sản Xanh',
  standardId: 'standard-1',
  standardName: 'VietGAP',
  code: 'VGP-2026-00125',
  issuedBy: 'Trung tâm Chứng nhận Chất lượng',
  issueDate: '2026-01-15',
  expiryDate: '2027-01-14',
  verificationStatus: 'PENDING',
  validityStatus: 'VALID',
  document: {
    fileName: 'vietgap-2026.pdf',
    contentType: 'application/pdf',
    fileSize: 248320,
    viewUrl: '/api/v1/admin/certifications/certificate-1/document',
  },
  reviewedBy: null,
  reviewedAt: null,
  reviewNote: null,
  rejectionReason: null,
  createdAt: '2026-09-09T08:00:00',
  updatedAt: null,
};

const renderPage = () => render(
  <MemoryRouter initialEntries={['/admin/certifications']}>
    <Routes>
      <Route path="/admin/certifications" element={<CertificateVerificationPage />} />
      <Route path="/admin/certifications/:certificateId" element={<div>Trang chi tiết</div>} />
    </Routes>
  </MemoryRouter>,
);

describe('CertificateVerificationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    certificateApi.getCertificateVerifications.mockResolvedValue({
      items: [MOCK_CERTIFICATE],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });
  });

  it('hiển thị danh sách chứng nhận theo dạng bảng', async () => {
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Xác thực chứng nhận' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Số hiệu' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Tổ chức' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Thao tác' })).toBeInTheDocument();
    expect(await screen.findByText('VGP-2026-00125')).toBeInTheDocument();
    expect(screen.getByText('HTX Nông sản Xanh')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Chi tiết/ })).toBeEnabled();
    expect(certificateApi.getCertificateVerifications).toHaveBeenCalledWith(
      expect.objectContaining({ verificationStatus: 'PENDING', page: 0, size: 10 }),
    );
    expect(certificateApi.getCertificateVerification).not.toHaveBeenCalled();
    expect(certificateApi.getCertificateDocument).not.toHaveBeenCalled();
  });

  it('mở trang chi tiết khi chọn nút Chi tiết', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: /Chi tiết/ }));

    expect(await screen.findByText('Trang chi tiết')).toBeInTheDocument();
  });
});
