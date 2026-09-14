import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import ProfileTemplateFormPage from '@/pages/export/ProfileTemplateFormPage';
import * as profileTemplateApi from '@/api/profileTemplateApi';

// Mock API
vi.mock('@/api/profileTemplateApi', () => ({
  getAvailableFields: vi.fn(),
  getProfileTemplateById: vi.fn(),
  createProfileTemplate: vi.fn(),
  updateProfileTemplate: vi.fn(),
  deleteProfileTemplate: vi.fn(),
  getOpenDataPreview: vi.fn(),
}));

// Mock useAuth
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      id: 'usr-1',
      username: 'manager_user',
      roleCode: 'VT-02',
      organizationId: 'org-1111-2222',
    },
  }),
}));

const mockAvailableGroups = [
  {
    group: 'ORGANIZATION',
    groupLabel: 'Thông tin tổ chức',
    fields: [
      { key: 'org_name', label: 'Tên tổ chức', isMandatory: true },
      { key: 'org_address', label: 'Địa chỉ', isMandatory: false },
    ],
  },
  {
    group: 'PRODUCTION_LOT',
    groupLabel: 'Lô sản xuất',
    fields: [
      { key: 'lot_code', label: 'Mã lô sản xuất', isMandatory: true },
      { key: 'product_name', label: 'Tên nông sản', isMandatory: true },
      { key: 'harvest_date', label: 'Ngày thu hoạch', isMandatory: true },
    ],
  },
];

describe('ProfileTemplateFormPage (NCL-07-CN-007)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(profileTemplateApi.getAvailableFields).mockResolvedValue(mockAvailableGroups);
  });

  it('renders form elements properly in create mode', async () => {
    render(
      <BrowserRouter>
        <ProfileTemplateFormPage />
      </BrowserRouter>
    );

    expect(await screen.findByText('Tạo mẫu hồ sơ truy xuất mới')).toBeInTheDocument();
    expect(screen.getByLabelText(/Tên mẫu hồ sơ/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Đối tác áp dụng/i)).toBeInTheDocument();
    expect(screen.getByRole('switch')).toBeInTheDocument();
    expect(screen.getByText('Thông tin tổ chức')).toBeInTheDocument();
    expect(screen.getByText('Lô sản xuất')).toBeInTheDocument();
  });

  it('displays validation error if template name is empty', async () => {
    render(
      <BrowserRouter>
        <ProfileTemplateFormPage />
      </BrowserRouter>
    );

    await screen.findByText('Tạo mẫu hồ sơ truy xuất mới');

    const submitBtn = screen.getByRole('button', { name: /Lưu mẫu hồ sơ/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Tên mẫu hồ sơ không được để trống')).toBeInTheDocument();
    });

    expect(profileTemplateApi.createProfileTemplate).not.toHaveBeenCalled();
  });

  it('submits form successfully when name is provided and mandatory fields exist', async () => {
    vi.mocked(profileTemplateApi.createProfileTemplate).mockResolvedValue({
      id: 'tpl-new-1',
      organizationId: 'org-1111-2222',
      name: 'Mẫu xuất khẩu AEON',
      partnerName: 'AEON Mall',
      isDefault: true,
      fields: [],
    });

    render(
      <BrowserRouter>
        <ProfileTemplateFormPage />
      </BrowserRouter>
    );

    await screen.findByText('Tạo mẫu hồ sơ truy xuất mới');

    // Nhập tên mẫu
    const nameInput = screen.getByLabelText(/Tên mẫu hồ sơ/i);
    fireEvent.change(nameInput, { target: { value: 'Mẫu xuất khẩu AEON' } });

    // Nhập đối tác
    const partnerInput = screen.getByLabelText(/Đối tác áp dụng/i);
    fireEvent.change(partnerInput, { target: { value: 'AEON Mall' } });

    // Submit
    const submitBtn = screen.getByRole('button', { name: /Lưu mẫu hồ sơ/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(profileTemplateApi.createProfileTemplate).toHaveBeenCalledWith(
        'org-1111-2222',
        expect.objectContaining({
          name: 'Mẫu xuất khẩu AEON',
          partnerName: 'AEON Mall',
        })
      );
    });
  });

  it('displays 422 error message from backend when violation occurs (TC-02)', async () => {
    vi.mocked(profileTemplateApi.createProfileTemplate).mockRejectedValue({
      response: {
        status: 422,
        data: {
          message: 'Hồ sơ truy xuất thiếu các trường bắt buộc theo QTN-11',
        },
      },
    });

    render(
      <BrowserRouter>
        <ProfileTemplateFormPage />
      </BrowserRouter>
    );

    await screen.findByText('Tạo mẫu hồ sơ truy xuất mới');

    const nameInput = screen.getByLabelText(/Tên mẫu hồ sơ/i);
    fireEvent.change(nameInput, { target: { value: 'Mẫu thử nghiệm thiếu trường' } });

    const submitBtn = screen.getByRole('button', { name: /Lưu mẫu hồ sơ/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText('Hồ sơ truy xuất thiếu các trường bắt buộc theo QTN-11')
      ).toBeInTheDocument();
    });
  });
});
