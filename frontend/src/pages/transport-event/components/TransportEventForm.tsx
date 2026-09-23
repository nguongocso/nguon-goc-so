import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter } from '@/components/ui/card';
import { TransportEventFields } from './TransportEventFields';
import { useTransportEventForm } from './useTransportEventForm';

/** Form ghi nhận sự kiện vận chuyển thực tế cho lô hàng. */
export function TransportEventForm() {
  const navigate = useNavigate();
  const controller = useTransportEventForm();

  return (
    <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
      <form onSubmit={controller.submitForm}>
        <CardContent className="space-y-6 pt-6">
          <TransportEventFields controller={controller} />
        </CardContent>
        <CardFooter className="flex justify-end gap-3">
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={() => navigate(-1)}
            className="border-emerald-200 text-emerald-700 hover:bg-emerald-50"
          >
            Hủy
          </Button>
          <Button
            type="submit"
            size="sm"
            disabled={controller.isSubmitting}
            variant="create"
          >
            {controller.isSubmitting ? 'Đang ghi...' : 'Ghi sự kiện vận chuyển'}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}
