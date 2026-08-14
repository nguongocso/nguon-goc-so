import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { Eye, EyeOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { addMember, getRoles } from '@/api/memberApi';
import type { AddMemberRequest, RoleOption } from '@/types/member';
import { getRoleLabel } from '@/config/roleAccess';

const createMemberSchema = z
  .object({
    username: z.string().min(1, 'Tên đăng nhập không được để trống'),
    password: z
      .string()
      .min(8, 'Mật khẩu phải có ít nhất 8 ký tự')
      .max(50, 'Mật khẩu tối đa 50 ký tự')
      .regex(
        /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]+$/,
        'Mật khẩu phải chứa ít nhất một chữ hoa, một chữ thường, một số và một ký tự đặc biệt (@$!%*?&)'
      ),
    confirmPassword: z.string().min(1, 'Vui lòng xác nhận mật khẩu'),
    fullName: z.string().min(1, 'Họ tên không được để trống'),
    phone: z.string().optional().nullable(),
    email: z.string().email('Email không hợp lệ').or(z.literal('')).optional().nullable(),
    roleId: z.number({ required_error: 'Vai trò là bắt buộc' }),
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
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    formState: { errors },
  } = useForm<CreateMemberFormValues>({
    resolver: zodResolver(createMemberSchema),
    defaultValues: {
      username: '',
      password: '',
      confirmPassword: '',
      fullName: '',
      phone: '',
      email: '',
      roleId: 0, // placeholder, sẽ được cập nhật sau
    },
  });

  // Load roles và tự động gán VT-03
  useEffect(() => {
    const loadRoles = async () => {
      try {
        setIsLoading(true);
        const allRoles = await getRoles();
        // Chỉ lấy VT-03 (Người ghi sự kiện)
        const vt03Roles = allRoles.filter((role) => role.code === 'VT-03');

        if (vt03Roles.length === 0) {
          toast.error('Không tìm thấy vai trò VT-03. Vui lòng kiểm tra dữ liệu.');
          return;
        }

        setRoles(vt03Roles);
        // Tự động gán roleId của VT-03
        const vt03Role = vt03Roles[0];
        setValue('roleId', vt03Role.roleId);
      } catch {
        toast.error('Không thể tải danh sách vai trò');
      } finally {
        setIsLoading(false);
      }
    };
    loadRoles();
  }, [setValue]);

  const onSubmit = async (values: CreateMemberFormValues) => {
    try {
      setIsSubmitting(true);
      const submitData: AddMemberRequest = {
        username: values.username,
        password: values.password,
        fullName: values.fullName,
        phone: values.phone?.trim() ? values.phone.trim() : null,
        email: values.email?.trim() ? values.email.trim() : null,
        roleId: values.roleId,
      };

      await addMember(submitData);
      toast.success('Thêm thành viên thành công');
      navigate('/members');
    } catch (error: any) {
      const msg = error.response?.data?.message || 'Có lỗi xảy ra khi thêm thành viên';
      toast.error(msg);

      const fieldErrors = error.response?.data?.errors as Record<string, string> | undefined;
      if (fieldErrors) {
        Object.entries(fieldErrors).forEach(([field, fieldMsg]) => {
          if (field in values) {
            setError(field as keyof CreateMemberFormValues, { type: 'server', message: fieldMsg });
          }
        });
      }
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
            <div className="relative">
              <Input
                id="password"
                type={showPassword ? 'text' : 'password'}
                className="pr-8 [&::-ms-reveal]:hidden [&::-ms-clear]:hidden"
                {...register('password')}
                placeholder="Mật khẩu (tối thiểu 8 ký tự)"
              />
              <button
                type="button"
                tabIndex={-1}
                onClick={() => setShowPassword((prev) => !prev)}
                aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                className="absolute inset-y-0 right-0 flex items-center px-2 text-muted-foreground hover:text-foreground"
              >
                {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
              </button>
            </div>
            <p className="text-xs text-muted-foreground">
              Tối thiểu 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt (@$!%*?&)
            </p>
            {errors.password && <p className="text-sm text-red-500">{errors.password.message}</p>}
          </div>

          {/* Xác nhận mật khẩu */}
          <div className="space-y-2">
            <Label htmlFor="confirmPassword">Xác nhận mật khẩu *</Label>
            <div className="relative">
              <Input
                id="confirmPassword"
                type={showConfirmPassword ? 'text' : 'password'}
                className="pr-8 [&::-ms-reveal]:hidden [&::-ms-clear]:hidden"
                {...register('confirmPassword')}
                placeholder="Nhập lại mật khẩu"
              />
              <button
                type="button"
                tabIndex={-1}
                onClick={() => setShowConfirmPassword((prev) => !prev)}
                aria-label={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                className="absolute inset-y-0 right-0 flex items-center px-2 text-muted-foreground hover:text-foreground"
              >
                {showConfirmPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
              </button>
            </div>
            {errors.confirmPassword && <p className="text-sm text-red-500">{errors.confirmPassword.message}</p>}
          </div>

          {/* Họ và tên */}
          <div className="space-y-2">
            <Label htmlFor="fullName">Họ và tên *</Label>
            <Input id="fullName" {...register('fullName')} placeholder="VD: Nguyễn Văn A" />
            {errors.fullName && <p className="text-sm text-red-500">{errors.fullName.message}</p>}
          </div>

          {/* Số điện thoại & Email */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="phone">Số điện thoại</Label>
              <Input id="phone" {...register('phone')} placeholder="Số điện thoại" />
              {errors.phone && <p className="text-sm text-red-500">{errors.phone.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" {...register('email')} placeholder="email@example.com" />
              {errors.email && <p className="text-sm text-red-500">{errors.email.message}</p>}
            </div>
          </div>

          {/* Hiển thị vai trò dưới dạng text (đã mặc định VT-03) */}
          <div className="space-y-2">
            <Label>Vai trò *</Label>
            <div className="text-sm font-medium text-muted-foreground">
              {roles.length > 0 ? getRoleLabel('VT-03') : 'Đang tải...'}
            </div>
            <input type="hidden" {...register('roleId', { valueAsNumber: true })} />
            {errors.roleId && <p className="text-sm text-red-500">{errors.roleId.message}</p>}
          </div>
        </CardContent>

        <CardFooter className="flex justify-end gap-2">
          <Button type="button" variant="outline" onClick={() => navigate('/members')}>
            Hủy
          </Button>
          <Button type="submit" variant="create" disabled={isSubmitting}>
            {isSubmitting ? 'Đang thêm...' : 'Thêm thành viên'}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}