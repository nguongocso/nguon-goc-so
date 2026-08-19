import { useEffect, useState } from 'react';
import {
  MapContainer,
  Marker,
  TileLayer,
  useMapEvents,
} from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Khắc phục lỗi icon marker của Leaflet khi chạy bằng Vite.
delete (L.Icon.Default.prototype as any)._getIconUrl;

L.Icon.Default.mergeOptions({
  iconRetinaUrl:
    'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl:
    'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl:
    'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

interface MapPosition {
  lat: number;
  lng: number;
}

interface LocationPickerProps {
  onLocationSelect: (
    latitude: number,
    longitude: number,
  ) => void;
  initialPosition?: MapPosition;
  height?: string;
}

interface LocationMarkerProps {
  onLocationSelect: (
    latitude: number,
    longitude: number,
  ) => void;
  initialPosition?: MapPosition;
}

function LocationMarker({
  onLocationSelect,
  initialPosition,
}: LocationMarkerProps) {
  const initialLatitude = initialPosition?.lat;
  const initialLongitude = initialPosition?.lng;

  const [position, setPosition] =
    useState<L.LatLng | null>(() => {
      if (
        typeof initialLatitude !== 'number' ||
        typeof initialLongitude !== 'number'
      ) {
        return null;
      }

      return L.latLng(
        initialLatitude,
        initialLongitude,
      );
    });

  const map = useMapEvents({
    click(event) {
      const { lat, lng } = event.latlng;

      setPosition(event.latlng);
      onLocationSelect(lat, lng);
    },
  });

  useEffect(() => {
    if (
      typeof initialLatitude !== 'number' ||
      typeof initialLongitude !== 'number'
    ) {
      return;
    }

    const nextPosition = L.latLng(
      initialLatitude,
      initialLongitude,
    );

    setPosition(nextPosition);

    map.setView(
      nextPosition,
      Math.max(map.getZoom(), 16),
      {
        animate: true,
      },
    );
  }, [
    initialLatitude,
    initialLongitude,
    map,
  ]);

  if (!position) {
    return null;
  }

  return <Marker position={position} />;
}

export function LocationPicker({
  onLocationSelect,
  initialPosition,
  height = '300px',
}: LocationPickerProps) {
  const defaultCenter: L.LatLngExpression =
    initialPosition
      ? [
          initialPosition.lat,
          initialPosition.lng,
        ]
      : [21.0285, 105.8542];

  return (
    <div
      style={{
        height,
        width: '100%',
      }}
      className="overflow-hidden rounded-md border"
    >
      <MapContainer
        center={defaultCenter}
        zoom={initialPosition ? 16 : 13}
        style={{
          height: '100%',
          width: '100%',
        }}
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        />

        <LocationMarker
          onLocationSelect={onLocationSelect}
          initialPosition={initialPosition}
        />
      </MapContainer>
    </div>
  );
}