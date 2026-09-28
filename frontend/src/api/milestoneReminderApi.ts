import apiClient from './axiosConfig';
import type { PageResponse } from '@/types/common';
import type {
  MilestoneReminder,
  MilestoneReminderQueryParams,
  MilestoneScanResult,
} from '@/types/milestoneReminder';

/**
 * Lấy danh sách nhắc việc mốc canh tác theo điều kiện lọc và phân trang.
 */
export const getMilestoneReminders = async (
  params?: MilestoneReminderQueryParams
): Promise<PageResponse<MilestoneReminder>> => {
  const response = await apiClient.get<{
    success: boolean;
    data: PageResponse<MilestoneReminder>;
  }>('/milestone-reminders', { params });
  return response.data.data;
};

/**
 * Lấy danh sách nhắc việc quá hạn đang mở (OPEN) của người dùng hiện tại.
 */
export const getMyActiveMilestoneReminders = async (): Promise<MilestoneReminder[]> => {
  const response = await apiClient.get<{
    success: boolean;
    data: MilestoneReminder[];
  }>('/milestone-reminders/my-active');
  return response.data.data;
};

/**
 * Kích hoạt quét mốc canh tác quá hạn thủ công (VT-01, VT-02).
 */
export const triggerMilestoneScan = async (): Promise<MilestoneScanResult> => {
  const response = await apiClient.post<{
    success: boolean;
    data: MilestoneScanResult;
  }>('/milestone-reminders/scan');
  return response.data.data;
};
