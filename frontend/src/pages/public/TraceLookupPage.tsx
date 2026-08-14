import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import {
  getPublicCertifications,
  getPublicTrace,
} from '@/api/publicApi';

import type { PublicTraceResponse } from '@/types/publicTrace';
import type { PublicLotCertificationsResponse } from '@/types/publicCertification';

import { ProductInfo } from '@/components/public/ProductInfo';
import { RecallAlert } from '@/components/public/RecallAlert';
import { Timeline } from '@/components/public/Timeline';
import { RouteMap } from '@/components/public/RouteMap';
import { ProductFeedbackForm } from '@/components/public/ProductFeedbackForm';
import { PublicCertificationsSection } from '@/components/public/PublicCertificationsSection';

import {
  Home,
  List,
  LoaderCircle,
  MapPin,
  MessageSquareWarning,
} from 'lucide-react';

import { Logo } from '@/components/common/Logo';

import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '@/components/ui/tabs';

export default function TraceLookupPage() {
  const { codeValue } = useParams<{ codeValue: string }>();

  const [data, setData] =
    useState<PublicTraceResponse | null>(null);

  const [loading, setLoading] = useState(true);

  const [error, setError] =
    useState<string | null>(null);

  const [certificationData, setCertificationData] =
    useState<PublicLotCertificationsResponse | null>(null);

  const [certificationLoading, setCertificationLoading] =
    useState(true);

  const [certificationError, setCertificationError] =
    useState<string | null>(null);

  useEffect(() => {
    if (!codeValue) {
      setError('Mã tra cứu không hợp lệ.');
      setLoading(false);
      setCertificationLoading(false);
      return;
    }

    /**
     * Tra cứu thông tin sản phẩm.
     *
     * Flow:
     * 1. Lấy GPS từ trình duyệt.
     * 2. Gửi latitude + longitude lên backend.
     * 3. Backend gọi LocationIQ để reverse geocoding.
     * 4. Backend lưu location vào trace_code_scan_logs.
     */
    const fetchTrace = async () => {
      setLoading(true);
      setError(null);

      try {
        /**
         * Gọi API tra cứu.
         *
         * FE chỉ gửi latitude + longitude.
         * Không gửi location.
         */
        const loadTrace = async (
          latitude?: number,
          longitude?: number
        ) => {
          console.log('Gửi GPS lên BE:', {
            latitude,
            longitude,
          });

          const result = await getPublicTrace(
            codeValue,
            latitude,
            longitude
          );

          setData(result);
        };

        /**
         * Kiểm tra trình duyệt có hỗ trợ Geolocation hay không.
         */
        if (!navigator.geolocation) {
          console.warn(
            'Trình duyệt không hỗ trợ Geolocation'
          );

          // Vẫn cho phép tra cứu nếu không có GPS.
          await loadTrace();

          return;
        }

        /**
         * getCurrentPosition() sử dụng callback,
         * vì vậy chuyển nó thành Promise để
         * fetchTrace() có thể await.
         */
        await new Promise<void>((resolve, reject) => {
          navigator.geolocation.getCurrentPosition(
            async (position) => {
              try {
                const { latitude, longitude } =
                  position.coords;

                console.log('GPS lấy được:', {
                  latitude,
                  longitude,
                });

                await loadTrace(
                  latitude,
                  longitude
                );

                resolve();
              } catch (error) {
                reject(error);
              }
            },

            async (geoError) => {
              console.warn(
                'Không lấy được vị trí:',
                geoError.code,
                geoError.message
              );

              try {
                /**
                 * Người dùng không cấp quyền GPS
                 * hoặc GPS bị lỗi.
                 *
                 * Vẫn cho phép tra cứu.
                 *
                 * Backend sẽ lưu latitude/longitude
                 * là NULL và location = "Không xác định".
                 */
                await loadTrace();

                resolve();
              } catch (error) {
                reject(error);
              }
            },

            {
              enableHighAccuracy: true,
              timeout: 10000,
              maximumAge: 0,
            }
          );
        });
      } catch (err: any) {
        const message =
          err.response?.data?.message ||
          'Không thể tra cứu thông tin.';

        setError(message);
      } finally {
        setLoading(false);
      }
    };

    /**
     * Lấy chứng nhận công khai.
     */
    const fetchCertifications = async () => {
      try {
        setCertificationLoading(true);
        setCertificationError(null);

        const result =
          await getPublicCertifications(codeValue);

        setCertificationData(result);
      } catch (err: any) {
        const status = err.response?.status;

        /**
         * Nếu backend chưa triển khai endpoint
         * hoặc không có dữ liệu thì ẩn section.
         */
        if (status === 404 || status === 501) {
          setCertificationError(null);
          setCertificationData(null);
        } else {
          const message =
            err.response?.data?.message ||
            'Không thể tải thông tin chứng nhận.';

          setCertificationError(message);
          setCertificationData(null);
        }
      } finally {
        setCertificationLoading(false);
      }
    };

    fetchTrace();
    fetchCertifications();
  }, [codeValue]);

  /**
   * Đang tra cứu.
   */
  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="text-center">
          <LoaderCircle className="mx-auto h-8 w-8 animate-spin text-emerald-600" />

          <p className="mt-4 text-gray-600">
            Đang tra cứu thông tin...
          </p>
        </div>
      </div>
    );
  }

  /**
   * Có lỗi khi tra cứu.
   */
  if (error) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-red-50">
            <MapPin className="h-6 w-6 text-red-500" />
          </div>

          <h2 className="mt-4 text-xl font-bold text-gray-900">
            Không tìm thấy
          </h2>

          <p className="mt-2 text-gray-600">
            {error}
          </p>

          <Link
            to="/"
            className="mt-6 inline-flex items-center gap-2 font-medium text-emerald-600 hover:text-emerald-700"
          >
            <Home className="h-4 w-4" />
            Về trang chủ
          </Link>
        </div>
      </div>
    );
  }

  /**
   * Không có dữ liệu.
   */
  if (!data) {
    return null;
  }

  /**
   * Kiểm tra các event có latitude + longitude.
   */
  const hasLocationData = data.events.some(
    (event) =>
      event.latitude !== null &&
      event.longitude !== null
  );

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="border-b border-gray-100 bg-white">
        <div className="mx-auto max-w-5xl px-4 py-5">
          <Logo />

          <p className="mt-1 text-sm text-gray-500">
            Tra cứu hành trình sản phẩm
          </p>
        </div>
      </header>

      <main className="mx-auto max-w-5xl space-y-4 px-4 py-6">
        {/* Mã tra cứu */}
        <div className="rounded-xl border border-gray-100 bg-white p-4 text-center shadow-sm">
          <span className="text-xs uppercase tracking-wider text-gray-400">
            Mã tra cứu
          </span>

          <p className="break-all font-mono text-lg font-semibold text-gray-800">
            {data.codeValue}
          </p>
        </div>

        {/* Thông tin sản phẩm */}
        <ProductInfo
          productName={data.productName}
          shipmentCode={data.shipmentCode}
          status={data.shipmentStatus}
        />

        {/* Cảnh báo thu hồi */}
        {data.recalled &&
          data.recallMessage && (
            <RecallAlert
              message={data.recallMessage}
            />
          )}

        {/* Chứng nhận công khai */}
        <PublicCertificationsSection
          data={certificationData}
          isLoading={certificationLoading}
          error={certificationError}
        />

        {/* Gửi phản ánh */}
        {data.productionLotId ? (
          <ProductFeedbackForm
            productionLotId={data.productionLotId}
            productName={data.productName}
          />
        ) : (
          <section className="rounded-xl border border-amber-200 bg-amber-50/60 p-5 shadow-sm">
            <div className="flex gap-3">
              <MessageSquareWarning className="mt-0.5 h-5 w-5 shrink-0 text-amber-700" />

              <div>
                <h2 className="font-semibold text-gray-900">
                  Gửi phản ánh sản phẩm
                </h2>

                <p className="mt-1 text-sm leading-5 text-gray-600">
                  Chức năng gửi phản ánh không khả dụng cho sản phẩm này.
                </p>
              </div>
            </div>
          </section>
        )}

        {/* Bản đồ và danh sách sự kiện */}
        <div className="overflow-hidden rounded-xl bg-white shadow-sm">
          <Tabs
            defaultValue={
              hasLocationData ? 'map' : 'list'
            }
            className="w-full"
          >
            <TabsList className="h-auto w-full justify-start rounded-none border-b bg-gray-50/50 p-0">
              <TabsTrigger
                value="map"
                disabled={!hasLocationData}
                className="flex items-center gap-2 rounded-none px-4 py-3 data-[state=active]:border-b-2 data-[state=active]:border-emerald-600 data-[state=active]:bg-transparent"
              >
                <MapPin className="h-4 w-4" />

                Bản đồ

                {!hasLocationData && (
                  <span className="text-xs font-normal text-gray-400">
                    (không có dữ liệu)
                  </span>
                )}
              </TabsTrigger>

              <TabsTrigger
                value="list"
                className="flex items-center gap-2 rounded-none px-4 py-3 data-[state=active]:border-b-2 data-[state=active]:border-emerald-600 data-[state=active]:bg-transparent"
              >
                <List className="h-4 w-4" />

                Danh sách sự kiện
              </TabsTrigger>
            </TabsList>

            <TabsContent
              value="map"
              className="p-0"
            >
              <RouteMap
                events={data.events}
              />
            </TabsContent>

            <TabsContent
              value="list"
              className="p-4"
            >
              <Timeline
                events={data.events}
              />
            </TabsContent>
          </Tabs>
        </div>

        {/* Footer */}
        <div className="border-t border-gray-200 py-4 text-center text-xs text-gray-400">
          © {new Date().getFullYear()} Nguồn gốc số.
          Thông tin chỉ mang tính tham khảo.
        </div>
      </main>
    </div>
  );
}