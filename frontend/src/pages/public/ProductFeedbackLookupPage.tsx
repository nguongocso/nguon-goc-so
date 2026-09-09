import { useRef, useState } from "react";
import type { FormEvent } from "react";
import { isAxiosError } from "axios";
import {
  ArrowRight,
  CheckCircle2,
  Clock3,
  LoaderCircle,
  MessageCircleMore,
  RefreshCw,
  Search,
  ShieldCheck,
  TriangleAlert,
} from "lucide-react";
import { Link } from "react-router-dom";

import { lookupPublicProductFeedback } from "@/api/productFeedbackApi";
import { Logo } from "@/components/common/Logo";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type {
  ProductFeedbackStatus,
  PublicProductFeedbackLookupResult,
} from "@/types/productFeedback";

type LookupErrorKind = "not-found" | "rate-limit" | "system";

const STATUS_CONTENT: Record<
  ProductFeedbackStatus,
  { label: string; description: string; className: string }
> = {
  NEW: {
    label: "Đã tiếp nhận",
    description: "Phản ánh đã được hệ thống ghi nhận và đang chờ tiếp nhận xử lý.",
    className: "border-sky-200 bg-sky-50 text-sky-700",
  },
  IN_PROGRESS: {
    label: "Đang xử lý",
    description: "Đơn vị phụ trách đang kiểm tra và xử lý phản ánh.",
    className: "border-amber-200 bg-amber-50 text-amber-700",
  },
  ESCALATED_TO_RECALL: {
    label: "Đã chuyển thu hồi",
    description: "Phản ánh đã được chuyển sang quy trình xem xét thu hồi.",
    className: "border-orange-200 bg-orange-50 text-orange-700",
  },
  CLOSED: {
    label: "Đã đóng",
    description: "Phản ánh đã hoàn tất xử lý.",
    className: "border-emerald-200 bg-emerald-50 text-emerald-700",
  },
};

const ERROR_CONTENT: Record<LookupErrorKind, { title: string; message: string }> = {
  "not-found": {
    title: "Không tìm thấy phản ánh",
    message: "Vui lòng kiểm tra lại mã tra cứu và thử lại.",
  },
  "rate-limit": {
    title: "Bạn đã tra cứu quá nhiều lần",
    message: "Vui lòng chờ một lúc rồi thực hiện lại.",
  },
  system: {
    title: "Chưa thể tra cứu lúc này",
    message: "Hệ thống đang gián đoạn. Vui lòng thử lại sau.",
  },
};

function normalizeLookupCode(value: string) {
  return value.trim().toUpperCase();
}

