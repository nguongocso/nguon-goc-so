import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import type { LotScanStats } from '@/types/lookupStatistics';

interface Props {
  data: LotScanStats[];
}

export const LotStatsTable = ({ data }: Props) => {
  if (!data || data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium">Theo lô sản xuất</CardTitle>
        </CardHeader>
        <CardContent className="text-center text-muted-foreground py-8">
          Chưa có dữ liệu quét
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-medium">Theo lô sản xuất</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Tên lô</TableHead>
              <TableHead className="text-right">Lượt quét</TableHead>
              <TableHead className="text-right">Bất thường</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.map((item) => (
              <TableRow key={item.lotId}>
                <TableCell className="font-medium">{item.lotName}</TableCell>
                <TableCell className="text-right">{item.scanCount}</TableCell>
                <TableCell className="text-right">
                  {item.abnormalScansCount > 0 ? (
                    <Badge variant="destructive">{item.abnormalScansCount}</Badge>
                  ) : (
                    <span className="text-muted-foreground">0</span>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
};