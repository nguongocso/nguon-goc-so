import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { BrowserQRCodeReader } from "@zxing/browser";
import { recordPublicScan } from "@/api/publicApi";
import {
  LogIn,
  ScanLine,
  Search,
  ShieldCheck,
  Truck,
  BadgeCheck,
} from "lucide-react";
import { Logo } from "@/components/common/Logo";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/hooks/useAuth";

export default function PublicHomePage() {
  const navigate = useNavigate();
  const { user, isLoading: isAuthLoading } = useAuth();

  const [code, setCode] = useState("");
  const [isScanning, setIsScanning] = useState(false);

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const controlsRef = useRef<{ stop: () => void } | null>(null);

  useEffect(() => {
    if (!isAuthLoading && user) {
      navigate("/dashboard", { replace: true });
    }
  }, [user, isAuthLoading, navigate]);

  const stopScanner = () => {
    controlsRef.current?.stop();
    controlsRef.current = null;
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    setIsScanning(false);
  };

  const startScanner = () => {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      toast.error("Trình duyệt không hỗ trợ camera");
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
          throw new Error("Không tìm thấy vùng hiển thị camera.");
        }

        const stream = await navigator.mediaDevices.getUserMedia({
          audio: false,
          video: { facingMode: { ideal: "environment" } },
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
            if (codeValue.includes("/public/trace/")) {
              codeValue = codeValue.split("/public/trace/")[1];
            }

            if (!codeValue) {
              toast.error("Mã QR không hợp lệ");
              return;
            }

            toast.success("Đã quét mã tra cứu.");
            controls.stop();
            stream.getTracks().forEach((track) => track.stop());
            streamRef.current = null;
            controlsRef.current = null;
            setIsScanning(false);

            // Luồng quét QR thực tế: gọi POST /public/trace/{codeValue}/scan
            // để tạo TraceCodeScanLog và kích hoạt đánh giá nghi vấn,
            // sau đó chuyển kết quả sang trang tra cứu qua router state.
            // (Không dùng GET lookup ở luồng quét.)
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
                  "Không thể ghi nhận lượt quét. Vui lòng thử lại.";
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
        if (scanError instanceof DOMException && scanError.name === "NotAllowedError") {
          toast.error("Bạn chưa cho phép dùng camera. Hãy cấp quyền camera rồi thử lại.");
          return;
        }
        if (scanError instanceof DOMException && scanError.name === "NotReadableError") {
          toast.error("Camera đang được ứng dụng khác sử dụng. Hãy đóng ứng dụng đó rồi thử lại.");
          return;
        }
        toast.error("Không thể mở camera. Hãy kiểm tra camera hoặc nhập mã thủ công.");
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
    if (!code.trim()) {
      toast.error("Vui lòng nhập mã tra cứu");
      return;
    }
    navigate(`/public/trace/${code.trim()}`);
  };

  const features = [
    { icon: ShieldCheck, title: "Minh bạch", desc: "Thông tin rõ ràng từ nông trại" },
    { icon: Truck, title: "Hành trình", desc: "Theo dõi từng công đoạn vận chuyển" },
    { icon: BadgeCheck, title: "Chứng nhận", desc: "Đạt chuẩn an toàn thực phẩm" },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-emerald-50 via-white to-green-50 flex flex-col items-center relative overflow-hidden">
      {/* Decorative Background Elements */}
      <div className="absolute top-0 left-0 w-full h-full overflow-hidden pointer-events-none">
        <div className="absolute -top-20 -left-20 w-80 h-80 bg-emerald-200/40 rounded-full blur-3xl" />
        <div className="absolute top-1/3 -right-20 w-96 h-96 bg-green-100/50 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-1/4 w-64 h-64 bg-lime-200/30 rounded-full blur-3xl" />
      </div>

      {/* Header */}
<header className="w-full h-25 px-6 flex justify-between items-center relative z-10">
  <Logo height={100} />

  {!isAuthLoading && !user && (
    <Button
      variant="outline"
      className="gap-2 border-emerald-300 text-emerald-700 hover:bg-emerald-50 hover:text-emerald-800"
      onClick={() => navigate("/login")}
    >
      <LogIn className="h-4 w-4" />
      Đăng nhập
    </Button>
  )}
</header>

      {/* Hero Section */}
      <main className="flex-1 w-full max-w-7xl mx-auto px-4 py-8 md:py-16 relative z-10 flex flex-col lg:flex-row items-center gap-12">
        {/* Left Content */}
        <div className="flex-1 text-center lg:text-left space-y-6">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-2 bg-emerald-100 text-emerald-800 px-3 py-1 rounded-full text-sm font-medium">
              Truy xuất nguồn gốc thực phẩm
            </div>
            <h1 className="text-4xl md:text-5xl lg:text-6xl font-extrabold tracking-tight">
              <span className="text-gray-800">Hành trình </span>
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-600 to-green-500">
                xanh
              </span>
              <br />
              <span className="text-gray-800">từ nông trại</span>
            </h1>
            <p className="text-lg text-muted-foreground max-w-md mx-auto lg:mx-0">
              Khám phá câu chuyện đằng sau mỗi sản phẩm nông sản bạn chọn. Minh bạch, an toàn, và trọn vẹn thiên nhiên.
            </p>
          </div>

          {/* Feature Icons */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 max-w-lg mx-auto lg:mx-0">
            {features.map(({ icon: Icon, title, desc }) => (
              <div key={title} className="flex flex-col items-center lg:items-start gap-1 p-3 rounded-xl bg-white/70 backdrop-blur-sm border border-emerald-100 shadow-sm">
                <Icon className="h-6 w-6 text-emerald-600" />
                <span className="text-sm font-semibold text-gray-700">{title}</span>
                <span className="text-xs text-muted-foreground text-center lg:text-left">{desc}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Right: QR Scanner Card */}
        <div className="flex-1 w-full max-w-md">
          <div className="bg-white/80 backdrop-blur-lg rounded-3xl shadow-2xl border border-emerald-100 p-6 md:p-8 space-y-5">
            {isScanning ? (
              <div className="space-y-4">
                <div className="overflow-hidden rounded-2xl bg-black relative group">
                  <video
                    ref={videoRef}
                    className="w-full aspect-square object-cover"
                    muted
                    playsInline
                  />
                  <div className="absolute inset-0 border-2 border-emerald-400 rounded-2xl pointer-events-none" />
                  <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                    <div className="w-48 h-48 border-2 border-emerald-400 rounded-lg" />
                  </div>
                </div>
                <Button
                  variant="outline"
                  onClick={stopScanner}
                  className="w-full border-gray-200 text-gray-600 hover:bg-gray-50"
                >
                  Hủy quét
                </Button>
              </div>
            ) : (
              <div className="space-y-5">
                <Button
                  onClick={startScanner}
                  className="w-full h-14 text-lg gap-2 bg-gradient-to-r from-emerald-600 to-green-500 hover:from-emerald-700 hover:to-green-600 text-white shadow-lg shadow-emerald-200/50 transition-all hover:shadow-xl"
                >
                  <ScanLine className="h-5 w-5" />
                  Quét mã QR
                </Button>

                <div className="relative">
                  <div className="absolute inset-0 flex items-center">
                    <span className="w-full border-t border-gray-200" />
                  </div>
                  <div className="relative flex justify-center text-xs uppercase">
                    <span className="bg-white px-2 text-muted-foreground">
                      Hoặc nhập mã
                    </span>
                  </div>
                </div>

                <form onSubmit={handleSubmit} className="flex gap-2">
                  <Input
                    type="text"
                    placeholder="Nhập mã tra cứu"
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    className="flex-1 border-emerald-200 focus-visible:ring-emerald-300"
                  />
                  <Button type="submit" variant="search">
                    <Search className="h-4 w-4" />
                    <span className="sr-only">Tìm kiếm</span>
                  </Button>
                </form>
              </div>
            )}
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="w-full py-6 text-center relative z-10">
        <p className="text-sm text-muted-foreground">
          © {new Date().getFullYear()} Nguồn gốc số – Thông tin minh bạch từ nông trại đến bàn ăn
        </p>
      </footer>
    </div>
  );
}