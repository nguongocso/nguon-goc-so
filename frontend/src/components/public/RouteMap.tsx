import { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { PublicChainEventItem, PublicFarmAreaBoundary } from '@/types/publicTrace';
import {
  getEventTypeLabel,
  getTranslatedEventData,
  formatDisplayDateTime,
} from '@/utils/eventFormatter';
import { useLanguage } from '@/context/LanguageContext';

delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

interface RouteMapProps {
  events: PublicChainEventItem[];
  farmAreaBoundary?: PublicFarmAreaBoundary | null;
}

export const createFarmAreaBoundaryPopupContent = (
  name: string | null | undefined,
  areaText: string,
  isEnglish = false,
): HTMLDivElement => {
  const container = document.createElement('div');
  container.style.cssText = 'font-family: system-ui; padding: 4px; min-width: 160px;';

  const title = document.createElement('strong');
  title.style.cssText = 'font-size: 14px; color: #059669;';
  title.textContent = isEnglish ? '🌿 Farm area' : '🌿 Vùng trồng';
  container.appendChild(title);

  const nameRow = document.createElement('div');
  nameRow.style.cssText = 'margin-top: 4px; font-size: 13px;';
  const nameLabel = document.createElement('strong');
  nameLabel.textContent = isEnglish ? 'Name:' : 'Tên:';
  nameRow.append(nameLabel, document.createTextNode(` ${name ?? '—'}`));
  container.appendChild(nameRow);

  const areaRow = document.createElement('div');
  areaRow.style.cssText = 'font-size: 13px;';
  const areaLabel = document.createElement('strong');
  areaLabel.textContent = isEnglish ? 'Calculated area:' : 'Diện tích tính toán:';
  areaRow.append(areaLabel, document.createTextNode(` ${areaText}`));
  container.appendChild(areaRow);

  const note = document.createElement('div');
  note.style.cssText = 'font-size: 11px; color: #6b7280; margin-top: 4px;';
  note.textContent = isEnglish
    ? 'The displayed boundary is for reference only.'
    : 'Ranh giới hiển thị chỉ mang tính tham khảo.';
  container.appendChild(note);

  return container;
};

export const RouteMap = ({ events, farmAreaBoundary }: RouteMapProps) => {
  const { lang, t } = useLanguage();
  const isEn = lang === 'en';

  const mapRef = useRef<HTMLDivElement>(null);
  const leafletMapRef = useRef<L.Map | null>(null);

  const locationEvents = events.filter(
    (e) => e.latitude !== null && e.longitude !== null,
  );
  const boundaryPoints = farmAreaBoundary?.points ?? [];
  const hasBoundary = boundaryPoints.length >= 3;

  useEffect(() => {
    if (!mapRef.current || (locationEvents.length === 0 && !hasBoundary)) return;

    const initialCenter: [number, number] = hasBoundary
      ? [
          boundaryPoints.reduce((sum, point) => sum + point.latitude, 0) / boundaryPoints.length,
          boundaryPoints.reduce((sum, point) => sum + point.longitude, 0) / boundaryPoints.length,
        ]
      : [locationEvents[0].latitude!, locationEvents[0].longitude!];

    if (!leafletMapRef.current) {
      leafletMapRef.current = L.map(mapRef.current).setView(initialCenter, hasBoundary ? 13 : 10);

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      }).addTo(leafletMapRef.current);
    }

    const map = leafletMapRef.current;

    map.eachLayer((layer) => {
      if (layer instanceof L.Marker || layer instanceof L.Polygon) {
        map.removeLayer(layer);
      }
    });

    if (hasBoundary) {
      const latlngs: L.LatLngExpression[] = boundaryPoints.map(
        (point) => [point.latitude, point.longitude],
      );
      const polygon = L.polygon(latlngs, {
        color: '#059669',
        weight: 2,
        fillColor: '#059669',
        fillOpacity: 0.12,
        dashArray: '4 4',
      }).addTo(map);
      const areaText = farmAreaBoundary?.calculatedArea != null
        ? `${Number(farmAreaBoundary.calculatedArea).toFixed(4)} ha`
        : isEn ? 'Not calculated' : 'Chưa tính';
      polygon.bindPopup(
        createFarmAreaBoundaryPopupContent(farmAreaBoundary?.name, areaText, isEn),
      );
    }

    const coords: [number, number][] = [];

    locationEvents.forEach((event, index) => {
      const lat = event.latitude!;
      const lng = event.longitude!;
      const rawLabel = getEventTypeLabel(event.eventType, lang);
      const eventTypeKey = `event_${event.eventType}` as any;
      const translatedLabel = t(eventTypeKey);
      const label = translatedLabel && !translatedLabel.startsWith('event_') ? translatedLabel : rawLabel;
      const date = formatDisplayDateTime(event.recordedAt, lang);

      coords.push([lat, lng]);

      const translatedData = getTranslatedEventData(
        event.eventType,
        (event.eventData as Record<string, unknown>) || {},
        lang,
      );

      const detailsHtml = Object.entries(translatedData)
        .map(
          ([fieldLabel, value]) =>
            `<div style="font-size: 13px;"><strong>${fieldLabel}:</strong> ${value}</div>`,
        )
        .join('');

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

      L.marker([lat, lng], { icon: numberIcon })
        .addTo(map)
        .bindPopup(`
          <div style="font-family: system-ui; padding: 4px; min-width: 180px;">
            <strong style="font-size: 16px;">${label}</strong>
            <div style="font-size: 13px; color: #666; margin-top: 2px;">${date}</div>
            ${detailsHtml ? `<div style="margin-top: 6px;">${detailsHtml}</div>` : ''}
            <div style="font-size: 12px; color: #999; margin-top: 4px;">
              ${
                isEn
                  ? `Event #${index + 1}/${locationEvents.length}`
                  : `Sự kiện #${index + 1}/${locationEvents.length}`
              }
            </div>
          </div>
        `);
    });

    const allCoords: L.LatLngExpression[] = [
      ...coords,
      ...boundaryPoints.map((point) => [point.latitude, point.longitude] as L.LatLngExpression),
    ];
    if (allCoords.length > 1) {
      const bounds = L.latLngBounds(allCoords as L.LatLngBoundsLiteral);
      map.fitBounds(bounds, {
        padding: [40, 40],
        maxZoom: 15,
      });
    }

    setTimeout(() => {
      map.invalidateSize();
    }, 200);

    return () => {
      if (leafletMapRef.current) {
        leafletMapRef.current.remove();
        leafletMapRef.current = null;
      }
    };
  }, [locationEvents, boundaryPoints, hasBoundary, farmAreaBoundary, isEn, lang, t]);

  if (locationEvents.length === 0 && !hasBoundary) {
    return (
      <div className="bg-white rounded-xl shadow-sm p-6 text-center text-gray-500">
        <p className="text-lg font-semibold">
          {isEn ? 'No location data available' : 'Không có dữ liệu vị trí'}
        </p>
        <p className="text-sm">
          {isEn
            ? 'Events in this shipment do not have GPS coordinates to show on map.'
            : 'Các sự kiện của lô hàng này chưa có tọa độ để hiển thị trên bản đồ.'}
        </p>
      </div>
    );
  }

  return (
    <div className="relative z-0 isolate bg-white rounded-xl shadow-sm overflow-hidden">
      <div ref={mapRef} style={{ height: '450px', width: '100%' }} />
      <div className="p-3 bg-gray-50 border-t border-gray-100 text-xs text-gray-400 flex justify-between items-center">
        <span>
          {locationEvents.length > 0 && `${locationEvents.length} ${isEn ? 'journey points' : 'điểm hành trình'}`}
          {locationEvents.length > 0 && hasBoundary && ' · '}
          {hasBoundary && (
            <span className="text-emerald-600 font-medium">
              🌿 {isEn ? 'Farm area' : 'Ranh giới vùng trồng'}: {farmAreaBoundary?.name}
              {farmAreaBoundary?.calculatedArea != null &&
                ` (${Number(farmAreaBoundary.calculatedArea).toFixed(4)} ha)`}
            </span>
          )}
        </span>
        <span>{isEn ? 'Click marker or boundary for details' : 'Click marker hoặc vùng để xem chi tiết'}</span>
      </div>
    </div>
  );
};
