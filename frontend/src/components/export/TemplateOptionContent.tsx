import React from 'react';

/**
 * Tên hiển thị tiếng Việt của mẫu hồ sơ mặc định do hệ thống cung cấp.
 */
export const DEFAULT_TEMPLATE_DISPLAY_NAME = 'Mẫu tiêu chuẩn HTX (Mặc định hệ thống)';

/**
 * Thuộc tính của component TemplateOptionContent.
 */
interface TemplateOptionContentProps {
  name: string;
  partnerName?: string | null;
  isDefault?: boolean;
}

/**
 * Hiển thị nội dung của một mẫu hồ sơ gồm tên mẫu, đối tác và nhãn "Mặc định".
 * Dùng chung cho cả danh sách lựa chọn và giá trị đang được chọn tại dialog xuất
 * hồ sơ đơn lẻ và trang xuất bộ hồ sơ nhiều lô.
 *
 * @param props Các thuộc tính tên mẫu, tên đối tác và cờ mặc định.
 * @returns Khối JSX hiển thị thông tin mẫu hồ sơ.
 */
export const TemplateOptionContent: React.FC<TemplateOptionContentProps> = ({
  name,
  partnerName,
  isDefault,
}) => (
  <div className="flex min-w-0 items-center gap-2">
    <span className="truncate">{name}</span>
    {partnerName ? (
      <span className="shrink-0 text-xs text-muted-foreground">({partnerName})</span>
    ) : null}
    {isDefault ? (
      <span className="shrink-0 rounded bg-amber-100 px-1.5 py-0.5 text-[10px] font-medium text-amber-800 dark:bg-amber-950 dark:text-amber-300">
        Mặc định
      </span>
    ) : null}
  </div>
);
