import React from 'react';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
} from '@/components/ui/select';
import { cn } from '@/lib/utils';

interface FilterOption {
  value: string;
  label: string;
}

interface FilterSelectProps {
  value: string;
  onValueChange: (value: string | null) => void;
  options: FilterOption[];
  placeholder?: string;
  ariaLabel?: string;
  className?: string;
  size?: 'default' | 'sm';
}

export const FilterSelect: React.FC<FilterSelectProps> = ({
  value,
  onValueChange,
  options,
  placeholder,
  ariaLabel,
  className,
  size = 'default',
}) => {
  const selectedLabel = options.find((opt) => opt.value === value)?.label;

  return (
    <Select value={value} onValueChange={onValueChange}>
      <SelectTrigger
        size={size}
        aria-label={ariaLabel}
        className={cn('w-full sm:w-auto min-w-[200px]', className)}
      >
        <span className="flex-1 text-left truncate pr-1">
          {selectedLabel || placeholder || 'Chọn...'}
        </span>
      </SelectTrigger>
      <SelectContent>
        {options.map((opt) => (
          <SelectItem key={opt.value} value={opt.value}>
            {opt.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
};
