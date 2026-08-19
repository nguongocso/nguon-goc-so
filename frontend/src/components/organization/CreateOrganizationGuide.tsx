import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Info,
  ShieldCheck,
  UserRound,
  Building2,
  PenLine,
  AlertTriangle,
  ChevronDown,
  ChevronUp,
} from "lucide-react";

const guideSections = [
  {
    icon: Building2,
    title: "Mã tổ chức",
    content: "Chỉ dùng A-Z, 0-9, gạch ngang & gạch dưới.",
  },
  {
    icon: ShieldCheck,
    title: "Mật khẩu",
    content: "Ít nhất 8 ký tự, có chữ hoa, thường, số, ký tự đặc biệt.",
  },
  {
    icon: UserRound,
    title: "Quản trị viên",
    content: "Tài khoản quản trị đầu tiên được tạo cùng tổ chức.",
  },
  {
    icon: PenLine,
    title: "Sau khi tạo",
    content: "Có thể cập nhật địa chỉ, SĐT, email trong Hồ sơ tổ chức.",
  },
  {
    icon: AlertTriangle,
    title: "Lưu ý",
    content: "Mã tổ chức và tên đăng nhập không thể thay đổi.",
  },
];

export function CreateOrganizationGuide() {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      {/* Desktop (xl trở lên): sticky card */}
      <Card className="hidden xl:block shadow-sm border-muted/60 bg-muted/30 sticky top-1/2 -translate-y-1/2">
        <CardHeader className="pb-2 pt-4 px-4">
          <div className="flex items-center gap-2">
            <Info className="h-4 w-4 text-primary" />
            <CardTitle className="text-sm font-semibold">Hướng dẫn</CardTitle>
          </div>
        </CardHeader>
        <CardContent className="px-4 pb-4 space-y-0 divide-y divide-border/50">
          {guideSections.map((section) => (
            <div key={section.title} className="flex gap-2.5 py-2.5 first:pt-0 last:pb-0">
              <section.icon className="h-4 w-4 text-muted-foreground mt-0.5 shrink-0" />
              <div>
                <h4 className="text-xs font-medium">{section.title}</h4>
                <p className="text-[11px] text-muted-foreground mt-0.5 leading-relaxed">
                  {section.content}
                </p>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Mobile/Tablet/Laptop (<xl): accordion */}
      <div className="xl:hidden border rounded-xl bg-muted/30">
        <button
          type="button"
          onClick={() => setIsOpen(!isOpen)}
          className="w-full flex items-center justify-between p-4 text-left font-medium text-sm"
        >
          <span className="flex items-center gap-2">
            <Info className="h-4 w-4 text-primary" />
            Hướng dẫn tạo tổ chức
          </span>
          {isOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
        </button>
        {isOpen && (
          <div className="px-4 pb-4 space-y-3 border-t pt-3">
            {guideSections.map((section) => (
              <div key={section.title} className="flex gap-3">
                <section.icon className="h-4 w-4 text-muted-foreground mt-0.5 shrink-0" />
                <div>
                  <h4 className="text-sm font-medium">{section.title}</h4>
                  <p className="text-xs text-muted-foreground">{section.content}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}