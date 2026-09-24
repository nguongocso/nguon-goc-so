import React from "react";
import { Link } from "react-router-dom";

export interface AiMarkdownRendererProps {
  /**
   * Nội dung văn bản Markdown cần hiển thị.
   */
  content: string;
}

/**
 * Component phân tích và render chuỗi văn bản Markdown tinh gọn, an toàn,
 * hỗ trợ định dạng in đậm, inline code, tiêu đề, danh sách, trích dẫn,
 * và tự động chuyển hướng đường dẫn nội bộ bằng React Router Link.
 */
export const AiMarkdownRenderer: React.FC<AiMarkdownRendererProps> = ({ content }) => {
  if (!content) return null;

  // Tách văn bản thành từng dòng (hỗ trợ cả LF và CRLF)
  const lines = content.split(/\r?\n/);

  // Phân tích các thành phần inline: in đậm (**text**), code (`code`), liên kết ([text](url))
  const renderInlineFormatted = (text: string): React.ReactNode[] => {
    // Biểu thức chính quy nhận diện [text](url), **bold**, `code`
    const regex = /(\[.*?\]\(.*?\)|\*\*.*?\*\*|`.*?`)/g;
    const parts = text.split(regex);

    return parts.map((part, index) => {
      if (!part) return null;

      // Liên kết Markdown: [Text](URL)
      const linkMatch = part.match(/^\[(.*?)\]\((.*?)\)$/);
      if (linkMatch) {
        const [, linkText, linkUrl] = linkMatch;
        const isExternal =
          linkUrl.startsWith("http://") || linkUrl.startsWith("https://");

        if (isExternal) {
          return (
            <a
              key={index}
              href={linkUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-emerald-600 hover:text-emerald-700 underline font-medium"
            >
              {linkText}
            </a>
          );
        }

        return (
          <Link
            key={index}
            to={linkUrl}
            className="text-emerald-600 hover:text-emerald-700 underline font-medium"
          >
            {linkText}
          </Link>
        );
      }

      // Chữ in đậm: **Text**
      const boldMatch = part.match(/^\*\*(.*?)\*\*$/);
      if (boldMatch) {
        return (
          <strong
            key={index}
            className="font-semibold text-slate-900 dark:text-slate-100"
          >
            {boldMatch[1]}
          </strong>
        );
      }

      // Mã lệnh nội dòng: `code`
      const codeMatch = part.match(/^`(.*?)`$/);
      if (codeMatch) {
        return (
          <code
            key={index}
            className="rounded bg-slate-200/70 dark:bg-slate-700 px-1 py-0.5 font-mono text-xs text-emerald-700 dark:text-emerald-300"
          >
            {codeMatch[1]}
          </code>
        );
      }

      // Văn bản thường
      return <span key={index}>{part}</span>;
    });
  };

  return (
    <div className="space-y-1.5 text-sm leading-relaxed text-slate-800 dark:text-slate-200">
      {lines.map((line, lineIndex) => {
        const trimmed = line.trim();

        // Dòng rỗng tạo khoảng cách nhẹ
        if (!trimmed) {
          return <div key={lineIndex} className="h-1" />;
        }

        // Tiêu đề cấp 3 (###) -> thẻ h4
        if (trimmed.startsWith("### ")) {
          return (
            <h4
              key={lineIndex}
              className="font-bold text-slate-900 dark:text-white pt-1"
            >
              {renderInlineFormatted(trimmed.slice(4))}
            </h4>
          );
        }

        // Tiêu đề cấp 2 (##) -> thẻ h3
        if (trimmed.startsWith("## ")) {
          return (
            <h3
              key={lineIndex}
              className="font-bold text-slate-900 dark:text-white pt-1"
            >
              {renderInlineFormatted(trimmed.slice(3))}
            </h3>
          );
        }

        // Tiêu đề cấp 1 (#) -> thẻ h2
        if (trimmed.startsWith("# ")) {
          return (
            <h2
              key={lineIndex}
              className="font-bold text-slate-900 dark:text-white pt-1"
            >
              {renderInlineFormatted(trimmed.slice(2))}
            </h2>
          );
        }

        // Khối trích dẫn / Ghi chú (>)
        if (trimmed.startsWith("> ") || trimmed.startsWith(">")) {
          const quoteContent = trimmed.startsWith("> ")
            ? trimmed.slice(2)
            : trimmed.slice(1);
          return (
            <div
              key={lineIndex}
              className="border-l-2 border-emerald-500 pl-2.5 py-0.5 my-1 text-xs italic text-slate-600 dark:text-slate-400 bg-emerald-50/50 dark:bg-emerald-950/20 rounded-r"
            >
              {renderInlineFormatted(quoteContent)}
            </div>
          );
        }

        // Danh sách gạch đầu dòng (- hoặc *)
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
          return (
            <div key={lineIndex} className="flex items-start gap-1.5 pl-2">
              <span className="text-emerald-500 font-bold select-none">•</span>
              <span className="flex-1">{renderInlineFormatted(trimmed.slice(2))}</span>
            </div>
          );
        }

        // Danh sách đánh số thứ tự (ví dụ: 1. 2.)
        const numberedMatch = trimmed.match(/^(\d+)\.\s+(.*)$/);
        if (numberedMatch) {
          return (
            <div key={lineIndex} className="flex items-start gap-1.5 pl-2">
              <span className="text-emerald-600 font-semibold select-none text-xs mt-0.5">
                {numberedMatch[1]}.
              </span>
              <span className="flex-1">{renderInlineFormatted(numberedMatch[2])}</span>
            </div>
          );
        }

        // Đoạn văn thông thường
        return <p key={lineIndex}>{renderInlineFormatted(line)}</p>;
      })}
    </div>
  );
};
