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
import { VerifiedAlert } from '@/components/public/VerifiedAlert';
import { Timeline } from '@/components/public/Timeline';
import { RouteMap } from '@/components/public/RouteMap';
import { ProductFeedbackForm } from '@/components/public/ProductFeedbackForm';
import { PublicCertificationsSection } from '@/components/public/PublicCertificationsSection';
import { PublicInspectionSection } from '@/components/public/PublicInspectionSection';
import { LanguageSwitcher } from '@/components/public/LanguageSwitcher';
import { LanguageProvider, useLanguage } from '@/context/LanguageContext';

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

function TraceLookupContent() {
  const { codeValue } = useParams<{ codeValue: string }>();
  const location = useLocation();
  const { t } = useLanguage();

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

    const alreadyScanned = !!scanResult;

    const fetchTrace = async () => {
      setLoading(true);
      setError(null);

      try {
        const loadTrace = async (
          latitude?: number,
          longitude?: number
        ) => {
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

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="text-center">
          <LoaderCircle className="mx-auto h-8 w-8 animate-spin text-emerald-600" />
          <p className="mt-4 text-gray-600">
            {t('loading_info')}
          </p>
        </div>
      </div>
    );
  }

  if (error) {
    const isCancelledError = error.includes('đã được đánh dấu HỦY') || error.includes('HỦY');

    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50 p-4">
        <div className="max-w-md w-full rounded-2xl bg-white p-8 text-center shadow-md border border-slate-100">
          <div className={`mx-auto flex size-14 items-center justify-center rounded-full ${isCancelledError ? 'bg-amber-100 text-amber-600' : 'bg-red-100 text-red-600'}`}>
            <MessageSquareWarning className="size-8" />
          </div>

          <h2 className="mt-5 text-xl font-bold text-slate-900">
            {isCancelledError ? t('cancelled_code_title') : t('invalid_code_title')}
          </h2>

          <p className="mt-3 text-sm leading-relaxed text-slate-600 bg-slate-50 p-4 rounded-xl border border-slate-200">
            {error}
          </p>

          <Link
            to="/"
            className="mt-6 inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-600 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-emerald-700 shadow-sm"
          >
            <Home className="size-4" />
            {t('back_to_home')}
          </Link>
        </div>
      </div>
    );
  }

  if (!data) {
    return null;
  }

  const hasLocationData = data.events.some(
    (event) =>
      event.latitude !== null &&
      event.longitude !== null
  );
  const hasFarmBoundary = (data.farmAreaBoundary?.points?.length ?? 0) >= 3;
  const hasMapData = hasLocationData || hasFarmBoundary;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="border-b border-gray-100 bg-white">
        <div className="mx-auto max-w-5xl px-4 py-4 flex items-center justify-between">
          <div>
            <Logo />
            <p className="mt-1 text-sm text-gray-500">
              {t('header_subtitle')}
            </p>
          </div>
          <LanguageSwitcher />
        </div>
      </header>

      <main className="mx-auto max-w-5xl space-y-4 px-4 py-6">
        {/* Mã tra cứu */}
        <div className="rounded-xl border border-gray-100 bg-white p-4 text-center shadow-sm">
          <span className="text-xs uppercase tracking-wider text-gray-400">
            {t('code_label')}
          </span>

          <p className="break-all font-mono text-lg font-semibold text-gray-800">
            {data.codeValue}
          </p>
        </div>

        {/* Thông tin sản phẩm */}
        <ProductInfo
          productName={data.productName}
          productNameEn={data.productNameEn}
          lotName={data.lotName}
          lotCode={data.lotCode}
          shipmentCode={data.shipmentCode}
          status={data.shipmentStatus}
        />

        {/* Cảnh báo mã bị khóa hoặc Ghi chú đã xác minh */}
        {data.locked ? (
          <LockAlert
            lockReason={data.lockReason}
            lockedAt={data.lockedAt}
          />
        ) : (
          data.verificationNote && (
            <VerifiedAlert
              verificationNote={data.verificationNote}
              unlockedAt={data.unlockedAt}
            />
          )
        )}

        {/* Cảnh báo thu hồi */}
        {data.recalled &&
          (data.recallMessage || data.recallMessageEn) && (
            <RecallAlert
              message={data.recallMessage || ''}
              messageEn={data.recallMessageEn}
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
          inspections={inspectionData?.inspections ?? data.inspections}
          data={inspectionData}
          isLoading={inspectionLoading}
          error={inspectionError}
        />

        {/* Gửi phản ánh */}
        {data.productionLotId ? (
          <ProductFeedbackForm
            productionLotId={data.productionLotId}
            productName={data.productName}
            traceCodeValue={codeValue}
          />
        ) : (
          <section className="rounded-xl border border-amber-200 bg-amber-50/60 p-5 shadow-sm">
            <div className="flex gap-3">
              <MessageSquareWarning className="mt-0.5 h-5 w-5 shrink-0 text-amber-700" />

              <div>
                <h2 className="font-semibold text-gray-900">
                  {t('feedback_title')}
                </h2>

                <p className="mt-1 text-sm leading-5 text-gray-600">
                  {t('feedback_not_available')}
                </p>
              </div>
            </div>
          </section>
        )}

        {/* Bản đồ và danh sách sự kiện */}
        <div className="overflow-hidden rounded-xl bg-white shadow-sm">
          <Tabs
            defaultValue={
              hasMapData ? 'map' : 'list'
            }
            className="w-full"
          >
            <TabsList className="h-auto w-full justify-start rounded-none border-b bg-gray-50/50 p-0">
              <TabsTrigger
                value="map"
                disabled={!hasMapData}
                className="flex items-center gap-2 rounded-none px-4 py-3 data-[state=active]:border-b-2 data-[state=active]:border-emerald-600 data-[state=active]:bg-transparent"
              >
                <MapPin className="h-4 w-4" />

                {t('map_tab')}

                {!hasMapData && (
                  <span className="text-xs font-normal text-gray-400">
                    {t('no_location_data')}
                  </span>
                )}
              </TabsTrigger>

              <TabsTrigger
                value="list"
                className="flex items-center gap-2 rounded-none px-4 py-3 data-[state=active]:border-b-2 data-[state=active]:border-emerald-600 data-[state=active]:bg-transparent"
              >
                <List className="h-4 w-4" />

                {t('list_tab')}
              </TabsTrigger>
            </TabsList>

            <TabsContent
              value="map"
              className="p-0"
            >
              <RouteMap
                events={data.events}
                farmAreaBoundary={data.farmAreaBoundary}
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
          © {new Date().getFullYear()} {t('footer_copyright')}
        </div>
      </main>
    </div>
  );
}

export default function TraceLookupPage() {
  return (
    <LanguageProvider>
      <TraceLookupContent />
    </LanguageProvider>
  );
}
