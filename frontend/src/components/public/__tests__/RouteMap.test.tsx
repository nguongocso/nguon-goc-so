import { describe, expect, it } from 'vitest';

import { createFarmAreaBoundaryPopupContent } from '../RouteMap';

describe('createFarmAreaBoundaryPopupContent', () => {
  it('hiển thị tên vùng trồng dưới dạng văn bản, không diễn giải HTML', () => {
    const maliciousName = '<img src=x onerror="window.__xss=true">Vùng thử nghiệm';

    const popup = createFarmAreaBoundaryPopupContent(maliciousName, '1.2500 ha');

    expect(popup.textContent).toContain(maliciousName);
    expect(popup.querySelector('img')).toBeNull();
    expect(popup.innerHTML).toContain('&lt;img');
  });

  it('hiển thị giá trị mặc định khi tên vùng trồng không tồn tại', () => {
    const popup = createFarmAreaBoundaryPopupContent(null, 'Chưa tính');

    expect(popup.textContent).toContain('Tên: —');
    expect(popup.textContent).toContain('Diện tích tính toán: Chưa tính');
  });
});
