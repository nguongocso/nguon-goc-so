import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';

import {
  getPublicCertifications,
  getPublicInspections,
  getPublicTrace,
} from '@/api/publicApi';

import type { PublicTraceResponse } from '@/types/publicTrace';
import type { PublicLotCertificationsResponse } from '@/types/publicCertification';
import type { PublicInspectionResponse } from '@/types/publicInspection';

import { ProductInfo } from '@/components/public/ProductInfo';
import { RecallAlert } from '@/components/public/RecallAlert';
import { LockAlert } from '@/components/public/LockAlert';
import { Timeline } from '@/components/public/Timeline';
import { RouteMap } from '@/components/public/RouteMap';
import { ProductFeedbackForm } from '@/components/public/ProductFeedbackForm';
import { PublicCertificationsSection } from '@/components/public/PublicCertificationsSection';
import { PublicInspectionSection } from '@/components/public/PublicInspectionSection';

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

interface TraceLookupLocationState {
  scanResult?: PublicTraceResponse;
}

export default function TraceLookupPage() {
  const { codeValue } = useParams<{ codeValue: string }>();
  const location = useLocation();

  // Luồng quét QR: scanner gọi POST /public/trace/{codeValue}/scan,
  // sau đó chuyển hướng cùng dữ liệu qua router state. State chỉ tồn tại
  // trong lần điều hướng đầu tiên — reload/mở lại URL sẽ mất state và
  // rơi về GET lookup (không tạo ScanLog).
  const scanResult = (location.state as TraceLookupLocationState | null)
    ?.scanResult;

  const [data, setData] = useState<PublicTraceResponse | null>(
    scanResult ?? null,
  );

  const [loading, setLoading] = useState(!scanResult);

  const [error, setError] = useState<string | null>(null);

  const [certificationData, setCertificationData] =
    useState<PublicLotCertificationsResponse | null>(null);

  const [certificationLoading, setCertificationLoading] =
    useState(true);

  const [certificationError, setCertificationError] =
    useState<string | null>(null);

  const [inspectionData, setInspectionData] =
    useState<PublicInspectionResponse | null>(null);

  const [inspectionLoading, setInspectionLoading] = useState(true);

  const [inspectionError, setInspectionError] =
    useState<string | null>(null);

  useEffect(() => {
    if (!codeValue) {
      setError('Mã tra cứu không hợp lệ.');
      setLoading(false);
      setCertificationLoading(false);
      setInspectionLoading(false);
      return;
    }

    // Nếu đã có kết quả quét QR (POST /scan đã thực hiện ở trang chủ)
    // thì không gọi GET tra cứu nữa — tránh tạo thêm ScanLog.
    const alreadyScanned = !!scanResult;

    /**
     * Tra cứu thông tin sản phẩm (GET lookup — đọc thuần túy).
     *
     * Flow:
     * 1. Lấy GPS từ trình duyệt.
     * 2. Gửi latitude + longitude lên backend.
     * 3. Backend gọi LocationIQ để reverse geocoding.
     * 4. Backend trả thông tin; KHÔNG tạo TraceCodeScanLog.
     */
    const fetchTrace = async () => {
      setLoading(true);
      setError(null);

      try {
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

        if (!navigator.geolocation) {
          await loadTrace();
          return;
        }

        await new Promise<void>((resolve, reject) => {
          navigator.geolocation.getCurrentPosition(
            async (position) => {
              try {
                const { latitude, longitude } =
                  position.coords;

                await loadTrace(latitude, longitude);
                resolve();
              } catch (error) {
                reject(error);
              }
            },

            async () => {
              try {
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

    /**
     * Lấy kết quả kiểm nghiệm công khai.
     */
    const fetchInspections = async () => {
      try {
        setInspectionLoading(true);
        setInspectionError(null);

        const result = await getPublicInspections(codeValue);

        setInspectionData(result);
      } catch (err: any) {
        const status = err.response?.status;

        if (status === 404 || status === 501) {
          setInspectionError(null);
          setInspectionData(null);
        } else {
          const message =
            err.response?.data?.message ||
            'Không thể tải kết quả kiểm nghiệm.';

          setInspectionError(message);
          setInspectionData(null);
        }
      } finally {
        setInspectionLoading(false);
      }
    };

    if (!alreadyScanned) {
      fetchTrace();
    }
    fetchCertifications();
    fetchInspections();
  }, [codeValue, scanResult]);

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

        {/* Cảnh báo mã bị khóa */}
        {data.locked && (
          <LockAlert
            lockReason={data.lockReason}
            lockedAt={data.lockedAt}
          />
        )}

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

        {/* Kết quả kiểm nghiệm công khai */}
        <PublicInspectionSection
          data={inspectionData}
          isLoading={inspectionLoading}
          error={inspectionError}
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