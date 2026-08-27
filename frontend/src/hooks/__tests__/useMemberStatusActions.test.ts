import { renderHook, act } from '@testing-library/react';
import { AxiosError } from 'axios';
import { describe, it, expect, vi, beforeEach } from 'vitest';

import { useMemberStatusActions } from '@/hooks/useMemberStatusActions';
import type { DeactivateOutcome, ReactivateOutcome } from '@/hooks/useMemberStatusActions';
import { toast } from 'sonner';

const mockDeactivate = vi.fn();
const mockReactivate = vi.fn();

vi.mock('@/api/memberApi', () => ({
  deactivateMember: (...args: unknown[]) => mockDeactivate(...args),
  reactivateMember: (...args: unknown[]) => mockReactivate(...args),
}));

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

const makeAxiosError = (status: number, data: unknown): AxiosError => {
  const error = new AxiosError('request failed');
  error.response = {
    status,
    data,
    statusText: '',
    headers: {},
    config: { headers: {} } as never,
  };
  return error;
};

async function callDeactivate(
  fn: (userId: string, reason: string, replacementUserId?: string, memberName?: string) => Promise<DeactivateOutcome>,
  ...args: Parameters<typeof fn>
): Promise<DeactivateOutcome> {
  return (await act(async () => await fn(...args))) as DeactivateOutcome;
}

async function callReactivate(
  fn: (userId: string, reason: string, memberName?: string) => Promise<ReactivateOutcome>,
  ...args: Parameters<typeof fn>
): Promise<ReactivateOutcome> {
  return (await act(async () => await fn(...args))) as ReactivateOutcome;
}

describe('TC-NCL01 - useMemberStatusActions (deactivate)', () => {
  beforeEach(() => {
    mockDeactivate.mockReset();
    mockReactivate.mockReset();
    (toast.success as ReturnType<typeof vi.fn>).mockClear();
    (toast.error as ReturnType<typeof vi.fn>).mockClear();
  });

  it('gọi thành công → outcome ok + toast thành công', async () => {
    mockDeactivate.mockResolvedValue({ membershipStatus: 'INACTIVE' });
    const onSuccess = vi.fn();

    const { result } = renderHook(() => useMemberStatusActions(onSuccess));

    const outcome = await callDeactivate(
      result.current.deactivate.bind(result.current),
      'user-1',
      'Thành viên nghỉ việc',
      undefined,
      'Nguyễn Văn Bình',
    );

    expect(outcome.ok).toBe(true);
    expect(mockDeactivate).toHaveBeenCalledWith('user-1', {
      reason: 'Thành viên nghỉ việc',
      replacementUserId: undefined,
    });
    expect(onSuccess).toHaveBeenCalled();
    expect(toast.success).toHaveBeenCalledWith(expect.stringContaining('Nguyễn Văn Bình'));
  });

  it('409 requiresReplacement → trả pendingLots, KHÔNG toast lỗi', async () => {
    const pendingLots = [
      { lotId: 'lot-a', lotName: 'Lô xoài', lotStatus: 'APPROVED', harvestDate: '2026-09-05' },
      { lotId: 'lot-b', lotName: 'Lô rau', lotStatus: 'PENDING', harvestDate: null },
    ];
    mockDeactivate.mockRejectedValue(
      makeAxiosError(409, {
        success: false,
        status: 409,
        message: 'Thành viên đang được phân công vào 2 lô chưa hoàn thành. Vui lòng chọn người thay thế',
        errors: {
          code: 'MEMBER_HAS_UNFINISHED_LOTS',
          requiresReplacement: true,
          pendingLots,
        },
      }),
    );

    const { result } = renderHook(() => useMemberStatusActions());

    const outcome = await callDeactivate(
      result.current.deactivate.bind(result.current),
      'user-1',
      'lý do',
    );

    expect(outcome.ok).toBe(false);
    expect(outcome.requiresReplacement).toBe(true);
    expect(outcome.pendingLots).toHaveLength(2);
    expect(outcome.pendingLots?.[0].lotId).toBe('lot-a');
    expect(toast.error).not.toHaveBeenCalled();
  });

  it('409 "đã ngừng hoạt động" → fatal + toast message backend', async () => {
    mockDeactivate.mockRejectedValue(
      makeAxiosError(409, {
        success: false,
        status: 409,
        message: 'Thành viên đã ngừng hoạt động',
        errors: 'CONFLICT',
      }),
    );

    const { result } = renderHook(() => useMemberStatusActions());

    const outcome = await callDeactivate(
      result.current.deactivate.bind(result.current),
      'user-1',
      'lý do',
    );

    expect(outcome.ok).toBe(false);
    expect(outcome.fatal).toBe(true);
    expect(outcome.requiresReplacement).toBeUndefined();
    expect(toast.error).toHaveBeenCalledWith('Thành viên đã ngừng hoạt động');
  });

  it('403 → fatal + toast "Bạn không có quyền thực hiện chức năng này"', async () => {
    mockDeactivate.mockRejectedValue(
      makeAxiosError(403, {
        success: false,
        status: 403,
        message: 'Bạn không có quyền thực hiện chức năng này',
        errors: 'ACCESS_DENIED',
      }),
    );

    const { result } = renderHook(() => useMemberStatusActions());

    const outcome = await callDeactivate(
      result.current.deactivate.bind(result.current),
      'user-1',
      'lý do',
    );

    expect(outcome.ok).toBe(false);
    expect(outcome.fatal).toBe(true);
    expect(toast.error).toHaveBeenCalledWith('Bạn không có quyền thực hiện chức năng này');
  });

  it('400 validation → không fatal + toast message backend', async () => {
    mockDeactivate.mockRejectedValue(
      makeAxiosError(400, {
        success: false,
        status: 400,
        message: 'Dữ liệu không hợp lệ',
        errors: { reason: 'Lý do không được để trống' },
      }),
    );

    const { result } = renderHook(() => useMemberStatusActions());

    const outcome = await callDeactivate(
      result.current.deactivate.bind(result.current),
      'user-1',
      '   ',
    );

    expect(outcome.ok).toBe(false);
    expect(outcome.fatal).toBeUndefined();
    expect(toast.error).toHaveBeenCalledWith('Dữ liệu không hợp lệ');
  });

  it('lỗi mạng (không response) → toast default, không fatal', async () => {
    mockDeactivate.mockRejectedValue(new AxiosError('network down'));

    const { result } = renderHook(() => useMemberStatusActions());

    const outcome = await callDeactivate(
      result.current.deactivate.bind(result.current),
      'user-1',
      'lý do',
    );

    expect(outcome.ok).toBe(false);
    expect(outcome.fatal).toBeUndefined();
    expect(toast.error).toHaveBeenCalledWith('Không thể kết nối đến máy chủ. Vui lòng thử lại.');
  });
});

