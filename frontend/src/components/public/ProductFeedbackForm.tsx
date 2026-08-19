import { isAxiosError } from "axios";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { MessageSquareWarning, Send } from "lucide-react";

import { createProductFeedback } from "@/api/productFeedbackApi";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

interface ProductFeedbackFormProps {
  productionLotId: string;
  productName: string;
}

interface ProductFeedbackFormValues {
  content: string;
}

export function ProductFeedbackForm({
  productionLotId,
  productName,
}: ProductFeedbackFormProps) {
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
      await createProductFeedback(productionLotId, {
        content: content.trim(),
      });

      reset();
      toast.success("Đã gửi phản ánh. Hợp tác xã sẽ kiểm tra thông tin.");
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
          {/* CHANGED: thêm variant="create" */}
          <Button type="submit" disabled={isSubmitting} variant="create">
            <Send className="h-4 w-4" />
            {isSubmitting ? "Đang gửi..." : "Gửi phản ánh"}
          </Button>
        </div>
      </form>
    </section>
  );
}