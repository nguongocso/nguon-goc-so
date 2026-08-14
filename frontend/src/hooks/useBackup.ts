import { useState, useEffect, useCallback } from 'react';
import { toast } from 'sonner';
import * as backupApi from '@/api/backupApi';
import type {
  BackupSchedule,
  BackupHistoryItem,
  BackupOperationType,
  BackupStatus,
  BackupScheduleRequest,
} from '@/types/backup';

export const useBackup = () => {
  const [schedule, setSchedule] = useState<BackupSchedule | null>(null);
  const [history, setHistory] = useState<BackupHistoryItem[]>([]);
  const [pageInfo, setPageInfo] = useState({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  });
  const [loading, setLoading] = useState(false);
  const [isBackupInProgress, setIsBackupInProgress] = useState(false);
  const [isRestoreInProgress, setIsRestoreInProgress] = useState(false);
  const [operationTypeFilter, setOperationTypeFilter] = useState<BackupOperationType | undefined>();
  const [statusFilter, setStatusFilter] = useState<BackupStatus | undefined>();

  const fetchSchedule = useCallback(async () => {
    try {
      const data = await backupApi.getSchedules();
      setSchedule(data.length > 0 ? data[0] : null);
    } catch (err) {
      toast.error('Không thể tải cấu hình lịch sao lưu');
    }
  }, []);

  const fetchHistory = useCallback(
    async (page: number = pageInfo.page, size: number = pageInfo.size) => {
      setLoading(true);
      try {
        const res = await backupApi.getHistory(page, size, operationTypeFilter, statusFilter);
        setHistory(res.content);
        setPageInfo({
          page: res.number ?? 0,
          size: res.size ?? 10,
          totalElements: res.totalElements,
          totalPages: res.totalPages,
        });
        const hasInProgress = res.content.some((item) => item.status === 'IN_PROGRESS');
        const hasRestoreInProgress = res.content.some(
          (item) => item.operationType === 'RESTORE' && item.status === 'IN_PROGRESS',
        );
        setIsBackupInProgress(hasInProgress && !hasRestoreInProgress);
        setIsRestoreInProgress(hasRestoreInProgress);
      } catch (err) {
        console.error('❌ Fetch history error:', err);
        toast.error('Không thể tải lịch sử sao lưu');
      } finally {
        setLoading(false);
      }
    },
    [operationTypeFilter, statusFilter],
  );

  const updateSchedule = useCallback(
    async (payload: BackupScheduleRequest) => {
      try {
        const data = await backupApi.configureSchedule(payload);
        setSchedule(data);
        toast.success('Cập nhật lịch sao lưu thành công');
        return data;
      } catch (err: any) {
        const msg = err.response?.data?.message || 'Cập nhật lịch sao lưu thất bại';
        toast.error(msg);
        throw err;
      }
    },
    [],
  );

  const triggerManualBackup = useCallback(async () => {
    try {
      const data = await backupApi.triggerBackup();
      toast.success('Bắt đầu sao lưu thủ công');
      setTimeout(() => fetchHistory(pageInfo.page, pageInfo.size), 500);
      return data;
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Sao lưu thất bại';
      toast.error(msg);
      throw err;
    }
  }, [fetchHistory, pageInfo.page, pageInfo.size]);

  const downloadBackupFile = useCallback(async (id: number) => {
    try {
      await backupApi.downloadBackup(id);
      toast.success('Tải file thành công');
    } catch (err) {
      toast.error('Tải file thất bại');
    }
  }, []);

  const deleteBackupRecord = useCallback(
    async (id: number) => {
      try {
        await backupApi.deleteBackup(id);
        toast.success('Xóa bản sao lưu thành công');
        fetchHistory(pageInfo.page, pageInfo.size);
      } catch (err) {
        toast.error('Xóa thất bại');
      }
    },
    [fetchHistory, pageInfo.page, pageInfo.size],
  );

  const restoreBackupRecord = useCallback(
    async (id: number) => {
      try {
        const data = await backupApi.restoreBackup(id);
        toast.success('Bắt đầu phục hồi dữ liệu');
        setIsRestoreInProgress(true);
        setTimeout(() => fetchHistory(pageInfo.page, pageInfo.size), 500);
        return data;
      } catch (err: any) {
        const msg = err.response?.data?.message || 'Phục hồi thất bại';
        toast.error(msg);
        throw err;
      }
    },
    [fetchHistory, pageInfo.page, pageInfo.size],
  );

  const changePage = useCallback(
    (newPage: number) => {
      if (newPage >= 0 && newPage < pageInfo.totalPages) {
        fetchHistory(newPage, pageInfo.size);
      }
    },
    [fetchHistory, pageInfo.totalPages, pageInfo.size],
  );

  const changeSize = useCallback(
    (newSize: number) => {
      fetchHistory(0, newSize);
    },
    [fetchHistory],
  );

  useEffect(() => {
    fetchSchedule();
    fetchHistory(0, 10);
  }, []);

  useEffect(() => {
    let interval: number | undefined;
    if (isBackupInProgress || isRestoreInProgress) {
      interval = window.setInterval(() => {
        fetchHistory(pageInfo.page, pageInfo.size);
      }, 5000);
    }
    return () => {
      if (interval !== undefined) {
        clearInterval(interval);
      }
    };
  }, [isBackupInProgress, isRestoreInProgress, fetchHistory, pageInfo.page, pageInfo.size]);

  return {
    schedule,
    history,
    pageInfo,
    loading,
    isBackupInProgress,
    isRestoreInProgress,
    operationTypeFilter,
    statusFilter,
    setOperationTypeFilter,
    setStatusFilter,
    fetchSchedule,
    fetchHistory,
    updateSchedule,
    triggerManualBackup,
    downloadBackupFile,
    deleteBackupRecord,
    restoreBackupRecord,
    changePage,
    changeSize,
  };
};