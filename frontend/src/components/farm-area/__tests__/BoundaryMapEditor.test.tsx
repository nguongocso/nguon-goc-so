import { act, render } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { BoundaryMapEditor } from '../BoundaryMapEditor';

const leafletMocks = vi.hoisted(() => ({
  clickHandler: undefined as ((event: { latlng: { lat: number; lng: number } }) => void) | undefined,
  setView: vi.fn(),
}));

vi.mock('leaflet', () => {
  const mapInstance = {
    setView: leafletMocks.setView,
    on: vi.fn((eventName: string, handler: typeof leafletMocks.clickHandler) => {
      if (eventName === 'click') leafletMocks.clickHandler = handler;
      return mapInstance;
    }),
    remove: vi.fn(),
    invalidateSize: vi.fn(),
    fitBounds: vi.fn(),
  };
  leafletMocks.setView.mockReturnValue(mapInstance);
  const layerGroup = { addTo: vi.fn(), clearLayers: vi.fn(), addLayer: vi.fn() };
  layerGroup.addTo.mockReturnValue(layerGroup);

  return {
    default: {
      Icon: { Default: { prototype: {}, mergeOptions: vi.fn() } },
      map: vi.fn(() => mapInstance),
      tileLayer: vi.fn(() => ({ addTo: vi.fn() })),
      layerGroup: vi.fn(() => layerGroup),
      marker: vi.fn(() => ({ on: vi.fn() })),
      polygon: vi.fn(() => ({})),
      polyline: vi.fn(() => ({})),
      divIcon: vi.fn(() => ({})),
      latLngBounds: vi.fn(() => ({})),
      DomEvent: { stopPropagation: vi.fn() },
    },
  };
});

describe('BoundaryMapEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    leafletMocks.clickHandler = undefined;
  });

  it('giữ tọa độ 0 làm tâm bản đồ thay vì dùng tọa độ mặc định', () => {
    render(
      <BoundaryMapEditor
        points={[]}
        initialCenter={{ latitude: 0, longitude: 0 }}
        onAddPoint={vi.fn()}
        onUpdatePoint={vi.fn()}
      />
    );

    expect(leafletMocks.setView).toHaveBeenCalledWith([0, 0], 14);
  });

  it('không thêm đỉnh từ listener Leaflet khi trạng thái chuyển sang disabled', () => {
    const onAddPoint = vi.fn();
    const { rerender } = render(
      <BoundaryMapEditor
        points={[]}
        onAddPoint={onAddPoint}
        onUpdatePoint={vi.fn()}
      />
    );

    rerender(
      <BoundaryMapEditor
        points={[]}
        onAddPoint={onAddPoint}
        onUpdatePoint={vi.fn()}
        disabled
      />
    );
    act(() => leafletMocks.clickHandler?.({ latlng: { lat: 21.1234567, lng: 105.1234567 } }));

    expect(onAddPoint).not.toHaveBeenCalled();
  });
});
