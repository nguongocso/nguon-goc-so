import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import CertificationListPage from '../CertificationListPage';
import * as certificationApi from '@/api/certificationApi';

vi.mock('@/api/certificationApi', () => ({
  getCertifications: vi.fn(),
}));

vi.mock('@/hooks/usePermission', () => ({
  usePermission: () => ({
    canCreateCertification: true,
  }),
}));

vi.mock('@/components/help/HelpButton', () => ({
  HelpButton: () => <button data-testid="help-button">Hướng dẫn</button>,
}));

vi.mock('@/components/certification/CertificationDetailDialog', () => ({
  CertificationDetailDialog: ({ open, certification, onClose }: any) =>
    open ? (
      <div data-testid="cert-detail-dialog">
        <span>Chi tiết chứng nhận: {certification?.id}</span>
        <button onClick={onClose}>Đóng</button>
      </div>
    ) : null,
}));

const mockCertifications = [
  {
    id: 'cert-1',
    name: 'Chứng nhận VietGAP',
    code: 'VG-2026-001',
    issuedBy: 'Chi cục Trồng trọt và BVTV',
    issueDate: '2026-01-15T00:00:00',
    expiryDate: '2027-01-15T00:00:00',
    isValid: true,
  },
  {
    id: 'cert-2',
    name: 'Chứng nhận GlobalGAP',
    code: 'GG-2026-002',
    issuedBy: 'Tổ chức Chứng nhận Quốc tế',
    issueDate: '2025-06-01T00:00:00',
    expiryDate: '2026-06-01T00:00:00',
    isValid: false,
  },
];

describe('CertificationListPage UI and Button Standardization Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(certificationApi.getCertifications).mockResolvedValue({
      items: mockCertifications as any,
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    });
  });

  it('hiển thị danh sách chứng nhận với nút "Chi tiết" chuẩn hệ thống', async () => {
    render(
      <MemoryRouter>
        <CertificationListPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('Quản lý chứng nhận')).toBeInTheDocument();
    expect(screen.getByText('Chứng nhận VietGAP')).toBeInTheDocument();
    expect(screen.getByText('Chứng nhận GlobalGAP')).toBeInTheDocument();

    // Nút thao tác đã đổi thành "Chi tiết"
    const detailButtons = screen.getAllByRole('button', { name: 'Chi tiết' });
    expect(detailButtons).toHaveLength(2);

    // Không còn nút mang chữ "Xem" riêng lẻ
    expect(screen.queryByRole('button', { name: 'Xem' })).not.toBeInTheDocument();
  });

  it('mở modal chi tiết khi click vào nút "Chi tiết"', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <CertificationListPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('Chứng nhận VietGAP')).toBeInTheDocument();

    const detailButtons = screen.getAllByRole('button', { name: 'Chi tiết' });
    await user.click(detailButtons[0]);

    await waitFor(() => {
      expect(screen.getByTestId('cert-detail-dialog')).toBeInTheDocument();
      expect(screen.getByText('Chi tiết chứng nhận: cert-1')).toBeInTheDocument();
    });
  });
});