export default function ProductFeedbackLookupPage() {
  const inputRef = useRef<HTMLInputElement>(null);
  const [lookupCode, setLookupCode] = useState("");
  const [result, setResult] = useState<PublicProductFeedbackLookupResult | null>(null);
  const [errorKind, setErrorKind] = useState<LookupErrorKind | null>(null);
  const [validationMessage, setValidationMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const executeLookup = async (normalizedCode: string) => {
    setErrorKind(null);
    setResult(null);
    setIsLoading(true);

    try {
      const data = await lookupPublicProductFeedback({ lookupCode: normalizedCode });
      setResult(data);
    } catch (error: unknown) {
      if (isAxiosError(error) && error.response?.status === 404) {
        setErrorKind("not-found");
      } else if (isAxiosError(error) && error.response?.status === 429) {
        setErrorKind("rate-limit");
      } else {
        setErrorKind("system");
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const normalizedCode = normalizeLookupCode(lookupCode);
    if (!normalizedCode) {
      setValidationMessage("Vui lòng nhập mã tra cứu phản ánh.");
      inputRef.current?.focus();
      return;
    }

    setLookupCode(normalizedCode);
    setValidationMessage(null);
    void executeLookup(normalizedCode);
  };

  const resetLookup = () => {
    setLookupCode("");
    setResult(null);
    setErrorKind(null);
    setValidationMessage(null);
    window.requestAnimationFrame(() => inputRef.current?.focus());
  };

  const statusContent = result ? STATUS_CONTENT[result.status] : null;
  const errorContent = errorKind ? ERROR_CONTENT[errorKind] : null;

  return (
    <div className="relative min-h-screen overflow-hidden bg-gradient-to-b from-emerald-50 via-white to-green-50">
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute -left-24 -top-24 h-80 w-80 rounded-full bg-emerald-200/40 blur-3xl" />
        <div className="absolute -right-24 top-1/3 h-96 w-96 rounded-full bg-green-100/60 blur-3xl" />
      </div>

      <header className="relative z-10 border-b border-emerald-100/80 bg-white/70 backdrop-blur">
        <div className="mx-auto flex h-24 max-w-6xl items-center justify-between px-4 sm:px-6">
          <Link to="/" aria-label="Về trang chủ">
            <Logo height={88} />
          </Link>
          <div className="hidden items-center gap-2 text-sm font-medium text-emerald-800 sm:flex">
            <ShieldCheck className="h-5 w-5" />
            Tra cứu an toàn và bảo mật
          </div>
        </div>
      </header>

      <main className="relative z-10 mx-auto grid max-w-6xl gap-8 px-4 py-10 sm:px-6 lg:grid-cols-[0.9fr_1.1fr] lg:items-start lg:py-16">
        <section className="pt-2 lg:pt-8">
          <div className="inline-flex items-center gap-2 rounded-full bg-emerald-100 px-3 py-1 text-sm font-medium text-emerald-800">
            <MessageCircleMore className="h-4 w-4" />
            Dành cho người gửi phản ánh
          </div>
          <h1 className="mt-5 max-w-xl text-4xl font-extrabold tracking-tight text-slate-900 sm:text-5xl">
            Tra cứu trạng thái phản ánh
          </h1>
          <p className="mt-4 max-w-lg text-base leading-7 text-slate-600 sm:text-lg">
            Nhập mã được cung cấp sau khi gửi phản ánh để theo dõi tiến độ và xem phản hồi công khai từ đơn vị xử lý.
          </p>

          <div className="mt-8 space-y-4 text-sm text-slate-600">
            <div className="flex gap-3">
              <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
              Không cần đăng nhập hoặc cung cấp thêm thông tin cá nhân.
            </div>
            <div className="flex gap-3">
              <Clock3 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
              Trạng thái được cập nhật theo tiến độ xử lý thực tế.
            </div>
          </div>
        </section>

        <section className="rounded-3xl border border-emerald-100 bg-white/90 p-5 shadow-xl shadow-emerald-100/60 backdrop-blur sm:p-8">
          <form onSubmit={handleSubmit} noValidate>
            <div className="space-y-2">
              <Label htmlFor="feedback-lookup-code" className="text-base font-semibold text-slate-800">
                Mã tra cứu phản ánh
              </Label>
              <Input
                ref={inputRef}
                id="feedback-lookup-code"
                value={lookupCode}
                onChange={(event) => {
                  setLookupCode(event.target.value);
                  if (validationMessage) setValidationMessage(null);
                }}
                placeholder="PA-XXXX-XXXX-XXXX-XXXX"
                autoComplete="off"
                autoCapitalize="characters"
                spellCheck={false}
                maxLength={64}
                aria-invalid={Boolean(validationMessage)}
                aria-describedby="feedback-code-help feedback-code-error"
                className="h-13 border-emerald-200 bg-white font-mono text-base uppercase tracking-wide focus-visible:ring-emerald-300"
              />
              <p id="feedback-code-help" className="text-sm leading-5 text-slate-500">
                Mã tra cứu nằm trong thông báo xác nhận sau khi bạn gửi phản ánh.
              </p>
              {validationMessage && (
                <p id="feedback-code-error" role="alert" className="text-sm text-red-600">
                  {validationMessage}
                </p>
              )}
            </div>

            <Button
              type="submit"
              disabled={isLoading}
              className="mt-5 h-12 w-full gap-2 bg-emerald-600 text-base font-semibold text-white hover:bg-emerald-700"
            >
              {isLoading ? <LoaderCircle className="h-5 w-5 animate-spin" /> : <Search className="h-5 w-5" />}
              {isLoading ? "Đang tra cứu..." : "Tra cứu trạng thái"}
            </Button>
          </form>

          {result && statusContent && (
            <div className="mt-7 border-t border-slate-100 pt-7" aria-live="polite">
              <p className="text-sm font-medium text-slate-500">Trạng thái phản ánh</p>
              <div className={`mt-2 inline-flex rounded-full border px-3 py-1 text-sm font-semibold ${statusContent.className}`}>
                {statusContent.label}
              </div>
              <p className="mt-3 text-sm leading-6 text-slate-600">{statusContent.description}</p>

              <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 p-4 sm:p-5">
                <p className="font-semibold text-slate-800">Phản hồi công khai</p>
                <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-600">
                  {result.publicResponse?.trim() || "Chưa có phản hồi công khai"}
                </p>
              </div>

              <Button type="button" variant="outline" onClick={resetLookup} className="mt-5 w-full gap-2 border-emerald-300 text-emerald-700 hover:bg-emerald-50 hover:text-emerald-800">
                <RefreshCw className="h-4 w-4" />
                Tra cứu mã khác
              </Button>
            </div>
          )}

          {errorContent && (
            <div className="mt-7 border-t border-slate-100 pt-7" role="alert">
              <div className="rounded-2xl border border-red-200 bg-red-50 p-4 sm:p-5">
                <div className="flex gap-3">
                  <TriangleAlert className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
                  <div>
                    <p className="font-semibold text-slate-900">{errorContent.title}</p>
                    <p className="mt-1 text-sm leading-6 text-slate-600">{errorContent.message}</p>
                  </div>
                </div>
              </div>
              <Button
                type="button"
                variant="outline"
                onClick={errorKind === "system" ? () => void executeLookup(lookupCode) : resetLookup}
                disabled={isLoading}
                className="mt-5 w-full gap-2 border-emerald-300 text-emerald-700 hover:bg-emerald-50 hover:text-emerald-800"
              >
                <RefreshCw className="h-4 w-4" />
                {errorKind === "system" ? "Thử lại" : "Kiểm tra mã khác"}
              </Button>
            </div>
          )}

          <Link to="/" className="mt-6 flex items-center justify-center gap-2 text-sm font-medium text-emerald-700 hover:text-emerald-800">
            Tra cứu nguồn gốc sản phẩm
            <ArrowRight className="h-4 w-4" />
          </Link>
        </section>
      </main>

      <footer className="relative z-10 px-4 py-6 text-center text-sm text-slate-500">
        © {new Date().getFullYear()} Nguồn gốc số – Thông tin minh bạch từ nông trại đến bàn ăn
      </footer>
    </div>
  );
}
