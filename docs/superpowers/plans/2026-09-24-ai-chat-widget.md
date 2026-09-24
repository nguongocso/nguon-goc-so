# AI Chatbot Widget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Xây dựng Widget Trợ lý AI Nguồn Gốc Số dạng nút tròn nổi (FAB) kèm khung chatbox tương tác, hỗ trợ render Markdown, gợi ý câu hỏi nhanh (Quick Chips) và lưu phiên hội thoại, tích hợp vào layout chung của ứng dụng frontend.

**Architecture:** Tách biệt thành các module độc lập: Types definition, API client layer (gọi Backend `/api/v1/ai`), Markdown renderer thuần hỗ trợ liên kết nội bộ `react-router-dom`, Message bubble hỗ trợ sao chép, Quick prompt chips theo vai trò người dùng, và Container Widget quản lý trạng thái hiển thị và lưu `sessionStorage`.

**Tech Stack:** React 19, TypeScript, Tailwind CSS v4, Lucide React icons, Vitest, React Testing Library, Axios client.

**Spec:** `docs/superpowers/specs/2026-09-24-ai-chat-widget-design.md`

## Global Constraints

- Không dùng thư viện ngoài chưa có trong `package.json` trừ khi cần thiết; tận dụng Tailwind CSS, Lucide icons và React chuẩn.
- Các bình luận và ghi chú code dùng tiếng Việt rõ ràng, đúng chính tả theo quy định repo.
- Đảm bảo tương thích hoàn toàn TypeScript không có lỗi `any` hoặc lint warning.
- Mọi bài test chạy bằng `npm run test` (Vitest) phải PASS 100%.

---

### Task 1: Type Definitions & API Client Layer

**Files:**
- Create: `frontend/src/types/aiChat.ts`
- Create: `frontend/src/api/aiChatApi.ts`
- Test: `frontend/src/api/__tests__/aiChatApi.test.ts`

**Interfaces:**
- Consumes: `apiClient` từ `@/api/axiosConfig`, `ApiResponse` từ `@/types/api`
- Produces: `AiChatMessage`, `AiChatRequest`, `AiChatResponse`, `AiPromptSuggestion`, `aiChatApi`

- [ ] **Step 1: Viết test thất bại cho aiChatApi**

```typescript
// frontend/src/api/__tests__/aiChatApi.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { aiChatApi } from '../aiChatApi';
import apiClient from '../axiosConfig';

vi.mock('../axiosConfig', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}));

describe('aiChatApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('gửi tin nhắn chat tới backend thành công', async () => {
    const mockResponse = {
      data: {
        success: true,
        status: 200,
        message: 'Thành công',
        data: {
          reply: 'Xin chào bạn!',
          timestamp: '2026-09-24T18:00:00',
          suggestedQuestions: ['Câu hỏi tiếp theo?'],
        },
      },
    };

    (apiClient.post as any).mockResolvedValueOnce(mockResponse);

    const result = await aiChatApi.sendMessage({
      message: 'Xin chào',
      history: [],
    });

    expect(apiClient.post).toHaveBeenCalledWith('/ai/chat', {
      message: 'Xin chào',
      history: [],
    });
    expect(result).toEqual(mockResponse.data.data);
  });

  it('lấy danh sách câu hỏi gợi ý thành công', async () => {
    const mockResponse = {
      data: {
        success: true,
        status: 200,
        message: 'Thành công',
        data: [
          {
            category: 'Phổ biến',
            prompts: ['Nguồn Gốc Số là gì?'],
          },
        ],
      },
    };

    (apiClient.get as any).mockResolvedValueOnce(mockResponse);

    const result = await aiChatApi.getSuggestedPrompts();

    expect(apiClient.get).toHaveBeenCalledWith('/ai/suggested-prompts');
    expect(result).toEqual(mockResponse.data.data);
  });
});
```

- [ ] **Step 2: Chạy test để xác nhận fail**

Run: `npm run test -- frontend/src/api/__tests__/aiChatApi.test.ts`
Expected: FAIL với "Cannot find module '../aiChatApi'"

- [ ] **Step 3: Tạo types và hiện thực aiChatApi**

