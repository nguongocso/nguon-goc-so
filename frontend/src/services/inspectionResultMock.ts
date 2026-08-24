import type { PublicInspectionResponse } from "@/types/publicInspection";

/**
 * Mock cho endpoint công khai của NCL-11-CN-003 (CV-04):
 *
 *   - GET /api/v1/public/trace/{codeValue}/inspections
 *
 * Mock chỉ bật khi được yêu cầu rõ ràng bằng:
 *
 *   VITE_USE_MOCK_INSPECTION_RESULT=true
 *
 * Mặc định (không khai báo biến) là OFF: frontend gọi API thật
 * (xem src/api/publicApi.ts). Khi backend chưa trả dữ liệu thì UI
 * hiển thị trạng thái "Chưa có kết quả kiểm nghiệm" — không fake dữ liệu.
 *
 * Các endpoint ghi nhận kết quả kiểm nghiệm đã có trên backend
 * (POST /inspection-criteria/{criterionId}/results,
 * PUT /inspection-requests/{requestId}/results...) nên không còn mock ở đây.
 */

export const USE_MOCK_INSPECTION_RESULT =
  import.meta.env.VITE_USE_MOCK_INSPECTION_RESULT === "true";

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
        id: `mock-res-${seed}-1`,
        criterionName: "Dư lượng thuốc BVTV (Pesticide Residue)",
        standardValue: "≤ 0.01 mg/kg (QCVN 01-188:2020/BNNPTNT)",
        measuredValue: passed ? "0.002 mg/kg" : "0.045 mg/kg",
        passed: passed,
        inspectorName: "Nguyễn Văn Kiểm Nghiệm",
        inspectionDate: toISODate(issueDate),
        expiryDate: toISODate(expiryDate),
        laboratoryName: "Trung tâm Kiểm nghiệm Nông sản Quốc gia",
      },
      {
        id: `mock-res-${seed}-2`,
        criterionName: "Kim loại nặng - Chì (Pb)",
        standardValue: "≤ 0.2 mg/kg (QCVN 8-2:2011/BYT)",
        measuredValue: passed ? "0.01 mg/kg" : "0.35 mg/kg",
        passed: passed,
        inspectorName: "Trần Thị Kiểm Nghiệm",
        inspectionDate: toISODate(issueDate),
        expiryDate: toISODate(expiryDate),
        laboratoryName: "Trung tâm Kiểm nghiệm Nông sản Quốc gia",
      },
    ],
  };
};