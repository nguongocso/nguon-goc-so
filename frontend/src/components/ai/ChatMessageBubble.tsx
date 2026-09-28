import React, { useState } from "react";
import { Bot, User, Check, Copy } from "lucide-react";
import { AiMarkdownRenderer } from "./AiMarkdownRenderer";
import type { ChatMessageItem } from "@/types/aiChat";

/**
 * Định dạng mốc thời gian hiển thị của tin nhắn thành dạng HH:mm nếu cần.
 */
const formatTimestamp = (timestamp?: string): string => {
  if (!timestamp) return "";
  // Nếu đã ở dạng giờ phút (ví dụ "10:30")
  if (!timestamp.includes("T") && !timestamp.includes("-")) {
    return timestamp;
  }
  const date = new Date(timestamp);
  if (isNaN(date.getTime())) {
    return timestamp;
  }
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${hours}:${minutes}`;
};

/**
 * Thuộc tính đầu vào cho component ChatMessageBubble.
 */
export interface ChatMessageBubbleProps {
  /** Thông tin tin nhắn cần hiển thị */
  message: ChatMessageItem;
  /** Hàm callback để thử lại khi tin nhắn bị lỗi */
  onRetry?: () => void;
}

/**
 * Component hiển thị một bong bóng tin nhắn đơn lẻ trong hộp thoại chat.
 * Hỗ trợ phân biệt tin nhắn của người dùng và trợ lý ảo AI, hiển thị văn bản Markdown,
 * sao chép nội dung câu trả lời và nút gửi lại khi gặp lỗi.
 */
export const ChatMessageBubble: React.FC<ChatMessageBubbleProps> = ({
  message,
  onRetry,
}) => {
  const [copied, setCopied] = useState(false);
  const isUser = message.role === "user";

  /**
   * Xử lý sao chép nội dung tin nhắn vào clipboard.
   */
  const handleCopy = async () => {
    if (!message.content) return;
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(message.content);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      }
    } catch (err) {
      console.error("Không thể sao chép văn bản vào clipboard:", err);
    }
  };

  return (
    <div
      className={`flex items-start gap-2.5 my-3 ${
        isUser ? "flex-row-reverse" : "flex-row"
      }`}
    >
      {/* Ảnh đại diện (Avatar) */}
      <div
        className={`shrink-0 flex items-center justify-center rounded-full h-8 w-8 text-xs font-semibold shadow-xs select-none ${
          isUser
            ? "bg-emerald-700 text-white"
            : "bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-700"
        }`}
      >
        {isUser ? <User className="h-4 w-4" /> : <Bot className="h-4 w-4" />}
      </div>

      {/* Khung nội dung tin nhắn */}
      <div
        className={`group relative max-w-[85%] rounded-2xl px-3.5 py-2.5 shadow-xs ${
          isUser
            ? "bg-emerald-600 text-white rounded-tr-xs"
            : message.isError
            ? "bg-red-50 text-red-900 border border-red-200 dark:bg-red-950/40 dark:text-red-200 rounded-tl-xs"
            : "bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-100 rounded-tl-xs border border-slate-200/60 dark:border-slate-700"
        }`}
      >
        {/* Nội dung tin nhắn: người dùng hiển thị text thường, bot hiển thị markdown */}
        {isUser ? (
          <p className="text-sm leading-relaxed whitespace-pre-wrap">
            {message.content}
          </p>
        ) : (
          <AiMarkdownRenderer content={message.content} />
        )}

        {/* Thanh chân trang: thời gian & nút sao chép */}
        <div
          className={`flex items-center justify-between gap-3 mt-1 text-[10px] select-none ${
            isUser ? "text-emerald-100" : "text-slate-400 dark:text-slate-400"
          }`}
        >
          <span>{formatTimestamp(message.timestamp)}</span>

          {!isUser && !message.isError && (
            <button
              type="button"
              onClick={handleCopy}
              title="Sao chép câu trả lời"
              aria-label="Sao chép câu trả lời"
              className="opacity-0 group-hover:opacity-100 focus:opacity-100 transition-opacity p-0.5 hover:text-emerald-600 dark:hover:text-emerald-400 cursor-pointer"
            >
              {copied ? (
                <span className="flex items-center gap-0.5 text-emerald-600 dark:text-emerald-400 font-medium">
                  <Check className="h-3 w-3" /> Đã chép
                </span>
              ) : (
                <Copy className="h-3 w-3" />
              )}
            </button>
          )}
        </div>

        {/* Nút gửi lại câu hỏi khi gặp lỗi */}
        {message.isError && onRetry && (
          <button
            type="button"
            onClick={onRetry}
            className="mt-2 text-xs font-medium text-red-600 hover:text-red-700 underline cursor-pointer"
          >
            Thử gửi lại câu hỏi
          </button>
        )}
      </div>
    </div>
  );
};