```typescript
// frontend/src/types/aiChat.ts
export type MessageRole = 'user' | 'model' | 'assistant';

export interface AiChatMessage {
  role: MessageRole;
  content: string;
}

export interface ChatMessageItem {
  id: string;
  role: 'user' | 'model';
  content: string;
  timestamp: string;
  isError?: boolean;
}

export interface AiChatRequest {
  message: string;
  history?: AiChatMessage[];
}

export interface AiChatResponse {
  reply: string;
  timestamp: string;
  suggestedQuestions: string[];
}

export interface AiPromptSuggestion {
  category: string;
  prompts: string[];
}
```

```typescript
// frontend/src/api/aiChatApi.ts
import apiClient from './axiosConfig';
import type { ApiResponse } from '@/types/api';
import type {
  AiChatRequest,
  AiChatResponse,
  AiPromptSuggestion,
} from '@/types/aiChat';

/**
 * Service API giao tiếp với phân hệ AI Chatbot Nguồn Gốc Số.
 */
export const aiChatApi = {
  /**
   * Gửi câu hỏi đến Trợ lý AI và nhận phản hồi nghiệp vụ.
   */
  sendMessage: async (data: AiChatRequest): Promise<AiChatResponse> => {
    const response = await apiClient.post<ApiResponse<AiChatResponse>>(
      '/ai/chat',
      data,
    );
    return response.data.data;
  },

  /**
   * Lấy danh sách câu hỏi gợi ý phù hợp theo vai trò người dùng.
   */
  getSuggestedPrompts: async (): Promise<AiPromptSuggestion[]> => {
    const response = await apiClient.get<ApiResponse<AiPromptSuggestion[]>>(
      '/ai/suggested-prompts',
    );
    return response.data.data;
  },
};
```

- [ ] **Step 4: Chạy lại test để xác nhận pass**

Run: `npm run test -- frontend/src/api/__tests__/aiChatApi.test.ts`
Expected: PASS

- [ ] **Step 5: Commit code Task 1**

```bash
git add frontend/src/types/aiChat.ts frontend/src/api/aiChatApi.ts frontend/src/api/__tests__/aiChatApi.test.ts
git commit -m "feat(fe-ai): add ai chat types and api client layer"
```

---

### Task 2: Component AiMarkdownRenderer (Hỗ trợ Bold, Lists, Code, Deep Links)

**Files:**
- Create: `frontend/src/components/ai/AiMarkdownRenderer.tsx`
- Test: `frontend/src/components/ai/__tests__/AiMarkdownRenderer.test.tsx`

**Interfaces:**
- Consumes: `Link` từ `react-router-dom`
- Produces: `<AiMarkdownRenderer content={string} />`

- [ ] **Step 1: Viết test thất bại cho AiMarkdownRenderer**

```typescript
// frontend/src/components/ai/__tests__/AiMarkdownRenderer.test.tsx
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect } from 'vitest';
import { AiMarkdownRenderer } from '../AiMarkdownRenderer';

describe('AiMarkdownRenderer', () => {
  it('hiển thị văn bản thuần bình thường', () => {
    render(
      <BrowserRouter>
        <AiMarkdownRenderer content="Xin chào trợ lý ảo" />
      </BrowserRouter>,
    );
    expect(screen.getByText('Xin chào trợ lý ảo')).toBeInTheDocument();
  });

  it('hiển thị chữ in đậm', () => {
    render(
      <BrowserRouter>
        <AiMarkdownRenderer content="Vui lòng nhấn **Bước 1** để tiếp tục" />
      </BrowserRouter>,
    );
    const boldEl = screen.getByText('Bước 1');
    expect(boldEl.tagName).toBe('STRONG');
  });

  it('hiển thị danh sách gạch đầu dòng', () => {
    render(
      <BrowserRouter>
        <AiMarkdownRenderer content="- Mục thứ nhất\n- Mục thứ hai" />
      </BrowserRouter>,
    );
    expect(screen.getByText('Mục thứ nhất')).toBeInTheDocument();
    expect(screen.getByText('Mục thứ hai')).toBeInTheDocument();
  });

  it('chuyển đổi link nội bộ thành React Router Link', () => {
    render(
      <BrowserRouter>
        <AiMarkdownRenderer content="Xem tại [Quản lý lô sản xuất](/production-lots)" />
      </BrowserRouter>,
    );
    const link = screen.getByRole('link', { name: 'Quản lý lô sản xuất' });
    expect(link).toHaveAttribute('href', '/production-lots');
  });
});
```

- [ ] **Step 2: Chạy test để xác nhận fail**

Run: `npm run test -- frontend/src/components/ai/__tests__/AiMarkdownRenderer.test.tsx`
Expected: FAIL với "Cannot find module '../AiMarkdownRenderer'"

