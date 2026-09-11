import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import CreateProductionLotForm from '../CreateProductionLotForm';
import type { CreateProductionLotRequest } from '@/types/productionLot';

const farmAreas = [{ id: 'area-1', name: 'Vùng trồng số 1' }];
const productCategories = [{ id: 'cat-1', name: 'Lúa' }];

const initialValues: CreateProductionLotRequest = {
  name: 'Lô lúa vụ hè 2025',
  farmAreaId: 'area-1',
  productCategoryId: 'cat-1',
  expectedQuantity: 1000,
  expectedQuantityUnit: 'kg',
  plantingDate: '2025-05-01',
};

describe('CreateProductionLotForm chế độ clone (NCL-02-CN-007)', () => {
  const mockOnCancel = vi.fn();
  const mockOnSubmit = vi.fn();

  beforeEach(() => {
    mockOnCancel.mockClear();
    mockOnSubmit.mockClear();
  });

  it('prefill tên, sản lượng và ngày gieo trồng từ lô mẫu', () => {
    render(
      <CreateProductionLotForm
        farmAreas={farmAreas}
        productCategories={productCategories}
        onCancel={mockOnCancel}
        onSubmit={mockOnSubmit}
        initialValues={initialValues}
        lockFarmAreaAndCategory
        submitLabel="Tạo lô từ mẫu"
        infoBanner={<p>Lô này được tạo từ mẫu vụ trước</p>}
      />,
    );

    expect(screen.getByDisplayValue('Lô lúa vụ hè 2025')).toBeInTheDocument();
    expect(screen.getByDisplayValue('1000')).toBeInTheDocument();
    expect(screen.getByDisplayValue('2025-05-01')).toBeInTheDocument();
    expect(
      screen.getByText('Lô này được tạo từ mẫu vụ trước'),
    ).toBeInTheDocument();
  });

  it('khóa vùng trồng và loại nông sản kế thừa từ lô mẫu', () => {
    render(
      <CreateProductionLotForm
        farmAreas={farmAreas}
        productCategories={productCategories}
        onCancel={mockOnCancel}
        onSubmit={mockOnSubmit}
        initialValues={initialValues}
        lockFarmAreaAndCategory
        submitLabel="Tạo lô từ mẫu"
      />,
    );

    // Chế độ khóa: giá trị kế thừa hiển thị dạng văn bản, không thể sửa.
    expect(screen.getByText('Vùng trồng số 1')).toBeInTheDocument();
    expect(screen.getByText('Lúa')).toBeInTheDocument();
    expect(screen.getAllByLabelText('Kế thừa từ lô mẫu')).toHaveLength(2);
    expect(
      screen.queryByRole('combobox', { name: /Vùng trồng/ }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('combobox', { name: /Loại nông sản/ }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByText('Vùng trồng được kế thừa từ lô mẫu, không thay đổi.'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('Loại nông sản được kế thừa từ lô mẫu, không thay đổi.'),
    ).toBeInTheDocument();
  });

  it('gửi dữ liệu đã chỉnh sửa khi bấm tạo lô từ mẫu', async () => {
    mockOnSubmit.mockResolvedValue(undefined);
    render(
      <CreateProductionLotForm
        farmAreas={farmAreas}
        productCategories={productCategories}
        onCancel={mockOnCancel}
        onSubmit={mockOnSubmit}
        initialValues={initialValues}
        lockFarmAreaAndCategory
        submitLabel="Tạo lô từ mẫu"
      />,
    );

    fireEvent.change(screen.getByLabelText(/Tên lô sản xuất/), {
      target: { value: 'Lô lúa vụ đông xuân 2026' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Tạo lô từ mẫu' }));

    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledOnce();
    });
    expect(mockOnSubmit).toHaveBeenCalledWith({
      ...initialValues,
      name: 'Lô lúa vụ đông xuân 2026',
    });
  });
});
