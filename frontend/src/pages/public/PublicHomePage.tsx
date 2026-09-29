import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { BrowserQRCodeReader } from '@zxing/browser';
import { recordPublicScan } from '@/api/publicApi';
import { lookupPublicProductFeedback } from '@/api/productFeedbackApi';
import type { PublicProductFeedbackLookupResult } from '@/types/productFeedback';
import {
  ProductFeedbackInlineResult,
  type LookupErrorKind,
} from '@/components/public/ProductFeedbackInlineResult';
import {
  BadgeCheck,
  LogIn,
  ScanLine,
  Search,
  ShieldCheck,
  Truck,
} from 'lucide-react';
import { Logo } from '@/components/common/Logo';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useAuth } from '@/hooks/useAuth';
import heroTraceabilityOrchard from '@/assets/hero-traceability-orchard.jpg';
import { DualMarqueeRibbon } from '@/components/public/DualMarqueeRibbon';

/** Nhận diện mã tra cứu phản ánh (tiền tố PA- hoặc dạng PA+16 ký tự Base32). */
export function isProductFeedbackLookupCode(rawCode: string): boolean {
  const trimmed = rawCode.trim();
  if (!trimmed) return false;
  const upper = trimmed.toUpperCase();
  if (upper.startsWith('PA-')) return true;
  const compact = upper.replace(/-/g, '');
  return compact.startsWith('PA') && compact.length === 18;
}