- [ ] **Step 3: Hiện thực AiMarkdownRenderer**

```typescript
// frontend/src/components/ai/AiMarkdownRenderer.tsx
import React from 'react';
import { Link } from 'react-router-dom';

interface AiMarkdownRendererProps {
  content: string;
}

/**
 * Phân tích và hiển thị chuỗi văn bản Markdown tinh gọn, an toàn, hỗ trợ Link nội bộ React Router.
 */
export const AiMarkdownRenderer: React.FC<AiMarkdownRendererProps> = ({ content }) => {
  if (!content) return null;

  // Tách văn bản thành từng dòng để render theo khối
  const lines = content.split('\n');

  // Hàm render inline: in đậm (**text**), in nghiêng (*text*), link ([text](url)), code inline (`text`)
  const renderInlineFormatted = (text: string): React.ReactNode[] => {
    // Regex nhận diện [text](url), **bold**, `code`
    const regex = /(\[.*?\]\(.*?\)|\*\*.*?\*\*|`.*?`)/g;
    const parts = text.split(regex);

    return parts.map((part, index) => {
      // Link: [Text](URL)
      const linkMatch = part.match(/^\[(.*?)\]\((.*?)\)$/);
      if (linkMatch) {
        const [, linkText, linkUrl] = linkMatch;
        const isExternal = linkUrl.startsWith('http://') || linkUrl.startsWith('https://');
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

      // Bold: **Text**
      const boldMatch = part.match(/^\*\*(.*?)\*\*$/);
      if (boldMatch) {
        return (
          <strong key={index} className="font-semibold text-slate-900 dark:text-slate-100">
            {boldMatch[1]}
          </strong>
        );
      }

      // Inline code: `code`
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

      return <span key={index}>{part}</span>;
    });
  };

  return (
    <div className="space-y-1.5 text-sm leading-relaxed text-slate-800 dark:text-slate-200">
      {lines.map((line, lineIndex) => {
        const trimmed = line.trim();

        // Dòng rỗng
        if (!trimmed) {
          return <div key={lineIndex} className="h-1" />;
        }

        // Tiêu đề Markdown ###
        if (trimmed.startsWith('### ')) {
          return (
            <h4 key={lineIndex} className="font-bold text-slate-900 dark:text-white pt-1">
              {renderInlineFormatted(trimmed.slice(4))}
            </h4>
          );
        }

        // Khối trích dẫn / Lưu ý >
        if (trimmed.startsWith('> ')) {
          return (
            <div
              key={lineIndex}
              className="border-l-2 border-emerald-500 pl-2.5 py-0.5 my-1 text-xs italic text-slate-600 dark:text-slate-400 bg-emerald-50/50 dark:bg-emerald-950/20 rounded-r"
            >
              {renderInlineFormatted(trimmed.slice(2))}
            </div>
          );
        }

        // Danh sách gạch đầu dòng - hoặc *
        if (trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
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
```

- [ ] **Step 4: Chạy lại test để xác nhận pass**

Run: `npm run test -- frontend/src/components/ai/__tests__/AiMarkdownRenderer.test.tsx`
Expected: PASS

- [ ] **Step 5: Commit code Task 2**

```bash
git add frontend/src/components/ai/AiMarkdownRenderer.tsx frontend/src/components/ai/__tests__/AiMarkdownRenderer.test.tsx
git commit -m "feat(fe-ai): add rich markdown renderer with internal link support"
```

---

### Task 3: Quick Prompt Chips & Chat Message Bubble Component

**Files:**
- Create: `frontend/src/components/ai/QuickPromptChips.tsx`
- Create: `frontend/src/components/ai/ChatMessageBubble.tsx`
- Test: `frontend/src/components/ai/__tests__/ChatMessageBubble.test.tsx`

**Interfaces:**
- Consumes: `AiPromptSuggestion`, `ChatMessageItem`, `AiMarkdownRenderer`
- Produces: `<QuickPromptChips />`, `<ChatMessageBubble />`

- [ ] **Step 1: Viết test cho ChatMessageBubble**

