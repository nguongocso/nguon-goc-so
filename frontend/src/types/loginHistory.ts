export type LoginHistoryResult = 'SUCCESS' | 'FAILED';

export interface LoginHistoryItem {
  id: string;
  userId: string | null;
  usernameInput: string;
  roleCode: string | null;
  result: LoginHistoryResult | string;
  ipAddress: string | null;
  countryCode: string | null;
  isNewCountry: boolean | null;
  createdAt: string;
}

export interface LoginHistoryParams {
  page?: number;
  size?: number;
  userId?: string;
  result?: string;
  startDate?: string;
  endDate?: string;
}
