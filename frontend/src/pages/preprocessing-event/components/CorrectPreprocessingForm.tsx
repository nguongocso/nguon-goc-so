import { AlertTriangle, FilePenLine, LoaderCircle } from 'lucide-react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { CorrectPreprocessingFields } from './CorrectPreprocessingFields';
import { useCorrectPreprocessingForm } from './useCorrectPreprocessingForm';

/** Form đính chính sự kiện sơ chế nông sản. */
export function CorrectPreprocessingForm() {
  const controller = useCorrectPreprocessingForm();
  const {
    formState: { isSubmitting },
    handleSubmit,
    id,
    navigate,
    onSubmit,
    serverError,
    sourceData,
    sourceEvent,
  } = controller;
  return (
    <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-amber-800">
          <FilePenLine className="size-5" /> Đính chính sự kiện sơ chế
        </CardTitle>
        <CardDescription>
          Hệ thống tạo một sự kiện đính chính mới; dữ liệu sự kiện gốc vẫn được
          giữ nguyên trong dòng thời gian.
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <CardContent className="space-y-6">
          {serverError && (
            <Alert variant="destructive" aria-live="assertive">
              <AlertTriangle className="size-4" />
              <AlertDescription>{serverError}</AlertDescription>
            </Alert>
          )}
          {sourceData ? (
            <div className="grid gap-3 rounded-xl border border-emerald-100 bg-emerald-50 p-4 text-sm sm:grid-cols-2">
              <div>
                <span className="text-muted-foreground">Lô sản xuất</span>
                <p className="font-medium text-emerald-900">
                  {sourceData.productionLotName}
                </p>
              </div>
              <div>
                <span className="text-muted-foreground">Sự kiện gốc</span>
                <p className="break-all font-mono text-xs text-emerald-900">
                  {sourceEvent?.id}
                </p>
              </div>
            </div>
          ) : (
            <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
              Không tải sẵn được dữ liệu sự kiện gốc. Bạn vẫn có thể nhập đầy
              đủ thông tin đính chính; backend sẽ kiểm tra ID và quyền truy cập.
            </div>
          )}
          <CorrectPreprocessingFields controller={controller} />
        </CardContent>
        <CardFooter className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate(-1)}
            disabled={isSubmitting}
          >
            Hủy
          </Button>
          <Button type="submit" variant="edit" disabled={isSubmitting || !id}>
            {isSubmitting && <LoaderCircle className="mr-2 size-4 animate-spin" />}
            {isSubmitting ? 'Đang đính chính...' : 'Tạo sự kiện đính chính'}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}
