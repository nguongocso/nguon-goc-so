import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ChevronDown, ChevronRight } from "lucide-react";
import type { AreaAnalysisStats } from "@/types/cropAreaAnalysis";

interface Props {
  data: AreaAnalysisStats[];
}

const formatNumber = (value: number) => value.toLocaleString();

export const AreaAnalysisTable = ({ data }: Props) => {
  const [expandedAreas, setExpandedAreas] = useState<Set<string>>(new Set());

  const toggleExpand = (id: string) => {
    setExpandedAreas((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(id)) newSet.delete(id);
      else newSet.add(id);
      return newSet;
    });
  };

  if (!data || data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium">Theo vùng trồng</CardTitle>
        </CardHeader>
        <CardContent className="text-center text-muted-foreground py-8">
          Không có dữ liệu cho khu vực này.
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-medium">Theo vùng trồng</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Vùng trồng</TableHead>
              <TableHead className="text-right">Diện tích (ha)</TableHead>
              <TableHead className="text-right">Số lô</TableHead>
              <TableHead className="text-right">Sản lượng dự kiến</TableHead>
              <TableHead className="text-right">Sản lượng thực tế</TableHead>
              <TableHead className="text-right">Tổ chức</TableHead>
              <TableHead className="w-[50px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.flatMap((area) => {
              const isExpanded = expandedAreas.has(area.farmAreaId);
              const rows = [
                <TableRow
                  key={area.farmAreaId}
                  className="cursor-pointer hover:bg-muted/50"
                  onClick={() => toggleExpand(area.farmAreaId)}
                >
                  <TableCell className="font-medium">
                    {area.farmAreaName}
                  </TableCell>
                  <TableCell className="text-right">
                    {area.areaSize}
                  </TableCell>
                  <TableCell className="text-right">
                    {area.totalLots}
                  </TableCell>
                  <TableCell className="text-right">
                    {formatNumber(area.expectedYield)}
                  </TableCell>
                  <TableCell className="text-right">
                    {formatNumber(area.actualYield)}
                  </TableCell>
                  <TableCell className="text-right">
                    {area.organizationName}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button variant="ghost" size="sm" className="p-0">
                      {isExpanded ? (
                        <ChevronDown className="h-4 w-4" />
                      ) : (
                        <ChevronRight className="h-4 w-4" />
                      )}
                    </Button>
                  </TableCell>
                </TableRow>,
              ];

              if (isExpanded && area.seasons && area.seasons.length > 0) {
                rows.push(
                  <TableRow key={`${area.farmAreaId}-expand`}>
                    <TableCell colSpan={7} className="p-0">
                      <div className="pl-8 pr-4 py-3 bg-muted/30">
                        <Table>
                          <TableHeader>
                            <TableRow>
                              <TableHead>Mùa vụ</TableHead>
                              <TableHead className="text-right">
                                Số lô
                              </TableHead>
                              <TableHead className="text-right">
                                Dự kiến (kg)
                              </TableHead>
                              <TableHead className="text-right">
                                Thực tế (kg)
                              </TableHead>
                            </TableRow>
                          </TableHeader>
                          <TableBody>
                            {area.seasons.map((season) => (
                              <TableRow key={season.seasonCode}>
                                <TableCell>
                                  <Badge variant="outline">
                                    {season.seasonName}
                                  </Badge>
                                </TableCell>
                                <TableCell className="text-right">
                                  {season.lotCount}
                                </TableCell>
                                <TableCell className="text-right">
                                  {formatNumber(season.expectedYield)}
                                </TableCell>
                                <TableCell className="text-right">
                                  {formatNumber(season.actualYield)}
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              }

              return rows;
            })}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
};