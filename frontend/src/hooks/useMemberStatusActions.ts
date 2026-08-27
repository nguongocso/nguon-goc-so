import { isAxiosError } from 'axios';
import { useState } from 'react';
import { toast } from 'sonner';

import { deactivateMember, reactivateMember } from '@/api/memberApi';
import type {
  MemberLotSummary,
  ReplacementRequiredError,
} from '@/types/member';

/** Kết quả thao tác vô hiệu hóa để dialog phân nhánh hiển thị. */
export interface DeactivateOutcome {
  ok: boolean;
  /**
   * Lỗi không thể xử lý tiếp tại chỗ (403 không có quyền, 404 không tồn tại,
   * 409 thành viên đã ngừng hoạt động) → dialog nên đóng và refresh dữ liệu.
   */
  fatal?: boolean;
  /**
   * `true` khi backend trả 409 kèm ReplacementRequiredError — thành viên
   * còn lô chưa hoàn thành, dialog phải hiển thị danh sách lô và yêu cầu
   * chọn người thay thế (QTN-32, TC-02).
   */
  requiresReplacement?: boolean;
  pendingLots?: MemberLotSummary[];
}

/** Kết quả thao tác kích hoạt lại. */
export interface ReactivateOutcome {
  ok: boolean;
  fatal?: boolean;
}

interface ApiErrorPayload {
  message?: string;
  errors?: unknown;
}

const NETWORK_ERROR_MESSAGE =
  'Không thể kết nối đến máy chủ. Vui lòng thử lại.';

const DEACTIVATE_FALLBACK_MESSAGE = 'Không thể vô hiệu hóa thành viên.';
const REACTIVATE_FALLBACK_MESSAGE = 'Không thể kích hoạt lại thành viên.';

/** Trích xuất message tiếng Việt + trạng thái HTTP từ payload lỗi ApiResult. */
const extractError = (
  error: unknown,
): { status?: number; message: string; errors?: unknown } => {
  if (isAxiosError(error)) {
    const status = error.response?.status;
    const data = error.response?.data as ApiErrorPayload | undefined;
      return {
      status,
      message: data?.message ?? NETWORK_ERROR_MESSAGE,
      errors: data?.errors,
    };
  }
  return { message: NETWORK_ERROR_MESSAGE };
};

/** Nhận diện payload `errors` của lỗi 409 còn lô chưa hoàn thành. */
const isReplacementRequiredError = (
  errors: unknown,
): errors is ReplacementRequiredError => {
  if (typeof errors !== 'object' || errors === null) return false;
  const candidate = errors as Partial<ReplacementRequiredError>;
  return candidate.requiresReplacement === true;
};

/**
 * Hook xử lý vô hiệu hóa / kích hoạt lại thành viên (NCL-01-CN-009, QTN-32).
 *
 * - Tách riêng khỏi component, tuân theo pattern của useRecallShipment.
 * - Toast lỗi nghiệp vụ hiển thị tại đây; riêng lỗi 409 "còn lô chưa
 *   hoàn thành" KHÔNG toast mà trả outcome để dialog hiển thị danh sách
 *   lô + form chọn người thay thế.
 * - Trạng thái UI chỉ được cập nhật sau khi backend trả thành công
 *   (thông qua onSuccess → refresh danh sách).
 *
 * @param onSuccess callback gọi lại sau khi thao tác thành công (reload danh sách).
 */
export const useMemberStatusActions = (onSuccess?: () => void) => {
  const [isDeactivating, setIsDeactivating] = useState(false);
  const [isReactivating, setIsReactivating] = useState(false);

  const deactivate = async (
    userId: string,
    reason: string,
    replacementUserId?: string,
    memberName?: string,
  ): Promise<DeactivateOutcome> => {
    setIsDeactivating(true);
    try {
      await deactivateMember(userId, { reason, replacementUserId });
      toast.success(
        memberName
          ? `Đã vô hiệu hóa ${memberName}. Thành viên mất toàn bộ quyền trong tổ chức.`
          : 'Đã vô hiệu hóa thành viên. Thành viên mất toàn bộ quyền trong tổ chức.',
      );
      onSuccess?.();
      return { ok: true };
    } catch (error) {
      const { status, message, errors } = extractError(error);

      // 409 còn lô chưa hoàn thành → trả dữ liệu cho dialog xử lý,
      // không toast vì dialog sẽ hiển thị danh sách lô cần chuyển giao.
      if (status === 409 && isReplacementRequiredError(errors)) {
        return {
          ok: false,
          requiresReplacement: true,
          pendingLots: errors.pendingLots ?? [],
        };
      }

                  toast.error(message || DEACTIVATE_FALLBACK_MESSAGE);
      const fatalStatus = status === 403 || status === 404 || status === 409;
      return {
        ok: false,
        fatal: fatalStatus || undefined,
      };
    } finally {
      setIsDeactivating(false);
    }
  };

  const reactivate = async (
    userId: string,
    reason: string,
    memberName?: string,
  ): Promise<ReactivateOutcome> => {
    setIsReactivating(true);
    try {
      await reactivateMember(userId, { reason });
      toast.success(
        memberName
          ? `Đã kích hoạt lại ${memberName}. Thành viên được khôi phục quyền với vai trò cũ.`
          : 'Đã kích hoạt lại thành viên.',
      );
      onSuccess?.();
      return { ok: true };
    } catch (error) {
      const { status, message } = extractError(error);
                  toast.error(message || REACTIVATE_FALLBACK_MESSAGE);
      const fatalStatus = status === 403 || status === 404 || status === 409;
      return {
        ok: false,
        fatal: fatalStatus || undefined,
      };
    } finally {
      setIsReactivating(false);
    }
  };

  return { isDeactivating, isReactivating, deactivate, reactivate };
};
