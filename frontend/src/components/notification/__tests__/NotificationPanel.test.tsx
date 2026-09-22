import '@testing-library/jest-dom/vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { NotificationPanel } from '../NotificationPanel';
import type { NotificationResponse } from '@/types/notification';

const mockNotifications: NotificationResponse[] = [
  {
    id: 'notif-1',
    type: 'ALERT',
    title: 'Cảnh báo bảo mật',
    content: 'OPEN',
    entityId: null,
    isRead: false,
    readAt: null,
    createdAt: '2026-09-22T08:00:00Z',
  },
];

describe('NotificationPanel Component', () => {
  it('chỉ hiển thị duy nhất liên kết "Xem tất cả" ở chân panel và không hiển thị "Xem x thông báo chưa đọc"', () => {
    const handleClose = vi.fn();

    render(
      <MemoryRouter>
        <NotificationPanel
          items={mockNotifications}
          isLoading={false}
          onItemClick={vi.fn()}
          unreadCount={5}
          onClose={handleClose}
        />
      </MemoryRouter>,
    );

    // Xác nhận có nút "Xem tất cả"
    const viewAllLink = screen.getByRole('link', { name: 'Xem tất cả' });
    expect(viewAllLink).toBeInTheDocument();
    expect(viewAllLink).toHaveAttribute('href', '/notifications');

    // Xác nhận không tồn tại nút hoặc chữ "thông báo chưa đọc" ở danh sách xem
    expect(screen.queryByText(/Xem \d+ thông báo chưa đọc/i)).not.toBeInTheDocument();

    // Nhấp vào "Xem tất cả" sẽ gọi hàm onClose
    fireEvent.click(viewAllLink);
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('gọi hàm onMarkAllAsRead khi người dùng bấm "Đánh dấu tất cả đã đọc"', () => {
    const handleMarkAllAsRead = vi.fn();

    render(
      <MemoryRouter>
        <NotificationPanel
          items={mockNotifications}
          isLoading={false}
          onItemClick={vi.fn()}
          unreadCount={3}
          onMarkAllAsRead={handleMarkAllAsRead}
        />
      </MemoryRouter>,
    );

    const markAllBtn = screen.getByRole('button', { name: 'Đánh dấu tất cả đã đọc' });
    expect(markAllBtn).toBeInTheDocument();

    fireEvent.click(markAllBtn);
    expect(handleMarkAllAsRead).toHaveBeenCalledTimes(1);
  });
});
