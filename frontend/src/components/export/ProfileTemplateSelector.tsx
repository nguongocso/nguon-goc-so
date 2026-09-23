import React, { useState, useEffect, useRef, useMemo } from 'react';
import { CheckCircle2, Sparkles } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useProfileTemplates } from '@/hooks/useProfileTemplates';
import { TemplateOptionContent, DEFAULT_TEMPLATE_DISPLAY_NAME } from './TemplateOptionContent';

import type { ProfileTemplate } from '@/types/profileTemplate';

/** Thuộc tính truyền vào ProfileTemplateSelector. */
export interface ProfileTemplateSelectorProps {
  organizationId: string;
  open?: boolean;
  onTemplateChange?: (templateId: string, template: ProfileTemplate | null) => void;
  disabled?: boolean;
  showInfoText?: boolean;
  triggerClassName?: string;
  triggerId?: string;
}

/** Chọn mẫu hồ sơ dùng chung cho luồng xuất đơn lẻ và xuất nhiều lô. */
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

  // Giữ callback mới nhất mà không gây kết xuất lại hoặc vòng lặp.
  const onTemplateChangeRef = useRef(onTemplateChange);
  onTemplateChangeRef.current = onTemplateChange;
  useEffect(() => {
    if (open && organizationId) {
      void refresh();
    }
  }, [open, organizationId, refresh]);
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

    // Chỉ cập nhật khi giá trị thay đổi để tránh kết xuất lại không cần thiết.
    if (newId !== selectedTemplateId) {
      setSelectedTemplateId(newId);
      onTemplateChangeRef.current?.(newId, defaultTpl || null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, templates, loadingTemplates]);

  const activeTemplate = templates.find((t) => t.id === selectedTemplateId);
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
              label={[
                tpl.name,
                tpl.partnerName ? ` (${tpl.partnerName})` : '',
                tpl.isDefault ? ' — Mặc định' : '',
              ].join('')}
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
      {showInfoText && (
        <div
          className={
            'flex items-start gap-2 rounded-lg border bg-slate-50 p-2.5 text-xs ' +
            'text-muted-foreground dark:bg-slate-900/50'
          }
        >
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
