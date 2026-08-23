export type ScheduleType = 'daily' | 'weekly' | 'monthly' | 'interval' | 'custom';

export interface VisualCronState {
  type: ScheduleType;
  hour: number; // 0-23
  minute: number; // 0-59
  dayOfWeek: string; // 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'
  dayOfMonth: number; // 1-28 or 31 (for last day)
  intervalHours: number; // 1, 2, 3, 4, 6, 8, 12
  customCron: string;
}

export const DAY_OF_WEEK_OPTIONS = [
  { label: 'Thứ Hai', value: 'MON' },
  { label: 'Thứ Ba', value: 'TUE' },
  { label: 'Thứ Tư', value: 'WED' },
  { label: 'Thứ Năm', value: 'THU' },
  { label: 'Thứ Sáu', value: 'FRI' },
  { label: 'Thứ Bảy', value: 'SAT' },
  { label: 'Chủ Nhật', value: 'SUN' },
];

export const INTERVAL_HOUR_OPTIONS = [
  { label: 'Mỗi 1 giờ', value: 1 },
  { label: 'Mỗi 2 giờ', value: 2 },
  { label: 'Mỗi 3 giờ', value: 3 },
  { label: 'Mỗi 4 giờ', value: 4 },
  { label: 'Mỗi 6 giờ', value: 6 },
  { label: 'Mỗi 8 giờ', value: 8 },
  { label: 'Mỗi 12 giờ', value: 12 },
];

const padZero = (n: number): string => (n < 10 ? `0${n}` : `${n}`);

/**
 * Builds a Spring-compatible 6-field Cron expression from visual state.
 */
export const buildCronFromVisualState = (state: VisualCronState): string => {
  const { type, hour, minute, dayOfWeek, dayOfMonth, intervalHours, customCron } = state;

  switch (type) {
    case 'daily':
      return `0 ${minute} ${hour} * * ?`;
    case 'weekly':
      return `0 ${minute} ${hour} ? * ${dayOfWeek}`;
    case 'monthly':
      if (dayOfMonth === 31) {
        return `0 ${minute} ${hour} L * ?`; // L = Last day of month
      }
      return `0 ${minute} ${hour} ${dayOfMonth} * ?`;
    case 'interval':
      return `0 ${minute} */${intervalHours} * * ?`;
    case 'custom':
      return customCron.trim() || '0 0 2 * * ?';
    default:
      return '0 0 2 * * ?';
  }
};

/**
 * Parses a Spring Cron expression into a visual UI state.
 */
export const parseCronToVisualState = (cron: string): VisualCronState => {
  const defaultState: VisualCronState = {
    type: 'daily',
    hour: 2,
    minute: 0,
    dayOfWeek: 'MON',
    dayOfMonth: 1,
    intervalHours: 6,
    customCron: cron || '0 0 2 * * ?',
  };

  if (!cron || typeof cron !== 'string') {
    return defaultState;
  }

  const parts = cron.trim().split(/\s+/);
  if (parts.length < 5 || parts.length > 6) {
    return { ...defaultState, type: 'custom', customCron: cron };
  }

  // Normalize to 6 fields if 5 fields provided
  const fields = parts.length === 5 ? ['0', ...parts] : parts;
  const [, minStr, hourStr, domStr, monthStr, dowStr] = fields;

  const min = parseInt(minStr, 10);
  const hour = parseInt(hourStr, 10);

  // Interval check: hour is */N
  if (hourStr.startsWith('*/')) {
    const interval = parseInt(hourStr.replace('*/', ''), 10);
    if (!isNaN(interval) && !isNaN(min)) {
      return {
        ...defaultState,
        type: 'interval',
        minute: min,
        intervalHours: interval,
        customCron: cron,
      };
    }
  }

  // Daily check: 0 [min] [hour] * * ? or 0 [min] [hour] * * *
  if (
    !isNaN(min) &&
    !isNaN(hour) &&
    (domStr === '*' || domStr === '?') &&
    monthStr === '*' &&
    (dowStr === '*' || dowStr === '?')
  ) {
    return {
      ...defaultState,
      type: 'daily',
      hour,
      minute: min,
      customCron: cron,
    };
  }

  // Weekly check: 0 [min] [hour] ? * [DOW]
  if (
    !isNaN(min) &&
    !isNaN(hour) &&
    (domStr === '?' || domStr === '*') &&
    monthStr === '*' &&
    dowStr !== '*' &&
    dowStr !== '?'
  ) {
    let dow = dowStr.toUpperCase();
    // Normalize numeric DOW (1-7 or 0-6)
    if (dow === '1' || dow === 'MON') dow = 'MON';
    else if (dow === '2' || dow === 'TUE') dow = 'TUE';
    else if (dow === '3' || dow === 'WED') dow = 'WED';
    else if (dow === '4' || dow === 'THU') dow = 'THU';
    else if (dow === '5' || dow === 'FRI') dow = 'FRI';
    else if (dow === '6' || dow === 'SAT') dow = 'SAT';
    else if (dow === '7' || dow === '0' || dow === 'SUN') dow = 'SUN';

    return {
      ...defaultState,
      type: 'weekly',
      hour,
      minute: min,
      dayOfWeek: dow,
      customCron: cron,
    };
  }

  // Monthly check: 0 [min] [hour] [DOM] * ?
  if (
    !isNaN(min) &&
    !isNaN(hour) &&
    domStr !== '*' &&
    domStr !== '?' &&
    monthStr === '*' &&
    (dowStr === '?' || dowStr === '*')
  ) {
    const dom = domStr === 'L' ? 31 : parseInt(domStr, 10);
    if (!isNaN(dom)) {
      return {
        ...defaultState,
        type: 'monthly',
        hour,
        minute: min,
        dayOfMonth: dom,
        customCron: cron,
      };
    }
  }

  // Fallback to custom
  return { ...defaultState, type: 'custom', customCron: cron };
};

/**
 * Returns a human-readable Vietnamese description from visual state or cron expression.
 */
export const getCronDescriptionInVietnamese = (state: VisualCronState): string => {
  const { type, hour, minute, dayOfWeek, dayOfMonth, intervalHours, customCron } = state;
  const timeStr = `${padZero(hour)}:${padZero(minute)}`;

  switch (type) {
    case 'daily':
      return `Sao lưu tự động hàng ngày vào lúc ${timeStr} sáng/tối`;
    case 'weekly': {
      const dayLabel = DAY_OF_WEEK_OPTIONS.find((d) => d.value === dayOfWeek)?.label || dayOfWeek;
      return `Sao lưu tự động vào lúc ${timeStr} ${dayLabel} hàng tuần`;
    }
    case 'monthly': {
      const dayText = dayOfMonth === 31 ? 'ngày cuối cùng' : `ngày ${dayOfMonth}`;
      return `Sao lưu tự động vào lúc ${timeStr} ${dayText} hàng tháng`;
    }
    case 'interval':
      return `Sao lưu tự động định kỳ mỗi ${intervalHours} giờ (vào phút thứ ${minute})`;
    case 'custom':
      return `Tự động sao lưu theo biểu thức Cron tùy biến: "${customCron}"`;
    default:
      return 'Sao lưu dữ liệu tự động';
  }
};
