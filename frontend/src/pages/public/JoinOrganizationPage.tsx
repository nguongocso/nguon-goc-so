import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  acceptInvitationSchema,
  type AcceptInvitationFormValues,
} from '@/utils/validators';
import {
  getInvitationDetails,
  acceptInvitation,
} from '@/api/invitationApi';
import type { InvitationPublicResponse } from '@/types/invitation';
import { Loader2, CheckCircle, AlertCircle } from 'lucide-react';

// Helper chỉ dùng roleName
const getRoleDisplay = (roleName?: string): string => {
  const map: Record<string, string> = {
    'EVENT_RECORDER': 'Người ghi sự kiện',
    'ORG_MANAGER': 'Quản lý hợp tác xã',
    'PROCUREMENT': 'Doanh nghiệp thu mua',
    'GOVERNMENT': 'Cán bộ quản lý ngành',
    'VT-01': 'Quản trị viên nền tảng',
    'VT-02': 'Quản lý hợp tác xã',
    'VT-03': 'Người ghi sự kiện',
    'VT-04': 'Doanh nghiệp thu mua',
    'VT-05': 'Cán bộ quản lý ngành',
  };
  return map[roleName || ''] || roleName || 'Vai trò không xác định';
};

const JoinOrganizationPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [invitation, setInvitation] = useState<InvitationPublicResponse | null>(
    null
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  const {
    control,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<AcceptInvitationFormValues>({
    resolver: zodResolver(acceptInvitationSchema),
    defaultValues: {
      userName: '',
      password: '',
      confirmPassword: '',
      fullName: '',
      phone: '',
    },
  });

  const watchPassword = watch('password');

  useEffect(() => {
    const fetchInvitation = async () => {
      if (!token) {
        setError('Không tìm thấy mã thư mời.');
        setLoading(false);
        return;
      }
      try {
        const data = await getInvitationDetails(token);
        setInvitation(data);
        if (data.isExistingUser) {
          setValue('userName', 'existing_member');
          setValue('fullName', 'Thành viên hiện có');
        }
      } catch (err: any) {
        setError(
          err.response?.data?.message ||
          'Thư mời không hợp lệ hoặc đã hết hạn.'
        );
      } finally {
        setLoading(false);
      }
    };
    fetchInvitation();
  }, [token, setValue]);

  useEffect(() => {
    if (invitation?.isExistingUser) {
      setValue('confirmPassword', watchPassword || '');
    }
  }, [watchPassword, invitation?.isExistingUser, setValue]);

  const onSubmit = async (data: AcceptInvitationFormValues) => {
    if (!token) return;
    setSubmitting(true);
    try {
      await acceptInvitation(token, {
        userName: invitation?.isExistingUser ? 'existing_member' : data.userName,
        password: data.password,
        fullName: invitation?.isExistingUser ? 'Thành viên hiện có' : data.fullName,
        phone: data.phone,
      });
      setSuccess(true);
      toast.success(
        invitation?.isExistingUser
          ? 'Xác nhận thành công! Bạn đã tham gia tổ chức.'
          : 'Đăng ký thành công! Bạn đã tham gia tổ chức.'
      );
      // Chuyển hướng đến trang đăng nhập sau vài giây
      setTimeout(() => {
        navigate('/login');
      }, 3000);
    } catch (err: any) {
      const status = err.response?.status;
      const message = err.response?.data?.message;
      if (status === 409) {
        toast.error('Tên đăng nhập đã tồn tại. Vui lòng chọn tên khác.');
      } else if (status === 400 && message?.includes('quá hạn')) {
        toast.error('Thư mời đã hết hạn. Vui lòng yêu cầu mời lại.');
        setError('Thư mời đã hết hạn.');
      } else {
        toast.error(message || 'Tham gia tổ chức thất bại.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex justify-center items-center min-h-screen p-4">
        <Card className="max-w-md w-full">
          <CardHeader>
            <CardTitle className="text-red-600 flex items-center gap-2">
              <AlertCircle className="h-6 w-6" />
              Không thể truy cập
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground">{error}</p>
            <p className="mt-4 text-sm">
              Vui lòng liên hệ với người quản lý để được hỗ trợ.
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (success) {
    return (
      <div className="flex justify-center items-center min-h-screen p-4">
        <Card className="max-w-md w-full">
          <CardHeader>
            <CardTitle className="text-green-600 flex items-center gap-2">
              <CheckCircle className="h-6 w-6" />
              {invitation?.isExistingUser ? 'Xác nhận tham gia thành công!' : 'Đăng ký thành công!'}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground">
              Bạn đã tham gia tổ chức{' '}
              <strong>{invitation?.organizationName}</strong> với vai trò{' '}
              <strong>{getRoleDisplay(invitation?.roleName)}</strong>.
            </p>
            <p className="mt-2 text-sm">
              Bạn sẽ được chuyển hướng đến trang đăng nhập...
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex justify-center items-center min-h-screen p-4 bg-gray-50">
      <Card className="max-w-lg w-full">
        <CardHeader>
          <CardTitle>Chào mừng bạn đến với {invitation?.organizationName}</CardTitle>
          <CardDescription>
            {invitation?.isExistingUser ? (
              <>
                Tài khoản email <strong>{invitation?.email}</strong> đã tồn tại trong hệ thống. Vui lòng nhập mật khẩu tài khoản của bạn để xác nhận gia nhập tổ chức với vai trò <strong>{invitation?.roleName}</strong>.
              </>
            ) : (
              <>
                Bạn đã được mời tham gia tổ chức với vai trò{' '}
                <strong>{invitation?.roleName}</strong>. Vui lòng hoàn tất đăng ký để bắt đầu.
              </>
            )}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-2">
              <Label>Email</Label>
              <Input value={invitation?.email} disabled className="bg-muted" />
            </div>

            {invitation?.isExistingUser ? (
              <div className="space-y-2">
                <Label htmlFor="password">Mật khẩu tài khoản hiện tại *</Label>
                <Controller
                  name="password"
                  control={control}
                  render={({ field }) => (
                    <Input
                      id="password"
                      type="password"
                      placeholder="Nhập mật khẩu tài khoản của bạn"
                      {...field}
                      disabled={submitting}
                    />
                  )}
                />
                {errors.password && (
                  <p className="text-sm text-red-500">{errors.password.message}</p>
                )}
              </div>
            ) : (
              <>
                <div className="space-y-2">
                  <Label htmlFor="userName">Tên đăng nhập *</Label>
                  <Controller
                    name="userName"
                    control={control}
                    render={({ field }) => (
                      <Input
                        id="userName"
                        placeholder="vd: nguyenvanA"
                        {...field}
                        disabled={submitting}
                      />
                    )}
                  />
                  {errors.userName && (
                    <p className="text-sm text-red-500">{errors.userName.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="fullName">Họ và tên *</Label>
                  <Controller
                    name="fullName"
                    control={control}
                    render={({ field }) => (
                      <Input
                        id="fullName"
                        placeholder="Nguyễn Văn A"
                        {...field}
                        disabled={submitting}
                      />
                    )}
                  />
                  {errors.fullName && (
                    <p className="text-sm text-red-500">{errors.fullName.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="password">Mật khẩu *</Label>
                  <Controller
                    name="password"
                    control={control}
                    render={({ field }) => (
                      <Input
                        id="password"
                        type="password"
                        placeholder="Mật khẩu mạnh"
                        {...field}
                        disabled={submitting}
                      />
                    )}
                  />
                  {errors.password && (
                    <p className="text-sm text-red-500">{errors.password.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="confirmPassword">Xác nhận mật khẩu *</Label>
                  <Controller
                    name="confirmPassword"
                    control={control}
                    render={({ field }) => (
                      <Input
                        id="confirmPassword"
                        type="password"
                        placeholder="Nhập lại mật khẩu"
                        {...field}
                        disabled={submitting}
                      />
                    )}
                  />
                  {errors.confirmPassword && (
                    <p className="text-sm text-red-500">
                      {errors.confirmPassword.message}
                    </p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="phone">Số điện thoại</Label>
                  <Controller
                    name="phone"
                    control={control}
                    render={({ field }) => (
                      <Input
                        id="phone"
                        type="tel"
                        placeholder="0987654321"
                        {...field}
                        disabled={submitting}
                      />
                    )}
                  />
                  {errors.phone && (
                    <p className="text-sm text-red-500">{errors.phone.message}</p>
                  )}
                </div>
              </>
            )}

            <Button type="submit" className="w-full" disabled={submitting}>
              {submitting ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Đang xử lý...
                </>
              ) : invitation?.isExistingUser ? (
                'Xác nhận gia nhập'
              ) : (
                'Đăng ký tham gia'
              )}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default JoinOrganizationPage;