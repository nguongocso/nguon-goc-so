import React, { useState, useEffect, useRef } from "react";
import {
  Bot,
  Send,
  Trash2,
  Minimize2,
  Sparkles,
  Loader2,
} from "lucide-react";
import { aiChatApi } from "@/api/aiChatApi";
import { ChatMessageBubble } from "./ChatMessageBubble";
import { QuickPromptChips } from "./QuickPromptChips";
import type { ChatMessageItem, AiChatMessage } from "@/types/aiChat";

/**
 * Khóa lưu trữ lịch sử hội thoại trong sessionStorage.
 */
const STORAGE_KEY = "nguongocso_ai_chat_session_v1";

/**
 * Tin nhắn chào mừng mặc định khi bắt đầu phiên hội thoại mới.
 */
const DEFAULT_WELCOME_MESSAGE: ChatMessageItem = {
  id: "welcome-msg",
  role: "model",
  content:
    "Xin chào! Tôi là **Trợ lý AI Nguồn Gốc Số**. Tôi có thể hỗ trợ gì cho bạn về quy trình canh tác, quản lý lô hàng hay tra cứu mã QR?",
  timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
};

/**
 * Component chính AiChatWidget:
 * Bao gồm nút nổi FAB (Floating Action Button) và cửa sổ chatbox mở rộng.
 * Hỗ trợ trò chuyện với trợ lý AI, gợi ý câu hỏi nhanh, tự động lưu phiên vào sessionStorage,
 * và hiển thị phản hồi định dạng Markdown.
 */
