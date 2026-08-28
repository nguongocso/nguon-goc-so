import React from 'react';
import { Card, CardContent } from '@/components/ui/card';

interface ListCardProps {
  children: React.ReactNode;
  className?: string;
}

export const ListCard: React.FC<ListCardProps> = ({ children, className }) => {
  return (
    <Card className={className}>
      <CardContent className="p-4 space-y-4">
        {children}
      </CardContent>
    </Card>
  );
};