```typescript
// frontend/src/components/ai/__tests__/ChatMessageBubble.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi } from 'vitest';
import { ChatMessageBubble } from '../ChatMessageBubble';

describe('ChatMessageBubble', () => {
  it('hiển thị tin nhắn người dùng bên phải', () => {
    render(
      <BrowserRouter>
        <ChatMessageBubble
          message={{
            id: '1',
            role: 'user',
            content: 'Tôi muốn tạo lô mới',
            timestamp: '18:30',
          }}
        />
      </BrowserRouter>,
    );
    expect(screen.getByText('Tôi muốn tạo lô mới')).toBeInTheDocument();
  });

  it('hiển thị tin nhắn trợ lý AI kèm nút sao chép', () => {
    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn().mockImplementation(() => Promise.resolve()),
      },
    });

    render(
      <BrowserRouter>
        <ChatMessageBubble
          message={{
            id: '2',
            role: 'model',
            content: 'Để tạo lô mới bạn vào mục Lô sản xuất',
            timestamp: '18:31',
          }}
        />
      </BrowserRouter>,
    );

    expect(screen.getByText(/Để tạo lô mới/)).toBeInTheDocument();
    const copyBtn = screen.getByTitle('Sao chép câu trả lời');
    expect(copyBtn).toBeInTheDocument();
    fireEvent.click(copyBtn);
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
      'Để tạo lô mới bạn vào mục Lô sản xuất',
    );
  });
});
```

- [ ] **Step 2: Chạy test để xác nhận fail**

Run: `npm run test -- frontend/src/components/ai/__tests__/ChatMessageBubble.test.tsx`
Expected: FAIL với "Cannot find module '../ChatMessageBubble'"

- [ ] **Step 3: Hiện thực QuickPromptChips và ChatMessageBubble**

```typescript
// frontend/src/components/ai/QuickPromptChips.tsx
import React from 'react';
import { Sparkles } from 'lucide-react';

interface QuickPromptChipsProps {
  prompts: string[];
  onSelectPrompt: (prompt: string) => void;
  disabled?: boolean;
}

export const QuickPromptChips: React.FC<QuickPromptChipsProps> = ({
  prompts,
  onSelectPrompt,
  disabled = false,
}) => {
  if (!prompts || prompts.length === 0) return null;

  return (
    <div className="flex flex-col gap-1.5 my-2">
      <div className="flex items-center gap-1 text-xs font-medium text-slate-500 dark:text-slate-400">
        <Sparkles className="h-3 w-3 text-emerald-600" />
        <span>Gợi ý câu hỏi nhanh:</span>
      </div>
      <div className="flex flex-wrap gap-1.5">
        {prompts.map((prompt, index) => (
          <button
            key={index}
            type="button"
            disabled={disabled}
            onClick={() => onSelectPrompt(prompt)}
            className="text-left text-xs bg-emerald-50 hover:bg-emerald-100 text-emerald-800 dark:bg-emerald-950/40 dark:hover:bg-emerald-900/60 dark:text-emerald-200 border border-emerald-200 dark:border-emerald-800 rounded-full px-3 py-1.5 transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-xs"
          >
            {prompt}
          </button>
        ))}
      </div>
    </div>
  );
};
```

