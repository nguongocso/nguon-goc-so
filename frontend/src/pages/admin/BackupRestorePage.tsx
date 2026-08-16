// frontend/src/pages/admin/BackupRestorePage.tsx
import { useState } from "react";
import { useBackup } from "@/hooks/useBackup";
import { BackupSchedule } from "@/components/backup/BackupSchedule";
import { BackupStatus } from "@/components/backup/BackupStatus";
import { BackupHistoryTable } from "@/components/backup/BackupHistoryTable";
import { BackupHistoryFilter } from "@/components/backup/BackupHistoryFilter";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Database, RefreshCw } from "lucide-react";
import { ScheduleEditDialog } from "@/components/backup/ScheduleEditDialog";
import type { BackupScheduleRequest } from "@/types/backup";

export default function BackupRestorePage() {
  const {
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
  } = useBackup();

  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);

  // Wrapper để khớp với kiểu Promise<void> của BackupSchedule
  const handleToggleActive = async (data: {
    cronExpression: string;
    description?: string;
    isActive: boolean;
  }) => {
    // console.log("🔄 Toggling schedule:", data);
    await updateSchedule(data);
    // console.log("✅ Schedule updated, new state:", schedule);
  };

  // Wrapper cho ScheduleEditDialog
  const handleSaveSchedule = async (data: BackupScheduleRequest) => {
    await updateSchedule(data);
  };

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            Sao lưu & Phục hồi dữ liệu
          </h1>
          <p className="text-sm text-muted-foreground">
            Quản lý lịch sao lưu tự động và phục hồi dữ liệu khi cần thiết
          </p>
        </div>
        <Button
          variant="outline"
          onClick={() => {
            fetchSchedule();
            fetchHistory(pageInfo.page, pageInfo.size);
          }}
          disabled={loading}
        >
          <RefreshCw
            className={`h-4 w-4 mr-1 ${loading ? "animate-spin" : ""}`}
          />
          Làm mới
        </Button>
      </div>

      {/* Cấu hình lịch */}
      <BackupSchedule
        schedule={schedule}
        onEdit={() => setIsEditDialogOpen(true)}
        onToggleActive={handleToggleActive}
        disabled={isBackupInProgress || isRestoreInProgress}
      />

      {/* Trạng thái & Nút sao lưu ngay */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <BackupStatus
          isBackupInProgress={isBackupInProgress}
          isRestoreInProgress={isRestoreInProgress}
        />
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Thao tác</CardTitle>
          </CardHeader>
          <CardContent>
            <Button
              onClick={triggerManualBackup}
              disabled={isBackupInProgress || isRestoreInProgress}
              className="gap-2"
            >
              <Database className="h-4 w-4" />
              {isBackupInProgress ? "Đang sao lưu..." : "Sao lưu ngay"}
            </Button>
          </CardContent>
        </Card>
      </div>

      {/* Lịch sử */}
      <Card>
        <CardHeader>
          <CardTitle>Lịch sử sao lưu & phục hồi</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <BackupHistoryFilter
            operationType={operationTypeFilter}
            status={statusFilter}
            onOperationTypeChange={setOperationTypeFilter}
            onStatusChange={setStatusFilter}
            onApply={() => fetchHistory(0, pageInfo.size)}
            onReset={() => {
              setOperationTypeFilter(undefined);
              setStatusFilter(undefined);
              fetchHistory(0, pageInfo.size);
            }}
          />
          <BackupHistoryTable
            history={history}
            loading={loading}
            onDownload={downloadBackupFile}
            onDelete={deleteBackupRecord}
            onRestore={restoreBackupRecord}
            disabled={isBackupInProgress || isRestoreInProgress}
          />
          {pageInfo.totalPages > 1 && (
            <div className="flex items-center justify-between pt-4 border-t">
              <div className="text-sm text-muted-foreground">
                Tổng số: {pageInfo.totalElements} bản ghi
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={pageInfo.page === 0}
                  onClick={() => changePage(pageInfo.page - 1)}
                >
                  Trước
                </Button>
                <span className="text-sm">
                  Trang {pageInfo.page + 1} / {pageInfo.totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={pageInfo.page >= pageInfo.totalPages - 1}
                  onClick={() => changePage(pageInfo.page + 1)}
                >
                  Sau
                </Button>
                <select
                  className="h-8 rounded-md border px-2 text-sm"
                  value={pageInfo.size}
                  onChange={(e) => changeSize(Number(e.target.value))}
                >
                  {[5, 10, 20, 50].map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <ScheduleEditDialog
        open={isEditDialogOpen}
        onClose={() => setIsEditDialogOpen(false)}
        schedule={schedule}
        onSave={handleSaveSchedule}
      />
    </div>
  );
}
