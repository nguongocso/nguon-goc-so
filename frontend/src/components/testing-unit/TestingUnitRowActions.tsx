import { Ban, CheckSquare, Pencil } from "lucide-react";

import { Button } from "@/components/ui/button";
import type { TestingUnit } from "@/types/certification";

/**
 * Chuẩn giao diện chung cho các nút hành động trong bảng
 * (lấy theo nút ngừng hoạt động: ghost, vuông 32px, bo góc, hover nền xám).
 */
const ACTION_BUTTON_CLASS = "h-8 w-8 p-0 rounded-lg hover:bg-muted";
const ACTION_ICON_CLASS = "h-4 w-4 text-slate-600";

interface TestingUnitRowActionsProps {
  /** Đơn vị kiểm nghiệm của dòng dữ liệu hiện tại. */
  unit: TestingUnit;
  /** Người dùng có quyền sửa / ngừng hoạt động hay không. */
  canManage: boolean;
  /** Mở màn hình quản lý phạm vi công nhận. */
  onManageScopes: () => void;
  /** Mở màn hình chỉnh sửa đơn vị. */
  onEdit: () => void;
  /** Mở hộp thoại xác nhận ngừng hoạt động. */
  onDeactivate: () => void;
}

/**
 * Cụm nút hành động của một dòng: quản lý phạm vi công nhận, chỉnh sửa
 * và ngừng hoạt động. Đơn vị đã ngừng hoạt động sẽ không hiển thị nút ngừng hoạt động.
 */
export const TestingUnitRowActions = ({
  unit,
  canManage,
  onManageScopes,
  onEdit,
  onDeactivate,
}: TestingUnitRowActionsProps) => {
  return (
    <div className="flex items-center justify-center gap-1">
      <Button
        type="button"
        variant="ghost"
        size="sm"
        onClick={onManageScopes}
        className={ACTION_BUTTON_CLASS}
        title="Phạm vi công nhận"
        aria-label="Phạm vi công nhận"
      >
        <CheckSquare className={ACTION_ICON_CLASS} />
      </Button>
      {canManage && (
        <>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={onEdit}
            className={ACTION_BUTTON_CLASS}
            title="Chỉnh sửa"
            aria-label="Chỉnh sửa"
          >
            <Pencil className={ACTION_ICON_CLASS} />
          </Button>
          {unit.isActive && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={onDeactivate}
              className={ACTION_BUTTON_CLASS}
              title="Ngừng hoạt động"
              aria-label="Ngừng hoạt động"
            >
              <Ban className={ACTION_ICON_CLASS} />
            </Button>
          )}
        </>
      )}
    </div>
  );
};
