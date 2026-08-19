import type { ActivityLog, ActivityLogParams } from "@/types/activityLog";
import apiClient from "./axiosConfig";
import type { PageResponse } from "@/types/common";

export const getActivityLogs = async (
  params: ActivityLogParams
): Promise<PageResponse<ActivityLog>> => {
  const response = await apiClient.get<{ data: PageResponse<ActivityLog>}>(
    '/organizations/activity-logs',
    { params }
  );
  return response.data.data;
};