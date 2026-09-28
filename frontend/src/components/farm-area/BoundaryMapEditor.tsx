import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { LocateFixed, MapPin, MousePointerClick } from 'lucide-react';
import { Button } from '@/components/ui/button';
import type { LatLng } from '@/types/farmArea';

// Khắc phục icon mặc định của Leaflet
delete (L.Icon.Default.prototype as unknown as { _getIconUrl?: unknown })._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

/** Thuộc tính của trình chỉnh sửa bản đồ ranh giới. */
interface BoundaryMapEditorProps {
  points: LatLng[];
  initialCenter?: { latitude: number; longitude: number };
  onAddPoint: (point: LatLng) => void;
  onUpdatePoint: (index: number, point: LatLng) => void;
  onSelectPoint?: (index: number) => void;
  selectedIndex?: number | null;
  disabled?: boolean;
  invalid?: boolean;
}

/** Tạo icon số thứ tự cho đỉnh ranh giới. */
function createVertexIcon(index: number, isSelected: boolean) {
  const bg = isSelected ? '#D97706' : '#059669'; // Amber khi chọn, Emerald mặc định
  return L.divIcon({
    html: `<div style="
      background: ${bg};
      color: white;
      border-radius: 50%;
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      font-size: 11px;
      font-family: system-ui, sans-serif;
      border: 2px solid white;
      box-shadow: 0 2px 6px rgba(0,0,0,0.35);
      cursor: grab;
      user-select: none;
    ">${index}</div>`,
    className: '',
    iconSize: [24, 24],
    iconAnchor: [12, 12],
  });
}

