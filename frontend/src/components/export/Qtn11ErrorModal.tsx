import { AlertTriangle, FileX, CalendarX, X } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';

export interface Qtn11ErrorDetail {
  id?: string;
  name?: string;
  lotCode?: string;
  missingEvents?: string[];
  missingDocs?: boolean;
  missingDocDetails?: string[];
}

interface Qtn11ErrorModalProps {
  open: boolean;
  onClose: () => void;
  errors: Qtn11ErrorDetail[];
}

export const Qtn11ErrorModal = ({
  open,
  onClose,
  errors,
}: Qtn11ErrorModalProps) => {
  return (
    <Dialog open={open} onOpenChange={(val) => !val && onClose()}>
      <DialogContent className="max-w-2xl max-h-[85vh] flex flex-col p-6">
        <DialogHeader className="space-y-2">
          <div className="flex items-center gap-2 text-destructive">
            <AlertTriangle className="h-6 w-6 text-red-600" />
            <DialogTitle className="text-xl font-bold text-red-600">
              Không đủ điều kiện xuất dữ liệu (Quy tắc QTN-11)
            </DialogTitle>
          </div>
          <DialogDescription className="text-sm text-muted-foreground">
            Danh sách các lô hàng/shipment trong phạm vi chọn chưa thể kết xuất do bị thiếu sự kiện chuỗi cung ứng hoặc chứng từ đính kèm theo quy định QTN-11:
          </DialogDescription>
        </DialogHeader>

        {/* Scrollable list */}
        <div className="flex-1 overflow-y-auto pr-1 my-3 space-y-3 max-h-[50vh]">
          {errors.map((item, index) => (
            <Card key={item.id || index} className="border-red-200 bg-red-50/40 dark:bg-red-950/10">
              <CardContent className="p-4 space-y-3">
                <div className="flex justify-between items-start">
                  <div>
                    <h4 className="font-semibold text-base text-foreground">
                      {item.name || 'Lô hàng không tên'}
                    </h4>
                    {item.lotCode && (
                      <p className="text-xs text-muted-foreground mt-0.5">
                        Mã lô: <span className="font-mono font-medium">{item.lotCode}</span>
                      </p>
                    )}
                  </div>
                  <Badge variant="destructive" className="text-xs">
                    Không đạt QTN-11
                  </Badge>
                </div>

                {/* Missing Events */}
                {item.missingEvents && item.missingEvents.length > 0 && (
                  <div className="space-y-1.5">
                    <div className="flex items-center gap-1.5 text-xs font-medium text-amber-700 dark:text-amber-400">
                      <CalendarX className="h-4 w-4" />
                      <span>Thiếu sự kiện chuỗi cung ứng:</span>
                    </div>
                    <div className="flex flex-wrap gap-1.5 pl-5">
                      {item.missingEvents.map((evt, idx) => (
                        <Badge
                          key={idx}
                          variant="outline"
                          className="bg-amber-100/70 border-amber-300 text-amber-900 text-xs dark:bg-amber-950 dark:text-amber-200"
                        >
                          {evt}
                        </Badge>
                      ))}
                    </div>
                  </div>
                )}

                {/* Missing Docs */}
                {((item.missingDocDetails && item.missingDocDetails.length > 0) || item.missingDocs) && (
                  <div className="space-y-1.5">
                    <div className="flex items-center gap-1.5 text-xs font-medium text-red-700 dark:text-red-400">
                      <FileX className="h-4 w-4" />
                      <span>Thiếu chứng từ / nhật ký:</span>
                    </div>
                    <div className="space-y-1 pl-5">
                      {item.missingDocDetails && item.missingDocDetails.length > 0 ? (
                        item.missingDocDetails.map((doc, idx) => (
                          <div key={idx} className="text-xs text-red-800 dark:text-red-300 flex items-start gap-1">
                            <span className="text-red-500">•</span>
                            <span>{doc}</span>
                          </div>
                        ))
                      ) : (
                        <p className="text-xs text-red-800 dark:text-red-300">
                          Chưa có nhật ký nông hộ hoặc tệp chứng nhận lô hàng đính kèm
                        </p>
                      )}
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>

        <DialogFooter className="sm:justify-between border-t pt-3 mt-2">
          <p className="text-xs text-muted-foreground flex items-center gap-1">
            💡 Vui lòng bổ sung đầy đủ nhật ký/sự kiện trước khi xuất dữ liệu.
          </p>
          <Button variant="outline" onClick={onClose}>
            <X className="mr-1.5 h-4 w-4" />
            Đóng
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
