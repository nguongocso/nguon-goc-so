import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { Smartphone } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { useIsMobileDevice } from '@/hooks/useIsMobileDevice';

interface MobileOnlyRouteProps {
  children: ReactNode;
}

/**
 * Cổng chỉ cho thiết bị di động (NCL-10-CN-012).
 *
 * - Thiết bị mobile thật (theo User-Agent) → render nội dung.
 * - Desktop → hiển thị trang báo, không redirect để tránh vòng lặp điều hướng.
 */
export function MobileOnlyRoute({ children }: MobileOnlyRouteProps) {
  const laMobile = useIsMobileDevice();
  const navigate = useNavigate();

  if (laMobile) {
    return <>{children}</>;
  }

  return (
    <div className="flex min-h-[60vh] items-center justify-center p-4">
      <Card className="w-full max-w-md text-center">
        <CardHeader className="items-center">
          <span className="flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100">
            <Smartphone className="h-6 w-6 text-emerald-700" aria-hidden />
          </span>
          <CardTitle>Chỉ khả dụng trên thiết bị di động</CardTitle>
          <CardDescription>
            Tính năng ghi nhật ký ngoại tuyến chỉ hoạt động khi bạn sử dụng điện
            thoại hoặc máy tính bảng. Vui lòng mở lại trang này trên thiết bị di
            động của bạn.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Button variant="outline" onClick={() => navigate(-1)}>
            Quay lại
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
