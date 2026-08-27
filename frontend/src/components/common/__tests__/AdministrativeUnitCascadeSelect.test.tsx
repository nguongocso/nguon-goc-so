import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AdministrativeUnitCascadeSelect } from '@/components/common/AdministrativeUnitCascadeSelect';
import { MOCK_ADMIN_UNITS } from '@/mocks/administrativeUnits';

const PHU_THO_ID = 'a2000000-0000-0000-0000-000000000002';
const THANH_SON_ID = 'a2000000-0000-0000-0000-000000020002';

/** Wrapper controlled giữ value để test luồng chọn/bỏ chọn đầy đủ. */
function Harness({ onChangeSpy }: { onChangeSpy?: (ids: string[]) => void }) {
  const [value, setValue] = useState<string[]>([]);
  return (
    <AdministrativeUnitCascadeSelect
      units={MOCK_ADMIN_UNITS}
      value={value}
      onChange={(ids) => {
        setValue(ids);
        onChangeSpy?.(ids);
      }}
    />
  );
}

async function expandProvince(user: ReturnType<typeof userEvent.setup>, name: string) {
  await user.click(screen.getByRole('button', { name: `Mở ${name}` }));
}

describe('TC-E - AdministrativeUnitCascadeSelect', () => {
  it('chọn tỉnh rồi chọn xã → onChange nhận đúng 1 id; bỏ chọn → onChange mảng rỗng', async () => {
    const onChangeSpy = vi.fn();
    const user = userEvent.setup();
    render(<Harness onChangeSpy={onChangeSpy} />);

    await expandProvince(user, 'Phú Thọ');

    await user.click(await screen.findByRole('checkbox', { name: 'Xã Thanh Sơn' }));

    // Lần gọi cuối nhận đúng mảng chứa duy nhất 1 id của xã vừa tick.
    const lastCall = onChangeSpy.mock.calls.at(-1)?.[0] as string[];
    expect(lastCall).toHaveLength(1);
    expect(lastCall).toContain(THANH_SON_ID);

    // UI xác nhận chip lựa chọn (tên xã xuất hiện ở chip; hàng xã vẫn trong danh sách).
    expect(await screen.findByText('Đã chọn 1 địa bàn:')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getAllByText('Xã Thanh Sơn').length).toBeGreaterThanOrEqual(1);
    });

    // Bỏ chọn → onChange nhận mảng rỗng, chip biến mất.
    await user.click(screen.getByRole('checkbox', { name: 'Xã Thanh Sơn' }));
    expect(onChangeSpy.mock.calls.at(-1)?.[0]).toEqual([]);
    await waitFor(() => {
      expect(screen.queryByText(/Đã chọn \d+ địa bàn/)).not.toBeInTheDocument();
    });
  });

  it('tick checkbox tỉnh chọn tất cả xã thuộc tỉnh đó', async () => {
    const onChangeSpy = vi.fn();
    const user = userEvent.setup();
    render(<Harness onChangeSpy={onChangeSpy} />);

    await expandProvince(user, 'Điện Biên');
    await user.click(
      await screen.findByRole('checkbox', {
        name: 'Chọn tất cả xã phường thuộc Điện Biên',
      }),
    );

    const lastCall = onChangeSpy.mock.calls.at(-1)?.[0] as string[];
    expect(lastCall).toHaveLength(
      MOCK_ADMIN_UNITS.find((p) => p.id === 'a3000000-0000-0000-0000-000000000003')!
        .children.length,
    );
  });

  it('tìm kiếm lọc danh sách tỉnh/xã', async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.type(
      screen.getByRole('textbox', { name: 'Tìm kiếm địa bàn' }),
      'Sóc Sơn',
    );

    // Chỉ Hà Nội còn trong danh sách (do khớp xã Sóc Sơn), các tỉnh khác bị lọc bỏ.
    expect(await screen.findByText('Hà Nội')).toBeInTheDocument();
    expect(screen.queryByText('Phú Thọ')).not.toBeInTheDocument();

    // Xã không khớp bị ẩn khỏi danh sách con khi mở rộng.
    await expandProvince(user, 'Hà Nội');
    expect(screen.getByRole('checkbox', { name: 'Xã Sóc Sơn' })).toBeInTheDocument();
    expect(
      screen.queryByRole('checkbox', { name: 'Phường Hoàn Kiếm' }),
    ).not.toBeInTheDocument();
  });

  it('không sinh id trùng khi tick chọn lặp lại nhiều nguồn', async () => {
    const onChangeSpy = vi.fn();
    const user = userEvent.setup();
    render(<Harness onChangeSpy={onChangeSpy} />);

    await expandProvince(user, 'Phú Thọ');
    await user.click(await screen.findByRole('checkbox', { name: 'Xã Thanh Sơn' }));
    // Tick lại xã khác sau khi đã chọn qua "chọn tất cả" rồi bỏ một xã...
    await user.click(
      screen.getByRole('checkbox', { name: 'Chọn tất cả xã phường thuộc Phú Thọ' }),
    );

    const lastCall = onChangeSpy.mock.calls.at(-1)?.[0] as string[];
    const uniqueIds = new Set(lastCall);
    expect(uniqueIds.size).toBe(lastCall.length);
    expect(lastCall).toContain(THANH_SON_ID);
    expect(new Set(lastCall).has(PHU_THO_ID)).toBe(false); // chỉ chứa id cấp xã
  });
});
