import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { createOrganizationSchema, type CreateOrganizationFormValues } from '@/utils/validators';
import { createOrganization } from '@/api/organizationApi';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Building2, UserRound, ShieldCheck, Plus } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { CREATABLE_ORGANIZATION_TYPES } from '@/utils/constants';
import type { CreateOrganizationRequest } from '@/types/organization';

const getPasswordStrength = (password: string): { score: number; label: string; color: string } => {
  let score = 0;
  if (password.length >= 8) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/[0-9]/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;
  if (password.length >= 12) score++;

  if (score <= 2) return { score, label: 'Yếu', color: 'bg-red-500' };
  if (score === 3) return { score, label: 'Trung bình', color: 'bg-yellow-500' };
  return { score, label: 'Mạnh', color: 'bg-green-500' };
};

export function CreateOrganizationForm() {
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
    setValue,
    watch,
  } = useForm<CreateOrganizationFormValues>({
    resolver: zodResolver(createOrganizationSchema),
    defaultValues: {
      organizationType: 'COOPERATIVE',
    },
  });

  const organizationType = watch('organizationType');
  const password = watch('password');
  const [passwordStrength, setPasswordStrength] = useState(getPasswordStrength(''));

  useEffect(() => {
    setPasswordStrength(getPasswordStrength(password || ''));
  }, [password]);

  const onSubmit = async (values: CreateOrganizationFormValues) => {
    try {
      const submitData: CreateOrganizationRequest = {
        organizationName: values.organizationName,
        organizationCode: values.organizationCode,
        organizationType: values.organizationType,
        fullName: values.fullName,
        userName: values.userName,
        password: values.password,
        managerEmail: values.managerEmail,
      };

      const result = await createOrganization(submitData);
      toast.success(`Tổ chức "${result.data.organizationName}" đã được tạo thành công!`);
      navigate('/organizations');
    } catch (error: any) {
      const response = error.response?.data;
      if (response?.status === 400 && response?.errors) {
        Object.entries(response.errors).forEach(([key, message]) => {
          setError(key as keyof CreateOrganizationFormValues, {
            message: message as string,
          });
        });
      } else {
        toast.error(response?.message || 'Có lỗi xảy ra khi tạo tổ chức');
      }
    }
  };

  const InputWithIcon = ({ icon: Icon, ...props }: React.ComponentProps<typeof Input> & { icon: React.ElementType }) => (
    <div className="relative">
      <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none text-muted-foreground">
        <Icon className="h-4 w-4" />
      </div>
      <Input {...props} className={`pl-9 ${props.className || ''}`} />
    </div>
  );

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Card className="w-full rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4 pt-5 px-5">
          <CardTitle className="text-lg font-semibold text-slate-900">
            Thông tin thiết lập tổ chức
          </CardTitle>
          <CardDescription className="text-xs text-muted-foreground mt-0.5">
            Điền đầy đủ thông tin tổ chức, thông tin quản trị viên đầu tiên và thiết lập mật khẩu bảo mật.
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-5 pt-6 pb-6 px-5">
          {/* Thông tin tổ chức */}
          <div className="rounded-xl border bg-muted/30 p-4 space-y-4">
            <div className="flex items-center gap-2 text-sm font-semibold text-foreground/80">
              <Building2 className="h-4 w-4" />
              Thông tin tổ chức
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="organizationName">Tên tổ chức *</Label>
                <InputWithIcon
                  id="organizationName"
                  icon={Building2}
                  {...register('organizationName')}
                  placeholder="VD: Công ty ABC"
                />
                {errors.organizationName && (
                  <p className="text-sm text-red-500">{errors.organizationName.message}</p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="organizationCode">Mã tổ chức *</Label>
                <InputWithIcon
                  id="organizationCode"
                  icon={Building2}
                  {...register('organizationCode', {
                    onChange: (e) => {
                      e.target.value = e.target.value.replace(/\s+/g, '').toUpperCase();
                    },
                  })}
                  placeholder="VD: TC01"
                />
                {errors.organizationCode && (
                  <p className="text-sm text-red-500">{errors.organizationCode.message}</p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="organizationType">Loại tổ chức *</Label>
                <Select
                  items={Object.entries(CREATABLE_ORGANIZATION_TYPES).map(([key, label]) => ({
                    value: key,
                    label,
                  }))}
                  value={organizationType}
                  onValueChange={(value) => setValue('organizationType', value as any)}
                >
                  <SelectTrigger id="organizationType" className="w-full">
                    <SelectValue placeholder="Chọn loại tổ chức" />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(CREATABLE_ORGANIZATION_TYPES).map(([key, label]) => (
                      <SelectItem key={key} value={key}>
                        {label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.organizationType && (
                  <p className="text-sm text-red-500">{errors.organizationType.message}</p>
                )}
              </div>
            </div>
          </div>

          {/* Quản trị viên */}
          <div className="rounded-xl border bg-muted/30 p-4 space-y-4">
            <div className="flex items-center gap-2 text-sm font-semibold text-foreground/80">
              <UserRound className="h-4 w-4" />
              Quản trị viên đầu tiên
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="fullName">Họ tên *</Label>
                <InputWithIcon
                  id="fullName"
                  icon={UserRound}
                  {...register('fullName')}
                  placeholder="Trần Văn B"
                />
                {errors.fullName && (
                  <p className="text-sm text-red-500">{errors.fullName.message}</p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="userName">Tên đăng nhập *</Label>
                <InputWithIcon
                  id="userName"
                  icon={UserRound}
                  {...register('userName')}
                  placeholder="admin01"
                />
                {errors.userName && (
                  <p className="text-sm text-red-500">{errors.userName.message}</p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="managerEmail">Email quản lý *</Label>
                <InputWithIcon
                  id="managerEmail"
                  type="email"
                  icon={UserRound}
                  {...register('managerEmail')}
                  placeholder="admin@abc.com"
                />
                {errors.managerEmail && (
                  <p className="text-sm text-red-500">{errors.managerEmail.message}</p>
                )}
              </div>
            </div>
          </div>

          {/* Bảo mật */}
          <div className="rounded-xl border bg-muted/30 p-4 space-y-4">
            <div className="flex items-center gap-2 text-sm font-semibold text-foreground/80">
              <ShieldCheck className="h-4 w-4" />
              Bảo mật
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="password">Mật khẩu *</Label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none text-muted-foreground">
                    <ShieldCheck className="h-4 w-4" />
                  </div>
                  <Input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    className="pl-9 pr-10"
                    {...register('password')}
                    placeholder="Nhập mật khẩu"
                  />
                  <button
                    type="button"
                    tabIndex={-1}
                    onClick={() => setShowPassword((prev) => !prev)}
                    aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                    className="absolute inset-y-0 right-0 flex items-center pr-3 text-muted-foreground hover:text-foreground"
                  >
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                {errors.password && (
                  <p className="text-sm text-red-500">{errors.password.message}</p>
                )}
                {password && (
                  <div className="mt-2">
                    <div className="flex gap-1 h-1.5">
                      <div className={`flex-1 rounded-full ${passwordStrength.score >= 1 ? passwordStrength.color : 'bg-gray-200'}`} />
                      <div className={`flex-1 rounded-full ${passwordStrength.score >= 2 ? passwordStrength.color : 'bg-gray-200'}`} />
                      <div className={`flex-1 rounded-full ${passwordStrength.score >= 3 ? passwordStrength.color : 'bg-gray-200'}`} />
                      <div className={`flex-1 rounded-full ${passwordStrength.score >= 4 ? passwordStrength.color : 'bg-gray-200'}`} />
                      <div className={`flex-1 rounded-full ${passwordStrength.score >= 5 ? passwordStrength.color : 'bg-gray-200'}`} />
                    </div>
                    <p className="text-xs text-muted-foreground mt-1">
                      Độ mạnh: <span className="font-medium">{passwordStrength.label}</span>
                    </p>
                  </div>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="confirmPassword">Xác nhận mật khẩu *</Label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none text-muted-foreground">
                    <ShieldCheck className="h-4 w-4" />
                  </div>
                  <Input
                    id="confirmPassword"
                    type={showConfirmPassword ? 'text' : 'password'}
                    className="pl-9 pr-10"
                    {...register('confirmPassword')}
                    placeholder="Nhập lại mật khẩu"
                  />
                  <button
                    type="button"
                    tabIndex={-1}
                    onClick={() => setShowConfirmPassword((prev) => !prev)}
                    aria-label={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                    className="absolute inset-y-0 right-0 flex items-center pr-3 text-muted-foreground hover:text-foreground"
                  >
                    {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                {errors.confirmPassword && (
                  <p className="text-sm text-red-500">{errors.confirmPassword.message}</p>
                )}
              </div>
            </div>
          </div>
        </CardContent>

        {/* Footer gọn gàng */}
        <div className="border-t px-5 py-4 flex justify-end gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate('/organizations')}
          >
            Hủy
          </Button>
          <Button type="submit" variant="create" disabled={isSubmitting}>
            <Plus className="h-4 w-4 mr-1" />
            {isSubmitting ? 'Đang tạo...' : 'Tạo tổ chức'}
          </Button>
        </div>
      </Card>
    </form>
  );
}