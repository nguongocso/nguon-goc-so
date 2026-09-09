import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import { LoaderCircle, AlertCircle} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { BulkRecallRequestForm } from '@/components/recall/BulkRecallRequestForm';
import { getImpactScopeTrace } from '@/api/impactScopeTraceApi';
import { getProductionLotById } from '@/api/productionLotApi';
import type { ImpactScopeTraceResponse } from '@/types/impactScopeTrace';
import type { ProductionLot } from '@/types/productionLot';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { HelpButton } from '@/components/help/HelpButton';

export const CreateBulkRecallRequestPage: React.FC = () => {
    const navigate = useNavigate();
    const { id: productionLotId } = useParams<{ id: string }>();
    const [searchParams] = useSearchParams();

    useSetBreadcrumb([
        { label: 'Vận hành sản xuất', href: '/production-lots' },
        { label: 'Lô sản xuất', href: `/production-lots/${productionLotId}` },
        { label: 'Tạo yêu cầu thu hồi' },
    ]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [traceData, setTraceData] = useState<ImpactScopeTraceResponse | null>(null);
    const [productionLot, setProductionLot] = useState<ProductionLot | null>(null);

    const loadData = useCallback(async () => {
        if (!productionLotId) {
            setError('Không tìm thấy ID lô sản xuất.');
            setLoading(false);
            return;
        }

        setLoading(true);
        setError(null);

        try {
            if (!productionLotId) {
                setError('Không tìm thấy ID lô sản xuất.');
                setLoading(false);
                return;
            }
            const lotData = await getProductionLotById(productionLotId);
            setProductionLot(lotData);

            // Thứ tự ưu tiên: traceCode từ URL > mã lô sản xuất > productionLotId (UUID)
            // Backend API hỗ trợ tìm kiếm theo: traceCode, shipment UUID/name, productionLot UUID/name
            const searchCode = searchParams.get('traceCode') || lotData.code || productionLotId || '';
            if (!searchCode) {
                setError('Không thể xác định mã tìm kiếm phạm vi ảnh hưởng.');
                setLoading(false);
                return;
            }
            const data = await getImpactScopeTrace(searchCode);
            setTraceData(data);
        } catch (err: any) {
            const message = err.response?.data?.message || 'Không thể tải phạm vi ảnh hưởng.';
            setError(message);
            toast.error(message);
        } finally {
            setLoading(false);
        }
    }, [productionLotId, searchParams]);

    useEffect(() => {
        loadData();
    }, [loadData]);

    const handleSuccess = (requestId: string) => {
        navigate(`/recall-requests/bulk/${requestId}`);
    };

    const handleCancel = () => {
        navigate(-1);
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <div className="text-center space-y-4">
                    <LoaderCircle className="w-8 h-8 animate-spin text-emerald-600 mx-auto" />
                    <p className="text-sm text-muted-foreground">Đang tải phạm vi ảnh hưởng...</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <Card className="max-w-md w-full border-red-200 bg-red-50">
                    <CardContent className="pt-6 text-center space-y-4">
                        <AlertCircle className="w-12 h-12 text-red-500 mx-auto" />
                        <h2 className="text-lg font-semibold text-red-700">Không thể tải phạm vi ảnh hưởng</h2>
                        <p className="text-sm text-red-600">{error}</p>
                        <div className="flex justify-center gap-2">
                            <Button variant="outline" onClick={() => navigate(-1)}>
                                Quay lại
                            </Button>
                            <Button onClick={loadData}>Thử lại</Button>
                        </div>
                    </CardContent>
                </Card>
            </div>
        );
    }

    if (!traceData || !productionLot) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <Card className="max-w-md w-full border-amber-200 bg-amber-50">
                    <CardContent className="pt-6 text-center space-y-4">
                        <AlertCircle className="w-12 h-12 text-amber-500 mx-auto" />
                        <h2 className="text-lg font-semibold text-amber-700">
                            Không có lô hàng phù hợp
                        </h2>
                        <p className="text-sm text-amber-600">
                            Không có lô hàng phù hợp để tạo yêu cầu thu hồi.
                        </p>
                        <Button variant="outline" onClick={() => navigate(-1)}>
                            Quay lại
                        </Button>
                    </CardContent>
                </Card>
            </div>
        );
    }

    const hasValidShipments = traceData.shipments.some(s => s.status !== 'RECALLED');
    if (!hasValidShipments) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <Card className="max-w-md w-full border-amber-200 bg-amber-50">
                    <CardContent className="pt-6 text-center space-y-4">
                        <AlertCircle className="w-12 h-12 text-amber-500 mx-auto" />
                        <h2 className="text-lg font-semibold text-amber-700">
                            Không còn lô hàng hợp lệ
                        </h2>
                        <p className="text-sm text-amber-600">
                            Tất cả lô hàng đã được thu hồi. Không thể tạo yêu cầu thu hồi mới.
                        </p>
                        <Button variant="outline" onClick={() => navigate(-1)}>
                            Quay lại
                        </Button>
                    </CardContent>
                </Card>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                    <div className="flex items-center gap-2 mb-2">
                        <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                            Tạo yêu cầu thu hồi theo phạm vi ảnh hưởng
                        </h1>
                    </div>
                    <p className="text-sm text-muted-foreground">
                        Chọn các lô hàng bị ảnh hưởng từ kết quả truy vết để tạo yêu cầu thu hồi.
                    </p>
                </div>
                <HelpButton screenKey="bulk-recall-request-create" />
            </div>

            <Card className="border-slate-200 bg-white shadow-sm rounded-xl">
                <CardContent className="pt-6">
                    <BulkRecallRequestForm
                        productionLotId={productionLotId || ''}
                        productionLot={traceData.productionLot}
                        shipments={traceData.shipments}
                        onSuccess={handleSuccess}
                        onCancel={handleCancel}
                    />
                </CardContent>
            </Card>
        </div>
    );
};

export default CreateBulkRecallRequestPage;
