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
  fn: (userId: string, reason: string, memberName?: string) => Promise<DeactivateOutcome>,
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
      'Nguyễn Văn Bình',
    );

    expect(outcome.ok).toBe(true);
    expect(mockDeactivate).toHaveBeenCalledWith('user-1', {
      reason: 'Thành viên nghỉ việc',
    });
    expect(onSuccess).toHaveBeenCalled();
    expect(toast.success).toHaveBeenCalledWith(expect.stringContaining('Nguyễn Văn Bình'));
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
