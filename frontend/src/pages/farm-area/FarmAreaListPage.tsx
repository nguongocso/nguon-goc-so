import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Plus, MapPin, ExternalLink, RefreshCw } from 'lucide-react';
import { HelpButton } from '@/components/help/HelpButton';
import { toast } from 'sonner';
import { getFarmAreas } from '@/api/farmAreaApi';
import type { FarmArea } from '@/types/farmArea';
import { AREA_UNIT_LABELS, convertAreaFromHa } from '@/types/farmArea';
import { useNavigate } from 'react-router-dom';
import { usePermission } from '@/hooks/usePermission';
import { ROLE_ACCESS } from '@/config/roleAccess';

export default function FarmAreaListPage() {
  const navigate = useNavigate();
  const [areas, setAreas] = useState<FarmArea[]>([]);
  const [loading, setLoading] = useState(true);

  const canCreate = usePermission(ROLE_ACCESS.farmAreaCreate);

  const fetchAreas = async () => {
    try {
      setLoading(true);
      const data = await getFarmAreas();
      setAreas(data);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải danh sách vùng trồng');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAreas();
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Vùng trồng</h1>
          <p className="text-sm text-muted-foreground">
            Quản lý các vùng trồng của tổ chức
          </p>
        </div>
        <div className="flex gap-2">
          <HelpButton screenKey="farm-area-list" />
          <Button variant="outline" onClick={fetchAreas} disabled={loading}>
            <RefreshCw className={`h-4 w-4 mr-1 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
          {canCreate && (
            <Button onClick={() => navigate('/farm-areas/create')} variant="create">
              <Plus className="h-4 w-4 mr-1" />
              Tạo vùng trồng
            </Button>
          )}
        </div>
      </div>

      <Card className="border-slate-200 bg-white shadow-sm rounded-xl">
        <CardHeader className="border-b border-slate-100">
          <CardTitle className="text-base font-semibold text-slate-900">
            Danh sách vùng trồng
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="flex justify-center items-center py-16 text-muted-foreground">
              <RefreshCw className="h-5 w-5 animate-spin mr-2 text-emerald-500" />
              Đang tải...
            </div>
          ) : areas.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-muted-foreground">
              <MapPin className="mx-auto h-12 w-12 text-emerald-300" />
              <p className="mt-2 font-semibold text-slate-800">Chưa có vùng trồng nào</p>
              <p className="text-sm">Nhấn "Tạo vùng trồng" để thêm mới.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow className="bg-slate-50">
                    <TableHead className="font-semibold text-slate-700">Tên vùng</TableHead>
                    <TableHead className="font-semibold text-slate-700">Loại cây trồng</TableHead>
                    <TableHead className="font-semibold text-slate-700">Diện tích</TableHead>
                    <TableHead className="font-semibold text-slate-700">Vị trí (tọa độ)</TableHead>
                    <TableHead className="font-semibold text-slate-700">Ngày tạo</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {areas.map((area) => (
                    <TableRow key={area.id} className="hover:bg-slate-50/80">
                      <TableCell className="font-medium text-slate-900">{area.name}</TableCell>
                      <TableCell>{area.cropTypeName}</TableCell>
                      <TableCell>
                        {convertAreaFromHa(area.area, area.areaUnit).toLocaleString('vi-VN', {
                          maximumFractionDigits: 2,
                        })}{' '}
                        {AREA_UNIT_LABELS[area.areaUnit]}
                      </TableCell>
                      <TableCell>
                        <a
                          href={`https://www.google.com/maps?q=${area.latitude},${area.longitude}`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center gap-1 text-xs font-mono text-emerald-700 hover:text-emerald-800 hover:underline"
                          title="Xem trên Google Maps"
                        >
                          <MapPin className="h-3 w-3 text-emerald-500" />
                          {area.latitude.toFixed(4)}, {area.longitude.toFixed(4)}
                          <ExternalLink className="h-3 w-3 opacity-50" />
                        </a>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {new Date(area.createdAt).toLocaleDateString('vi-VN')}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}