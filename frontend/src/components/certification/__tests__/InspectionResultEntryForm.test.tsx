import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { InspectionResultEntryForm } from '../InspectionResultEntryForm';

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('InspectionResultEntryForm', () => {
  const mockCriteria = [
    {
      criterionId: 'crit-1',
      code: 'PB-01',
      name: 'Dư lượng chì',
      standardName: 'QCVN 8-2:2011/BYT',
    },
    {
      criterionId: 'crit-2',
      code: 'PEST-02',
      name: 'Dư lượng thuốc trừ sâu',
      standardName: 'VietGAP',
    },
  ];

  const defaultProps = {
    criteria: mockCriteria,
    onSubmit: vi.fn(),
    onUploadFile: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders all criteria items initially as unset', () => {
    render(<InspectionResultEntryForm {...defaultProps} />);

    expect(screen.getByText('Dư lượng chì')).toBeInTheDocument();
    expect(screen.getByText('Dư lượng thuốc trừ sâu')).toBeInTheDocument();
    expect(screen.getByText(/chưa nhập: 2/i)).toBeInTheDocument();
    expect(screen.getByText('Hiển thị 1 - 2 trên tổng số 2 chỉ tiêu')).toBeInTheDocument();
    expect(screen.getByText('1 / 1')).toBeInTheDocument();

    const submitBtn = screen.getByRole('button', {
      name: /xác nhận và gửi kết quả kiểm nghiệm/i,
    });
    expect(submitBtn).toBeDisabled();
  });

  it('phân trang 10 chỉ tiêu và giữ dữ liệu đã nhập khi chuyển trang', () => {
    const criteria = Array.from({ length: 11 }, (_, index) => ({
      criterionId: `crit-${index + 1}`,
      code: `CODE-${index + 1}`,
      name: `Chỉ tiêu ${index + 1}`,
    }));

    render(
      <InspectionResultEntryForm
        {...defaultProps}
        criteria={criteria}
      />
    );

    expect(screen.getByText('Chỉ tiêu 1')).toBeInTheDocument();
    expect(screen.queryByText('Chỉ tiêu 11')).not.toBeInTheDocument();
    expect(screen.getByText('Hiển thị 1 - 10 trên tổng số 11 chỉ tiêu')).toBeInTheDocument();

    fireEvent.click(screen.getAllByRole('button', { name: /^đạt$/i })[0]);
    fireEvent.click(screen.getByRole('button', { name: /trang sau/i }));

    expect(screen.queryByText('Chỉ tiêu 1')).not.toBeInTheDocument();
    expect(screen.getByText('Chỉ tiêu 11')).toBeInTheDocument();
    expect(screen.getByText('Hiển thị 11 - 11 trên tổng số 11 chỉ tiêu')).toBeInTheDocument();
    expect(screen.getByText('2 / 2')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /trang trước/i }));

    expect(screen.getByText('Chỉ tiêu 1')).toBeInTheDocument();
    expect(screen.getByText('Đạt: 1')).toBeInTheDocument();
  });

  it('marks all as passed when clicking "Tất cả Đạt"', () => {
    render(<InspectionResultEntryForm {...defaultProps} />);

    const markAllBtn = screen.getByRole('button', { name: /tất cả đạt/i });
    fireEvent.click(markAllBtn);

    expect(screen.getByText(/đạt: 2/i)).toBeInTheDocument();
    expect(screen.queryByText(/chưa nhập:/i)).not.toBeInTheDocument();

    const submitBtn = screen.getByRole('button', {
      name: /xác nhận và gửi kết quả kiểm nghiệm/i,
    });
    expect(submitBtn).toBeEnabled();
  });

  it('allows setting individual criterion to passed or failed', () => {
    render(<InspectionResultEntryForm {...defaultProps} />);

    // Click "Đạt" trên chỉ tiêu 1
    const passBtns = screen.getAllByRole('button', { name: /^đạt$/i });
    fireEvent.click(passBtns[0]);

    // Click "Không đạt" trên chỉ tiêu 2
    const failBtns = screen.getAllByRole('button', { name: /^không đạt$/i });
    fireEvent.click(failBtns[1]);

    expect(screen.getByText('Đạt: 1')).toBeInTheDocument();
    expect(screen.getByText('Không đạt: 1')).toBeInTheDocument();
  });

  it('opens confirmation modal and calls onSubmit on confirmation', async () => {
    render(<InspectionResultEntryForm {...defaultProps} />);

    // Đánh dấu tất cả Đạt
    fireEvent.click(screen.getByRole('button', { name: /tất cả đạt/i }));

    // Click submit button
    const submitBtn = screen.getByRole('button', {
      name: /xác nhận và gửi kết quả kiểm nghiệm/i,
    });
    fireEvent.click(submitBtn);

    // Modal xác nhận xuất hiện
    expect(screen.getByText(/xác nhận nộp kết quả kiểm nghiệm/i)).toBeInTheDocument();

    // Xác nhận trong modal
    const confirmBtn = screen.getByRole('button', { name: /đồng ý nộp kết quả/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(defaultProps.onSubmit).toHaveBeenCalledWith(
        expect.arrayContaining([
          expect.objectContaining({ criterionId: 'crit-1', passed: true }),
          expect.objectContaining({ criterionId: 'crit-2', passed: true }),
        ])
      );
    });
  });

  it('báo lỗi khi ngày cấp kết quả trước ngày gửi mẫu', async () => {
    const { toast } = await import('sonner');
    const { container } = render(
      <InspectionResultEntryForm
        {...defaultProps}
        sampleSentDate="2026-09-15"
      />
    );

    // Đánh dấu tất cả Đạt
    fireEvent.click(screen.getByRole('button', { name: /tất cả đạt/i }));

    // Sửa ngày cấp của chỉ tiêu đầu tiên thành ngày trước ngày gửi mẫu (2026-09-10 < 2026-09-15)
    const dateInputs = container.querySelectorAll<HTMLInputElement>('input[type="date"]');
    expect(dateInputs.length).toBeGreaterThan(0);
    // dateInputs[0] là ngày cấp của chỉ tiêu 1
    fireEvent.change(dateInputs[0], { target: { value: '2026-09-10' } });

    // Click submit button
    const submitBtn = screen.getByRole('button', {
      name: /xác nhận và gửi kết quả kiểm nghiệm/i,
    });
    fireEvent.click(submitBtn);

    expect(toast.error).toHaveBeenCalledWith(
      expect.stringContaining('không thể trước Ngày gửi mẫu')
    );
    expect(screen.queryByText(/xác nhận nộp kết quả kiểm nghiệm/i)).not.toBeInTheDocument();
  });
});
