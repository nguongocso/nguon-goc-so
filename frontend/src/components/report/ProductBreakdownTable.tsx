import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import type { ProductBreakdownItem } from '@/types/report';

interface Props {
  data: ProductBreakdownItem[];
}

export const ProductBreakdownTable = ({ data }: Props) => {
  if (!data || data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium">Phân tích theo sản phẩm</CardTitle>
        </CardHeader>
        <CardContent className="text-center text-muted-foreground py-8">
          Chưa có dữ liệu phân tích sản phẩm.
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-medium">Phân tích theo sản phẩm</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Sản phẩm</TableHead>
              <TableHead className="text-right">Số lô hàng</TableHead>
              <TableHead className="text-right">Tổng sản lượng (kg)</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.map((item, index) => (
              <TableRow key={index}>
                <TableCell className="font-medium">{item.productCategoryName}</TableCell>
                <TableCell className="text-right">{item.shipmentCount}</TableCell>
                <TableCell className="text-right">{item.totalQuantity.toLocaleString()}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
};