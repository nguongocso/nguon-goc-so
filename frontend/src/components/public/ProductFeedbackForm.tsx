import { isAxiosError } from "axios";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import { CheckCircle2, Copy, MessageSquareWarning, Search, Send } from "lucide-react";

import { createProductFeedback } from "@/api/productFeedbackApi";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { PublicProductFeedbackCreated } from "@/types/productFeedback";

interface ProductFeedbackFormProps {
  productionLotId: string;
  productName: string;
  traceCodeValue?: string;
}

interface ProductFeedbackFormValues {
  content: string;
}

export function ProductFeedbackForm({
  productionLotId,
  productName,
  traceCodeValue,
}: ProductFeedbackFormProps) {
  const [submittedFeedback, setSubmittedFeedback] =
    useState<PublicProductFeedbackCreated | null>(null);
  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<ProductFeedbackFormValues>({
    defaultValues: { content: "" },
  });

  const contentLength = watch("content")?.length ?? 0;

  const onSubmit = async ({ content }: ProductFeedbackFormValues) => {
    try {
      const createdFeedback = await createProductFeedback(productionLotId, {
        content: content.trim(),
        traceCodeValue,
      });

      reset();
      setSubmittedFeedback(createdFeedback);
      toast.success("Đã gửi phản ánh. Vui lòng lưu mã tra cứu.");
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message ??
          (error.response
            ? "Không thể gửi phản ánh. Vui lòng thử lại."
            : "Không thể kết nối đến máy chủ. Vui lòng thử lại sau.")
        : "Đã xảy ra lỗi khi gửi phản ánh.";

      toast.error(message);
    }
  };

  const copyLookupCode = async () => {
    if (!submittedFeedback?.lookupCode || !navigator.clipboard) {
      toast.error("Không thể sao chép tự động. Vui lòng chọn và sao chép mã.");
      return;
    }

    try {
      await navigator.clipboard.writeText(submittedFeedback.lookupCode);
      toast.success("Đã sao chép mã tra cứu.");
    } catch {
      toast.error("Không thể sao chép tự động. Vui lòng chọn và sao chép mã.");
    }
  };

  if (submittedFeedback) {
    return (
      <section className="rounded-xl border border-emerald-200 bg-emerald-50/70 p-5 shadow-sm">
        <div className="flex gap-3">
          <CheckCircle2 className="mt-0.5 h-6 w-6 shrink-0 text-emerald-700" />
          <div>
            <h2 className="font-semibold text-gray-900">Đã gửi phản ánh</h2>
            <p className="mt-1 text-sm leading-5 text-gray-600">
              Hợp tác xã sẽ tiếp nhận và cập nhật tiến độ xử lý trên hệ thống.
            </p>
          </div>
        </div>

        <div className="mt-5 rounded-xl border border-emerald-200 bg-white p-4 text-center">
          <p className="text-sm font-medium text-gray-600">Mã tra cứu phản ánh của bạn</p>
          <p className="mt-2 select-all break-all font-mono text-xl font-bold tracking-wide text-emerald-800">
            {submittedFeedback.lookupCode}
          </p>
          <p className="mt-2 text-xs leading-5 text-amber-700">
            Hãy lưu mã này ngay. Vì lý do bảo mật, hệ thống không thể hiển thị lại mã sau khi bạn rời trang.
          </p>
        </div>

        <div className="mt-4 grid gap-2 sm:grid-cols-2">
          <Button type="button" onClick={() => void copyLookupCode()} className="gap-2">
            <Copy className="h-4 w-4" />
            Sao chép mã
          </Button>
          <Link
            to="/public/product-feedbacks/lookup"
            className="inline-flex h-9 items-center justify-center gap-2 rounded-lg border border-emerald-600 bg-white px-3 text-sm font-medium text-emerald-700 transition hover:bg-emerald-50"
          >
            <Search className="h-4 w-4" />
            Tra cứu trạng thái
          </Link>
        </div>

        <button
          type="button"
          onClick={() => setSubmittedFeedback(null)}
          className="mt-4 text-sm font-medium text-emerald-700 underline-offset-4 hover:underline"
        >
          Gửi phản ánh khác
        </button>
      </section>
    );
  }

  return (
    <section className="rounded-xl border border-amber-200 bg-amber-50/60 p-5 shadow-sm">
      <div className="flex gap-3">
        <MessageSquareWarning className="mt-0.5 h-5 w-5 shrink-0 text-amber-700" />

        <div>
          <h2 className="font-semibold text-gray-900">
            Gửi phản ánh sản phẩm
          </h2>

          <p className="mt-1 text-sm leading-5 text-gray-600">
            Nếu bạn nghi ngờ tem giả hoặc thấy thông tin của {productName} chưa
            chính xác, hãy gửi phản ánh để hợp tác xã kiểm tra.
          </p>
        </div>
      </div>

      <form className="mt-4 space-y-3" onSubmit={handleSubmit(onSubmit)}>
        <div className="space-y-2">
          <div className="flex items-center justify-between gap-4">
            <Label htmlFor="feedback-content">Nội dung phản ánh *</Label>

            <span className="text-xs text-gray-500">
              {contentLength}/1000
            </span>
          </div>

          <Textarea
            id="feedback-content"
            className="min-h-28 resize-y bg-white"
            placeholder="Ví dụ: Thông tin ngày thu hoạch trên hệ thống không khớp với bao bì sản phẩm."
            maxLength={1000}
            aria-invalid={Boolean(errors.content)}
            {...register("content", {
              validate: (value) =>
                value.trim().length > 0 ||
                "Vui lòng nhập nội dung phản ánh.",
              maxLength: {
                value: 1000,
                message: "Nội dung phản ánh không được vượt quá 1000 ký tự.",
              },
            })}
          />

          {errors.content && (
            <p className="text-sm text-destructive">
              {errors.content.message}
            </p>
          )}
        </div>

        <div className="flex justify-end">
          <Button type="submit" disabled={isSubmitting} variant="create">
            <Send className="h-4 w-4" />
            {isSubmitting ? "Đang gửi..." : "Gửi phản ánh"}
          </Button>
        </div>
      </form>
    </section>
  );
}
