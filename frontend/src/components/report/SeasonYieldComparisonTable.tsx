import { ArrowDownRight, ArrowUpRight, Minus } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { SeasonYieldItem } from "@/types/seasonYieldComparison";
import { cn } from "@/lib/utils";

interface SeasonYieldComparisonTableProps {
  data: SeasonYieldItem[];
  baselineYear: number | null;
  baselineSeasonCode: string | null;
}

const numberFormatter = new Intl.NumberFormat("vi-VN", {
  maximumFractionDigits: 2,
});

function TrendValue({ value, suffix = "" }: { value: number | null; suffix?: string }) {
  if (value === null) {
    return <span className="text-muted-foreground">Không xác định</span>;
  }

  const positive = value > 0;
  const negative = value < 0;
  const Icon = positive ? ArrowUpRight : negative ? ArrowDownRight : Minus;

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 font-medium",
        positive && "text-emerald-700",
        negative && "text-orange-700",
        !positive && !negative && "text-muted-foreground",
      )}
    >
      <Icon className="h-4 w-4" />
      {positive ? "+" : ""}
      {numberFormatter.format(value)}
      {suffix}
    </span>
  );
}

export function SeasonYieldComparisonTable({
  data,
  baselineYear,
  baselineSeasonCode,
}: SeasonYieldComparisonTableProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Chi tiết so sánh</CardTitle>
        <CardDescription>
          Chênh lệch được tính so với mùa vụ gốc do backend lựa chọn.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Mùa vụ</TableHead>
              <TableHead className="text-right">Số lô</TableHead>
              <TableHead className="text-right">Tổng sản lượng</TableHead>
              <TableHead className="text-right">Chênh lệch</TableHead>
              <TableHead className="text-right">Tỷ lệ</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.map((item) => {
              const isBaseline =
                item.year === baselineYear && item.seasonCode === baselineSeasonCode;

              return (
                <TableRow key={`${item.year}-${item.seasonCode}`} className={cn(isBaseline && "bg-blue-50/70")}>
                  <TableCell>
                    <div className="flex min-w-44 items-center gap-2">
                      <div>
                        <p className="font-medium">{item.seasonName}</p>
                        <p className="text-xs text-muted-foreground">Năm {item.year}</p>
                      </div>
                      {isBaseline && (
                        <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">
                          Mùa vụ gốc
                        </span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    {numberFormatter.format(item.lotCount)}
                  </TableCell>
                  <TableCell className="text-right font-semibold">
                    {numberFormatter.format(item.totalQuantity)} kg
                  </TableCell>
                  <TableCell className="text-right">
                    <TrendValue value={item.delta} suffix=" kg" />
                  </TableCell>
                  <TableCell className="text-right">
                    <TrendValue value={item.deltaPercent} suffix="%" />
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