export const AiChatWidget: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [inputValue, setInputValue] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [suggestedPrompts, setSuggestedPrompts] = useState<string[]>([]);
  const [messages, setMessages] = useState<ChatMessageItem[]>(() => {
    try {
      const saved = sessionStorage.getItem(STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed;
        }
      }
    } catch {
      // Bỏ qua lỗi truy cập sessionStorage
    }
    return [DEFAULT_WELCOME_MESSAGE];
  });

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Tự động lưu lịch sử tin nhắn vào sessionStorage mỗi khi có cập nhật
  useEffect(() => {
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(messages));
    } catch {
      // Bỏ qua lỗi vượt hạn mức bộ nhớ lưu trữ
    }
  }, [messages]);

  // Tự động cuộn xuống đáy danh sách tin nhắn khi có tin mới hoặc khi mở widget
  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView?.({ behavior: "smooth" });
    }
  }, [messages, isLoading, isOpen]);

  // Tự động điều chỉnh chiều cao của ô nhập liệu textarea theo nội dung
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 96)}px`;
    }
  }, [inputValue]);

  // Tiêu điểm tự động vào ô nhập liệu khi mở cửa sổ chat
  useEffect(() => {
    if (isOpen) {
      const timer = setTimeout(() => {
        textareaRef.current?.focus();
      }, 100);
      return () => clearTimeout(timer);
    }
  }, [isOpen]);

  // Tải danh sách câu hỏi gợi ý ban đầu từ API khi người dùng mở chat
  useEffect(() => {
    if (isOpen && suggestedPrompts.length === 0) {
      aiChatApi
        .getSuggestedPrompts()
        .then((groups) => {
          if (groups && groups.length > 0) {
            const allPrompts = groups.flatMap((g) => g.prompts);
            setSuggestedPrompts(allPrompts.slice(0, 5));
          }
        })
        .catch(() => {
          // Gợi ý mặc định khi không kết nối được tới API
          setSuggestedPrompts([
            "Quy trình tạo lô sản xuất mới?",
            "Cách ghi nhật ký canh tác chuẩn VietGAP?",
            "Làm sao để xuất mã QR cho lô hàng?",
          ]);
        });
    }
  }, [isOpen, suggestedPrompts.length]);

  /**
   * Xử lý gửi tin nhắn tới trợ lý AI.
   *
   * @param textToSend Nội dung tin nhắn cần gửi (nếu không truyền sẽ lấy từ ô nhập)
   */
  const handleSendMessage = async (textToSend?: string) => {
    const query = (textToSend || inputValue).trim();
    if (!query || isLoading) return;

    const userTimestamp = new Date().toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });

    const userMessage: ChatMessageItem = {
      id: `usr-${Date.now()}`,
      role: "user",
      content: query,
      timestamp: userTimestamp,
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputValue("");
    setIsLoading(true);

    // Chuẩn bị lịch sử trò chuyện gửi lên server (loại bỏ tin chào mừng và các tin nhắn lỗi)
    const history: AiChatMessage[] = messages
      .filter((m) => !m.isError && m.id !== "welcome-msg")
      .map((m) => ({
        role: m.role,
        content: m.content,
      }));

    try {
      const response = await aiChatApi.sendMessage({
        message: query,
        history,
      });

      const botMessage: ChatMessageItem = {
        id: `bot-${Date.now()}`,
        role: "model",
        content: response.reply,
        timestamp: new Date().toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        }),
      };

      setMessages((prev) => [...prev, botMessage]);

      // Cập nhật gợi ý câu hỏi tiếp theo nếu có
      if (response.suggestedQuestions && response.suggestedQuestions.length > 0) {
        setSuggestedPrompts(response.suggestedQuestions);
      }
    } catch {
      const errorMessage: ChatMessageItem = {
        id: `err-${Date.now()}`,
        role: "model",
        content:
          "Rất tiếc, kết nối đến Trợ lý AI đang gặp sự cố gián đoạn tạm thời. Bạn vui lòng thử lại sau ít phút.",
        timestamp: new Date().toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        }),
        isError: true,
      };
      setMessages((prev) => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Xử lý sự kiện nhấn phím trong ô nhập tin nhắn:
   * Enter để gửi tin, Shift+Enter để xuống dòng mới.
   */
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  /**
   * Xóa toàn bộ lịch sử trò chuyện trong phiên và đặt lại tin nhắn chào mừng.
   */
  const handleClearHistory = () => {
    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      // Bỏ qua lỗi
    }
    setMessages([
      {
        id: "welcome-msg",
        role: "model",
        content:
          "Phiên hội thoại đã được làm mới. Tôi có thể hỗ trợ gì thêm cho bạn?",
        timestamp: new Date().toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        }),
      },
    ]);
  };

  /**
   * Thử gửi lại câu hỏi trước đó khi gặp lỗi kết nối.
   */
  const handleRetry = (errorMsgId: string) => {
    const errorIndex = messages.findIndex((m) => m.id === errorMsgId);
    const searchSlice = errorIndex !== -1 ? messages.slice(0, errorIndex) : messages;
    const previousUserMsg = [...searchSlice].reverse().find((m) => m.role === "user");

    if (previousUserMsg) {
      handleSendMessage(previousUserMsg.content);
    } else {
      handleSendMessage();
    }
  };

  return (
    <>
      {/* Nút nổi Floating Action Button (FAB) */}
      {!isOpen && (
        <div className="fixed bottom-6 right-6 z-50">
          <button
            type="button"
            onClick={() => setIsOpen(true)}
            aria-label="Mở Trợ lý AI"
            className="group relative flex h-14 w-14 items-center justify-center rounded-full bg-gradient-to-tr from-emerald-600 to-teal-500 text-white shadow-xl hover:shadow-2xl hover:scale-105 active:scale-95 transition-all duration-300 focus:outline-none focus:ring-4 focus:ring-emerald-400/40 cursor-pointer"
          >
            <Bot className="h-7 w-7 transition-transform group-hover:rotate-6" />
            <span className="absolute -top-1 -right-1 flex h-4 w-4">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-4 w-4 bg-emerald-400 border-2 border-white"></span>
            </span>
          </button>
        </div>
      )}

      {/* Cửa sổ Chatbox Widget */}
      {isOpen && (
        <div className="fixed inset-0 sm:inset-auto sm:bottom-6 sm:right-6 sm:w-[400px] sm:h-[580px] z-50 flex flex-col bg-white dark:bg-slate-900 sm:rounded-2xl shadow-2xl border border-slate-200/80 dark:border-slate-800 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
          {/* Thanh tiêu đề Header */}
          <div className="flex items-center justify-between px-4 py-3 bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-xs">
            <div className="flex items-center gap-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-white/20 backdrop-blur-xs">
                <Bot className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-sm font-semibold leading-tight">
                  Trợ lý ảo Nguồn Gốc Số
                </h3>
                <div className="flex items-center gap-1.5 text-[11px] text-emerald-100">
                  <span className="h-2 w-2 rounded-full bg-emerald-300"></span>
                  <span>Trực tuyến 24/7</span>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-1">
              <button
                type="button"
                onClick={handleClearHistory}
                title="Xóa lịch sử trò chuyện"
                aria-label="Xóa lịch sử trò chuyện"
                className="rounded-lg p-1.5 text-white/80 hover:bg-white/20 hover:text-white transition-colors cursor-pointer"
              >
                <Trash2 className="h-4 w-4" />
              </button>
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                title="Thu nhỏ"
                aria-label="Thu nhỏ"
                className="rounded-lg p-1.5 text-white/80 hover:bg-white/20 hover:text-white transition-colors cursor-pointer"
              >
                <Minimize2 className="h-4 w-4" />
              </button>
            </div>
          </div>

          {/* Vùng cuộn tin nhắn hội thoại */}
          <div className="flex-1 overflow-y-auto p-4 space-y-2 bg-slate-50/50 dark:bg-slate-900/50">
            {messages.map((message) => (
              <ChatMessageBubble
                key={message.id}
                message={message}
                onRetry={
                  message.isError ? () => handleRetry(message.id) : undefined
                }
              />
            ))}

            {/* Hiệu ứng trạng thái đang phản hồi */}
            {isLoading && (
              <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400 py-2">
                <div className="flex h-7 w-7 items-center justify-center rounded-full bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300">
                  <Loader2 className="h-4 w-4 animate-spin" />
                </div>
                <span>Trợ lý AI đang soạn câu trả lời...</span>
              </div>
            )}

            {/* Thẻ câu hỏi gợi ý nhanh */}
            {!isLoading && suggestedPrompts.length > 0 && (
              <QuickPromptChips
                prompts={suggestedPrompts}
                onSelectPrompt={(prompt) => handleSendMessage(prompt)}
                disabled={isLoading}
              />
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Khung chân trang nhập câu hỏi */}
          <div className="p-3 bg-white dark:bg-slate-900 border-t border-slate-200/80 dark:border-slate-800">
            <div className="relative flex items-end gap-2 rounded-xl bg-slate-100 dark:bg-slate-800 p-1.5 focus-within:ring-2 focus-within:ring-emerald-500 focus-within:bg-white dark:focus-within:bg-slate-900 border border-slate-200 dark:border-slate-700 transition-all">
              <textarea
                ref={textareaRef}
                rows={1}
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Nhập câu hỏi của bạn..."
                aria-label="Nội dung tin nhắn"
                className="flex-1 resize-none bg-transparent px-2.5 py-1 text-sm text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none max-h-24 overflow-y-auto"
              />
              <button
                type="button"
                disabled={!inputValue.trim() || isLoading}
                onClick={() => handleSendMessage()}
                aria-label="Gửi tin nhắn"
                title="Gửi tin nhắn"
                className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-40 disabled:hover:bg-emerald-600 transition-colors cursor-pointer"
              >
                <Send className="h-4 w-4" />
              </button>
            </div>
            <div className="mt-1 flex items-center justify-center gap-1 text-[10px] text-slate-400">
              <Sparkles className="h-2.5 w-2.5 text-emerald-500" />
              <span>Nguồn Gốc Số AI Assistant</span>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
