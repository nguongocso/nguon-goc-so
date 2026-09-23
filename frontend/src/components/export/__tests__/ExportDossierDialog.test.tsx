import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import userEvent from '@testing-library/user-event';
import { ExportDossierDialog } from '../ExportDossierDialog';
import * as profileTemplateApi from '@/api/profileTemplateApi';
import type { ProfileTemplate } from '@/types/profileTemplate';

vi.mock('@/api/dossierApi', () => ({
  exportDossier: vi.fn(),
  checkDossierEligibility: vi.fn(),
}));

vi.mock('@/api/exportApi', () => ({
  exportShipmentWithTemplate: vi.fn(),
}));

vi.mock('@/api/profileTemplateApi', () => ({
  getProfileTemplates: vi.fn(),
  getAvailableFields: vi.fn(),
  getOpenDataPreview: vi.fn(),
  getDefaultProfileTemplate: vi.fn(),
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

const mockTemplates: ProfileTemplate[] = [
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

const renderDialog = () =>
  render(
    <ExportDossierDialog
      open
      onOpenChange={vi.fn()}
      shipmentId="ship-1"
      shipmentName="Lô hàng A"
      shipmentCode="SHIP-001"
    />
  );

describe('ExportDossierDialog - hiển thị mẫu hồ sơ áp dụng (NCL-07-CN-007)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(profileTemplateApi.getProfileTemplates).mockResolvedValue(mockTemplates);
    vi.mocked(profileTemplateApi.getAvailableFields).mockResolvedValue([]);
  });

  it('hiển thị tên mẫu tiếng Việt của mẫu mặc định thay vì mã mẫu', async () => {
    renderDialog();

    const trigger = await screen.findByLabelText(/Mẫu hồ sơ áp dụng/i);

    // Giá trị hiển thị là tên mẫu + đối tác + nhãn mặc định, không phải id (mã) của mẫu
    expect(trigger).toHaveTextContent('Mẫu xuất khẩu AEON');
    expect(trigger).toHaveTextContent('(AEON Mall)');
    expect(trigger).toHaveTextContent('Mặc định');
    expect(trigger).not.toHaveTextContent('tpl-100');
  });

  it('hiển thị đúng nhãn tiếng Việt của mẫu mặc định hệ thống khi tổ chức không có mẫu mặc định', async () => {
    vi.mocked(profileTemplateApi.getProfileTemplates).mockResolvedValue([]);

    renderDialog();

    const trigger = await screen.findByLabelText(/Mẫu hồ sơ áp dụng/i);

    expect(trigger).toHaveTextContent('Mẫu tiêu chuẩn HTX (Mặc định hệ thống)');
    expect(trigger).not.toHaveTextContent('default');
  });

  it('cập nhật giá trị hiển thị theo tên mẫu khi người dùng chọn mẫu khác trong danh sách', async () => {
    const user = userEvent.setup();
    renderDialog();

    const trigger = await screen.findByLabelText(/Mẫu hồ sơ áp dụng/i);
    await user.click(trigger);

    const option = await screen.findByRole('option', { name: /Mẫu nội địa Central/i });
    await user.click(option);

    expect(trigger).toHaveTextContent('Mẫu nội địa Central');
    expect(trigger).toHaveTextContent('(Central Retail)');
    expect(trigger).not.toHaveTextContent('tpl-200');
  });

  it('ở chế độ templatePreviewMode hiển thị đúng thông tin mẫu đang cấu hình và số trường đã chọn', async () => {
    render(
      <ExportDossierDialog
        open
        onOpenChange={vi.fn()}
        templatePreviewMode
        templateInfo={{
          name: 'Mẫu xuất khẩu Châu Âu',
          partnerName: 'AEON Mall',
          isDefault: true,
          selectedFieldsCount: 8,
          mockData: {
            organization: { name: 'HTX Nông nghiệp Xanh' },
          },
        }}
      />
    );

    // Tiêu đề dialog
    expect(screen.getByText('Xuất hồ sơ truy xuất nguồn gốc')).toBeInTheDocument();

    // Hiển thị tên mẫu và badge đối tác
    expect(screen.getAllByText('Mẫu xuất khẩu Châu Âu').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Đối tác: AEON Mall')).toBeInTheDocument();
    expect(screen.getByText('Mặc định')).toBeInTheDocument();

    // Hiển thị số trường đã cấu hình trong thông báo
    expect(screen.getByText(/Hồ sơ xuất ra sẽ được lọc chính xác theo cấu hình/i)).toBeInTheDocument();
    expect(screen.getByText(/8 trường đã chọn/i)).toBeInTheDocument();

    // Nút xem trước hồ sơ và tải về có mặt
    expect(screen.getByRole('button', { name: /Xem trước hồ sơ/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Tải hồ sơ về máy/i })).toBeInTheDocument();
  });

  it('ở chế độ templatePreviewMode khi bấm nút Xem trước hồ sơ sẽ mở modal xem trước', async () => {
    const user = userEvent.setup();
    render(
      <ExportDossierDialog
        open
        onOpenChange={vi.fn()}
        templatePreviewMode
        templateInfo={{
          name: 'Mẫu thử nghiệm',
          selectedFieldsCount: 10,
          mockData: {
            organization: { name: 'HTX Đơn Dương' },
          },
        }}
      />
    );

    const previewBtn = screen.getByRole('button', { name: /Xem trước hồ sơ/i });
    expect(previewBtn).not.toBeDisabled();

    await user.click(previewBtn);

    // Modal xem trước hồ sơ xuất hiện
    expect(await screen.findByText('Bản xem trước xuất hồ sơ')).toBeInTheDocument();
    expect(screen.getAllByText(/Mẫu thử nghiệm/i).length).toBeGreaterThanOrEqual(1);
  });
});