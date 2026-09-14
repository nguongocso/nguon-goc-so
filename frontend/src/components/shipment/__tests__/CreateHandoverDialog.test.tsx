import { describe, expect, it } from "vitest";
import { buildRecipientLabelMap, getRecipientDisplayName, renderRecipientSelectValue, toHandoverAssetUrl } from "../CreateHandoverDialog";
import type { Organization } from "@/types/organization";

const baseOrg = (overrides: Partial<Organization>): Organization => ({
  id: "327a40dc-a396-11f1-aea2-32ec817c7ea4",
  name: "Công ty Nông Sản Việt Demo",
  code: "DEMO_NSV",
  type: "ENTERPRISE",
  status: "ACTIVE",
  createdAt: "2026-08-29T10:41:31",
  ...overrides,
});

describe("getRecipientDisplayName (NCL-05-CN-008: dropdown hiện tên thay vì mã)", () => {
  it("hiển thị 'Tên (Mã)' khi đủ dữ liệu", () => {
    expect(getRecipientDisplayName(baseOrg({}))).toBe(
      "Công ty Nông Sản Việt Demo (DEMO_NSV)",
    );
  });

  it("không bao giờ trả về UUID trần khi thiếu tên", () => {
    expect(getRecipientDisplayName(baseOrg({ name: "" }))).toBe("DEMO_NSV");
    expect(
      getRecipientDisplayName(baseOrg({ name: "", code: "" })),
    ).toBe("327a40dc-a396-11f1-aea2-32ec817c7ea4");
  });

  it("chỉ hiện tên khi thiếu mã", () => {
    expect(getRecipientDisplayName(baseOrg({ code: "" }))).toBe(
      "Công ty Nông Sản Việt Demo",
    );
  });
});

describe("buildRecipientLabelMap (Select.Value hiển thị label thay vì UUID)", () => {
  const otherOrg: Organization = {
    id: "1a2b3c4d-0000-0000-0000-000000000001",
    name: "Hợp tác xã Rau Sạch Long An",
    code: "LA_RSAU",
    type: "COOPERATIVE",
    status: "ACTIVE",
    createdAt: "2026-08-29T10:41:31",
  };

  it("ánh xạ id → 'Tên (Mã)' cho từng tổ chức", () => {
    const map = buildRecipientLabelMap([
      baseOrg({}),
      otherOrg,
    ]);
    expect(map.get("327a40dc-a396-11f1-aea2-32ec817c7ea4")).toBe(
      "Công ty Nông Sản Việt Demo (DEMO_NSV)",
    );
    expect(map.get("1a2b3c4d-0000-0000-0000-000000000001")).toBe(
      "Hợp tác xã Rau Sạch Long An (LA_RSAU)",
    );
  });

  it("trả Map rỗng khi không có tổ chức (Value fallback về placeholder)", () => {
    expect(buildRecipientLabelMap([])).toEqual(new Map());
  });
});

describe("renderRecipientSelectValue (Select.Value nhận RAW VALUE — lỗi hiển thị placeholder khi đã chọn)", () => {
  const labels = buildRecipientLabelMap([
    baseOrg({}),
  ]);

  it("nhận raw value (string) và hiển thị tên tổ chức khi đã chọn", () => {
    expect(
      renderRecipientSelectValue(
        "327a40dc-a396-11f1-aea2-32ec817c7ea4",
        labels,
      ),
    ).toBe("Công ty Nông Sản Việt Demo (DEMO_NSV)");
  });

  it("hiển thị placeholder khi chưa chọn (value falsy)", () => {
    expect(renderRecipientSelectValue(undefined, labels)).toBe(
      "Chọn tổ chức nhận",
    );
    expect(renderRecipientSelectValue(null, labels)).toBe(
      "Chọn tổ chức nhận",
    );
    expect(renderRecipientSelectValue("", labels)).toBe(
      "Chọn tổ chức nhận",
    );
  });

  it("không hiển thị placeholder khi value hợp lệ (chống destructure { value })", () => {
    const label = renderRecipientSelectValue(
      "327a40dc-a396-11f1-aea2-32ec817c7ea4",
      labels,
    );
    expect(label).not.toBe("Chọn tổ chức nhận");
  });

  it("fallback về chính raw value khi không có label (không trả placeholder)", () => {
    expect(
      renderRecipientSelectValue("unknown-org-id", labels),
    ).toBe("unknown-org-id");
  });
});

describe("toHandoverAssetUrl (URL xem file chứng từ giao hàng)", () => {
  it("chuyển filePath dạng ./uploads/... thành URL /uploads/...", () => {
    const url = toHandoverAssetUrl("./uploads/handovers/abc123.png");
    expect(url).toMatch(/uploads\/handovers\/abc123\.png$/);
    expect(url).not.toBeUndefined();
  });

  it("chuyển filePath dạng /app/uploads/... thành URL /uploads/...", () => {
    const url = toHandoverAssetUrl("/app/uploads/handovers/abc123.png");
    expect(url).toMatch(/uploads\/handovers\/abc123\.png$/);
    expect(url).not.toBeUndefined();
  });

  it("trả undefined khi filePath không chứa /uploads/ hoặc rỗng", () => {
    expect(toHandoverAssetUrl("http://example.com/x.png")).toBeUndefined();
    expect(toHandoverAssetUrl("")).toBeUndefined();
    expect(toHandoverAssetUrl(null)).toBeUndefined();
  });
});
