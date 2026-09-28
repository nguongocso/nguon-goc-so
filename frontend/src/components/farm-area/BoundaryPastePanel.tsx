import React, { useState } from 'react';
import { AlertCircle, Check, ClipboardPaste } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { parseCoordinatesText } from '@/utils/geoAreaCalculator';
import type { LatLng } from '@/types/farmArea';

/** Thuộc tính của panel dán danh sách toạ độ. */
interface BoundaryPastePanelProps {
  onApplyPoints: (points: LatLng[]) => void;
  disabled?: boolean;
}

export const BoundaryPastePanel: React.FC<BoundaryPastePanelProps> = ({ onApplyPoints, disabled = false }) => {
  const [text, setText] = useState('');
  const [errors, setErrors] = useState<string[]>([]);
  const [isOpen, setIsOpen] = useState(false);

  const handleApply = () => {
    const result = parseCoordinatesText(text);
    if (result.errors.length > 0) {
      setErrors(result.errors);
      return;
    }

    setErrors([]);
    onApplyPoints(result.points);
    setText('');
    setIsOpen(false);
  };

  return (
    <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <ClipboardPaste className="size-4 text-emerald-600 dark:text-emerald-400" />
          <h4 className="text-sm font-semibold text-foreground">Dán danh sách tọa độ</h4>
        </div>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => setIsOpen((prev) => !prev)}
          className="text-xs text-muted-foreground hover:text-foreground"
        >
          {isOpen ? 'Thu gọn' : 'Mở rộng'}
        </Button>
      </div>

      {isOpen && (
        <div className="mt-3 space-y-3">
          <p className="text-xs text-muted-foreground leading-relaxed">
            Dán danh sách tọa độ (từ GPS/Google Earth). Mỗi dòng một điểm theo định dạng:{' '}
            <code className="rounded bg-muted px-1 py-0.5 font-mono text-[11px] text-foreground">
              vĩ độ, kinh độ
            </code>
            . Tối thiểu 3 đỉnh phân biệt, không cần lặp điểm đóng vòng.
          </p>

          <Textarea
            value={text}
            onChange={(e) => {
              setText(e.target.value);
              if (errors.length > 0) setErrors([]);
            }}
            placeholder={'21.0285, 105.8542\n21.0300, 105.8560\n21.0270, 105.8580'}
            rows={4}
            disabled={disabled}
            className="font-mono text-xs"
          />

          {errors.length > 0 && (
            <Alert variant="destructive" className="py-2.5">
              <AlertCircle className="size-4" />
              <AlertTitle className="text-xs font-semibold">Tọa độ chưa hợp lệ</AlertTitle>
              <AlertDescription className="text-xs">
                <ul className="mt-1 list-disc space-y-0.5 pl-4">
                  {errors.slice(0, 3).map((err, idx) => (
                    <li key={idx}>{err}</li>
                  ))}
                  {errors.length > 3 && (
                    <li>...và {errors.length - 3} lỗi khác</li>
                  )}
                </ul>
              </AlertDescription>
            </Alert>
          )}

          <div className="flex justify-end gap-2 pt-1">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => {
                setText('');
                setErrors([]);
              }}
              disabled={disabled || !text}
            >
              Xóa nội dung
            </Button>

            <Button
              type="button"
              variant="default"
              size="sm"
              onClick={handleApply}
              disabled={disabled || !text.trim()}
              className="flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white"
            >
              <Check className="size-3.5" />
              Áp dụng danh sách
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};
