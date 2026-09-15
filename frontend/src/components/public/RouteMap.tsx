import { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { PublicChainEventItem, PublicFarmAreaBoundary } from '@/types/publicTrace';
import {
  getEventTypeLabel,
  getTranslatedEventData,
  formatDisplayDateTime,
} from '@/utils/eventFormatter';

// Fix icon mặc định của Leaflet
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

interface RouteMapProps {
  events: PublicChainEventItem[];
  /** Ranh giới vùng trồng (CV-05). Hiển thị dạng polygon màu xanh lá read-only. */
  farmAreaBoundary?: PublicFarmAreaBoundary | null;
}

export const RouteMap = ({ events, farmAreaBoundary }: RouteMapProps) => {
  const mapRef = useRef<HTMLDivElement>(null);
  const leafletMapRef = useRef<L.Map | null>(null);

  // Lọc các sự kiện có tọa độ
  const locationEvents = events.filter(
    (e) => e.latitude !== null && e.longitude !== null
  );

  // Kiểm tra polygon có điểm hợp lệ không
  const boundaryPoints = farmAreaBoundary?.points ?? [];
  const hasBoundary = boundaryPoints.length >= 3;

  useEffect(() => {
    if (!mapRef.current) return;
    if (locationEvents.length === 0 && !hasBoundary) return;

    // Tính điểm trung tâm khởi tạo bản đồ
    let initialCenter: [number, number];
    if (hasBoundary) {
      const latAvg =
        boundaryPoints.reduce((sum, p) => sum + p.latitude, 0) / boundaryPoints.length;
      const lngAvg =
        boundaryPoints.reduce((sum, p) => sum + p.longitude, 0) / boundaryPoints.length;
      initialCenter = [latAvg, lngAvg];
    } else {
      initialCenter = [locationEvents[0].latitude!, locationEvents[0].longitude!];
    }

    // Khởi tạo bản đồ nếu chưa có
    if (!leafletMapRef.current) {
      leafletMapRef.current = L.map(mapRef.current).setView(initialCenter, 13);

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      }).addTo(leafletMapRef.current);
    }

    const map = leafletMapRef.current;

    // Xóa lớp cũ (marker + polygon)
    map.eachLayer((layer) => {
      if (layer instanceof L.Marker || layer instanceof L.Polygon) {
        map.removeLayer(layer);
      }
    });

    // ─── Vẽ polygon ranh giới vùng trồng (CV-05) ───
    if (hasBoundary) {
      const latlngs: L.LatLngExpression[] = boundaryPoints.map(
        (p) => [p.latitude, p.longitude] as L.LatLngExpression
      );

      const polygon = L.polygon(latlngs, {
        color: '#059669',
        weight: 2,
        fillColor: '#059669',
        fillOpacity: 0.12,
        dashArray: '4 4',
      }).addTo(map);

      const areaText =
        farmAreaBoundary?.calculatedArea != null
          ? `${Number(farmAreaBoundary.calculatedArea).toFixed(4)} ha`
          : 'Chưa tính';

      polygon.bindPopup(
        `<div style="font-family: system-ui; padding: 4px; min-width: 160px;">
          <strong style="font-size: 14px; color: #059669;">🌿 Vùng trồng</strong>
          <div style="margin-top: 4px; font-size: 13px;">
            <strong>Tên:</strong> ${farmAreaBoundary?.name ?? '—'}
          </div>
          <div style="font-size: 13px;">
            <strong>Diện tích tính toán:</strong> ${areaText}
          </div>
          <div style="font-size: 11px; color: #6b7280; margin-top: 4px;">
            Ranh giới hiển thị chỉ mang tính tham khảo.
          </div>
        </div>`
      );
    }

    // ─── Vẽ marker sự kiện ───
    const coords: [number, number][] = [];

    locationEvents.forEach((event, index) => {
      const lat = event.latitude!;
      const lng = event.longitude!;
      const label = getEventTypeLabel(event.eventType);
      const date = formatDisplayDateTime(event.recordedAt);

      coords.push([lat, lng]);

      // Build translated popup content using shared formatter
      const translatedData = getTranslatedEventData(
        event.eventType,
        (event.eventData as Record<string, unknown>) || {},
      );

      const detailsHtml = Object.entries(translatedData)
        .map(
          ([fieldLabel, value]) =>
            `<div style="font-size: 13px;"><strong>${fieldLabel}:</strong> ${value}</div>`,
        )
        .join('');

      // Tạo icon có số thứ tự
      const numberIcon = L.divIcon({
        html: `<div style="
          background: #059669;
          color: white;
          border-radius: 50%;
          width: 24px;
          height: 24px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: bold;
          font-size: 12px;
          border: 2px solid white;
          box-shadow: 0 2px 4px rgba(0,0,0,0.3);
        ">${index + 1}</div>`,
        className: '',
        iconSize: [24, 24],
        iconAnchor: [12, 12],
      });

      // Thêm marker với số thứ tự
      L.marker([lat, lng], { icon: numberIcon })
        .addTo(map)
        .bindPopup(`
          <div style="font-family: system-ui; padding: 4px; min-width: 180px;">
            <strong style="font-size: 16px;">${label}</strong>
            <div style="font-size: 13px; color: #666; margin-top: 2px;">${date}</div>
            ${detailsHtml ? `<div style="margin-top: 6px;">${detailsHtml}</div>` : ''}
            <div style="font-size: 12px; color: #999; margin-top: 4px;">
              Sự kiện #${index + 1}/${locationEvents.length}
            </div>
          </div>
        `);
    });

    // Fit bounds ưu tiên polygon + marker
    const allCoords: L.LatLngExpression[] = [
      ...coords,
      ...boundaryPoints.map((p) => [p.latitude, p.longitude] as L.LatLngExpression),
    ];

    if (allCoords.length > 1) {
      const bounds = L.latLngBounds(allCoords as L.LatLngBoundsLiteral);
      map.fitBounds(bounds, { padding: [40, 40], maxZoom: 15 });
    }

    // Invalidate size khi component mount
    setTimeout(() => {
      map.invalidateSize();
    }, 200);

    return () => {
      if (leafletMapRef.current) {
        leafletMapRef.current.remove();
        leafletMapRef.current = null;
      }
    };
  }, [locationEvents, hasBoundary, boundaryPoints, farmAreaBoundary]);

  // Nếu không có tọa độ và không có polygon, không hiển thị
  if (locationEvents.length === 0 && !hasBoundary) {
    return (
      <div className="bg-white rounded-xl shadow-sm p-6 text-center text-gray-500">
        <p className="text-lg font-semibold">Không có dữ liệu vị trí</p>
        <p className="text-sm">Các sự kiện của lô hàng này chưa có tọa độ để hiển thị trên bản đồ.</p>
      </div>
    );
  }

  return (
    <div className="relative z-0 isolate bg-white rounded-xl shadow-sm overflow-hidden">
      <div ref={mapRef} style={{ height: '450px', width: '100%' }} />
      <div className="p-3 bg-gray-50 border-t border-gray-100 text-xs text-gray-400 flex justify-between items-center">
        <span>
          {locationEvents.length > 0 && `${locationEvents.length} điểm hành trình`}
          {locationEvents.length > 0 && hasBoundary && ' · '}
          {hasBoundary && (
            <span className="text-emerald-600 font-medium">
              🌿 Ranh giới vùng trồng: {farmAreaBoundary?.name}
              {farmAreaBoundary?.calculatedArea != null &&
                ` (${Number(farmAreaBoundary.calculatedArea).toFixed(4)} ha)`}
            </span>
          )}
        </span>
        <span>Click marker hoặc vùng để xem chi tiết</span>
      </div>
    </div>
  );
};