```typescript
// frontend/src/components/ai/ChatMessageBubble.tsx
import React, { useState } from 'react';
import { Bot, User, Check, Copy } from 'lucide-react';
import { AiMarkdownRenderer } from './AiMarkdownRenderer';
import type { ChatMessageItem } from '@/types/aiChat';

interface ChatMessageBubbleProps {
  message: ChatMessageItem;
  onRetry?: () => void;
}

export const ChatMessageBubble: React.FC<ChatMessageBubbleProps> = ({
  message,
  onRetry,
}) => {
  const [copied, setCopied] = useState(false);
  const isUser = message.role === 'user';

  const handleCopy = async () => {
    if (!message.content) return;
    try {
      await navigator.clipboard.writeText(message.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Không thể sao chép văn bản:', err);
    }
  };

  return (
    <div
      className={`flex items-start gap-2.5 my-3 ${
        isUser ? 'flex-row-reverse' : 'flex-row'
      }`}
    >
      {/* Avatar */}
      <div
        className={`shrink-0 flex items-center justify-center rounded-full h-8 w-8 text-xs font-semibold shadow-xs select-none ${
          isUser
            ? 'bg-emerald-700 text-white'
            : 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-700'
        }`}
      >
        {isUser ? <User className="h-4 w-4" /> : <Bot className="h-4 w-4" />}
      </div>

      {/* Bubble Container */}
      <div
        className={`group relative max-w-[85%] rounded-2xl px-3.5 py-2.5 shadow-xs ${
          isUser
            ? 'bg-emerald-600 text-white rounded-tr-xs'
            : message.isError
            ? 'bg-red-50 text-red-900 border border-red-200 dark:bg-red-950/40 dark:text-red-200 rounded-tl-xs'
            : 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-100 rounded-tl-xs border border-slate-200/60 dark:border-slate-700'
        }`}
      >
        {isUser ? (
          <p className="text-sm leading-relaxed whitespace-pre-wrap">{message.content}</p>
        ) : (
          <AiMarkdownRenderer content={message.content} />
        )}

        {/* Footer thời gian & Nút Copy */}
        <div
          className={`flex items-center justify-between gap-3 mt-1 text-[10px] select-none ${
            isUser ? 'text-emerald-100' : 'text-slate-400 dark:text-slate-400'
          }`}
        >
          <span>{message.timestamp}</span>

          {!isUser && !message.isError && (
            <button
              type="button"
              onClick={handleCopy}
              title="Sao chép câu trả lời"
              className="opacity-0 group-hover:opacity-100 transition-opacity p-0.5 hover:text-emerald-600 dark:hover:text-emerald-400 cursor-pointer"
            >
              {copied ? (
                <span className="flex items-center gap-0.5 text-emerald-600 font-medium">
                  <Check className="h-3 w-3" /> Đã chép
                </span>
              ) : (
                <Copy className="h-3 w-3" />
              )}
            </button>
          )}
        </div>

        {/* Nút Thử lại khi tin nhắn lỗi */}
        {message.isError && onRetry && (
          <button
            type="button"
            onClick={onRetry}
            className="mt-2 text-xs font-medium text-red-600 hover:text-red-700 underline"
          >
            Thử gửi lại câu hỏi
          </button>
        )}
      </div>
    </div>
  );
};
```

- [ ] **Step 4: Chạy test ChatMessageBubble để xác nhận pass**

Run: `npm run test -- frontend/src/components/ai/__tests__/ChatMessageBubble.test.tsx`
Expected: PASS

- [ ] **Step 5: Commit code Task 3**

```bash
git add frontend/src/components/ai/QuickPromptChips.tsx frontend/src/components/ai/ChatMessageBubble.tsx frontend/src/components/ai/__tests__/ChatMessageBubble.test.tsx
git commit -m "feat(fe-ai): add quick prompt chips and message bubble components"
```

---

### Task 4: Complete AiChatWidget Component (FAB + Chatbox Window)

**Files:**
- Create: `frontend/src/components/ai/AiChatWidget.tsx`
- Test: `frontend/src/components/ai/__tests__/AiChatWidget.test.tsx`

**Interfaces:**
- Consumes: `aiChatApi`, `QuickPromptChips`, `ChatMessageBubble`, `useMediaQuery`
- Produces: `<AiChatWidget />`

- [ ] **Step 1: Viết test cho AiChatWidget**

```typescript
// frontend/src/components/ai/__tests__/AiChatWidget.test.tsx
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AiChatWidget } from '../AiChatWidget';
import { aiChatApi } from '@/api/aiChatApi';

vi.mock('@/api/aiChatApi', () => ({
  aiChatApi: {
    sendMessage: vi.fn(),
    getSuggestedPrompts: vi.fn().mockResolvedValue([
      {
        category: 'Gợi ý',
        prompts: ['Cách tạo lô sản xuất?'],
      },
    ]),
  },
}));

describe('AiChatWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
  });

  it('render nút nổi FAB ban đầu và mở chat window khi bấm', async () => {
    render(
      <BrowserRouter>
        <AiChatWidget />
      </BrowserRouter>,
    );

    const fabButton = screen.getByLabelText('Mở Trợ lý AI');
    expect(fabButton).toBeInTheDocument();

    fireEvent.click(fabButton);

    expect(screen.getByText('Trợ lý ảo Nguồn Gốc Số')).toBeInTheDocument();
  });

  it('gửi tin nhắn và hiển thị phản hồi từ AI', async () => {
    (aiChatApi.sendMessage as any).mockResolvedValueOnce({
      reply: 'Đây là câu trả lời từ AI.',
      timestamp: '2026-09-24T18:00:00',
      suggestedQuestions: ['Câu hỏi tiếp theo?'],
    });

    render(
      <BrowserRouter>
        <AiChatWidget />
      </BrowserRouter>,
    );

    // Mở widget
    fireEvent.click(screen.getByLabelText('Mở Trợ lý AI'));

    // Nhập câu hỏi
    const input = screen.getByPlaceholderText('Nhập câu hỏi của bạn...');
    fireEvent.change(input, { target: { value: 'Hướng dẫn tạo lô' } });

    // Bấm gửi
    const sendButton = screen.getByLabelText('Gửi tin nhắn');
    fireEvent.click(sendButton);

    await waitFor(() => {
      expect(screen.getByText('Đây là câu trả lời từ AI.')).toBeInTheDocument();
    });
  });
});
```

