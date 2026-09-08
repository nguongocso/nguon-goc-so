import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import {
  TraceCodeStatusBadge,
  TRACE_CODE_STATUS_LABELS,
} from '../TraceCodeStatusBadge';
import type { TraceCodeStatus } from '@/types/traceCode';

describe('TraceCodeStatusBadge', () => {
  const statuses: TraceCodeStatus[] = [
    'INACTIVE',
    'ACTIVE',
    'LOCKED',
    'CANCELLED',
    'RECALLED',
    'SUSPECT',
  ];

  it.each(statuses)('Hiển thị đúng nhãn tiếng Việt cho trạng thái %s', (status) => {
    render(<TraceCodeStatusBadge status={status} />);
    const label = TRACE_CODE_STATUS_LABELS[status];
    expect(screen.getByText(label)).toBeDefined();
  });
});
