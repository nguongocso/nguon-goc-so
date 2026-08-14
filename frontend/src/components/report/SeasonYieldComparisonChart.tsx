import { useMemo } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import type { SeasonYieldItem } from "@/types/seasonYieldComparison";

interface SeasonYieldComparisonChartProps {
  data: SeasonYieldItem[];
  baselineYear: number | null;
  baselineSeasonCode: string | null;
}

const quantityFormatter = new Intl.NumberFormat("vi-VN", {
  maximumFractionDigits: 1,
});

export function SeasonYieldComparisonChart({
  data,
  baselineYear,
  baselineSeasonCode,
}: SeasonYieldComparisonChartProps) {
  const chartData = useMemo(
    () =>
      data.map((item) => ({
        ...item,
        label: `${item.seasonName.replace("Vụ ", "")} ${item.year}`,
        isBaseline:
          item.year === baselineYear && item.seasonCode === baselineSeasonCode,
      })),
    [baselineSeasonCode, baselineYear, data],
  );

  return (
    <Card>
      <CardHeader>
        <CardTitle>Biểu đồ sản lượng theo mùa vụ</CardTitle>
        <CardDescription>
          Mỗi cột là một tổ hợp năm và mùa vụ do backend tổng hợp.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="h-[360px] w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 12, right: 12, left: 8, bottom: 38 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis
                dataKey="label"
                interval={0}
                angle={-12}
                textAnchor="end"
                height={58}
                tick={{ fontSize: 12 }}
              />
              <YAxis
                width={82}
                tickFormatter={(value: number) => `${quantityFormatter.format(value)} kg`}
                tick={{ fontSize: 12 }}
              />
              <Tooltip
                cursor={{ fill: "rgba(15, 118, 110, 0.06)" }}
                formatter={(value) => [
                  `${quantityFormatter.format(Number(value ?? 0))} kg`,
                  "Tổng sản lượng",
                ]}
                labelFormatter={(label) => String(label)}
              />
              <Bar dataKey="totalQuantity" name="Tổng sản lượng" radius={[7, 7, 0, 0]} maxBarSize={72}>
                {chartData.map((item) => {
                  const fill = item.isBaseline
                    ? "#2563eb"
                    : item.delta > 0
                      ? "#16a34a"
                      : item.delta < 0
                        ? "#ea580c"
                        : "#64748b";
                  return <Cell key={`${item.year}-${item.seasonCode}`} fill={fill} />;
                })}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="mt-2 flex flex-wrap justify-center gap-x-5 gap-y-2 text-xs text-muted-foreground">
          <span className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-sm bg-blue-600" /> Mùa vụ gốc
          </span>
          <span className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-sm bg-green-600" /> Tăng so với mốc
          </span>
          <span className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-sm bg-orange-600" /> Giảm so với mốc
          </span>
        </div>
      </CardContent>
    </Card>
  );
}