import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import '@testing-library/jest-dom/vitest';
import App from '../App';

vi.mock('@/api/authApi', () => ({
  getMyOrganizations: vi.fn().mockResolvedValue({ success: true, data: [] }),
  login: vi.fn(),
  selectOrganization: vi.fn(),
}));

vi.mock('@/api/publicApi', () => ({
  recordPublicScan: vi.fn().mockResolvedValue({ success: true, data: null }),
  getPublicTrace: vi.fn().mockResolvedValue({ success: true, data: null }),
  getPublicCertifications: vi.fn().mockResolvedValue({ success: true, data: [] }),
  getPublicInspections: vi.fn().mockResolvedValue({ success: true, data: [] }),
}));

vi.mock('@/api/aiChatApi', () => ({
  aiChatApi: {
    sendMessage: vi.fn(),
    getSuggestedPrompts: vi.fn().mockResolvedValue([]),
  },
}));

describe('App component global integration', () => {
  it('hiển thị nút mở Trợ lý AI trên trang chủ công khai (/)', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>
    );

    const aiButton = screen.getByRole('button', { name: 'Mở Trợ lý AI' });
    expect(aiButton).toBeInTheDocument();
  });

  it('hiển thị nút mở Trợ lý AI trên trang tra cứu công khai (/public/trace/BL1600000049)', () => {
    render(
      <MemoryRouter initialEntries={['/public/trace/BL1600000049']}>
        <App />
      </MemoryRouter>
    );

    const aiButton = screen.getByRole('button', { name: 'Mở Trợ lý AI' });
    expect(aiButton).toBeInTheDocument();
  });
});
