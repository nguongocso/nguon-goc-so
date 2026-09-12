import { render, screen, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { BulkRecallRequestForm } from "../BulkRecallRequestForm";
import type { ProductionLotTraceDto, ShipmentTraceDto } from "@/types/impactScopeTrace";

vi.mock("@/api/recallApi", () => ({
    createBulkRecallRequest: vi.fn(),
}));

function makeShipment(index: number, overrides: Partial<ShipmentTraceDto> = {}): ShipmentTraceDto {
    const padded = String(index).padStart(3, "0");
    return {
        id: `ship-${padded}`,
        code: `CODE-${padded}`,
        name: `Lo xoai ${padded}`,
        status: "ACTIVATED",
        totalQuantity: 100,
        createdAt: "2026-01-01T00:00:00Z",
        activatedStampsCount: 10,
        receivingOrganizations: [
            {
                organizationId: `org-${padded}`,
                organizationName: `HTX ${padded}`,
                receivedAt: "2026-01-02T00:00:00Z",
                eventType: "RECEIVE",
            },
        ],
        ...overrides,
    };
}

const productionLot: ProductionLotTraceDto = {
    id: "lot-1",
    code: "LOT-001",
    name: "Lo san xuat 001",
    status: "HARVESTED",
    expectedQuantity: 1000,
    expectedQuantityUnit: "kg",
};

function renderForm(shipments: ShipmentTraceDto[]) {
    return render(
        <BulkRecallRequestForm
            productionLotId="lot-1"
            productionLot={productionLot}
            shipments={shipments}
            onSuccess={vi.fn()}
        />,
    );
}

describe("BulkRecallRequestForm - bang Pham vi thu hoi", () => {
    it("danh so STT dung theo trang (page 2 bat dau tu 11)", () => {
        const shipments = Array.from({ length: 12 }, (_, i) => makeShipment(i + 1));
        renderForm(shipments);

        expect(screen.getByText("Hiển thị 1–10 trong tổng số 12 lô")).toBeInTheDocument();
        expect(screen.getByText("1", { selector: "td" })).toBeInTheDocument();

        fireEvent.click(screen.getByRole("button", { name: "Trang sau" }));
        expect(screen.getByText("Hiển thị 11–12 trong tổng số 12 lô")).toBeInTheDocument();
        expect(screen.getByText("11", { selector: "td" })).toBeInTheDocument();
        expect(screen.getByText("12", { selector: "td" })).toBeInTheDocument();
    });

    it("tim kiem khong phan biet hoa thuong, trim khoang trang va reset ve trang 1", () => {
        const shipments = Array.from({ length: 12 }, (_, i) => makeShipment(i + 1));
        renderForm(shipments);
        fireEvent.click(screen.getByRole("button", { name: "Trang sau" }));
        expect(screen.getByText("Hiển thị 11–12 trong tổng số 12 lô")).toBeInTheDocument();

        fireEvent.change(screen.getByLabelText("Tìm theo mã lô"), {
            target: { value: "  LO XOAI 001  " },
        });
        expect(screen.getByText("Hiển thị 1–1 trong tổng số 1 lô")).toBeInTheDocument();
        expect(screen.getByText("Lo xoai 001")).toBeInTheDocument();
    });

    it("bo loc trang thai ket hop voi tim kiem; lo da thu hoi khong the chon", () => {
        renderForm([
            makeShipment(1, { name: "Lo xoai A", status: "ACTIVATED" }),
            makeShipment(2, { name: "Lo xoai B", status: "RECALLED" }),
        ]);

        fireEvent.change(screen.getByLabelText("Lọc trạng thái"), {
            target: { value: "RECALLED" },
        });
        expect(screen.getByText("Lo xoai B")).toBeInTheDocument();
        expect(screen.queryByText("Lo xoai A")).not.toBeInTheDocument();

        fireEvent.change(screen.getByLabelText("Lọc trạng thái"), {
            target: { value: "ALL" },
        });
        fireEvent.change(screen.getByLabelText("Tìm theo mã lô"), { target: { value: "xoai" } });
        fireEvent.change(screen.getByLabelText("Lọc trạng thái"), {
            target: { value: "SELECTABLE" },
        });
        expect(screen.getByText("Lo xoai A")).toBeInTheDocument();
        expect(screen.queryByText("Lo xoai B")).not.toBeInTheDocument();

        // Lo da thu hoi phai disabled, lo thuong van chon duoc.
        const checkboxes = screen.getAllByRole("checkbox");
        expect(checkboxes.length).toBeGreaterThanOrEqual(2);
        expect(screen.getByText("Đã chọn 1/1 lô")).toBeInTheDocument();
    });

    it("doi page size reset ve trang 1 va hien thi dung range", () => {
        const shipments = Array.from({ length: 25 }, (_, i) => makeShipment(i + 1));
        renderForm(shipments);
        fireEvent.click(screen.getByRole("button", { name: "Trang sau" }));
        expect(screen.getByText("Hiển thị 11–20 trong tổng số 25 lô")).toBeInTheDocument();

        fireEvent.change(screen.getByLabelText("Số dòng mỗi trang"), { target: { value: "20" } });
        expect(screen.getByText("Hiển thị 1–20 trong tổng số 25 lô")).toBeInTheDocument();
    });

    it("lo cha da tach chi hien thi de truy vet va khong the chon", () => {
        renderForm([
            makeShipment(1, { status: "SPLIT" }),
            makeShipment(2, { status: "CODE_PRINTED" }),
        ]);

        expect(screen.getByText("Lô cha đã tách chỉ dùng để truy vết")).toBeInTheDocument();
        expect(screen.getByText("Đã chọn 1/1 lô")).toBeInTheDocument();

        const row = screen.getByText("Lo xoai 001").closest("tr");
        expect(row?.querySelector('[role="checkbox"]')).toHaveAttribute("aria-disabled", "true");
    });

    it("tim kiem khong co ket qua hien thi empty-state", () => {
        renderForm([makeShipment(1)]);
        fireEvent.change(screen.getByLabelText("Tìm theo mã lô"), {
            target: { value: "khong-ton-tai" },
        });
        expect(
            screen.getByText("Không tìm thấy lô hàng phù hợp với tìm kiếm/bộ lọc."),
        ).toBeInTheDocument();
        expect(screen.getByText("Hiển thị 0–0 trong tổng số 0 lô")).toBeInTheDocument();
    });
});
