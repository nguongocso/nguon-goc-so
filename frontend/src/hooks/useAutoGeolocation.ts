import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";

interface UseAutoGeolocationOptions {
  /**
   * Được gọi khi lấy vị trí thành công.
   */
  onLocation: (
    latitude: number,
    longitude: number,
  ) => void;

  /**
   * Được gọi khi lấy vị trí thủ công thất bại.
   * Những lần tự động lấy vị trí sẽ không hiển thị lỗi.
   */
  onError?: (message: string) => void;

  /**
   * Chỉ kích hoạt tự động lấy vị trí khi true.
   * Hữu ích với dialog chỉ cần lấy GPS khi đang mở.
   */
  enabled?: boolean;
}

const getGeolocationErrorMessage = (
  error: GeolocationPositionError,
): string => {
  switch (error.code) {
    case error.PERMISSION_DENIED:
      return "Bạn chưa cấp quyền truy cập vị trí";

    case error.POSITION_UNAVAILABLE:
      return "Không thể xác định vị trí hiện tại";

    case error.TIMEOUT:
      return "Quá thời gian chờ lấy vị trí";

    default:
      return error.message || "Không thể lấy vị trí hiện tại";
  }
};

export function useAutoGeolocation({
  onLocation,
  onError,
  enabled = true,
}: UseAutoGeolocationOptions) {
  const [locationLoading, setLocationLoading] =
    useState(false);

  const onLocationRef = useRef(onLocation);
  const onErrorRef = useRef(onError);

  const mountedRef = useRef(true);
  const enabledRef = useRef(enabled);
  const requestInProgressRef = useRef(false);
  const requestIdRef = useRef(0);

  onLocationRef.current = onLocation;
  onErrorRef.current = onError;
  enabledRef.current = enabled;

  const fetchLocation = useCallback(
    (silent = false) => {
      if (!enabledRef.current) {
        return;
      }

      if (!navigator.geolocation) {
        if (!silent) {
          onErrorRef.current?.(
            "Trình duyệt không hỗ trợ định vị",
          );
        }

        return;
      }

      /*
       * Không chạy nhiều yêu cầu GPS đồng thời.
       * Tránh trường hợp useEffect và sự kiện đổi quyền
       * cùng gọi getCurrentPosition.
       */
      if (requestInProgressRef.current) {
        return;
      }

      requestInProgressRef.current = true;

      const currentRequestId =
        ++requestIdRef.current;

      if (mountedRef.current) {
        setLocationLoading(true);
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
          /*
           * Bỏ qua callback nếu component đã unmount,
           * hook đã disabled hoặc đây là request cũ.
           */
          if (
            !mountedRef.current ||
            !enabledRef.current ||
            currentRequestId !== requestIdRef.current
          ) {
            requestInProgressRef.current = false;
            return;
          }

          requestInProgressRef.current = false;
          setLocationLoading(false);

          onLocationRef.current(
            position.coords.latitude,
            position.coords.longitude,
          );
        },

        (error) => {
          if (
            !mountedRef.current ||
            currentRequestId !== requestIdRef.current
          ) {
            requestInProgressRef.current = false;
            return;
          }

          requestInProgressRef.current = false;
          setLocationLoading(false);

          if (!silent) {
            onErrorRef.current?.(
              getGeolocationErrorMessage(error),
            );
          }
        },

        {
          enableHighAccuracy: true,
          timeout: 10_000,
          maximumAge: 0,
        },
      );
    },
    [],
  );

  useEffect(() => {
    mountedRef.current = true;

    return () => {
      mountedRef.current = false;
      requestIdRef.current += 1;
      requestInProgressRef.current = false;
    };
  }, []);

  useEffect(() => {
    enabledRef.current = enabled;

    if (!enabled) {
      /*
       * Vô hiệu hóa callback của request đang chạy.
       */
      requestIdRef.current += 1;
      requestInProgressRef.current = false;

      if (mountedRef.current) {
        setLocationLoading(false);
      }

      return;
    }

    let permissionStatus: PermissionStatus | null =
      null;

    let effectCancelled = false;

    const handlePermissionChange = () => {
      if (
        effectCancelled ||
        permissionStatus?.state !== "granted"
      ) {
        return;
      }

      /*
       * Người dùng vừa cấp lại quyền trong trình duyệt.
       * Lấy vị trí tự động và không hiện lỗi nếu thất bại.
       */
      fetchLocation(true);
    };

    const initializeGeolocation = async () => {
      if (!navigator.geolocation) {
        return;
      }

      if (!navigator.permissions?.query) {
        fetchLocation(true);
        return;
      }

      try {
        const status =
          await navigator.permissions.query({
            name: "geolocation" as PermissionName,
          });

        if (effectCancelled) {
          return;
        }

        permissionStatus = status;

        status.addEventListener(
          "change",
          handlePermissionChange,
        );

        /*
         * granted:
         * Quyền đã được cấp, lấy vị trí ngay.
         *
         * prompt:
         * Trình duyệt chưa hỏi hoặc người dùng chưa quyết định.
         * Chủ động gọi GPS để trình duyệt hiện hộp cấp quyền.
         *
         * denied:
         * Không tự gọi lại, tránh làm phiền người dùng.
         * Người dùng vẫn có thể bấm nút lấy vị trí thủ công
         * sau khi thay đổi quyền trong cài đặt trình duyệt.
         */
        if (
          status.state === "granted" ||
          status.state === "prompt"
        ) {
          fetchLocation(true);
        }
      } catch {
        if (!effectCancelled) {
          fetchLocation(true);
        }
      }
    };

    void initializeGeolocation();

    return () => {
      effectCancelled = true;

      permissionStatus?.removeEventListener(
        "change",
        handlePermissionChange,
      );
    };
  }, [enabled, fetchLocation]);

  return {
    locationLoading,
    fetchLocation,
  };
}