export const BoundaryMapEditor: React.FC<BoundaryMapEditorProps> = ({
  points,
  initialCenter,
  onAddPoint,
  onUpdatePoint,
  onSelectPoint,
  selectedIndex = null,
  disabled = false,
  invalid = false,
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const leafletMapRef = useRef<L.Map | null>(null);
  const layerGroupRef = useRef<L.LayerGroup | null>(null);

  // Ref giữ callback mới nhất để tránh stale closure trong Leaflet event listener
  const onAddPointRef = useRef(onAddPoint);
  onAddPointRef.current = onAddPoint;

  const onUpdatePointRef = useRef(onUpdatePoint);
  onUpdatePointRef.current = onUpdatePoint;

  const onSelectPointRef = useRef(onSelectPoint);
  onSelectPointRef.current = onSelectPoint;

  const disabledRef = useRef(disabled);
  disabledRef.current = disabled;

  // Khởi tạo bản đồ 1 lần duy nhất
  useEffect(() => {
    if (!mapContainerRef.current || leafletMapRef.current) return;

    const defaultLat = initialCenter?.latitude ?? 21.587568;
    const defaultLng = initialCenter?.longitude ?? 105.826176;

    const map = L.map(mapContainerRef.current).setView([defaultLat, defaultLng], 14);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(map);

    // Lắng nghe sự kiện click trên bản đồ để thêm đỉnh
    map.on('click', (e: L.LeafletMouseEvent) => {
      if (disabledRef.current) return;
      onAddPointRef.current({
        latitude: Number(e.latlng.lat.toFixed(6)),
        longitude: Number(e.latlng.lng.toFixed(6)),
      });
    });

    const layerGroup = L.layerGroup().addTo(map);
    layerGroupRef.current = layerGroup;
    leafletMapRef.current = map;

    // Invalidate size sau khi mount
    setTimeout(() => {
      map.invalidateSize();
    }, 150);

    return () => {
      map.remove();
      leafletMapRef.current = null;
      layerGroupRef.current = null;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Cập nhật các lớp Marker và Polygon khi danh sách đỉnh thay đổi
  useEffect(() => {
    const map = leafletMapRef.current;
    const layerGroup = layerGroupRef.current;
    if (!map || !layerGroup) return;

    layerGroup.clearLayers();

    // 1. Vẽ Marker cho từng đỉnh
    points.forEach((pt, index) => {
      const isSelected = selectedIndex === index;
      const marker = L.marker([pt.latitude, pt.longitude], {
        icon: createVertexIcon(index + 1, isSelected),
        draggable: !disabled,
        title: `Đỉnh ${index + 1}: ${pt.latitude.toFixed(6)}, ${pt.longitude.toFixed(6)}`,
      });

      marker.on('dragend', (e) => {
        const latlng = (e.target as L.Marker).getLatLng();
        onUpdatePointRef.current(index, {
          latitude: Number(latlng.lat.toFixed(6)),
          longitude: Number(latlng.lng.toFixed(6)),
        });
      });

      marker.on('click', (e) => {
        L.DomEvent.stopPropagation(e);
        onSelectPointRef.current?.(index);
      });

      layerGroup.addLayer(marker);
    });

    // 2. Vẽ Polygon nếu >= 3 đỉnh, hoặc Polyline nếu 2 đỉnh
    if (points.length >= 3) {
      const latlngs: L.LatLngExpression[] = points.map((p) => [p.latitude, p.longitude]);
      const boundaryColor = invalid ? '#dc2626' : '#059669';
      const polygon = L.polygon(latlngs, {
        color: boundaryColor,
        weight: 2.5,
        fillColor: boundaryColor,
        fillOpacity: 0.16,
        dashArray: '4 4',
      });
      layerGroup.addLayer(polygon);
    } else if (points.length === 2) {
      const latlngs: L.LatLngExpression[] = points.map((p) => [p.latitude, p.longitude]);
      const polyline = L.polyline(latlngs, {
        color: '#059669',
        weight: 2,
        dashArray: '3 3',
      });
      layerGroup.addLayer(polyline);
    }
  }, [points, selectedIndex, disabled, invalid]);

  // Căn chỉnh góc nhìn vừa toàn bộ các đỉnh
  const handleFitBounds = () => {
    const map = leafletMapRef.current;
    if (!map) return;

    if (points.length > 0) {
      const bounds = L.latLngBounds(points.map((p) => [p.latitude, p.longitude]));
      map.fitBounds(bounds, { padding: [40, 40], maxZoom: 16 });
    } else if (initialCenter) {
      map.setView([initialCenter.latitude, initialCenter.longitude], 14);
    }
  };

  return (
    <div className="relative isolate z-0 overflow-hidden rounded-xl border border-border bg-slate-100 shadow-sm dark:bg-muted/30">
      {/* Banner hướng dẫn trên đầu bản đồ */}
      <div className="absolute top-2.5 left-2.5 right-2.5 z-[1000] flex items-center justify-between gap-2 rounded-lg bg-white/95 px-3 py-2 text-xs text-slate-700 shadow-md backdrop-blur dark:bg-card/95 dark:text-foreground">
        <div className="flex items-center gap-2">
          <MousePointerClick className="size-4 shrink-0 text-emerald-600 dark:text-emerald-400" />
          <span>
            {invalid
              ? 'Ranh giới đang tự cắt nhau. Hãy điều chỉnh lại vị trí hoặc thứ tự các đỉnh.'
              : disabled
              ? 'Chế độ chỉ xem ranh giới.'
              : points.length < 3
              ? `Chấm ít nhất 3 điểm trên bản đồ để tạo ranh giới (hiện có ${points.length} điểm).`
              : `Đã có ${points.length} đỉnh. Nhấp để thêm đỉnh mới, kéo đỉnh để căn chỉnh vị trí.`}
          </span>
        </div>

        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={handleFitBounds}
          className="h-7 text-[11px] gap-1 shrink-0 px-2"
          title="Căn giữa ranh giới vùng trồng"
        >
          <LocateFixed className="size-3.5" />
          Căn giữa
        </Button>
      </div>

      {/* Vùng chứa bản đồ Leaflet */}
      <div ref={mapContainerRef} style={{ height: '480px', width: '100%' }} />

      {/* Footer nhỏ dưới bản đồ */}
      <div className="flex items-center justify-between border-t border-border bg-slate-50/90 px-3 py-2 text-xs text-muted-foreground dark:bg-card">
        <div className="flex items-center gap-1.5">
          <MapPin className="size-3.5 text-emerald-600" />
          <span>Tọa độ chuẩn hệ WGS84 (SRID 4326)</span>
        </div>
        <span>{points.length} đỉnh</span>
      </div>
    </div>
  );
};
