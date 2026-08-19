import React, { useEffect, useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { getStandards } from '@/api/standardApi';
import { createCertification } from '@/api/certificationApi';
import { createCertificationSchema, type CreateCertificationFormValues } from '@/utils/validators';
import type { Standard } from '@/types/standard';

export const CreateCertificationForm: React.FC = () => {
  const navigate = useNavigate();
  const [standards, setStandards] = useState<Standard[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const {
    control,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<CreateCertificationFormValues>({
    resolver: zodResolver(createCertificationSchema),
    defaultValues: {
      standardId: '',
      code: '',
      issuedBy: '',
      issueDate: '',
      expiryDate: '',
    },
  });

  useEffect(() => {
    const fetchStandards = async () => {
      try {
        const data = await getStandards({ isActive: true, page: 0, size: 100 });
        setStandards(data.items || []);
      } catch (error) {
        toast.error('Không thể tải danh sách tiêu chuẩn');
      } finally {
        setLoading(false);
      }
    };
    fetchStandards();
  }, []);

  const onSubmit = async (data: CreateCertificationFormValues) => {
    setSubmitting(true);
    try {
      await createCertification({
        standardId: data.standardId,
        code: data.code,
        issuedBy: data.issuedBy || undefined,
        issueDate: data.issueDate,
        expiryDate: data.expiryDate,
      });
      toast.success('Tạo chứng nhận thành công!');
      reset();
      navigate('/certifications');
    } catch (error: any) {
      const msg = error.response?.data?.message || 'Tạo chứng nhận thất bại';
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="flex justify-center p-8">Đang tải danh sách tiêu chuẩn...</div>;
  }

  return (
    <Card className="max-w-2xl mx-auto">
      <CardHeader>
        <CardTitle>Tạo mới chứng nhận cho tổ chức</CardTitle>
        <CardDescription>
          Tạo mới chứng nhận dựa trên tiêu chuẩn chất lượng có sẵn.
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-4">
          {/* Tiêu chuẩn */}
          <div className="space-y-2">
            <Label htmlFor="standardId">Tiêu chuẩn *</Label>
            <Controller
              name="standardId"
              control={control}
              render={({ field }) => {
                const selectedStandard = standards.find((s) => s.id === field.value);
                return (
                  <Select
                    value={field.value}
                    onValueChange={field.onChange}
                    disabled={submitting}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="Chọn tiêu chuẩn">
                        {selectedStandard?.name}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent className="min-w-[300px] max-h-[200px]">
                      {standards.map((std) => (
                        <SelectItem key={std.id} value={std.id}>
                          {std.name} {std.issuingBody ? `(${std.issuingBody})` : ''}
                        </SelectItem>
                      ))}
                      {standards.length === 0 && (
                        <div className="px-2 py-1 text-sm text-muted-foreground">
                          Không có tiêu chuẩn nào
                        </div>
                      )}
                    </SelectContent>
                  </Select>
                );
              }}
            />
            {errors.standardId && (
              <p className="text-sm text-red-500">{errors.standardId.message}</p>
            )}
          </div>

          {/* Số hiệu chứng nhận */}
          <div className="space-y-2">
            <Label htmlFor="code">Số hiệu chứng nhận *</Label>
            <Controller
              name="code"
              control={control}
              render={({ field }) => (
                <Input
                  id="code"
                  placeholder="VD: VG-2025-001"
                  {...field}
                  disabled={submitting}
                />
              )}
            />
            {errors.code && (
              <p className="text-sm text-red-500">{errors.code.message}</p>
            )}
          </div>

          {/* Cơ quan cấp */}
          <div className="space-y-2">
            <Label htmlFor="issuedBy">Cơ quan cấp</Label>
            <Controller
              name="issuedBy"
              control={control}
              render={({ field }) => (
                <Input
                  id="issuedBy"
                  placeholder="VD: Bộ Nông nghiệp"
                  {...field}
                  disabled={submitting}
                />
              )}
            />
            {errors.issuedBy && (
              <p className="text-sm text-red-500">{errors.issuedBy.message}</p>
            )}
          </div>

          {/* Ngày cấp */}
          <div className="space-y-2">
            <Label htmlFor="issueDate">Ngày cấp *</Label>
            <Controller
              name="issueDate"
              control={control}
              render={({ field }) => (
                <Input
                  id="issueDate"
                  type="date"
                  {...field}
                  disabled={submitting}
                />
              )}
            />
            {errors.issueDate && (
              <p className="text-sm text-red-500">{errors.issueDate.message}</p>
            )}
          </div>

          {/* Ngày hết hạn */}
          <div className="space-y-2">
            <Label htmlFor="expiryDate">Ngày hết hạn *</Label>
            <Controller
              name="expiryDate"
              control={control}
              render={({ field }) => (
                <Input
                  id="expiryDate"
                  type="date"
                  {...field}
                  disabled={submitting}
                />
              )}
            />
            {errors.expiryDate && (
              <p className="text-sm text-red-500">{errors.expiryDate.message}</p>
            )}
          </div>
        </CardContent>
        <CardFooter className="flex justify-end gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate(-1)}
            disabled={submitting}
          >
            Hủy
          </Button>
          <Button type="submit" variant="create" disabled={submitting}>
            {submitting ? 'Đang tạo...' : 'Tạo chứng nhận'}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
};