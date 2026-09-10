// @vitest-environment jsdom

import { beforeEach, describe, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { Toaster } from 'sonner';
import { CertificateVerificationDetailPage } from '@/pages/admin/CertificateVerificationDetailPage';
import type { CertificateVerification } from '@/types/certificateVerification';

const certificateApi = vi.hoisted(() => ({
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
  <MemoryRouter initialEntries={['/admin/certifications/certificate-1']}>
    <Toaster />
    <Routes>
      <Route
        path="/admin/certifications/:certificateId"
        element={<CertificateVerificationDetailPage />}
      />
    </Routes>
  </MemoryRouter>,
);

describe('CertificateVerificationDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    certificateApi.getCertificateVerification.mockResolvedValue(MOCK_CERTIFICATE);
    certificateApi.getCertificateDocument.mockResolvedValue(
      new Blob(['certificate'], { type: 'application/pdf' }),
    );
    certificateApi.verifyCertificate.mockResolvedValue({
      ...MOCK_CERTIFICATE,
      verificationStatus: 'VERIFIED',
      reviewedAt: '2026-09-09T09:15:00',
      reviewedBy: { userId: 'admin-1', fullName: 'Quản trị viên hệ thống' },
    });
    certificateApi.rejectCertificate.mockResolvedValue({
      ...MOCK_CERTIFICATE,
      verificationStatus: 'REJECTED',
      reviewedAt: '2026-09-09T09:20:00',
      rejectionReason: 'Số hiệu trên tệp không khớp với thông tin khai báo.',
    });
    Object.defineProperties(URL, {
      createObjectURL: {
        configurable: true,
        value: vi.fn(() => 'blob:certificate-document'),
      },
      revokeObjectURL: {
        configurable: true,
        value: vi.fn(),
      },
    });
  });

  it('hiển thị tệp và thông tin cần đối chiếu trên trang chi tiết riêng', async () => {
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Chi tiết chứng nhận' })).toBeInTheDocument();
    expect(await screen.findByTitle('Tệp chứng nhận VGP-2026-00125')).toBeInTheDocument();
    expect(screen.getAllByText('VietGAP').length).toBeGreaterThanOrEqual(2);
    expect(screen.getAllByText('HTX Nông sản Xanh').length).toBeGreaterThanOrEqual(2);
    expect(screen.getByText(/vietgap-2026\.pdf/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Xác thực$/ })).toBeEnabled();
    expect(certificateApi.getCertificateVerification).toHaveBeenCalledWith('certificate-1');
    expect(certificateApi.getCertificateDocument).toHaveBeenCalledWith('certificate-1');
  });

  it('gửi ghi chú khi quản trị viên xác thực', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: /^Xác thực$/ }));
    const dialog = await screen.findByRole('dialog');
    await user.type(
      within(dialog).getByLabelText('Ghi chú xác thực (không bắt buộc)'),
      'Thông tin khớp với tài liệu.',
    );
    await user.click(within(dialog).getByRole('button', { name: 'Xác nhận xác thực' }));

    await waitFor(() => {
      expect(certificateApi.verifyCertificate).toHaveBeenCalledWith('certificate-1', {
        reviewNote: 'Thông tin khớp với tài liệu.',
      });
    });
  });

  it('yêu cầu lý do hợp lệ và gửi quyết định từ chối', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: /^Từ chối$/ }));
    const dialog = await screen.findByRole('dialog');
    const reasonInput = within(dialog).getByLabelText('Lý do từ chối');
    await user.type(reasonInput, 'ngắn');
    await user.click(within(dialog).getByRole('button', { name: 'Xác nhận từ chối' }));
    expect(certificateApi.rejectCertificate).not.toHaveBeenCalled();

    await user.clear(reasonInput);
    await user.type(reasonInput, 'Số hiệu trên tệp không khớp với thông tin khai báo.');
    await user.click(within(dialog).getByRole('button', { name: 'Xác nhận từ chối' }));

    await waitFor(() => {
      expect(certificateApi.rejectCertificate).toHaveBeenCalledWith('certificate-1', {
        rejectionReason: 'Số hiệu trên tệp không khớp với thông tin khai báo.',
      });
    });
  });
});
