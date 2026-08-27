import { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { PublicChainEventItem } from '@/types/publicTrace';
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
}

export const RouteMap = ({ events }: RouteMapProps) => {
  const mapRef = useRef<HTMLDivElement>(null);
  const leafletMapRef = useRef<L.Map | null>(null);

  // Lọc các sự kiện có tọa độ
  const locationEvents = events.filter(
    (e) => e.latitude !== null && e.longitude !== null
  );

  useEffect(() => {
    if (!mapRef.current || locationEvents.length === 0) return;

    // Khởi tạo bản đồ nếu chưa có
    if (!leafletMapRef.current) {
      leafletMapRef.current = L.map(mapRef.current).setView(
        [locationEvents[0].latitude!, locationEvents[0].longitude!],
        10
      );

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      }).addTo(leafletMapRef.current);
    }

    const map = leafletMapRef.current;

    // Xóa marker cũ
    map.eachLayer((layer) => {
      if (layer instanceof L.Marker) {
        map.removeLayer(layer);
      }
    });

    // Mảng tọa độ để tính bounds
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

    // Fit bounds nếu có nhiều hơn 1 điểm
    if (coords.length > 1) {
      const bounds = L.latLngBounds(coords);
      map.fitBounds(bounds, {
        padding: [40, 40],
        maxZoom: 15,
      });
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
  }, [locationEvents]);

  // Nếu không có tọa độ, không hiển thị
  if (locationEvents.length === 0) {
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
      <div className="p-3 bg-gray-50 border-t border-gray-100 text-xs text-gray-400 flex justify-between">
        <span>{locationEvents.length} điểm hành trình</span>
        <span>Click marker để xem chi tiết</span>
      </div>
    </div>
  );
};