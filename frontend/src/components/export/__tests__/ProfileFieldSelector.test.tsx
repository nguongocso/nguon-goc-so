import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ProfileFieldSelector } from '../ProfileFieldSelector';
import type { FieldGroupDefinition, FieldSelectionItem } from '@/types/profileTemplate';

const mockGroups: FieldGroupDefinition[] = [
  {
    group: 'ORGANIZATION',
    groupLabel: 'Thông tin tổ chức',
    fields: [
      { key: 'org_name', label: 'Tên tổ chức', isMandatory: true },
      { key: 'org_tax_code', label: 'Mã số thuế', isMandatory: false },
    ],
  },
  {
    group: 'PRODUCTION_LOT',
    groupLabel: 'Lô sản xuất',
    fields: [
      { key: 'lot_code', label: 'Mã lô sản xuất', isMandatory: true },
      { key: 'product_name', label: 'Tên nông sản', isMandatory: true },
      { key: 'harvest_date', label: 'Ngày thu hoạch', isMandatory: true },
      { key: 'cultivar', label: 'Giống cây trồng', isMandatory: false },
    ],
  },
];

const mockSelectedMandatory: FieldSelectionItem[] = [
  { fieldKey: 'org_name', fieldGroup: 'ORGANIZATION', isMandatory: true, sortOrder: 1 },
  { fieldKey: 'lot_code', fieldGroup: 'PRODUCTION_LOT', isMandatory: true, sortOrder: 2 },
  { fieldKey: 'product_name', fieldGroup: 'PRODUCTION_LOT', isMandatory: true, sortOrder: 3 },
  { fieldKey: 'harvest_date', fieldGroup: 'PRODUCTION_LOT', isMandatory: true, sortOrder: 4 },
];

describe('ProfileFieldSelector (NCL-07-CN-007)', () => {
  it('renders all groups and field labels correctly', () => {
    render(
      <ProfileFieldSelector
        availableGroups={mockGroups}
        selectedFields={mockSelectedMandatory}
        onChange={vi.fn()}
      />
    );

    expect(screen.getByText('Thông tin tổ chức')).toBeInTheDocument();
    expect(screen.getByText('Lô sản xuất')).toBeInTheDocument();
    expect(screen.getByText('Tên tổ chức')).toBeInTheDocument();
    expect(screen.getByText('Mã số thuế')).toBeInTheDocument();
    expect(screen.getByText('Mã lô sản xuất')).toBeInTheDocument();
    expect(screen.getByText('Giống cây trồng')).toBeInTheDocument();
  });

  it('marks mandatory fields as disabled and displays Bắt buộc badge (TC-02 UX)', () => {
    render(
      <ProfileFieldSelector
        availableGroups={mockGroups}
        selectedFields={mockSelectedMandatory}
        onChange={vi.fn()}
      />
    );

    const mandatoryCheckboxes = screen.getAllByRole('checkbox');
    // Trường bắt buộc đầu tiên (org_name)
    expect(mandatoryCheckboxes[0]).toHaveAttribute('aria-disabled', 'true');
    expect(mandatoryCheckboxes[0]).toHaveAttribute('data-disabled');

    const mandatoryBadges = screen.getAllByText(/Bắt buộc/i);
    expect(mandatoryBadges.length).toBeGreaterThanOrEqual(4);
  });

  it('allows optional fields to be enabled and toggled', () => {
    const handleChange = vi.fn();
    render(
      <ProfileFieldSelector
        availableGroups={mockGroups}
        selectedFields={mockSelectedMandatory}
        onChange={handleChange}
      />
    );

    // Checkbox thứ 2 là org_tax_code (tùy chọn)
    const checkboxes = screen.getAllByRole('checkbox');
    const optionalCheckbox = checkboxes[1];
    expect(optionalCheckbox).not.toHaveAttribute('aria-disabled', 'true');

    // Click checkbox để chọn
    fireEvent.click(optionalCheckbox);

    expect(handleChange).toHaveBeenCalledWith([
      ...mockSelectedMandatory,
      expect.objectContaining({
        fieldKey: 'org_tax_code',
        fieldGroup: 'ORGANIZATION',
        isMandatory: false,
      }),
    ]);
  });

  it('does not trigger onChange when clicking mandatory field checkbox', () => {
    const handleChange = vi.fn();
    render(
      <ProfileFieldSelector
        availableGroups={mockGroups}
        selectedFields={mockSelectedMandatory}
        onChange={handleChange}
      />
    );

    const checkboxes = screen.getAllByRole('checkbox');
    const mandatoryCheckbox = checkboxes[0]; // org_name
    fireEvent.click(mandatoryCheckbox);

    expect(handleChange).not.toHaveBeenCalled();
  });

  it('supports selecting all fields in a group', () => {
    const handleChange = vi.fn();
    render(
      <ProfileFieldSelector
        availableGroups={mockGroups}
        selectedFields={mockSelectedMandatory}
        onChange={handleChange}
      />
    );

    const selectAllButtons = screen.getAllByRole('button', { name: /Chọn tất cả/i });
    fireEvent.click(selectAllButtons[0]); // Group 1

    expect(handleChange).toHaveBeenCalledWith([
      ...mockSelectedMandatory,
      expect.objectContaining({ fieldKey: 'org_tax_code' }),
    ]);
  });
});
