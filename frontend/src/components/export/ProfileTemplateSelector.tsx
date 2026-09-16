import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { CheckCircle2, Sparkles } from 'lucide-react';
import { useProfileTemplates } from '@/hooks/useProfileTemplates';
import { TemplateOptionContent, DEFAULT_TEMPLATE_DISPLAY_NAME } from './TemplateOptionContent';
import type { ProfileTemplate } from '@/types/profileTemplate';

/** Props truyền vào ProfileTemplateSelector */
export interface ProfileTemplateSelectorProps {
  /** ID tổ chức (lấy từ user.organizationId) */
  organizationId: string;
  /**
   * Trạng thái mở/đóng của dialog hoặc trang chứa selector.
   * Khi `false`, selector không fetch mẫu cũng không tự động chọn mặc định.
   * Mặc định: `true`.
   */
  open?: boolean;
  /**
   * Callback được gọi mỗi khi người dùng (hoặc tự động) chọn một mẫu.
   * @param templateId  ID của mẫu được chọn; `'default'` nếu chọn mẫu hệ thống.
   * @param template    Đối tượng mẫu được chọn; `null` nếu chọn mẫu mặc định hệ thống.
   */
  onTemplateChange?: (templateId: string, template: ProfileTemplate | null) => void;
  /** Vô hiệu hoá selector (ví dụ: đang xuất) */
  disabled?: boolean;
  /** Có hiển thị đoạn thông tin giải thích dưới select hay không */
  showInfoText?: boolean;
  /** Class CSS tùy chỉnh cho SelectTrigger */
  triggerClassName?: string;
  /** HTML id của SelectTrigger — dùng để gán Label htmlFor */
  triggerId?: string;
}

/**
 * Component chọn mẫu hồ sơ áp dụng dùng chung cho cả hai chức năng:
 * 1. Xuất hồ sơ truy xuất nguồn gốc đơn lẻ (ExportDossierDialog)
 * 2. Xuất bộ hồ sơ truy xuất nguồn gốc nhiều lô (BatchDossierExportPage)
 *
 * Component tự quản lý state `selectedTemplateId`, tự động chọn mẫu mặc
 * định của tổ chức khi danh sách mẫu được tải, và thông báo kết quả qua
 * callback `onTemplateChange`.
 *
 * Luôn hiển thị tiếng Việt cho: nhãn, placeholder, tên mẫu, nhãn "Mặc định",
 * và tên mẫu mặc định hệ thống — không bao giờ hiển thị mã (id) của mẫu.
 */
export const ProfileTemplateSelector: React.FC<ProfileTemplateSelectorProps> = ({
  organizationId,
  open = true,
  onTemplateChange,
  disabled = false,
  showInfoText = false,
  triggerClassName = 'w-full',
  triggerId = 'template-select',
}) => {
  const { templates, refresh, loading: loadingTemplates } = useProfileTemplates(organizationId);
  const [selectedTemplateId, setSelectedTemplateId] = useState<string>('default');

  // Dùng ref để luôn gọi callback mới nhất mà không gây re-render hay vòng lặp
  const onTemplateChangeRef = useRef(onTemplateChange);
  onTemplateChangeRef.current = onTemplateChange;

  // Làm mới danh sách mẫu mỗi khi mở lại selector
  useEffect(() => {
    if (open && organizationId) {
      void refresh();
    }
  }, [open, organizationId, refresh]);

  // Tự động chọn mẫu mặc định của tổ chức sau khi danh sách mẫu đã tải
  useEffect(() => {
    if (!open || loadingTemplates) return;

    if (templates.length === 0) {
      if (selectedTemplateId !== 'default') {
        setSelectedTemplateId('default');
      }
      onTemplateChangeRef.current?.('default', null);
      return;
    }

    const defaultTpl = templates.find((t) => t.isDefault);
    const newId = defaultTpl?.id || 'default';

    // Chỉ cập nhật nếu khác giá trị hiện tại để tránh re-render không cần thiết
    if (newId !== selectedTemplateId) {
      setSelectedTemplateId(newId);
      onTemplateChangeRef.current?.(newId, defaultTpl || null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, templates, loadingTemplates]);

  const activeTemplate = templates.find((t) => t.id === selectedTemplateId);

  // Nội dung hiển thị trên SelectTrigger (giá trị đang được chọn)
  const selectedTemplateContent = useMemo(() => {
    if (activeTemplate) {
      return (
        <TemplateOptionContent
          name={activeTemplate.name}
          partnerName={activeTemplate.partnerName}
          isDefault={activeTemplate.isDefault}
        />
      );
    }
    return <span className="truncate font-medium">{DEFAULT_TEMPLATE_DISPLAY_NAME}</span>;
  }, [activeTemplate]);

  const handleTemplateChange = (value: string | null) => {
    const resolved = value ?? 'default';
    setSelectedTemplateId(resolved);
    const tpl = templates.find((t) => t.id === resolved) || null;
    onTemplateChange?.(resolved, tpl);
  };

  return (
    <div className="space-y-2">
      {/* Nhãn + badge đối tác */}
      <div className="flex items-center justify-between">
        <Label
          htmlFor={triggerId}
          className="text-sm font-semibold flex items-center gap-1.5"
        >
          <Sparkles className="size-4 text-emerald-600" />
          Mẫu hồ sơ áp dụng
        </Label>
        {activeTemplate?.partnerName && (
          <Badge
            variant="outline"
            className="text-xs border-emerald-300 text-emerald-700 bg-emerald-50/60"
          >
            Đối tác: {activeTemplate.partnerName}
          </Badge>
        )}
      </div>

      {/* Select chọn mẫu */}
      <Select
        value={selectedTemplateId}
        onValueChange={handleTemplateChange}
        disabled={loadingTemplates || disabled}
      >
        <SelectTrigger id={triggerId} className={triggerClassName}>
          <SelectValue placeholder="Chọn mẫu hồ sơ">
            {selectedTemplateContent}
          </SelectValue>
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="default" label={DEFAULT_TEMPLATE_DISPLAY_NAME}>
            <span className="font-medium">{DEFAULT_TEMPLATE_DISPLAY_NAME}</span>
          </SelectItem>
          {templates.map((tpl) => (
            <SelectItem
              key={tpl.id}
              value={tpl.id}
              label={`${tpl.name}${tpl.partnerName ? ` (${tpl.partnerName})` : ''}${tpl.isDefault ? ' — Mặc định' : ''}`}
            >
              <TemplateOptionContent
                name={tpl.name}
                partnerName={tpl.partnerName}
                isDefault={tpl.isDefault}
              />
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {/* Thông tin giải thích về mẫu đang chọn */}
      {showInfoText && (
        <div className="p-2.5 rounded-lg bg-slate-50 dark:bg-slate-900/50 border text-xs text-muted-foreground flex items-start gap-2">
          <CheckCircle2 className="size-4 text-emerald-500 shrink-0 mt-0.5" />
          <div>
            {selectedTemplateId === 'default' ? (
              <span>
                Áp dụng biểu mẫu mặc định của HTX gồm đầy đủ các trường bắt buộc và
                toàn bộ thông tin sản xuất, canh tác, kiểm nghiệm.
              </span>
            ) : (
              <span>
                Áp dụng mẫu <strong className="text-foreground">{activeTemplate?.name}</strong>
                {activeTemplate?.partnerName
                  ? ` thiết kế cho đối tác ${activeTemplate.partnerName}`
                  : ''}
                . Hồ sơ xuất ra sẽ được lọc chính xác theo cấu hình{' '}
                {activeTemplate?.fields?.length || 0} trường đã chọn.
              </span>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
