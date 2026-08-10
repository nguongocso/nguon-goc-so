import React, { useEffect, useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  createInvitationSchema,
  type CreateInvitationFormValues,
} from "@/utils/validators";
import { createInvitation } from "@/api/invitationApi";
import { getRoles } from "@/api/memberApi";
import type { RoleOption } from "@/types/member";
import { getRoleLabel } from "@/config/roleAccess";
import { Loader2, Mail, MailPlus, ArrowLeft, Copy, Check, CheckCircle2 } from "lucide-react";
import { useNavigate } from "react-router-dom";

export const CreateInvitationForm: React.FC = () => {
  const navigate = useNavigate();
  const [roles, setRoles] = useState<RoleOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [createdInvitation, setCreatedInvitation] = useState<{
    email: string;
    joinUrl: string;
  } | null>(null);
  const [copied, setCopied] = useState(false);

  const {
    control,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<CreateInvitationFormValues>({
    resolver: zodResolver(createInvitationSchema),
    defaultValues: {
      email: "",
      roleId: undefined,
      expiryDays: 7,
    },
  });

  useEffect(() => {
    const fetchRoles = async () => {
      try {
        const data = await getRoles();
        const filtered = data.filter(
          (r) => r.code === "VT-03",
        );
        setRoles(filtered);
      } catch (error) {
        toast.error("Không thể tải danh sách vai trò");
      } finally {
        setLoading(false);
      }
    };
    fetchRoles();
  }, []);

  const onSubmit = async (data: CreateInvitationFormValues) => {
    setSubmitting(true);
    setCreatedInvitation(null);
    try {
      const res = await createInvitation(data);
      const url = res.joinUrl || `${window.location.origin}/join?token=${res.token}`;
      setCreatedInvitation({
        email: res.email,
        joinUrl: url,
      });
      toast.success("Thư mời đã được khởi tạo thành công!");
      reset();
    } catch (error: any) {
      const status = error.response?.status;
      const message = error.response?.data?.message;
      if (status === 409) {
        toast.error("Người dùng này đã là thành viên của tổ chức.");
      } else if (status === 404) {
        toast.error("Vai trò không tồn tại.");
      } else {
        toast.error(message || "Không thể gửi thư mời.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleCopyLink = (url: string) => {
    navigator.clipboard.writeText(url);
    setCopied(true);
    toast.success("Đã sao chép liên kết thư mời vào clipboard!");
    setTimeout(() => setCopied(false), 3000);
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin mr-2 text-emerald-500" />
        Đang tải...
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-emerald-50/50 via-white to-green-50/30 px-4 py-8 md:px-8">
      <div className="mx-auto max-w-lg space-y-6">
        {/* Nút quay lại */}
        <Button
          variant="outline"
          onClick={() => navigate(-1)}
          className="border-emerald-200 text-emerald-700 hover:bg-emerald-50"
        >
          <ArrowLeft className="h-4 w-4 mr-1" />
          Quay lại
        </Button>

        {createdInvitation && (
          <Card className="border-emerald-200 bg-emerald-50/60 shadow-sm transition-all animate-in fade-in slide-in-from-top-2">
            <CardHeader className="pb-3">
              <div className="flex items-center gap-2 text-emerald-800">
                <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0" />
                <CardTitle className="text-base font-semibold">
                  Thư mời đã được khởi tạo cho: {createdInvitation.email}
                </CardTitle>
              </div>
            </CardHeader>
            <CardContent className="space-y-3 text-sm text-slate-700">
              <p>
                Liên kết xác nhận tham gia tổ chức:
              </p>
              <div className="flex items-center gap-2">
                <Input
                  readOnly
                  value={createdInvitation.joinUrl}
                  className="bg-white border-emerald-200 text-xs font-mono text-slate-800"
                />
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={() => handleCopyLink(createdInvitation.joinUrl)}
                  className="border-emerald-300 text-emerald-700 hover:bg-emerald-100 shrink-0"
                >
                  {copied ? (
                    <>
                      <Check className="h-3.5 w-3.5 mr-1 text-emerald-600" />
                      Đã chép
                    </>
                  ) : (
                    <>
                      <Copy className="h-3.5 w-3.5 mr-1" />
                      Sao chép
                    </>
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
          <CardHeader className="border-b border-emerald-100">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-100">
                <MailPlus className="h-5 w-5 text-emerald-700" />
              </div>
              <div>
                <CardTitle className="text-xl font-bold text-emerald-800">
                  Mời thành viên
                </CardTitle>
                <CardDescription>
                  Gửi thư mời qua email để thành viên mới đăng ký và tham gia tổ
                  chức.
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <form onSubmit={handleSubmit(onSubmit)}>
            <CardContent className="space-y-5 pt-6">
              {/* Email */}
              <div className="space-y-1.5">
                <Label
                  htmlFor="email"
                  className="text-sm font-medium text-emerald-800"
                >
                  Email <span className="text-red-500">*</span>
                </Label>
                <Controller
                  name="email"
                  control={control}
                  render={({ field }) => (
                    <Input
                      id="email"
                      type="email"
                      placeholder="member@example.com"
                      className="border-emerald-200 focus-visible:ring-emerald-100"
                      {...field}
                      disabled={submitting}
                    />
                  )}
                />
                {errors.email && (
                  <p className="text-sm text-red-500">{errors.email.message}</p>
                )}
              </div>

              {/* Vai trò */}
              <div className="space-y-1.5">
                <Label
                  htmlFor="roleId"
                  className="text-sm font-medium text-emerald-800"
                >
                  Vai trò <span className="text-red-500">*</span>
                </Label>
                <Controller
                  name="roleId"
                  control={control}
                  render={({ field }) => {
                    const selectedRole = roles.find(
                      (r) => r.roleId === field.value,
                    );
                    return (
                      <Select
                        value={field.value?.toString() || ""}
                        onValueChange={(val) => {
                          if (val !== null && val !== undefined) {
                            field.onChange(parseInt(val));
                          }
                        }}
                        disabled={submitting}
                      >
                        <SelectTrigger className="border-emerald-200 focus:ring-emerald-100">
                          <SelectValue placeholder="Chọn vai trò">
                            {selectedRole
                              ? getRoleLabel(selectedRole.code)
                              : undefined}
                          </SelectValue>
                        </SelectTrigger>
                        <SelectContent className="min-w-[220px] w-auto">
                          {roles.map((role) => (
                            <SelectItem
                              className="w-[350px]"
                              key={role.roleId}
                              value={role.roleId.toString()}
                            >
                              {getRoleLabel(role.code)} ({role.code})
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    );
                  }}
                />
                {errors.roleId && (
                  <p className="text-sm text-red-500">
                    {errors.roleId.message}
                  </p>
                )}
              </div>

              {/* Thời hạn */}
              <div className="space-y-1.5">
                <Label
                  htmlFor="expiryDays"
                  className="text-sm font-medium text-emerald-800"
                >
                  Thời hạn (ngày)
                </Label>
                <Controller
                  name="expiryDays"
                  control={control}
                  render={({ field }) => (
                    <Input
                      id="expiryDays"
                      type="number"
                      min="1"
                      max="30"
                      className="border-emerald-200 focus-visible:ring-emerald-100"
                      {...field}
                      onChange={(e) =>
                        field.onChange(parseInt(e.target.value) || 7)
                      }
                      disabled={submitting}
                    />
                  )}
                />
                {errors.expiryDays && (
                  <p className="text-sm text-red-500">
                    {errors.expiryDays.message}
                  </p>
                )}
              </div>
            </CardContent>
            <CardFooter className="flex justify-end gap-2 border-t border-emerald-100 pt-5">
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate(-1)}
                disabled={submitting}
              >
                Hủy
              </Button>
              <Button type="submit" variant="create" disabled={submitting}>
                {submitting ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    Đang gửi...
                  </>
                ) : (
                  <>
                    <Mail className="h-4 w-4 mr-2" />
                    Gửi thư mời
                  </>
                )}
              </Button>
            </CardFooter>
          </form>
        </Card>
      </div>
    </div>
  );
};
