import { AlertTriangle, LoaderCircle, Wheat } from 'lucide-react';
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
import { CreatePreprocessingFields } from './CreatePreprocessingFields';
import { PreprocessingImagesSection } from './PreprocessingImagesSection';
import { useCreatePreprocessingForm } from './useCreatePreprocessingForm';

/** Form ghi nhận sự kiện sơ chế nông sản từ lô đã thu hoạch. */
export function CreatePreprocessingForm() {
  const controller = useCreatePreprocessingForm();
  const {
    formState: { isSubmitting },
    handleImageChange,
    handleSubmit,
    imageFiles,
    imagePreviews,
    loadingLots,
    loadLots,
    lotsError,
    navigate,
    onSubmit,
    removeImage,
    selectedLotId,
    serverError,
    validation,
    validationLoading,
  } = controller;
  return (
    <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
      <CardHeader className="border-b border-emerald-50 bg-emerald-50/40">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-lg bg-emerald-100 text-emerald-800">
            <Wheat className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl font-bold text-emerald-900">
              Ghi sự kiện sơ chế
            </CardTitle>
            <CardDescription className="text-emerald-700">
              Ghi nhận phân loại, hao hụt và xử lý sơ bộ nông sản sau thu hoạch
            </CardDescription>
          </div>
        </div>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <CardContent className="space-y-6 pt-6">
          {serverError && (
            <Alert variant="destructive">
              <AlertTriangle className="size-4" />
              <AlertDescription>{serverError}</AlertDescription>
            </Alert>
          )}
          {lotsError && (
            <Alert variant="destructive">
              <AlertTriangle className="size-4" />
              <AlertDescription className="flex items-center justify-between gap-2">
                <span>{lotsError}</span>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => void loadLots()}
                >
                  Tải lại
                </Button>
              </AlertDescription>
            </Alert>
          )}
          <CreatePreprocessingFields controller={controller} />
          <PreprocessingImagesSection
            imageFiles={imageFiles}
            imagePreviews={imagePreviews}
            isSubmitting={isSubmitting}
            onImageChange={handleImageChange}
            onRemoveImage={removeImage}
          />
          <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
            <p className="font-medium">Sau khi ghi nhận thành công:</p>
            <ul className="mt-1 list-disc space-y-1 pl-5">
              <li>Lô chuyển từ “Đã thu hoạch” sang “Đã sơ chế”.</li>
              <li>Sản lượng thực tế được cập nhật bằng khối lượng sau sơ chế.</li>
              <li>Sự kiện đã ghi không bị sửa trực tiếp; sai sót phải được đính chính.</li>
            </ul>
          </div>
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
          <Button
            type="submit"
            variant="create"
            disabled={isSubmitting || loadingLots || validationLoading ||
              !selectedLotId || !validation?.valid}
          >
            {isSubmitting && <LoaderCircle className="mr-2 size-4 animate-spin" />}
            {isSubmitting ? 'Đang ghi nhận...' : 'Ghi sự kiện sơ chế'}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}