- [ ] **Step 2: Chạy test để xác nhận fail**

Run: `npm run test -- frontend/src/components/ai/__tests__/AiChatWidget.test.tsx`
Expected: FAIL với "Cannot find module '../AiChatWidget'"

- [ ] **Step 3: Hiện thực AiChatWidget**

```typescript
// frontend/src/components/ai/AiChatWidget.tsx
import React, { useState, useEffect, useRef } from 'react';
import {
  Bot,
  X,
  Send,
  Trash2,
  Minimize2,
  Sparkles,
  Loader2,
} from 'lucide-react';
import { aiChatApi } from '@/api/aiChatApi';
import { ChatMessageBubble } from './ChatMessageBubble';
import { QuickPromptChips } from './QuickPromptChips';
import type { ChatMessageItem, AiChatMessage } from '@/types/aiChat';

const STORAGE_KEY = 'nguongocso_ai_chat_session_v1';

export const AiChatWidget: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [suggestedPrompts, setSuggestedPrompts] = useState<string[]>([]);
  const [messages, setMessages] = useState<ChatMessageItem[]>(() => {
    try {
      const saved = sessionStorage.getItem(STORAGE_KEY);
      if (saved) {
        return JSON.parse(saved);
      }
    } catch {
      // Fallback
    }
    return [
      {
        id: 'welcome-msg',
        role: 'model',
        content:
          'Xin chào! Tôi là **Trợ lý AI Nguồn Gốc Số**. Tôi có thể hỗ trợ gì cho bạn về quy trình canh tác, quản lý lô hàng hay tra cứu mã QR?',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      },
    ];
  });

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Lưu tin nhắn vào sessionStorage
  useEffect(() => {
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(messages));
    } catch {
      // Bỏ qua lỗi quota
    }
  }, [messages]);

  // Tự động cuộn xuống dưới cùng khi có tin nhắn mới
  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isLoading, isOpen]);

  // Tải danh sách gợi ý câu hỏi khi mở chat
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
          setSuggestedPrompts([
            'Quy trình tạo lô sản xuất mới?',
            'Cách ghi nhật ký canh tác chuẩn VietGAP?',
            'Làm sao để xuất mã QR cho lô hàng?',
          ]);
        });
    }
  }, [isOpen, suggestedPrompts.length]);

  const handleSendMessage = async (textToSend?: string) => {
    const query = (textToSend || inputValue).trim();
    if (!query || isLoading) return;

    const userTimestamp = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const userMessage: ChatMessageItem = {
      id: `usr-${Date.now()}`,
      role: 'user',
      content: query,
      timestamp: userTimestamp,
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputValue('');
    setIsLoading(true);

    // Chuyển đổi lịch sử chat cho backend
    const history: AiChatMessage[] = messages
      .filter((m) => !m.isError && m.id !== 'welcome-msg')
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
        role: 'model',
        content: response.reply,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };

      setMessages((prev) => [...prev, botMessage]);

      if (response.suggestedQuestions && response.suggestedQuestions.length > 0) {
        setSuggestedPrompts(response.suggestedQuestions);
      }
    } catch {
      const errorMessage: ChatMessageItem = {
        id: `err-${Date.now()}`,
        role: 'model',
        content:
          'Rất tiếc, kết nối đến Trợ lý AI đang gặp sự cố gián đoạn tạm thời. Bạn vui lòng thử lại sau ít phút.',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        isError: true,
      };
      setMessages((prev) => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const handleClearHistory = () => {
    sessionStorage.removeItem(STORAGE_KEY);
    setMessages([
      {
        id: 'welcome-msg',
        role: 'model',
        content:
          'Phiên hội thoại đã được làm mới. Tôi có thể hỗ trợ gì thêm cho bạn?',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      },
    ]);
  };

  return (
    <>
      {/* Floating Action Button (FAB) */}
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
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-xs">
            <div className="flex items-center gap-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-white/20 backdrop-blur-xs">
                <Bot className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-sm font-semibold leading-tight">Trợ lý ảo Nguồn Gốc Số</h3>
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
                className="rounded-lg p-1.5 text-white/80 hover:bg-white/20 hover:text-white transition-colors cursor-pointer"
              >
                <Trash2 className="h-4 w-4" />
              </button>
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                title="Thu nhỏ"
                className="rounded-lg p-1.5 text-white/80 hover:bg-white/20 hover:text-white transition-colors cursor-pointer"
              >
                <Minimize2 className="h-4 w-4" />
              </button>
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                title="Đóng"
                className="rounded-lg p-1.5 text-white/80 hover:bg-white/20 hover:text-white transition-colors cursor-pointer"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          </div>

          {/* Vùng cuộn tin nhắn */}
          <div className="flex-1 overflow-y-auto p-4 space-y-2 bg-slate-50/50 dark:bg-slate-900/50">
            {messages.map((message) => (
              <ChatMessageBubble
                key={message.id}
                message={message}
                onRetry={message.isError ? () => handleSendMessage() : undefined}
              />
            ))}

            {/* Loading Indicator */}
            {isLoading && (
              <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400 py-2">
                <div className="flex h-7 w-7 items-center justify-center rounded-full bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300">
                  <Loader2 className="h-4 w-4 animate-spin" />
                </div>
                <span>Trợ lý AI đang soạn câu trả lời...</span>
              </div>
            )}

            {/* Quick Chips gợi ý */}
            {!isLoading && suggestedPrompts.length > 0 && (
              <QuickPromptChips
                prompts={suggestedPrompts}
                onSelectPrompt={(prompt) => handleSendMessage(prompt)}
                disabled={isLoading}
              />
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Footer Input */}
          <div className="p-3 bg-white dark:bg-slate-900 border-t border-slate-200/80 dark:border-slate-800">
            <div className="relative flex items-end gap-2 rounded-xl bg-slate-100 dark:bg-slate-800 p-1.5 focus-within:ring-2 focus-within:ring-emerald-500 focus-within:bg-white dark:focus-within:bg-slate-900 border border-slate-200 dark:border-slate-700 transition-all">
              <textarea
                ref={textareaRef}
                rows={1}
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Nhập câu hỏi của bạn..."
                className="flex-1 resize-none bg-transparent px-2.5 py-1 text-sm text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none max-h-24 overflow-y-auto"
              />
              <button
                type="button"
                disabled={!inputValue.trim() || isLoading}
                onClick={() => handleSendMessage()}
                aria-label="Gửi tin nhắn"
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
```

