import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ChevronLeft, ChevronRight, Plus, Eye } from "lucide-react";
import { useShipments } from "@/hooks/useShipments";
import type { CreateShipmentPayload } from "@/types/shipment";
import { CreateShipmentModal } from "@/components/shipment/CreateShipmentModal";

const statusLabelMap: Record<string, string> = {
  DRAFT: "Nháp",
  CODE_PRINTED: "Đã in mã",
  ACTIVATED: "Đã kích hoạt",
  RECALLED: "Đã thu hồi",
};

const statusColorMap: Record<string, string> = {
  DRAFT: "bg-status-draft/10 text-status-draft",
  CODE_PRINTED: "bg-status-packaged/10 text-status-packaged",
  ACTIVATED: "bg-status-approved/10 text-status-approved",
  RECALLED: "bg-status-rejected/10 text-status-rejected",
};

interface ShipmentListProps {
  productionLotId: string;
  productionLotStatus: string;
  canCreate: boolean;
}

export const ShipmentList = ({
  productionLotId,
  productionLotStatus,
  canCreate,
}: ShipmentListProps) => {
  const navigate = useNavigate();
  const [modalOpen, setModalOpen] = useState(false);

  const {
    shipments,
    isLoading,
    createShipment,
    isCreating,
    page,
    totalPages,
    totalElements,
    setPage,
  } = useShipments(productionLotId);

  const handleCreate = async (payload: CreateShipmentPayload) => {
    await createShipment(payload);
  };

  const formatDate = (dateStr: string) => {
    try {
      return new Date(dateStr).toLocaleString("vi-VN");
    } catch {
      return dateStr;
    }
  };

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Danh sách lô hàng</CardTitle>

            {canCreate && productionLotStatus === "PACKAGED" && (
              <Button onClick={() => setModalOpen(true)}>
                <Plus className="mr-1 h-4 w-4" />
                Tạo lô hàng
              </Button>
            )}
          </div>
        </CardHeader>

        <CardContent>
          {isLoading ? (
            <div className="py-8 text-center">Đang tải...</div>
          ) : shipments.length === 0 ? (
            <div className="py-8 text-center text-muted-foreground">
              Chưa có lô hàng nào cho lô sản xuất này.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Tên lô hàng</TableHead>
                    <TableHead className="text-center">Số lượng</TableHead>
                    <TableHead>Quy cách</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead>Ngày tạo</TableHead>
                    <TableHead className="text-center">Số mã</TableHead>
                    <TableHead className="text-center">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>

                <TableBody>
                  {shipments.map((shipment) => (
                    <TableRow key={shipment.id}>
                      <TableCell className="font-medium">
                        {shipment.name}
                      </TableCell>

                      <TableCell className="text-center">
                        {shipment.totalQuantity}
                      </TableCell>

                      <TableCell>
                        {shipment.packagingInfo || "—"}
                      </TableCell>

                      <TableCell>
                        <span
                          className={`rounded-full px-2.5 py-1 text-xs font-semibold ${statusColorMap[shipment.status] ||
                            "bg-status-draft/10 text-status-draft"
                            }`}
                        >
                          {statusLabelMap[shipment.status] || shipment.status}
                        </span>
                      </TableCell>

                      <TableCell>
                        {formatDate(shipment.createdAt)}
                      </TableCell>

                      <TableCell className="text-center">
                        {shipment.traceCodes?.length || 0}
                      </TableCell>

                      <TableCell className="text-center">
                        <div className="flex items-center justify-center gap-1.5">
                          <Button
                            size="sm"
                            variant="outline"
                            className="h-auto px-2.5 py-1 text-xs"
                            onClick={() =>
                              navigate(
                                `/production-lots/${productionLotId}/shipments/${shipment.id}`,
                              )
                            }
                          >
                            <Eye className="mr-1 h-3 w-3" />
                            Chi tiết
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}

          {/* Pagination bar */}
          {!isLoading && totalElements > 0 && totalPages > 1 && (
            <div className="flex items-center justify-between border-t pt-4 mt-2">
              <p className="text-sm text-muted-foreground">
                Trang{" "}
                <span className="font-medium text-foreground">{page + 1}</span>
                {" "}/{" "}
                <span className="font-medium text-foreground">{totalPages}</span>
                {" "}&#183;{" "}
                Tổng{" "}
                <span className="font-medium text-foreground">
                  {totalElements}
                </span>{" "}
                lô hàng
              </p>
              <div className="flex items-center gap-1">
                <Button
                  variant="outline"
                  size="sm"
                  className="h-8 w-8 p-0"
                  disabled={page === 0}
                  onClick={() => setPage(page - 1)}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  className="h-8 w-8 p-0"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage(page + 1)}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <CreateShipmentModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSubmit={handleCreate}
        productionLotId={productionLotId}
        loading={isCreating}
      />
    </>
  );
};