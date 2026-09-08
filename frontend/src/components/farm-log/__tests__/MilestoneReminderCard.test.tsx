import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { MilestoneReminderCard } from '../MilestoneReminderCard';
import * as milestoneReminderApi from '@/api/milestoneReminderApi';
import type { MilestoneReminder } from '@/types/milestoneReminder';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('@/hooks/useAuth', () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from '@/hooks/useAuth';
const mockUseAuth = vi.mocked(useAuth);

describe('MilestoneReminderCard', () => {
  const sampleReminder: MilestoneReminder = {
    id: 'rem-1',
    lotId: 'lot-123',
    lotName: 'Lô Lúa ST25',
    milestoneId: 101,
    milestoneName: 'Bón phân đợt một',
    activityType: 'FERTILIZING',
    overdueDays: 3,
    expectedDate: '2026-09-04',
    status: 'OPEN',
    reminderDate: '2026-09-07',
    completedAt: null,
    createdAt: '2026-09-07T02:00:00',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('hiển thị danh sách nhắc việc quá hạn cho người ghi nhật ký (VT-03)', async () => {
    mockUseAuth.mockReturnValue({
      user: { roleCode: 'VT-03', username: 'recorder' } as any,
    } as any);

    vi.spyOn(milestoneReminderApi, 'getMyActiveMilestoneReminders').mockResolvedValue([
      sampleReminder,
    ]);

    render(
      <MemoryRouter>
        <MilestoneReminderCard userOnly={true} />
      </MemoryRouter>
    );

    expect(screen.getByText(/Đang kiểm tra lịch nhắc việc/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Bón phân đợt một')).toBeInTheDocument();
      expect(screen.getByText('Lô Lúa ST25')).toBeInTheDocument();
      expect(screen.getByText(/Quá hạn 3 ngày/i)).toBeInTheDocument();
      expect(screen.getByText('Bón phân')).toBeInTheDocument();
    });

    // VT-03 nhìn thấy nút "Quét quá hạn" và có thể kích hoạt
    expect(screen.getByText('Quét quá hạn ngay')).toBeInTheDocument();

    // Nhấn nút "Ghi nhật ký ngay"
    const recordBtn = screen.getByRole('button', { name: /Ghi nhật ký ngay/i });
    fireEvent.click(recordBtn);
    expect(mockNavigate).toHaveBeenCalledWith(
      '/farm-logs/create?productionLotId=lot-123&activityType=FERTILIZING&milestoneId=101'
    );
  });

  it('không hiển thị nút Quét quá hạn cho các vai trò khác (như VT-04, VT-05)', async () => {
    mockUseAuth.mockReturnValue({
      user: { roleCode: 'VT-04', username: 'buyer' } as any,
    } as any);

    vi.spyOn(milestoneReminderApi, 'getMilestoneReminders').mockResolvedValue({
      items: [sampleReminder],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });

    render(
      <MemoryRouter>
        <MilestoneReminderCard />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Bón phân đợt một')).toBeInTheDocument();
    });

    expect(screen.queryByText('Quét quá hạn ngay')).not.toBeInTheDocument();
  });

  it('hiển thị thông báo khi không có mốc nào quá hạn', async () => {
    mockUseAuth.mockReturnValue({
      user: { roleCode: 'VT-03', username: 'recorder' } as any,
    } as any);

    vi.spyOn(milestoneReminderApi, 'getMyActiveMilestoneReminders').mockResolvedValue([]);

    render(
      <MemoryRouter>
        <MilestoneReminderCard userOnly={true} />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(
        screen.getByText(/Tất cả mốc canh tác bắt buộc hiện tại đều đã được ghi nhật ký đầy đủ/i)
      ).toBeInTheDocument();
    });
  });

  it('hiển thị nút Quét quá hạn cho quản lý HTX (VT-02) và kích hoạt quét', async () => {
    mockUseAuth.mockReturnValue({
      user: { roleCode: 'VT-02', username: 'manager' } as any,
    } as any);

    vi.spyOn(milestoneReminderApi, 'getMilestoneReminders').mockResolvedValue({
      items: [sampleReminder],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });

    const triggerScanSpy = vi
      .spyOn(milestoneReminderApi, 'triggerMilestoneScan')
      .mockResolvedValue({
        scannedLotsCount: 3,
        remindersCreatedCount: 1,
        message: 'Đã quét xong',
      });

    render(
      <MemoryRouter>
        <MilestoneReminderCard />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Quét quá hạn ngay')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Quét quá hạn ngay'));
    expect(triggerScanSpy).toHaveBeenCalledTimes(1);
  });
});