- [ ] **Step 4: Chạy test AiChatWidget để xác nhận pass**

Run: `npm run test -- frontend/src/components/ai/__tests__/AiChatWidget.test.tsx`
Expected: PASS

- [ ] **Step 5: Commit code Task 4**

```bash
git add frontend/src/components/ai/AiChatWidget.tsx frontend/src/components/ai/__tests__/AiChatWidget.test.tsx
git commit -m "feat(fe-ai): complete ai chat widget component with fab and chat window"
```

---

### Task 5: Integration into MainLayout and End-to-End Verification

**Files:**
- Modify: `frontend/src/components/layout/MainLayout.tsx`
- Run: `npm run test`
- Run: `npm run build`

**Interfaces:**
- Consumes: `<AiChatWidget />`
- Produces: Toàn bộ các trang đăng nhập được đính kèm nút Trợ lý AI

- [ ] **Step 1: Nhúng AiChatWidget vào MainLayout**

Mở `frontend/src/components/layout/MainLayout.tsx`, import `AiChatWidget` và đặt `<AiChatWidget />` ở cuối component (cạnh thẻ đóng layout).

- [ ] **Step 2: Chạy toàn bộ test suite frontend để kiểm tra regression**

Run: `npm run test`
Expected: Tất cả các file test (bao gồm các test mới và 74 test suites cũ) đều PASS 100%.

- [ ] **Step 3: Chạy build frontend để đảm bảo không lỗi TypeScript & Vite**

Run: `npm run build`
Expected: `tsc -b && vite build` hoàn tất thành công.

- [ ] **Step 4: Commit code Task 5**

```bash
git add frontend/src/components/layout/MainLayout.tsx
git commit -m "feat(fe-ai): integrate ai chat widget into main layout"
```
