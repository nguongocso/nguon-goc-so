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
    <div className="min-h-screen bg-background text-foreground py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Header thương hiệu và bảo mật */}
        <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-border/60">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-primary text-primary-foreground flex items-center justify-center font-bold shadow-sm">
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
              className="bg-primary/10 text-primary border-primary/20 font-medium px-3 py-1 flex items-center gap-1.5"
            >
              <Lock className="h-3.5 w-3.5" />
              Phiên bảo mật 1 lần
            </Badge>
          </div>
        </header>

        {/* Trạng thái: Đang tải */}
        {pageStatus === 'LOADING' && (
          <div className="py-24 text-center space-y-4">
            <div className="inline-block h-10 w-10 animate-spin rounded-full border-4 border-primary border-r-transparent align-[-0.125em]" />
            <p className="text-sm font-medium text-muted-foreground">
              Đang xác thực liên kết bảo mật và tải thông tin yêu cầu kiểm nghiệm...
            </p>
          </div>
        )}

        {/* Trạng thái: Hết hạn (HTTP 410) */}
        {pageStatus === 'EXPIRED' && (
          <Card className="border-warning/30 bg-warning/10 shadow-card">
            <CardHeader className="text-center pb-2">
              <div className="mx-auto w-12 h-12 rounded-full bg-warning/20 flex items-center justify-center text-warning mb-2">
                <Clock className="h-6 w-6" />
              </div>
              <CardTitle className="text-xl font-bold text-foreground">
                Liên kết nhập kết quả đã hết hạn
              </CardTitle>
              <CardDescription className="text-muted-foreground max-w-md mx-auto text-sm pt-1">
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
          <Card className="border-primary/30 bg-primary/10 shadow-card">
            <CardHeader className="text-center pb-2">
              <div className="mx-auto w-12 h-12 rounded-full bg-primary/20 flex items-center justify-center text-primary mb-2">
                <ShieldCheck className="h-6 w-6" />
              </div>
              <CardTitle className="text-xl font-bold text-foreground">
                Kết quả kiểm nghiệm đã được ghi nhận
              </CardTitle>
              <CardDescription className="text-muted-foreground max-w-md mx-auto text-sm pt-1">
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
          <Card className="border-destructive/30 bg-destructive/10 shadow-card">
            <CardHeader className="text-center pb-2">
              <div className="mx-auto w-12 h-12 rounded-full bg-destructive/20 flex items-center justify-center text-destructive mb-2">
                <FileX2 className="h-6 w-6" />
              </div>
              <CardTitle className="text-xl font-bold text-foreground">
                Liên kết không hợp lệ
              </CardTitle>
              <CardDescription className="text-muted-foreground max-w-md mx-auto text-sm pt-1">
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
          <Card className="border-destructive/30 bg-destructive/10 shadow-card">
            <CardHeader className="text-center pb-2">
              <div className="mx-auto w-12 h-12 rounded-full bg-destructive/20 flex items-center justify-center text-destructive mb-2">
                <AlertCircle className="h-6 w-6" />
              </div>
              <CardTitle className="text-xl font-bold text-foreground">
                Không thể truy cập cổng nhập kết quả
              </CardTitle>
              <CardDescription className="text-muted-foreground max-w-md mx-auto text-sm pt-1">
                {errorMessage}
              </CardDescription>
            </CardHeader>
          </Card>
        )}

        {/* Trạng thái: Nộp thành công */}
        {pageStatus === 'SUBMITTED' && (
          <Card className="border-border bg-card shadow-card py-6">
            <CardContent className="text-center space-y-5">
              <div className="mx-auto w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center text-primary">
                <CheckCircle2 className="h-10 w-10" />
              </div>
              <div className="space-y-2 max-w-lg mx-auto">
                <h2 className="text-2xl font-bold tracking-tight text-foreground">
                  Nộp kết quả kiểm nghiệm thành công!
                </h2>
                <p className="text-sm text-muted-foreground">
                  Hệ thống Nguồn Gốc Số đã tiếp nhận toàn bộ kết quả kiểm nghiệm từ đơn vị của Quý
                  khách. Nguồn dữ liệu đã được xác nhận với định danh{' '}
                  <strong className="text-primary font-semibold">
                    "Đơn vị kiểm nghiệm khai"
                  </strong>
                  .
                </p>
              </div>

              <div className="p-4 bg-muted/30 rounded-xl max-w-md mx-auto text-xs text-muted-foreground text-left space-y-1.5 border border-border">
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
              <Card className="border-border bg-card shadow-card">
                <CardContent className="p-4 flex items-center gap-3">
                  <div className="h-10 w-10 rounded-lg bg-primary/10 text-primary flex items-center justify-center shrink-0">
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

              <Card className="border-border bg-card shadow-card">
                <CardContent className="p-4 flex items-center gap-3">
                  <div className="h-10 w-10 rounded-lg bg-info/10 text-info flex items-center justify-center shrink-0">
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

              <Card className="border-border bg-card shadow-card">
                <CardContent className="p-4 flex items-center gap-3">
                  <div className="h-10 w-10 rounded-lg bg-warning/10 text-warning flex items-center justify-center shrink-0">
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

              <Card className="border-border bg-card shadow-card">
                <CardContent className="p-4 flex items-center gap-3">
                  <div className="h-10 w-10 rounded-lg bg-destructive/10 text-destructive flex items-center justify-center shrink-0">
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
            <div className="p-4 bg-muted/30 border border-border rounded-xl flex items-start gap-3 text-xs text-muted-foreground">
              <HelpCircle className="h-4 w-4 shrink-0 text-primary mt-0.5" />
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
              sampleSentDate={portalData.sampleSentDate}
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
