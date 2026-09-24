import React from "react";
import { Sparkles } from "lucide-react";

/**
 * Thuộc tính đầu vào cho component QuickPromptChips.
 */
export interface QuickPromptChipsProps {
  /** Danh sách các câu hỏi gợi ý nhanh */
  prompts: string[];
  /** Hàm callback khi người dùng bấm vào một câu hỏi */
  onSelectPrompt: (prompt: string) => void;
  /** Trạng thái vô hiệu hóa các nút bấm */
  disabled?: boolean;
}

/**
 * Component hiển thị danh sách các thẻ câu hỏi gợi ý nhanh (Quick Prompt Chips)
 * cho phép người dùng click để gửi trực tiếp câu hỏi đến trợ lý AI.
 */
export const QuickPromptChips: React.FC<QuickPromptChipsProps> = ({
  prompts,
  onSelectPrompt,
  disabled = false,
}) => {
  if (!prompts || prompts.length === 0) {
    return null;
  }

  return (
    <div className="flex flex-col gap-1.5 my-2">
      {/* Tiêu đề nhóm gợi ý */}
      <div className="flex items-center gap-1.5 text-xs font-medium text-slate-500 dark:text-slate-400">
        <Sparkles className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-400" />
        <span>Gợi ý câu hỏi nhanh:</span>
      </div>

      {/* Danh sách các chip gợi ý */}
      <div className="flex flex-wrap gap-1.5">
        {prompts.map((prompt, index) => (
          <button
            key={index}
            type="button"
            disabled={disabled}
            onClick={() => onSelectPrompt(prompt)}
            className="text-left text-xs bg-emerald-50 hover:bg-emerald-100 text-emerald-800 dark:bg-emerald-950/40 dark:hover:bg-emerald-900/60 dark:text-emerald-200 border border-emerald-200 dark:border-emerald-800 rounded-full px-3 py-1.5 transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-xs cursor-pointer"
          >
            {prompt}
          </button>
        ))}
      </div>
    </div>
  );
};
