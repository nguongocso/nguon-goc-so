import { useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { AlertCircle, ArrowLeft, LoaderCircle, QrCode } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { QrCodeGrid } from "@/components/shipment/QrCodeGrid";
import { getShipmentById } from "@/api/shipmentApi";
import type { Shipment } from "@/types/shipment";

const statusLabelMap: Record<Shipment["status"], string> = {
  DRAFT: "Nháp",
  CODE_PRINTED: "Đã in mã",
  ACTIVATED: "Đã kích hoạt",
  RECALLED: "Đã thu hồi",
};

const statusClassMap: Record<Shipment["status"], string> = {
  DRAFT: "bg-status-draft/10 text-status-draft",
  CODE_PRINTED: "bg-status-packaged/10 text-status-packaged",
  ACTIVATED: "bg-status-approved/10 text-status-approved",
  RECALLED: "bg-status-rejected/10 text-status-rejected",
};

const formatDateTime = (value: string): string => {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("vi-VN");
};

const infoRows = (s: Shipment): Array<[string, string]> => [
  ["Lô sản xuất", s.productionLotName || "—"],
  ["ID lô sản xuất", s.productionLotId],
  ["Số lượng", s.totalQuantity.toLocaleString("vi-VN")],
  ["Quy cách đóng gói", s.packagingInfo || "—"],
  ["Số mã truy xuất", s.traceCodes.length.toLocaleString("vi-VN")],
  ["Người tạo", s.createdByName || "—"],
  ["Ngày tạo", formatDateTime(s.createdAt)],
];

export const ShipmentDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialTab = searchParams.get("tab") === "qr" ? "qr" : "info";

  const [shipment, setShipment] = useState<Shipment | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) {
      setError("Không tìm thấy ID lô hàng.");
      setIsLoading(false);
      return;
    }

    let isMounted = true;

    const loadShipment = async () => {
      setIsLoading(true);
      setError(null);
      setShipment(null);
      try {
        const data = await getShipmentById(id);
        if (isMounted) setShipment(data);
      } catch (err: any) {
        if (isMounted)
          setError(
            err.response?.data?.message ||
              "Không thể tải thông tin chi tiết lô hàng.",
          );
      } finally {
        if (isMounted) setIsLoading(false);
      }
    };

    void loadShipment();
    return () => {
      isMounted = false;
    };
  }, [id]);

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-20 text-muted-foreground">
        <LoaderCircle className="h-5 w-5 animate-spin mr-2 text-emerald-500" />
        Đang tải thông tin lô hàng...
      </div>
    );
  }

  if (error || !shipment) {
    return (
      <div className="container mx-auto py-6">
        <div className="mb-4">
          <Button variant="outline" onClick={() => navigate(-1)}>
            <ArrowLeft className="h-4 w-4 mr-1" />
            Quay lại
          </Button>
        </div>
        <div className="flex items-start gap-3 rounded-lg border border-red-200 bg-red-50 p-4 text-red-700">
          <AlertCircle className="mt-0.5 size-5 shrink-0" />
          <div>
            <p className="font-semibold">Không thể tải dữ liệu</p>
            <p className="mt-1 text-sm">{error || "Không tìm thấy lô hàng."}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <Button variant="outline" onClick={() => navigate(-1)}>
          <ArrowLeft className="h-4 w-4 mr-1" />
          Quay lại
        </Button>
      </div>

      <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
        <CardHeader className="flex flex-row items-start justify-between pb-4">
          <div>
            <CardTitle className="text-xl font-bold text-emerald-800">
              {shipment.name}
            </CardTitle>
            <p className="text-sm text-muted-foreground mt-1">ID: {shipment.id}</p>
          </div>
          <span
            className={`rounded-full px-2.5 py-1 text-xs font-semibold whitespace-nowrap ${
              statusClassMap[shipment.status]
            }`}
          >
            {statusLabelMap[shipment.status]}
          </span>
        </CardHeader>

        <CardContent>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Lô sản xuất
              </span>
              <p className="mt-1 text-lg font-semibold text-emerald-800">
                {shipment.productionLotName || "—"}
              </p>
            </div>
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Số lượng
              </span>
              <p className="mt-1 text-lg font-semibold text-emerald-800">
                {shipment.totalQuantity.toLocaleString("vi-VN")}
              </p>
            </div>
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Quy cách đóng gói
              </span>
              <p className="mt-1 text-sm font-medium whitespace-pre-wrap">
                {shipment.packagingInfo || "—"}
              </p>
            </div>
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Số mã truy xuất
              </span>
              <p className="mt-1 text-lg font-semibold text-emerald-800">
                {shipment.traceCodes.length.toLocaleString("vi-VN")}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      <Tabs defaultValue={initialTab} className="w-full">
        <TabsList className="bg-white/80 backdrop-blur-sm border border-emerald-100 p-1 rounded-xl gap-1 min-h-11 max-w-full overflow-x-auto overflow-y-hidden">
          <TabsTrigger
            value="info"
            className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white"
          >
            Thông tin chung
          </TabsTrigger>
          <TabsTrigger
            value="qr"
            className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white"
          >
            Mã QR
          </TabsTrigger>
        </TabsList>

        <TabsContent value="info" className="mt-4">
          <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
            <CardContent className="pt-6">
              <dl className="divide-y rounded-lg border">
                {infoRows(shipment).map(([label, value]) => (
                  <div
                    key={label}
                    className="grid gap-1 px-4 py-3 sm:grid-cols-[180px_1fr] sm:gap-4"
                  >
                    <dt className="text-sm text-muted-foreground">{label}</dt>
                    <dd className="break-all whitespace-pre-wrap text-sm font-medium">
                      {value}
                    </dd>
                  </div>
                ))}
              </dl>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="qr" className="mt-4">
          <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
            <CardHeader className="pb-2">
              <CardTitle className="text-lg font-semibold text-emerald-800 flex items-center gap-2">
                <QrCode className="h-5 w-5" />
                Mã QR truy xuất
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-4">
              <QrCodeGrid traceCodes={shipment.traceCodes} />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};