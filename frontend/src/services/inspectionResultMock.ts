import type { PublicInspectionResponse } from "@/types/publicInspection";

/**
 * Mock cho endpoint công khai của NCL-11-CN-003 (CV-04):
 *
 *   - GET /api/v1/public/trace/{codeValue}/inspections
 *
 * Backend chưa có endpoint này nên frontend tạm dùng mock.
 * Khi backend bổ sung endpoint thật, chỉ cần tắt cờ
 * VITE_USE_MOCK_INSPECTION_RESULT=false là code gọi API thật
 * (xem src/api/publicApi.ts).
 *
 * Các endpoint ghi nhận kết quả kiểm nghiệm đã có trên backend
 * (POST /inspection-criteria/{criterionId}/results,
 * GET /inspection-requests/{requestId}...) nên không còn mock ở đây.
 */

export const USE_MOCK_INSPECTION_RESULT =
  import.meta.env.VITE_USE_MOCK_INSPECTION_RESULT !== "false";

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/** Băm chuỗi sang số ổn định để dữ liệu mock deterministic theo codeValue. */
const hashCode = (value: string): number => {
  let hash = 0;
  for (let i = 0; i < value.length; i += 1) {
    hash = (hash << 5) - hash + value.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash);
};

/**
 * GET /api/v1/public/trace/{codeValue}/inspections (mock, CV-04).
 * Trả kết quả kiểm nghiệm mẫu theo mã tra cứu.
 */
export const mockFetchPublicInspections = async (
  codeValue: string
): Promise<PublicInspectionResponse> => {
  await delay(350);

  const seed = hashCode(codeValue);
  const hasInspection = seed % 3 !== 0;
  const passed = seed % 2 === 0;

  if (!hasInspection) {
    return {
      productionLotId: "mock-public-lot",
      lotName: "Lô sản xuất mẫu",
      hasInspection: false,
      inspections: [],
    };
  }

  const now = new Date();
  const toISODate = (date: Date) => {
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${date.getFullYear()}-${month}-${day}`;
  };
  const issueDate = new Date(now);
  issueDate.setMonth(issueDate.getMonth() - 1);
  const expiryDate = new Date(now);
  expiryDate.setMonth(expiryDate.getMonth() + 5);

  return {
    productionLotId: "mock-public-lot",
    lotName: "Lô sản xuất mẫu",
    hasInspection: true,
    inspections: [
      {
        requestId: `mock-request-${seed}`,
        overallResult: passed ? "PASSED" : "FAILED",
        overallResultLabel: passed ? "Đạt" : "Không đạt",
        issueDate: toISODate(issueDate),
        expiryDate: toISODate(expiryDate),
        statusLabel: passed ? "Đạt" : "Không đạt",
      },
    ],
  };
};