import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { ExportOpenDataForm } from '../ExportOpenDataForm';
import * as exportApi from '@/api/exportApi';
import * as profileTemplateApi from '@/api/profileTemplateApi';
import * as productCategoryApi from '@/api/productCategoryApi';

vi.mock('@/api/exportApi', () => ({
  exportOpenData: vi.fn(),
  getExportPreview: vi.fn(),
}));

vi.mock('@/api/profileTemplateApi', () => ({
  getProfileTemplates: vi.fn(),
  getAvailableFields: vi.fn(),
  getDefaultProfileTemplate: vi.fn(),
  getOpenDataPreview: vi.fn(),
}));

vi.mock('@/api/productCategoryApi', () => ({
  getProductCategories: vi.fn(),
}));

vi.mock('@/api/organizationApi', () => ({
  getOrganizations: vi.fn().mockResolvedValue([]),
}));

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      id: 'usr-1',
      roleCode: 'VT-02',
      organizationId: 'org-1111-2222',
    },
  }),
}));

const mockTemplates = [
  {
    id: 'tpl-100',
    organizationId: 'org-1111-2222',
    name: 'Mẫu xuất khẩu AEON',
    partnerName: 'AEON Mall',
    isDefault: true,
    fields: [],
  },
  {
    id: 'tpl-200',
    organizationId: 'org-1111-2222',
    name: 'Mẫu nội địa Central',
    partnerName: 'Central Retail',
    isDefault: false,
    fields: [],
  },
];

describe('ExportOpenDataForm with ProfileTemplate (NCL-07-CN-007)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(productCategoryApi.getProductCategories).mockResolvedValue([]);
    vi.mocked(profileTemplateApi.getProfileTemplates).mockResolvedValue(mockTemplates);
    vi.mocked(profileTemplateApi.getAvailableFields).mockResolvedValue([]);
    global.URL.createObjectURL = vi.fn().mockReturnValue('blob:http://localhost/test');
    global.URL.revokeObjectURL = vi.fn();
  });

  it('renders template selection field and management link for VT-02', async () => {
    render(
      <BrowserRouter>
        <ExportOpenDataForm />
      </BrowserRouter>
    );

    expect(await screen.findByText('Xuất dữ liệu mở')).toBeInTheDocument();
    expect(screen.getByText('Mẫu hồ sơ truy xuất')).toBeInTheDocument();
    expect(screen.getByText('Quản lý mẫu hồ sơ')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Xem trước/i })).toBeInTheDocument();
  });

  it('submits export request without templateId when default is chosen (TC-03)', async () => {
    const mockBlob = new Blob(['{"status":"OK"}'], { type: 'application/json' });
    vi.mocked(exportApi.exportOpenData).mockResolvedValue(mockBlob);

    render(
      <BrowserRouter>
        <ExportOpenDataForm />
      </BrowserRouter>
    );

    await screen.findByText('Xuất dữ liệu mở');

    const submitBtn = screen.getByRole('button', { name: /Xuất dữ liệu/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(exportApi.exportOpenData).toHaveBeenCalledWith(
        expect.not.objectContaining({
          templateId: expect.anything(),
        })
      );
    });
  });
});
