import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';

import { DossierPreviewDialog } from '../DossierPreviewDialog';

import { exportDossier } from '@/api/dossierApi';
import { exportShipmentWithTemplate } from '@/api/exportApi';
import { getOpenDataPreview } from '@/api/profileTemplateApi';

vi.mock('@/api/dossierApi', () => ({
  exportDossier: vi.fn(),
}));

vi.mock('@/api/exportApi', () => ({
  exportShipmentWithTemplate: vi.fn(),
}));

vi.mock('@/api/profileTemplateApi', () => ({
  getOpenDataPreview: vi.fn(),
}));

describe('DossierPreviewDialog Component Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    global.URL.createObjectURL = vi.fn().mockReturnValue('blob:http://localhost/test-preview-blob');
    global.URL.revokeObjectURL = vi.fn();
  });

  it('renders PDF preview with iframe when activeFormat is pdf', async () => {
    const mockPdfBlob = new Blob(['%PDF-1.4 mock content'], { type: 'application/pdf' });
    vi.mocked(exportDossier).mockResolvedValue(mockPdfBlob);

    render(
      <DossierPreviewDialog
        open={true}
        onClose={vi.fn()}
        shipmentId="ship-100"
        shipmentName="Lô dâu tây Đà Lạt"
        activeFormat="pdf"
      />
    );

    expect(await screen.findByTitle('Bản in PDF hồ sơ truy xuất')).toBeInTheDocument();
    expect(exportDossier).toHaveBeenCalledWith('ship-100', undefined);
    expect(global.URL.createObjectURL).toHaveBeenCalledWith(mockPdfBlob);
  });

  it('renders JSON preview with formatted text when activeFormat is json', async () => {
    const mockJsonData = {
      shipmentId: 'ship-100',
      shipmentName: 'Lô dâu tây Đà Lạt',
      status: 'ACTIVATED',
    };
    vi.mocked(getOpenDataPreview).mockResolvedValue(mockJsonData);

    render(
      <DossierPreviewDialog
        open={true}
        onClose={vi.fn()}
        shipmentId="ship-100"
        shipmentName="Lô dâu tây Đà Lạt"
        activeFormat="json"
      />
    );

    expect(await screen.findByText(/application\/json/i)).toBeInTheDocument();
    expect(await screen.findByText(/"status": "ACTIVATED"/i)).toBeInTheDocument();
    expect(getOpenDataPreview).toHaveBeenCalledWith('ship-100', undefined);
  });

  it('renders CSV preview table when activeFormat is csv', async () => {
    const csvContent = '# THÔNG TIN CHUNG\nSTT,Trường dữ liệu,Giá trị\n1,Mã lô,LO-001\n';
    const mockCsvBlob = new Blob([csvContent], { type: 'text/csv' });
    vi.mocked(exportShipmentWithTemplate).mockResolvedValue(mockCsvBlob);

    render(
      <DossierPreviewDialog
        open={true}
        onClose={vi.fn()}
        shipmentId="ship-100"
        shipmentName="Lô dâu tây Đà Lạt"
        activeFormat="csv"
      />
    );

    expect(await screen.findByText('THÔNG TIN CHUNG')).toBeInTheDocument();
    expect(screen.getByText('Trường dữ liệu')).toBeInTheDocument();
    expect(screen.getByText('LO-001')).toBeInTheDocument();
    expect(exportShipmentWithTemplate).toHaveBeenCalledWith('ship-100', undefined, 'csv');
  });

  it('displays error message when API call fails', async () => {
    vi.mocked(exportDossier).mockRejectedValue(new Error('Lỗi kết nối máy chủ'));

    render(
      <DossierPreviewDialog
        open={true}
        onClose={vi.fn()}
        shipmentId="ship-100"
        activeFormat="pdf"
      />
    );

    expect(await screen.findByText('Không thể tạo bản xem trước')).toBeInTheDocument();
    expect(screen.getByText('Lỗi kết nối máy chủ')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Thử tải lại/i })).toBeInTheDocument();
  });

  it('cleans up object URL when modal is closed or unmounted', async () => {
    const mockPdfBlob = new Blob(['%PDF-1.4 mock content'], { type: 'application/pdf' });
    vi.mocked(exportDossier).mockResolvedValue(mockPdfBlob);

    const { unmount } = render(
      <DossierPreviewDialog
        open={true}
        onClose={vi.fn()}
        shipmentId="ship-100"
        activeFormat="pdf"
      />
    );

    await waitFor(() => {
      expect(global.URL.createObjectURL).toHaveBeenCalled();
    });

    unmount();

    expect(global.URL.revokeObjectURL).toHaveBeenCalledWith('blob:http://localhost/test-preview-blob');
  });
});
