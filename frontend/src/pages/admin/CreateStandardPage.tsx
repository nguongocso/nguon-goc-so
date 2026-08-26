import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { z } from 'zod';
import { Award, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { HelpButton } from '@/components/help/HelpButton';
import { createStandard } from '@/api/standardApi';

const formSchema = z.object({
  name: z.string().min(1, 'Tên tiêu chuẩn không được để trống').max(255),
  issuingBody: z.string().max(255).optional(),
  description: z.string().optional(),
});

type FormValues = z.infer<typeof formSchema>;

export const CreateStandardPage: React.FC = () => {
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: '',
      issuingBody: '',
      description: '',
    },
  });

  const onSubmit = async (data: FormValues) => {
    try {
      await createStandard({
        name: data.name,
        description: data.description || undefined,
        issuingBody: data.issuingBody || undefined,
      });
      toast.success('Thêm tiêu chuẩn chất lượng thành công');
      navigate('/admin/standards');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tạo tiêu chuẩn');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Award className="size-6 text-emerald-600" />
            Thêm mới tiêu chuẩn chất lượng
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Khai báo tiêu chuẩn chất lượng và cơ quan ban hành áp dụng trong hệ thống.
          </p>
        </div>
        <HelpButton screenKey="admin-standards" />
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <CardTitle className="text-lg font-semibold text-slate-900">
            Thông tin tiêu chuẩn chất lượng
          </CardTitle>
          <CardDescription>
            Điền tên tiêu chuẩn (VD: VietGAP, GlobalGAP, OCOP 4 sao), cơ quan ban hành và mô tả quy chuẩn.
          </CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-5 pt-6">
            <div className="space-y-1.5">
              <Label htmlFor="name" className="text-sm font-medium">
                Tên tiêu chuẩn <span className="text-red-500">*</span>
              </Label>
              <Input
                id="name"
                {...register('name')}
                placeholder="VD: TCVN 11892-1:2017 (VietGAP Trồng trọt)"
              />
              {errors.name && (
                <p className="text-sm text-red-500">{errors.name.message}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="issuingBody" className="text-sm font-medium">
                Cơ quan ban hành
              </Label>
              <Input
                id="issuingBody"
                {...register('issuingBody')}
                placeholder="VD: Bộ Nông nghiệp & PTNT"
              />
              {errors.issuingBody && (
                <p className="text-sm text-red-500">{errors.issuingBody.message}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="description" className="text-sm font-medium">
                Mô tả quy chuẩn
              </Label>
              <Textarea
                id="description"
                {...register('description')}
                placeholder="Mô tả tóm tắt phạm vi áp dụng, chỉ tiêu kiểm nghiệm chính..."
                rows={4}
              />
              {errors.description && (
                <p className="text-sm text-red-500">{errors.description.message}</p>
              )}
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate('/admin/standards')}
                disabled={isSubmitting}
              >
                Hủy
              </Button>
              <Button type="submit" variant="create" disabled={isSubmitting}>
                <Plus className="h-4 w-4 mr-1.5" />
                {isSubmitting ? 'Đang lưu...' : 'Thêm mới'}
              </Button>
            </div>
          </CardContent>
        </form>
      </Card>
    </div>
  );
};

export default CreateStandardPage;