describe('TC-NCL01 - useMemberStatusActions (reactivate)', () => {
  beforeEach(() => {
    mockReactivate.mockReset();
    (toast.success as ReturnType<typeof vi.fn>).mockClear();
    (toast.error as ReturnType<typeof vi.fn>).mockClear();
  });

  it('gọi thành công → outcome ok + toast', async () => {
    mockReactivate.mockResolvedValue({ membershipStatus: 'ACTIVE' });
    const onSuccess = vi.fn();

    const { result } = renderHook(() => useMemberStatusActions(onSuccess));

    const outcome = await callReactivate(
      result.current.reactivate.bind(result.current),
      'user-1',
      'quay lại',
      'Nguyễn Văn Bình',
    );

    expect(outcome.ok).toBe(true);
    expect(mockReactivate).toHaveBeenCalledWith('user-1', { reason: 'quay lại' });
    expect(onSuccess).toHaveBeenCalled();
    expect(toast.success).toHaveBeenCalledWith(expect.stringContaining('Nguyễn Văn Bình'));
  });

  it('409 đang hoạt động → fatal + toast', async () => {
    mockReactivate.mockRejectedValue(
      makeAxiosError(409, {
        success: false,
        status: 409,
        message: 'Thành viên đang hoạt động, không thể kích hoạt lại',
      }),
    );

    const { result } = renderHook(() => useMemberStatusActions());

    const outcome = await callReactivate(
      result.current.reactivate.bind(result.current),
      'user-1',
      'lý do',
    );

    expect(outcome.ok).toBe(false);
    expect(outcome.fatal).toBe(true);
    expect(toast.error).toHaveBeenCalledWith('Thành viên đang hoạt động, không thể kích hoạt lại');
  });
});
