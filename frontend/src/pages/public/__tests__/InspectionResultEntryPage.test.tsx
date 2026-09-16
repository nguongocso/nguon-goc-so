import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { InspectionResultEntryPage } from '../InspectionResultEntryPage';
import * as api from '@/api/inspectionResultPortalApi';

vi.mock('@/api/inspectionResultPortalApi', () => ({
  getPublicPortalData: vi.fn(),
  uploadPortalResultFile: vi.fn(),
  submitPortalResults: vi.fn(),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('InspectionResultEntryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderWithToken = (tokenValue: string) => {
    return render(
      <MemoryRouter initialEntries={[`/inspection-result-entry/${tokenValue}`]}>
        <Routes>
          <Route path="/inspection-result-entry/:token" element={<InspectionResultEntryPage />} />
        </Routes>
      </MemoryRouter>
    );
  };

  it('renders portal data and form when token is valid', async () => {
    vi.mocked(api.getPublicPortalData).mockResolvedValueOnce({
      testingUnit: 'Trung tâm QUATEST 3',
      lotCode: 'LOT-2026-001',
      lotName: 'Lô Xoài Cát Chu',
      sampleSentDate: '2026-09-10',
      expiresAt: '2026-09-23T12:00:00',
      criteria: [
        {
          criterionId: 'crit-1',
          code: 'PB-01',
          name: 'Dư lượng chì',
          standardName: 'QCVN 8-2:2011/BYT',
        },
      ],
    });

    renderWithToken('valid-token-123');

    await waitFor(() => {
      expect(screen.getByText('Trung tâm QUATEST 3')).toBeInTheDocument();
      expect(screen.getByText('Lô Xoài Cát Chu')).toBeInTheDocument();
      expect(screen.getByText('Dư lượng chì')).toBeInTheDocument();
    });
  });

  it('renders expired message on HTTP 410 Expired (TC-02)', async () => {
    const error = {
      response: {
        status: 410,
        data: {
          message:
            'Liên kết nhập kết quả đã hết hạn. Vui lòng liên hệ hợp tác xã để được cấp liên kết mới.',
        },
      },
    };
    vi.mocked(api.getPublicPortalData).mockRejectedValueOnce(error);

    renderWithToken('expired-token');

    await waitFor(() => {
      expect(screen.getAllByText(/liên kết nhập kết quả đã hết hạn/i).length).toBeGreaterThan(0);
      expect(
        screen.getByText(/vui lòng liên hệ hợp tác xã để được cấp liên kết mới/i)
      ).toBeInTheDocument();
    });
  });

  it('renders used message on HTTP 410 Used (TC-03)', async () => {
    const error = {
      response: {
        status: 410,
        data: {
          message: 'Liên kết này đã được sử dụng để nhập kết quả trước đó.',
        },
      },
    };
    vi.mocked(api.getPublicPortalData).mockRejectedValueOnce(error);

    renderWithToken('used-token');

    await waitFor(() => {
      expect(screen.getByText(/kết quả kiểm nghiệm đã được ghi nhận/i)).toBeInTheDocument();
      expect(
        screen.getByText(/liên kết này đã được sử dụng để nhập kết quả trước đó/i)
      ).toBeInTheDocument();
    });
  });

  it('renders not found message on HTTP 404', async () => {
    const error = {
      response: {
        status: 404,
        data: {
          message: 'Không tìm thấy liên kết nhập kết quả.',
        },
      },
    };
    vi.mocked(api.getPublicPortalData).mockRejectedValueOnce(error);

    renderWithToken('invalid-token');

    await waitFor(() => {
      expect(screen.getByText(/liên kết không hợp lệ/i)).toBeInTheDocument();
    });
  });
});