export default function PublicHomePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { user, isLoading: isAuthLoading } = useAuth();

  const [code, setCode] = useState('');
  const [isScanning, setIsScanning] = useState(false);

  const [feedbackResult, setFeedbackResult] =
    useState<PublicProductFeedbackLookupResult | null>(null);
  const [feedbackErrorKind, setFeedbackErrorKind] =
    useState<LookupErrorKind | null>(null);
  const [isFeedbackLoading, setIsFeedbackLoading] = useState(false);
  const [searchedFeedbackCode, setSearchedFeedbackCode] = useState('');

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const controlsRef = useRef<{ stop: () => void } | null>(null);

  useEffect(() => {
    if (!isAuthLoading && user) {
      navigate('/dashboard', { replace: true });
    }
  }, [user, isAuthLoading, navigate]);

  const executeFeedbackLookup = async (rawLookupCode: string) => {
    const normalized = rawLookupCode.trim().toUpperCase();
    if (!normalized) return;

    setFeedbackErrorKind(null);
    setFeedbackResult(null);
    setSearchedFeedbackCode(normalized);
    setIsFeedbackLoading(true);

    try {
      const data = await lookupPublicProductFeedback({ lookupCode: normalized });
      setFeedbackResult(data);
    } catch (error: unknown) {
      if (isAxiosError(error) && error.response?.status === 404) {
        setFeedbackErrorKind('not-found');
      } else if (isAxiosError(error) && error.response?.status === 429) {
        setFeedbackErrorKind('rate-limit');
      } else {
        setFeedbackErrorKind('system');
      }
    } finally {
      setIsFeedbackLoading(false);
    }
  };

  const handleResetFeedback = () => {
    setFeedbackResult(null);
    setFeedbackErrorKind(null);
    setSearchedFeedbackCode('');
  };

  useEffect(() => {
    const feedbackCodeParam = searchParams.get('feedbackCode');
    if (feedbackCodeParam && feedbackCodeParam.trim()) {
      const cleanCode = feedbackCodeParam.trim();
      setCode(cleanCode);
      void executeFeedbackLookup(cleanCode);
    }
  }, [searchParams]);

  const stopScanner = () => {
    controlsRef.current?.stop();
    controlsRef.current = null;
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    setIsScanning(false);
  };

  const startScanner = () => {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      toast.error('Trình duyệt không hỗ trợ camera');
      return;
    }
    setIsScanning(true);
  };

  useEffect(() => {
    if (!isScanning) return;

    let isActive = true;
    const codeReader = new BrowserQRCodeReader();

    const startScanning = async () => {
      try {
        await new Promise((resolve) => window.setTimeout(resolve, 150));
        const video = videoRef.current;
        if (!video) {
          throw new Error('Không tìm thấy vùng hiển thị camera.');
        }

        const stream = await navigator.mediaDevices.getUserMedia({
          audio: false,
          video: { facingMode: { ideal: 'environment' } },
        });

        if (!isActive) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }

        streamRef.current = stream;
        video.srcObject = stream;

        const controls = await codeReader.decodeFromVideoElement(
          video,
          (result) => {
            if (!result || !isActive) return;

            let codeValue = result.getText();
            if (codeValue.includes('/public/trace/')) {
              codeValue = codeValue.split('/public/trace/')[1];
            }

            if (!codeValue) {
              toast.error('Mã QR không hợp lệ');
              return;
            }

            toast.success('Đã quét mã tra cứu.');
            controls.stop();
            stream.getTracks().forEach((track) => track.stop());
            streamRef.current = null;
            controlsRef.current = null;
            setIsScanning(false);

            const submitScan = async () => {
              try {
                let latitude: number | undefined;
                let longitude: number | undefined;

                if (navigator.geolocation) {
                  await new Promise<void>((resolve) => {
                    navigator.geolocation.getCurrentPosition(
                      (position) => {
                        latitude = position.coords.latitude;
                        longitude = position.coords.longitude;
                        resolve();
                      },
                      () => resolve(),
                      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 },
                    );
                  });
                }

                const scanResult = await recordPublicScan(
                  codeValue,
                  latitude,
                  longitude,
                );

                navigate(`/public/trace/${codeValue}`, {
                  state: { scanResult },
                });
              } catch (scanError: any) {
                const message =
                  scanError.response?.data?.message ||
                  'Không thể ghi nhận lượt quét. Vui lòng thử lại.';
                toast.error(message);
              }
            };

            void submitScan();
          },
        );

        if (!isActive) {
          controls.stop();
          return;
        }
        controlsRef.current = controls;
      } catch (scanError: unknown) {
        if (!isActive) return;
        if (scanError instanceof DOMException && scanError.name === 'NotAllowedError') {
          toast.error('Bạn chưa cho phép dùng camera. Hãy cấp quyền camera rồi thử lại.');
          return;
        }
        if (scanError instanceof DOMException && scanError.name === 'NotReadableError') {
          toast.error('Camera đang được ứng dụng khác sử dụng. Hãy đóng ứng dụng đó rồi thử lại.');
          return;
        }
        toast.error('Không thể mở camera. Hãy kiểm tra camera hoặc nhập mã thủ công.');
      } finally {
        if (isActive && !controlsRef.current) {
          setIsScanning(false);
        }
      }
    };

    void startScanning();

    return () => {
      isActive = false;
      controlsRef.current?.stop();
      controlsRef.current = null;
      streamRef.current?.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    };
  }, [isScanning, navigate]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = code.trim();
    if (!trimmed) {
      toast.error('Vui lòng nhập mã tra cứu');
      return;
    }

    if (isProductFeedbackLookupCode(trimmed)) {
      void executeFeedbackLookup(trimmed);
      return;
    }

    navigate(`/public/trace/${trimmed}`);
  };

  const features = [
    { icon: ShieldCheck, title: 'Minh bạch', desc: 'Thông tin rõ ràng từ nông trại' },
    { icon: Truck, title: 'Hành trình', desc: 'Theo dõi từng công đoạn vận chuyển' },
    { icon: BadgeCheck, title: 'Chứng nhận', desc: 'Đạt chuẩn an toàn thực phẩm' },
  ];

  return (
    <div className="flex min-h-screen flex-col bg-white">
      <header className="relative z-20 w-full border-b border-emerald-950/10 bg-white">
        <div className="mx-auto flex h-18 w-full max-w-[1600px] items-center justify-between px-4 sm:h-20 sm:px-6 lg:px-10">
          <Logo height={56} className="max-w-[210px] sm:max-w-none" />

          {!isAuthLoading && !user && (
            <Button
              variant="ghost"
              className="gap-2 text-emerald-800 hover:bg-emerald-50 hover:text-emerald-900"
              onClick={() => navigate('/login')}
            >
              <LogIn className="h-4 w-4" />
              <span>Đăng nhập</span>
            </Button>
          )}
        </div>
      </header>

      <main className="flex-1">
        <section
          data-testid="public-home-hero"
          className="relative isolate min-h-[calc(100svh-4.5rem)] overflow-hidden bg-emerald-950 sm:min-h-[calc(100svh-5rem)]"
        >
          <img
            src={heroTraceabilityOrchard}
            alt="Kỹ thuật viên và người nông dân kiểm tra nông sản trong vườn"
            width={1942}
            height={809}
            fetchPriority="high"
            className="absolute inset-0 -z-20 h-full w-full object-cover object-[68%_center] lg:object-center"
          />
          <div className="absolute inset-0 -z-10 bg-gradient-to-r from-emerald-950/65 via-emerald-950/25 to-transparent sm:from-emerald-950/65 sm:via-emerald-950/20 sm:to-transparent" />
          <div className="absolute inset-x-0 bottom-0 -z-10 h-24 bg-gradient-to-t from-emerald-950/30 to-transparent" />

          <div className="mx-auto flex min-h-[calc(100svh-4.5rem)] w-full max-w-[1600px] flex-col justify-between px-4 py-8 sm:min-h-[calc(100svh-5rem)] sm:px-6 sm:py-12 lg:px-10 lg:py-16">
            <div className="w-full max-w-3xl">
              <div className="mb-5 inline-flex items-center gap-2 rounded-none border border-white/25 bg-emerald-950/35 px-3 py-1.5 text-sm font-medium text-white backdrop-blur-sm">
                <ShieldCheck className="h-4 w-4 text-emerald-300" />
                Nền tảng truy xuất nguồn gốc nông sản
              </div>

              <h1 className="max-w-4xl text-4xl font-bold leading-[1.12] tracking-tight text-white drop-shadow-md sm:text-5xl lg:text-6xl xl:text-7xl">
                <span className="block whitespace-nowrap">Minh bạch nguồn gốc,</span>
                <span className="block text-emerald-300 drop-shadow-sm">vững niềm tin</span>
              </h1>
              <p className="mt-5 max-w-2xl text-base leading-7 text-white/95 drop-shadow sm:text-lg sm:leading-8">
                Theo dõi hành trình nông sản từ vùng trồng đến bàn ăn, xác thực
                thông tin và lựa chọn sản phẩm an toàn chỉ với một mã truy xuất.
              </p>

              <div className="mt-8 w-full max-w-2xl">
                {isScanning ? (
                  <div className="max-w-md rounded-none border border-white/25 bg-white/95 p-4 shadow-2xl backdrop-blur-md sm:p-5">
                    <div className="group relative overflow-hidden rounded-none bg-black">
                      <video
                        ref={videoRef}
                        className="aspect-square w-full object-cover"
                        muted
                        playsInline
                      />
                      <div className="pointer-events-none absolute inset-0 border-2 border-emerald-400" />
                      <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
                        <div className="h-48 w-48 border-2 border-emerald-300/90" />
                      </div>
                    </div>
                    <Button
                      variant="outline"
                      onClick={stopScanner}
                      className="mt-4 w-full rounded-none"
                    >
                      Hủy quét
                    </Button>
                  </div>
                ) : (
                  <div className="space-y-3">
                    <form
                      onSubmit={handleSubmit}
                      className="flex flex-col gap-2 rounded-none border border-white/40 bg-white p-2 shadow-2xl sm:flex-row"
                    >
                      <label htmlFor="public-trace-code" className="sr-only">
                        Mã truy xuất hoặc mã phản ánh
                      </label>
                      <div className="flex min-w-0 flex-1 items-center gap-2 px-2 sm:px-3">
                        <Search className="h-5 w-5 shrink-0 text-slate-400" />
                        <Input
                          id="public-trace-code"
                          type="text"
                          placeholder="Nhập mã truy xuất hoặc mã phản ánh (PA-...)"
                          value={code}
                          onChange={(e) => {
                            setCode(e.target.value);
                            if (feedbackErrorKind || feedbackResult) {
                              setFeedbackErrorKind(null);
                              setFeedbackResult(null);
                            }
                          }}
                          className="h-12 min-w-0 rounded-none border-0 bg-transparent px-0 text-base shadow-none focus-visible:ring-0"
                        />
                      </div>
                      <Button
                        type="submit"
                        className="h-12 shrink-0 rounded-none bg-emerald-700 px-7 text-base font-semibold text-white hover:bg-emerald-800 transition-colors"
                      >
                        Truy xuất
                      </Button>
                    </form>

                    <Button
                      type="button"
                      variant="outline"
                      onClick={startScanner}
                      className="h-11 gap-2 rounded-none border border-white/35 bg-white/12 text-white backdrop-blur-sm hover:bg-white/20 hover:text-white transition-colors"
                    >
                      <ScanLine className="h-5 w-5" />
                      Quét mã QR bằng camera
                    </Button>

                    <div className="rounded-none bg-white shadow-xl">
                      <ProductFeedbackInlineResult
                        isLoading={isFeedbackLoading}
                        lookupCode={searchedFeedbackCode}
                        result={feedbackResult}
                        errorKind={feedbackErrorKind}
                        onReset={handleResetFeedback}
                        onRetry={() => void executeFeedbackLookup(searchedFeedbackCode)}
                      />
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="mt-12 grid max-w-4xl grid-cols-1 gap-3 border-t border-white/20 pt-5 pr-16 sm:grid-cols-3 sm:gap-6 sm:pr-0">
              {features.map(({ icon: Icon, title, desc }) => (
                <div key={title} className="flex items-start gap-3 text-white">
                  <div className="rounded-none border border-white/20 bg-white/12 p-2.5 backdrop-blur-sm">
                    <Icon className="h-5 w-5 text-emerald-300" />
                  </div>
                  <div>
                    <p className="font-semibold">{title}</p>
                    <p className="mt-0.5 text-sm leading-5 text-white/70">{desc}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Dual Marquee Angled Tech Ribbons at Footer */}
        <DualMarqueeRibbon />
      </main>

      <footer className="w-full bg-emerald-950 px-4 py-5 text-center text-sm text-white/65 border-t border-emerald-900/60">
        © {new Date().getFullYear()} Nguồn Gốc Số – Minh bạch từ nông trại đến bàn ăn
      </footer>
    </div>
  );
}
