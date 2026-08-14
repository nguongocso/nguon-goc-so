import { useCallback, useState } from "react";
import { isAxiosError } from "axios";
import { scanLookupTraceCode } from "@/api/chainEventApi";
import type { ScanLookupResponse } from "@/types/scan";

export type ScanErrorCode =
  | "INVALID_CODE"
  | "FORBIDDEN_ORG"
  | "FORBIDDEN_ROLE"
  | "RECALLED"
  | "NETWORK"
  | "UNKNOWN";

export interface ScanError {
  code: ScanErrorCode;
  message: string;
  status?: number;
}

interface UseScanLookupReturn {
  data: ScanLookupResponse | null;
  isLoading: boolean;
  error: ScanError | null;
  lookup: (code: string) => Promise<ScanLookupResponse | null>;
  reset: () => void;
}

export function useScanLookup(): UseScanLookupReturn {
  const [data, setData] = useState<ScanLookupResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<ScanError | null>(null);

  const lookup = useCallback(
    async (code: string): Promise<ScanLookupResponse | null> => {
      const normalizedCode = code.trim();

      if (!normalizedCode) {
        setError({
          code: "INVALID_CODE",
          message: "Mã truy xuất không được để trống.",
          status: 400,
        });
        return null;
      }

      setIsLoading(true);
      setError(null);
      setData(null);

      try {
        const result = await scanLookupTraceCode(normalizedCode);
        setData(result);
        return result;
      } catch (requestError: unknown) {
        let scanError: ScanError;

        if (!isAxiosError<{ message?: string }>(requestError)) {
          scanError = {
            code: "UNKNOWN",
            message: "Đã xảy ra lỗi không xác định.",
          };
        } else if (!requestError.response) {
          scanError = {
            code: "NETWORK",
            message: "Không thể kết nối tới máy chủ.",
          };
        } else {
          const status = requestError.response.status;
          const message =
            requestError.response.data?.message ??
            "Không thể tra cứu mã truy xuất.";

          let errorCode: ScanErrorCode = "UNKNOWN";

          if (status === 400 || status === 404) {
            errorCode = "INVALID_CODE";
          } else if (status === 403) {
            errorCode =
              message.toLowerCase().includes("tổ chức") ||
              message.toLowerCase().includes("organization")
                ? "FORBIDDEN_ORG"
                : "FORBIDDEN_ROLE";
          } else if (status === 409) {
            errorCode = "RECALLED";
          }

          scanError = {
            code: errorCode,
            message,
            status,
          };
        }

        setError(scanError);
        return null;
      } finally {
        setIsLoading(false);
      }
    },
    [],
  );

  const reset = useCallback(() => {
    setData(null);
    setError(null);
    setIsLoading(false);
  }, []);

  return {
    data,
    isLoading,
    error,
    lookup,
    reset,
  };
}