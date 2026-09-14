import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AdministrativeUnitSingleSelect } from '@/components/common/AdministrativeUnitSingleSelect';
import { MOCK_ADMIN_UNITS } from '@/mocks/administrativeUnits';

const HANOI_ID = 'a1000000-0000-0000-0000-000000000001';
const HOAN_KIEM_ID = 'a1000000-0000-0000-0000-000000010001';
const PHU_THO_ID = 'a2000000-0000-0000-0000-000000000002';
const THANH_SON_ID = 'a2000000-0000-0000-0000-000000020002';

function Harness({
  onProvinceChangeSpy,
  onCommuneChangeSpy,
}: {
  onProvinceChangeSpy?: (id: string) => void;
  onCommuneChangeSpy?: (id: string) => void;
}) {
  const [provinceId, setProvinceId] = useState<string | null>(null);
  const [communeId, setCommuneId] = useState<string | null>(null);

  return (
    <AdministrativeUnitSingleSelect
      units={MOCK_ADMIN_UNITS}
      provinceId={provinceId}
      communeId={communeId}
      onProvinceChange={(id) => {
        setProvinceId(id);
        onProvinceChangeSpy?.(id);
      }}
      onCommuneChange={(id) => {
        setCommuneId(id);
        onCommuneChangeSpy?.(id);
      }}
    />
  );
}

describe('AdministrativeUnitSingleSelect Component', () => {
  it('hiển thị danh sách tỉnh và disable xã khi chưa chọn tỉnh', () => {
    render(<Harness />);

    const provinceSelect = screen.getByLabelText(/Tỉnh \/ Thành phố/i);
    const communeSelect = screen.getByLabelText(/Xã \/ Phường/i);

    expect(provinceSelect).toBeInTheDocument();
    expect(communeSelect).toBeDisabled();
    expect(screen.getByRole('option', { name: /-- Vui lòng chọn Tỉnh\/Thành phố trước --/i })).toBeInTheDocument();
  });

  it('chọn tỉnh mở khóa dropdown xã với đúng danh sách xã trực thuộc', async () => {
    const user = userEvent.setup();
    const onProvinceSpy = vi.fn();
    const onCommuneSpy = vi.fn();

    render(<Harness onProvinceChangeSpy={onProvinceSpy} onCommuneChangeSpy={onCommuneSpy} />);

    const provinceSelect = screen.getByLabelText(/Tỉnh \/ Thành phố/i);
    await user.selectOptions(provinceSelect, HANOI_ID);

    expect(onProvinceSpy).toHaveBeenCalledWith(HANOI_ID);
    expect(onCommuneSpy).toHaveBeenCalledWith('');

    const communeSelect = screen.getByLabelText(/Xã \/ Phường/i);
    expect(communeSelect).not.toBeDisabled();

    // Các xã của Hà Nội phải có trong danh sách
    expect(screen.getByRole('option', { name: /Phường Hoàn Kiếm/i })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /Xã Sóc Sơn/i })).toBeInTheDocument();
    // Xã của Phú Thọ không được xuất hiện
    expect(screen.queryByRole('option', { name: /Xã Thanh Sơn/i })).not.toBeInTheDocument();
  });

  it('chọn xã gọi onCommuneChange với id xã tương ứng', async () => {
    const user = userEvent.setup();
    const onCommuneSpy = vi.fn();

    render(<Harness onCommuneChangeSpy={onCommuneSpy} />);

    const provinceSelect = screen.getByLabelText(/Tỉnh \/ Thành phố/i);
    await user.selectOptions(provinceSelect, HANOI_ID);

    const communeSelect = screen.getByLabelText(/Xã \/ Phường/i);
    await user.selectOptions(communeSelect, HOAN_KIEM_ID);

    expect(onCommuneSpy).toHaveBeenLastCalledWith(HOAN_KIEM_ID);
  });

  it('chuyển sang tỉnh khác sẽ reset xã về rỗng và cập nhật danh sách xã mới', async () => {
    const user = userEvent.setup();
    const onCommuneSpy = vi.fn();

    render(<Harness onCommuneChangeSpy={onCommuneSpy} />);

    const provinceSelect = screen.getByLabelText(/Tỉnh \/ Thành phố/i);
    await user.selectOptions(provinceSelect, HANOI_ID);

    const communeSelect = screen.getByLabelText(/Xã \/ Phường/i);
    await user.selectOptions(communeSelect, HOAN_KIEM_ID);

    // Chuyển sang tỉnh Phú Thọ
    await user.selectOptions(provinceSelect, PHU_THO_ID);
    expect(onCommuneSpy).toHaveBeenLastCalledWith('');

    // Danh sách xã của Phú Thọ xuất hiện
    expect(screen.getByRole('option', { name: /Xã Thanh Sơn/i })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: /Phường Hoàn Kiếm/i })).not.toBeInTheDocument();
  });
});
