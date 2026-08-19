import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ChevronDown, ChevronRight } from 'lucide-react';
import type { SeasonAnalysisStats } from '@/types/cropAreaAnalysis';

interface Props {
  data: SeasonAnalysisStats[];
}

const formatNumber = (value: number) => value.toLocaleString();

export const SeasonAnalysisTable = ({ data }: Props) => {
  const [expandedSeasons, setExpandedSeasons] = useState<Set<string>>(new Set());

  const toggleExpand = (code: string) => {
    setExpandedSeasons((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(code)) newSet.delete(code);
      else newSet.add(code);
      return newSet;
    });
  };

  if (!data || data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium">Theo mùa vụ</CardTitle>
        </CardHeader>
        <CardContent className="text-center text-muted-foreground py-8">
          Không có dữ liệu cho mùa vụ này.
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-medium">Theo mùa vụ</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Mùa vụ</TableHead>
              <TableHead className="text-right">Năm</TableHead>
              <TableHead className="text-right">Số lô</TableHead>
              <TableHead className="text-right">Dự kiến (kg)</TableHead>
              <TableHead className="text-right">Thực tế (kg)</TableHead>
              <TableHead className="w-[50px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.flatMap((season) => {
              const isExpanded = expandedSeasons.has(season.seasonCode);
              const rows = [
                <TableRow
                  key={season.seasonCode}
                  className="cursor-pointer hover:bg-muted/50"
                  onClick={() => toggleExpand(season.seasonCode)}
                >
                  <TableCell className="font-medium">
                    <Badge>{season.seasonName}</Badge>
                  </TableCell>
                  <TableCell className="text-right">{season.year}</TableCell>
                  <TableCell className="text-right">{season.totalLots}</TableCell>
                  <TableCell className="text-right">{formatNumber(season.expectedYield)}</TableCell>
                  <TableCell className="text-right">{formatNumber(season.actualYield)}</TableCell>
                  <TableCell className="text-right">
                    <Button variant="ghost" size="sm" className="p-0">
                      {isExpanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                    </Button>
                  </TableCell>
                </TableRow>,
              ];

              if (isExpanded && season.areas && season.areas.length > 0) {
                rows.push(
                  <TableRow key={`${season.seasonCode}-expand`}>
                    <TableCell colSpan={6} className="p-0">
                      <div className="pl-8 pr-4 py-3 bg-muted/30">
                        <Table>
                          <TableHeader>
                            <TableRow>
                              <TableHead>Vùng trồng</TableHead>
                              <TableHead className="text-right">Số lô</TableHead>
                              <TableHead className="text-right">Dự kiến (kg)</TableHead>
                              <TableHead className="text-right">Thực tế (kg)</TableHead>
                            </TableRow>
                          </TableHeader>
                          <TableBody>
                            {season.areas.map((area) => (
                              <TableRow key={area.farmAreaId}>
                                <TableCell>{area.farmAreaName}</TableCell>
                                <TableCell className="text-right">{area.lotCount}</TableCell>
                                <TableCell className="text-right">{formatNumber(area.expectedYield)}</TableCell>
                                <TableCell className="text-right">{formatNumber(area.actualYield)}</TableCell>
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