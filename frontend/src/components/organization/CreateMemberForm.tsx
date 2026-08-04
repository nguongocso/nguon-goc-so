import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { addMember, getRoles } from '@/api/memberApi';
import type { RoleOption } from '@/types/member';

const createMemberSchema = z
  .object({
    username: z.string().min(1, 'Tên đăng nhập không được để trống'),
    password: z.string().min(6, 'Mật khẩu phải có ít nhất 6 ký tự'),
    confirmPassword: z.string().min(1, 'Vui lòng xác nhận mật khẩu'),
    fullName: z.string().min(1, 'Họ tên không được để trống'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  });

type CreateMemberFormValues = z.infer<typeof createMemberSchema>;

export function CreateMemberForm() {
  const navigate = useNavigate();
  const [roles, setRoles] = useState<RoleOption[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CreateMemberFormValues>({
    resolver: zodResolver(createMemberSchema),
    defaultValues: {
      username: '',
      password: '',
      confirmPassword: '',
      fullName: '',
    },
  });

  useEffect(() => {
    const loadRoles = async () => {
      try {
        setIsLoading(true);
        const data = await getRoles();
        // Chỉ hiển thị VT-03 (Người ghi sự kiện) khi tạo mới
        setRoles(data.filter((role) => role.code === 'VT-03'));
      } catch {
        toast.error('Không thể tải danh sách vai trò');
      } finally {
        setIsLoading(false);
      }
    };
    loadRoles();
  }, []);

  const onSubmit = async (values: CreateMemberFormValues) => {
    const defaultRole = roles.find((role) => role.code === 'VT-03');
    if (!defaultRole) {
      toast.error('Không thể xác định vai trò mặc định');
      return;
    }
    try {
      setIsSubmitting(true);
      const { confirmPassword, ...rest } = values;
      await addMember({ ...rest, roleId: defaultRole.roleId });
      toast.success('Thêm thành viên thành công');
      navigate('/members');
    } catch (error: any) {
      const msg = error.response?.data?.message || 'Có lỗi xảy ra khi thêm thành viên';
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) return <div className="p-8 text-center">Đang tải...</div>;

  return (
    <Card className="max-w-2xl mx-auto">
      <CardHeader>
        <CardTitle>Thêm thành viên mới</CardTitle>
        <CardDescription>Nhập thông tin thành viên để thêm vào tổ chức.</CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-4">
          {/* Tên đăng nhập */}
          <div className="space-y-2">
            <Label htmlFor="username">Tên đăng nhập *</Label>
            <Input id="username" {...register('username')} placeholder="VD: nguyenvana" />
            {errors.username && <p className="text-sm text-red-500">{errors.username.message}</p>}
          </div>

          {/* Mật khẩu */}
          <div className="space-y-2">
            <Label htmlFor="password">Mật khẩu *</Label>
            <Input id="password" type="password" {...register('password')} placeholder="Mật khẩu (tối thiểu 6 ký tự)" />
            {errors.password && <p className="text-sm text-red-500">{errors.password.message}</p>}
          </div>

          {/* Xác nhận mật khẩu */}
          <div className="space-y-2">
            <Label htmlFor="confirmPassword">Xác nhận mật khẩu *</Label>
            <Input id="confirmPassword" type="password" {...register('confirmPassword')} placeholder="Nhập lại mật khẩu" />
            {errors.confirmPassword && <p className="text-sm text-red-500">{errors.confirmPassword.message}</p>}
          </div>

          {/* Họ và tên */}
          <div className="space-y-2">
            <Label htmlFor="fullName">Họ và tên *</Label>
            <Input id="fullName" {...register('fullName')} placeholder="VD: Nguyễn Văn A" />
            {errors.fullName && <p className="text-sm text-red-500">{errors.fullName.message}</p>}
          </div>

        </CardContent>

        <CardFooter className="flex justify-end gap-2">
          <Button type="button" variant="outline" onClick={() => navigate('/members')}>
            Hủy
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Đang thêm...' : 'Thêm thành viên'}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}