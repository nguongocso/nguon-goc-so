import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { DossierPreviewDialog } from '../DossierPreviewDialog';
import * as profileTemplateApi from '@/api/profileTemplateApi';
import * as dossierApi from '@/api/dossierApi';

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      userId: 'user-01',
      fullName: 'Tráng Văn C',
      organizationId: 'org-test-123',
      role: 'VT-02',
    },
  }),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
    loading: vi.fn(),
    dismiss: vi.fn(),
  },
}));

describe('DossierPreviewDialog - Đồng bộ giao diện và logic form xem trước', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    // Mock URL.createObjectURL và revokeObjectURL
    window.URL.createObjectURL = vi.fn().mockReturnValue('blob:http://localhost/test-pdf-url');
    window.URL.revokeObjectURL = vi.fn();
    window.open = vi.fn();
  });

  it('gọi previewTemplatePdf và render iframe PDF khi ở chế độ tạo mẫu hồ sơ mới', async () => {
    const fakeBlob = new Blob(['%PDF-1.4 test content'], { type: 'application/pdf' });
    const spyPreviewPdf = vi
      .spyOn(profileTemplateApi, 'previewTemplatePdf')
      .mockResolvedValue(fakeBlob);

    render(
      <DossierPreviewDialog
        open={true}
        onClose={vi.fn()}
        templateName="Mẫu hồ sơ mới"
        partnerName="AEON Mall"
        selectedFieldKeys={['organization.name', 'productionLot.name']}
        activeFormat="pdf"
        initialData={{
          organization: { name: 'HTX Nông nghiệp Xanh' },
        }}
      />
    );

    // Chờ gọi previewTemplatePdf
    await waitFor(() => {
      expect(spyPreviewPdf).toHaveBeenCalledWith(
        'org-test-123',
        expect.objectContaining({
          name: 'Mẫu hồ sơ mới',
          partnerName: 'AEON Mall',
          selectedFieldKeys: ['organization.name', 'productionLot.name'],
        })
      );
    });

    // Kiểm tra header hiển thị đầy đủ thông tin mẫu và lô hàng mẫu
    expect(screen.getByText('Bản xem trước xuất hồ sơ')).toBeInTheDocument();
    expect(screen.getByText('Mẫu hồ sơ mới')).toBeInTheDocument();
    expect(screen.getByText('SHIP-MOCK-2026-DEMO')).toBeInTheDocument();

    // Kiểm tra hiển thị iframe chứa PDF thay vì HTML mô phỏng
    await waitFor(() => {
      const iframe = screen.getByTitle('Bản in PDF hồ sơ truy xuất');
      expect(iframe).toBeInTheDocument();
      expect(iframe).toHaveAttribute('src', 'blob:http://localhost/test-pdf-url#toolbar=1&navpanes=0&view=Fit');
    });

    // Kiểm tra footer có đầy đủ các nút
    expect(screen.getByText('Nội dung xem trước trùng khớp 100% với tệp tải về')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Mở tab mới/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Tải tệp này về máy \(PDF\)/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Đóng/i })).toBeInTheDocument();
  });

  it('mở tab mới khi người dùng bấm nút "Mở tab mới" cho PDF', async () => {
    const fakeBlob = new Blob(['%PDF-1.4 test content'], { type: 'application/pdf' });
    vi.spyOn(profileTemplateApi, 'previewTemplatePdf').mockResolvedValue(fakeBlob);

    render(
      <DossierPreviewDialog
        open={true}
        onClose={vi.fn()}
        templateName="Mẫu xuất khẩu"
        activeFormat="pdf"
        selectedFieldKeys={['organization.name']}
        initialData={{ organization: { name: 'HTX' } }}
      />
    );

    const openTabBtn = await screen.findByRole('button', { name: /Mở tab mới/i });
    fireEvent.click(openTabBtn);

    expect(window.open).toHaveBeenCalledWith('blob:http://localhost/test-pdf-url', '_blank');
  });

  it('ở chế độ xem trước của lô hàng thực tế gọi exportDossier và hiển thị tên lô hàng', async () => {
    const fakeBlob = new Blob(['%PDF-1.4 real shipment content'], { type: 'application/pdf' });
    const spyExport = vi.spyOn(dossierApi, 'exportDossier').mockResolvedValue(fakeBlob);

    render(
      <DossierPreviewDialog
        open={true}
        onClose={vi.fn()}
        shipmentId="ship-123"
        shipmentName="Lô Cà Rốt VietGAP"
        templateName="Mẫu mặc định"
        activeFormat="pdf"
      />
    );

    await waitFor(() => {
      expect(spyExport).toHaveBeenCalledWith('ship-123', undefined);
    });

    expect(screen.getByText('Lô Cà Rốt VietGAP')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Tải tệp này về máy \(PDF\)/i })).toBeInTheDocument();
  });
});
