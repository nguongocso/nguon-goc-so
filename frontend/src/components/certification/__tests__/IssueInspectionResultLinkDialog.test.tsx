import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { IssueInspectionResultLinkDialog } from '../IssueInspectionResultLinkDialog';
import * as api from '@/api/inspectionResultPortalApi';

vi.mock('@/api/inspectionResultPortalApi', () => ({
  issueInspectionResultEntryLink: vi.fn(),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('IssueInspectionResultLinkDialog', () => {
  const defaultProps = {
    requestId: 'req-12345',
    testingUnitName: 'Trung tâm QUATEST 3',
    defaultEmail: 'lab@quatest3.vn',
    isOpen: true,
    onClose: vi.fn(),
    onSuccess: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders dialog form when open', () => {
    render(<IssueInspectionResultLinkDialog {...defaultProps} />);

    expect(
      screen.getByText(/cấp liên kết nhập kết quả cho đơn vị kiểm nghiệm/i)
    ).toBeInTheDocument();
    expect(screen.getByDisplayValue('lab@quatest3.vn')).toBeInTheDocument();
    expect(screen.getByDisplayValue('7')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /cấp và gửi liên kết/i })).toBeInTheDocument();
  });

  it('keeps the success screen open and refreshes the parent only after completion', async () => {
    const mockResponse = {
      id: 'link-123',
      status: 'ACTIVE' as const,
      recipientEmail: 'lab@quatest3.vn',
      expiresAt: '2026-09-23T12:00:00',
      createdAt: '2026-09-16T12:00:00',
      entryUrl: 'https://nguongocso.vn/inspection-result-entry/secret-token-xyz',
    };

    vi.mocked(api.issueInspectionResultEntryLink).mockResolvedValueOnce(mockResponse);

    render(<IssueInspectionResultLinkDialog {...defaultProps} />);

    fireEvent.click(screen.getByRole('button', { name: /cấp và gửi liên kết/i }));

    await waitFor(() => {
      expect(api.issueInspectionResultEntryLink).toHaveBeenCalledWith('req-12345', {
        recipientEmail: 'lab@quatest3.vn',
        expiryDays: 7,
      });
    });

    expect(screen.getByText(/cấp liên kết thành công/i)).toBeInTheDocument();
    expect(
      screen.getByDisplayValue('https://nguongocso.vn/inspection-result-entry/secret-token-xyz')
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sao chép/i })).toBeInTheDocument();
    expect(defaultProps.onSuccess).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: /hoàn tất/i }));

    expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
    expect(defaultProps.onSuccess).toHaveBeenCalledWith(mockResponse);
  });
});
