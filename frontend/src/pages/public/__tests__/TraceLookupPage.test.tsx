import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/api/publicApi", () => ({
  getPublicTrace: vi.fn(),
  getPublicCertifications: vi.fn(),
  getPublicInspections: vi.fn(),
}));

import {
  getPublicTrace,
  getPublicCertifications,
  getPublicInspections,
} from "@/api/publicApi";
import TraceLookupPage from "@/pages/public/TraceLookupPage";

const mockTraceData = {
  codeValue: "TC-TEST-001",
  productionLotId: "lot-123",
  lotName: "Lô xoài xuất khẩu Cát Chu",
  lotCode: "LOT-001",
  productName: "Xoài Cát Chu",
  productNameEn: "Cat Chu Mango",
  shipmentCode: "SHIP-8888",
  shipmentStatus: "ACTIVE",
  recalled: false,
  recallMessage: null,
  recallMessageEn: null,
  locked: false,
  lockReason: null,
  lockedAt: null,
  events: [
    {
      eventType: "HARVEST",
      eventData: { harvestDate: "2026-08-01", notes: "Thu hoạch chín cây" },
      recordedAt: "2026-08-01T08:00:00Z",
      latitude: 10.7769,
      longitude: 106.7009,
    },
  ],
};

const mockCertData = {
  productionLotId: "lot-123",
  lotName: "Lô xoài xuất khẩu Cát Chu",
  hasCertification: true,
  certifications: [
    {
      certificationId: "cert-1",
      certificationName: "Chứng nhận GlobalGAP",
      certificationNameEn: "GlobalGAP Standard",
      certificationCode: "GLOBALGAP-001",
      issuedBy: "SGS Vietnam",
      issueDate: "2026-01-01",
      expiryDate: "2027-01-01",
      status: "VALID" as const,
      statusLabel: "Hợp lệ",
    },
  ],
};

const mockInspectionData = {
  productionLotId: "lot-123",
  lotName: "Lô xoài xuất khẩu Cát Chu",
  hasInspection: true,
  totalCriteria: 1,
  passedCriteria: 1,
  failedCriteriaCount: 0,
  failedRatio: 0.0,
  inspections: [
    {
      id: "insp-1",
      criterionName: "Dư lượng thuốc BVTV",
      criterionNameEn: "Pesticide Residue",
      standardValue: "< 0.01 mg/kg",
      standardValueEn: "< 0.01 mg/kg",
      measuredValue: "Đạt chuẩn (Trong ngưỡng an toàn)",
      passed: true,
      inspectionDate: "2026-08-02",
      expiryDate: "2027-08-02",
    },
  ],
};

function renderPage(code = "TC-TEST-001") {
  return render(
    <MemoryRouter initialEntries={[`/trace/${code}`]}>
      <Routes>
        <Route path="/trace/:codeValue" element={<TraceLookupPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe("TraceLookupPage English Public Lookup (NCL-06-CN-004)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    sessionStorage.setItem("public_lookup_lang", "vi");
    vi.mocked(getPublicTrace).mockResolvedValue(mockTraceData);
    vi.mocked(getPublicCertifications).mockResolvedValue(mockCertData);
    vi.mocked(getPublicInspections).mockResolvedValue(mockInspectionData);
  });

  it("switches language between VI and EN and displays English labels", async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getAllByText("Xoài Cát Chu").length).toBeGreaterThan(0);
    });

    // Toggle language to EN
    const enButton = screen.getByRole("button", { name: "EN" });
    fireEvent.click(enButton);

    // Verify English Product Name fallback (TC-04)
    expect((await screen.findAllByText("Cat Chu Mango")).length).toBeGreaterThan(0);

    // Verify [Original] badge for lotName and shipmentCode (TC-02)
    const originalBadges = screen.getAllByText("[Original]");
    expect(originalBadges.length).toBeGreaterThan(0);

    // Verify English Certification Name (TC-04)
    expect(screen.getByText("GlobalGAP Standard")).toBeInTheDocument();

    // Verify English Inspection Criterion Name (TC-04)
    expect(screen.getByText("Pesticide Residue")).toBeInTheDocument();
  });

  it("displays English recall alert when shipment is recalled (TC-03)", async () => {
    vi.mocked(getPublicTrace).mockResolvedValue({
      ...mockTraceData,
      recalled: true,
      shipmentStatus: "RECALLED",
      recallMessage: "Cảnh báo thu hồi tiếng Việt",
      recallMessageEn: "WARNING: This shipment has been recalled. Reason: Pesticide excess",
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText("Cảnh báo thu hồi tiếng Việt")).toBeInTheDocument();
    });

    // Switch to EN
    fireEvent.click(screen.getByRole("button", { name: "EN" }));

    expect(await screen.findByText("RECALL WARNING")).toBeInTheDocument();
    expect(screen.getByText("WARNING: This shipment has been recalled. Reason: Pesticide excess")).toBeInTheDocument();
  });
});
