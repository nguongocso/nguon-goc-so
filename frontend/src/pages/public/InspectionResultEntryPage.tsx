import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  AlertCircle,
  Building2,
  Calendar,
  CheckCircle2,
  Clock,
  FileX2,
  HelpCircle,
  Lock,
  Package,
  ShieldCheck,
} from 'lucide-react';
import { toast } from 'sonner';

import {
  getPublicPortalData,
  submitPortalResults,
  uploadPortalResultFile,
} from '@/api/inspectionResultPortalApi';
import type {
  PublicInspectionResultEntryData,
  InspectionCriterionResultItemInput,
} from '@/types/inspectionResultPortal';
import { InspectionResultEntryForm } from '@/components/certification/InspectionResultEntryForm';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

type PageStatus = 'LOADING' | 'ACTIVE' | 'EXPIRED' | 'USED' | 'NOT_FOUND' | 'SUBMITTED' | 'ERROR';

export const InspectionResultEntryPage: React.FC = () => {
  const { token } = useParams<{ token: string }>();

  const [pageStatus, setPageStatus] = useState<PageStatus>('LOADING');
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [portalData, setPortalData] = useState<PublicInspectionResultEntryData | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!token) {
      setPageStatus('NOT_FOUND');
      setErrorMessage('Không tìm thấy mã liên kết hợp lệ.');
      return;
    }

    const fetchPortalData = async () => {
      setPageStatus('LOADING');
      try {
        const data = await getPublicPortalData(token);
        setPortalData(data);
        setPageStatus('ACTIVE');
      } catch (err: unknown) {
        const status = (err as { response?: { status?: number } })?.response?.status;
        const msg =
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
          (err instanceof Error ? err.message : '');

        if (status === 410) {
          if (msg.toLowerCase().includes('đã được sử dụng') || msg.toLowerCase().includes('used')) {
            setPageStatus('USED');
            setErrorMessage(msg || 'Liên kết này đã được sử dụng để nhập kết quả trước đó.');
          } else {
            setPageStatus('EXPIRED');
            setErrorMessage(
              msg || 'Liên kết nhập kết quả đã hết hạn. Vui lòng liên hệ hợp tác xã để được cấp liên kết mới.'
            );
          }
        } else if (status === 404) {
          setPageStatus('NOT_FOUND');
          setErrorMessage(
            msg || 'Liên kết không tồn tại hoặc đã bị thu hồi do cấp mới liên kết khác.'
          );
        } else {
          setPageStatus('ERROR');
          setErrorMessage(msg || 'Có lỗi xảy ra khi truy cập cổng nhập kết quả.');
        }
      }
    };

    fetchPortalData();
  }, [token]);

  const handleUploadFile = async (criterionId: string, file: File): Promise<string> => {
    if (!token) throw new Error('Mã liên kết không hợp lệ');
    return await uploadPortalResultFile(token, criterionId, file);
  };

  const handleSubmit = async (results: InspectionCriterionResultItemInput[]) => {
    if (!token) return;
    setIsSubmitting(true);
    try {
      await submitPortalResults(token, { results });
      setPageStatus('SUBMITTED');
      toast.success('Nộp kết quả kiểm nghiệm thành công!');
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (err instanceof Error ? err.message : 'Có lỗi khi gửi kết quả.');

      if (status === 410) {
        setPageStatus('USED');
        setErrorMessage(msg);
      } else {
        toast.error(msg);
      }
      throw err;
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-50 via-slate-50/80 to-slate-100 dark:from-slate-950 dark:via-slate-900 dark:to-slate-950 text-foreground py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Header thương hiệu và bảo mật */}
        <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-border/60">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-emerald-600 text-white flex items-center justify-center font-bold shadow-md shadow-emerald-600/20">
              NGS
            </div>
            <div>
              <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-foreground flex items-center gap-2">
                Cổng nhập kết quả kiểm nghiệm
              </h1>
              <p className="text-xs text-muted-foreground">
                Hệ sinh thái Truy xuất nguồn gốc & Chứng nhận số Quốc gia
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Badge
              variant="outline"
              className="bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-400 font-medium px-3 py-1 flex items-center gap-1.5"
            >
              <Lock className="h-3.5 w-3.5" />
              Phiên bảo mật 1 lần
            </Badge>
          </div>
        </header>

        {/* Trạng thái: Đang tải */}
        {pageStatus === 'LOADING' && (
          <div className="py-24 text-center space-y-4">
            <div className="inline-block h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-r-transparent align-[-0.125em]" />
            <p className="text-sm font-medium text-muted-foreground">
              Đang xác thực liên kết bảo mật và tải thông tin yêu cầu kiểm nghiệm...
            </p>
          </div>
        )}

        {/* Trạng thái: Hết hạn (HTTP 410) */}
        {pageStatus === 'EXPIRED' && (
          <Card className="border-amber-200 bg-amber-50/40 dark:bg-amber-950/20 shadow-md">
            <CardHeader className="text-center pb-2">
              <div className="mx-auto w-12 h-12 rounded-full bg-amber-100 dark:bg-amber-900/50 flex items-center justify-center text-amber-600 dark:text-amber-400 mb-2">
                <Clock className="h-6 w-6" />
              </div>
              <CardTitle className="text-xl font-bold text-amber-900 dark:text-amber-300">
                Liên kết nhập kết quả đã hết hạn
              </CardTitle>
              <CardDescription className="text-amber-800/80 dark:text-amber-400/80 max-w-md mx-auto text-sm pt-1">
                {errorMessage}
              </CardDescription>
            </CardHeader>
            <CardContent className="text-center pt-2 pb-6 space-y-3">
              <p className="text-xs text-muted-foreground">
                Vì lý do an toàn thông tin, mỗi liên kết chỉ có hiệu lực trong thời gian được quy
                định. Quý đơn vị vui lòng thông báo cho Hợp tác xã để phát hành lại liên kết mới.
              </p>
            </CardContent>
          </Card>
        )}

        {/* Trạng thái: Đã sử dụng (HTTP 410 - Double Submit) */}
        {pageStatus === 'USED' && (
          <Card className="border-emerald-200 bg-emerald-50/40 dark:bg-emerald-950/20 shadow-md">
            <CardHeader className="text-center pb-2">
              <div className="mx-auto w-12 h-12 rounded-full bg-emerald-100 dark:bg-emerald-900/50 flex items-center justify-center text-emerald-600 dark:text-emerald-400 mb-2">
                <ShieldCheck className="h-6 w-6" />
              </div>
              <CardTitle className="text-xl font-bold text-emerald-900 dark:text-emerald-300">
                Kết quả kiểm nghiệm đã được ghi nhận
              </CardTitle>
              <CardDescription className="text-emerald-800/80 dark:text-emerald-400/80 max-w-md mx-auto text-sm pt-1">
                {errorMessage}
              </CardDescription>
            </CardHeader>
            <CardContent className="text-center pt-2 pb-6 space-y-3">
              <p className="text-xs text-muted-foreground">
                Hệ thống đã khóa liên kết này để bảo toàn tính minh bạch và tránh việc ghi đè ngoài ý
                muốn. Quý đơn vị có thể an tâm đóng tab trình duyệt này.
              </p>
            </CardContent>
          </Card>
        )}

        {/* Trạng thái: Không tồn tại hoặc đã thu hồi (HTTP 404) */}
        {pageStatus === 'NOT_FOUND' && (
          <Card className="border-rose-200 bg-rose-50/40 dark:bg-rose-950/20 shadow-md">
            <CardHeader className="text-center pb-2">
              <div className="mx-auto w-12 h-12 rounded-full bg-rose-100 dark:bg-rose-900/50 flex items-center justify-center text-rose-600 dark:text-rose-400 mb-2">
                <FileX2 className="h-6 w-6" />
              </div>
              <CardTitle className="text-xl font-bold text-rose-900 dark:text-rose-300">
                Liên kết không hợp lệ
              </CardTitle>
              <CardDescription className="text-rose-800/80 dark:text-rose-400/80 max-w-md mx-auto text-sm pt-1">
                {errorMessage}
              </CardDescription>
            </CardHeader>
            <CardContent className="text-center pt-2 pb-6 space-y-3">
              <p className="text-xs text-muted-foreground">
                Đường link này có thể đã bị gõ sai, bị thu hồi do hợp tác xã cấp liên kết mới hơn,
                hoặc kết quả đã được hợp tác xã nhập trực tiếp vào hệ thống.
              </p>
            </CardContent>
          </Card>
        )}

        {/* Trạng thái: Lỗi chung */}
        {pageStatus === 'ERROR' && (
          <Card className="border-rose-200 bg-rose-50/40 dark:bg-rose-950/20 shadow-md">
            <CardHeader className="text-center pb-2">
              <div className="mx-auto w-12 h-12 rounded-full bg-rose-100 dark:bg-rose-900/50 flex items-center justify-center text-rose-600 dark:text-rose-400 mb-2">
                <AlertCircle className="h-6 w-6" />
              </div>
              <CardTitle className="text-xl font-bold text-rose-900 dark:text-rose-300">
                Không thể truy cập cổng nhập kết quả
              </CardTitle>
              <CardDescription className="text-rose-800/80 dark:text-rose-400/80 max-w-md mx-auto text-sm pt-1">
                {errorMessage}
              </CardDescription>
            </CardHeader>
          </Card>
        )}

        {/* Trạng thái: Nộp thành công */}
        {pageStatus === 'SUBMITTED' && (
          <Card className="border-emerald-200 bg-gradient-to-b from-emerald-50/60 to-white dark:from-emerald-950/30 dark:to-card shadow-lg py-6">
            <CardContent className="text-center space-y-5">
              <div className="mx-auto w-16 h-16 rounded-full bg-emerald-100 dark:bg-emerald-900/60 flex items-center justify-center text-emerald-600 dark:text-emerald-300">
                <CheckCircle2 className="h-10 w-10" />
              </div>
              <div className="space-y-2 max-w-lg mx-auto">
                <h2 className="text-2xl font-bold text-emerald-900 dark:text-emerald-200">
                  Nộp kết quả kiểm nghiệm thành công!
                </h2>
                <p className="text-sm text-muted-foreground">
                  Hệ thống Nguồn Gốc Số đã tiếp nhận toàn bộ kết quả kiểm nghiệm từ đơn vị của Quý
                  khách. Nguồn dữ liệu đã được xác nhận với định danh{' '}
                  <strong className="text-emerald-700 dark:text-emerald-400">
                    "Đơn vị kiểm nghiệm khai"
                  </strong>
                  .
                </p>
              </div>

              <div className="p-4 bg-muted/40 rounded-lg max-w-md mx-auto text-xs text-muted-foreground text-left space-y-1.5 border">
                <div>
                  • <strong>Đơn vị kiểm nghiệm:</strong> {portalData?.testingUnitName || portalData?.testingUnit}
                </div>
                <div>
                  • <strong>Lô nông sản:</strong> {portalData?.lotName} ({portalData?.lotCode})
                </div>
                <div>
                  • <strong>Trạng thái liên kết:</strong> Đã sử dụng (Khóa bảo mật)
                </div>
              </div>

              <p className="text-xs text-muted-foreground pt-2">
                Quý khách có thể an tâm đóng cửa sổ trình duyệt này. Xin trân trọng cảm ơn sự phối
                hợp của Quý đơn vị!
              </p>
            </CardContent>
          </Card>
        )}

        {/* Trạng thái: ACTIVE - Hiển thị thông tin và Form nhập */}
        {pageStatus === 'ACTIVE' && portalData && (
          <div className="space-y-6">
            {/* Banner tóm tắt thông tin lô và đơn vị */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <Card className="border-border/80 shadow-sm">
                <CardContent className="p-4 flex items-center gap-3">
                  <div className="h-10 w-10 rounded-lg bg-emerald-50 dark:bg-emerald-950/50 text-emerald-700 dark:text-emerald-400 flex items-center justify-center shrink-0">
                    <Building2 className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-xs text-muted-foreground font-medium">Đơn vị kiểm nghiệm</p>
                    <p className="text-sm font-semibold truncate text-foreground" title={portalData.testingUnitName || portalData.testingUnit}>
                      {portalData.testingUnitName || portalData.testingUnit}
                    </p>
                  </div>
                </CardContent>
              </Card>

              <Card className="border-border/80 shadow-sm">
                <CardContent className="p-4 flex items-center gap-3">
                  <div className="h-10 w-10 rounded-lg bg-blue-50 dark:bg-blue-950/50 text-blue-700 dark:text-blue-400 flex items-center justify-center shrink-0">
                    <Package className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-xs text-muted-foreground font-medium">Lô sản xuất</p>
                    <p className="text-sm font-semibold truncate text-foreground" title={portalData.lotName}>
                      {portalData.lotName}
                    </p>
                  </div>
                </CardContent>
              </Card>

              <Card className="border-border/80 shadow-sm">
                <CardContent className="p-4 flex items-center gap-3">
                  <div className="h-10 w-10 rounded-lg bg-amber-50 dark:bg-amber-950/50 text-amber-700 dark:text-amber-400 flex items-center justify-center shrink-0">
                    <Calendar className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-xs text-muted-foreground font-medium">Ngày gửi mẫu</p>
                    <p className="text-sm font-semibold truncate text-foreground">
                      {portalData.sampleSentDate}
                    </p>
                  </div>
                </CardContent>
              </Card>

              <Card className="border-border/80 shadow-sm">
                <CardContent className="p-4 flex items-center gap-3">
                  <div className="h-10 w-10 rounded-lg bg-rose-50 dark:bg-rose-950/50 text-rose-700 dark:text-rose-400 flex items-center justify-center shrink-0">
                    <Clock className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-xs text-muted-foreground font-medium">Hạn chót nhập</p>
                    <p className="text-sm font-semibold truncate text-foreground">
                      {new Date(portalData.expiresAt).toLocaleDateString('vi-VN')}
                    </p>
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Hướng dẫn ngắn */}
            <div className="p-4 bg-muted/40 border rounded-lg flex items-start gap-3 text-xs text-muted-foreground">
              <HelpCircle className="h-4 w-4 shrink-0 text-emerald-600 mt-0.5" />
              <div className="space-y-1 leading-relaxed">
                <p>
                  <strong>Hướng dẫn:</strong> Quý đơn vị vui lòng đánh giá kết quả{' '}
                  <strong>Đạt</strong> hoặc <strong>Không đạt</strong> cho từng chỉ tiêu dưới đây.
                  Đối với chỉ tiêu Đạt, vui lòng xác nhận Ngày cấp và Ngày hết hạn hiệu lực theo
                  phiếu kiểm nghiệm. Bạn có thể đính kèm tập tin phiếu kết quả (PDF hoặc ảnh scan) để
                  làm căn cứ đối soát minh bạch.
                </p>
              </div>
            </div>

            {/* Form nhập kết quả kiểm nghiệm */}
            <InspectionResultEntryForm
              criteria={portalData.criteria}
              onSubmit={handleSubmit}
              onUploadFile={handleUploadFile}
              isSubmitting={isSubmitting}
              submitButtonText="Xác nhận & Nộp kết quả kiểm nghiệm"
            />
          </div>
        )}
      </div>
    </div>
  );
};
