import React from 'react';

interface ListToolbarProps {
  left?: React.ReactNode;
  right?: React.ReactNode;
}

export const ListToolbar: React.FC<ListToolbarProps> = ({ left, right }) => {
  return (
    <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
      <div className="flex flex-1 flex-col sm:flex-row items-stretch sm:items-center gap-3">
        {left}
      </div>
      {right && (
        <div className="flex items-center gap-2">
          {right}
        </div>
      )}
    </div>
  );
};
