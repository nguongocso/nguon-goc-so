import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { z } from 'zod';
import { Award, Save, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Checkbox } from '@/components/ui/checkbox';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { HelpButton } from '@/components/help/HelpButton';
import { getStandards, updateStandard } from '@/api/standardApi';
import type { Standard } from '@/types/standard';

const formSchema = z.object({
  name: z.string().min(1, 'Tên tiêu chuẩn không được để trống').max(255),
  issuingBody: z.string().max(255).optional(),
  description: z.string().optional(),
  isActive: z.boolean().optional(),
});

type FormValues = z.infer<typeof formSchema>;

export const EditStandardPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [standard, setStandard] = useState<Standard | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: '',
      issuingBody: '',
      description: '',
      isActive: true,
    },
  });

  const isActiveValue = watch('isActive');

  useEffect(() => {
    const fetchStandard = async () => {
      if (!id) return;
      try {
        setLoading(true);
        const res = await getStandards({ page: 0, size: 500 });
        const found = res.items.find((s) => s.id === id);
        if (found) {
          setStandard(found);
          setValue('name', found.name);
          setValue('issuingBody', found.issuingBody || '');
          setValue('description', found.description || '');
          setValue('isActive', found.isActive);
        } else {
          toast.error('Không tìm thấy tiêu chuẩn chất lượng');
          navigate('/admin/standards');
        }
      } catch (error) {
        toast.error('Không thể tải thông tin tiêu chuẩn');
      } finally {
        setLoading(false);
      }
    };

    fetchStandard();
  }, [id, setValue, navigate]);

  const onSubmit = async (data: FormValues) => {
    if (!id) return;
    try {
      await updateStandard(id, {
        name: data.name,
        description: data.description || undefined,
        issuingBody: data.issuingBody || undefined,
        isActive: data.isActive ?? true,
      });
      toast.success('Cập nhật tiêu chuẩn chất lượng thành công');
      navigate('/admin/standards');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể cập nhật tiêu chuẩn');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="h-6 w-6 animate-spin mr-2 text-emerald-600" />
        Đang tải thông tin tiêu chuẩn...
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Award className="size-6 text-emerald-600" />
            Cập nhật tiêu chuẩn chất lượng
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Chỉnh sửa tiêu chuẩn: <span className="font-semibold text-slate-900">{standard?.name}</span>
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
            Cập nhật tên tiêu chuẩn, cơ quan ban hành và nội dung mô tả quy chuẩn.
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

            <div className="flex items-center space-x-2 pt-1">
              <Checkbox
                id="isActive"
                checked={isActiveValue}
                onCheckedChange={(checked) => setValue("isActive", Boolean(checked))}
              />
              <Label
                htmlFor="isActive"
                className="text-sm font-medium leading-none cursor-pointer"
              >
                Đang hoạt động
              </Label>
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
                <Save className="h-4 w-4 mr-1.5" />
                {isSubmitting ? 'Đang lưu...' : 'Cập nhật'}
              </Button>
            </div>
          </CardContent>
        </form>
      </Card>
    </div>
  );
};

export default EditStandardPage;
