import { render, screen, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { InspectionRequestActionButtons } from "../InspectionRequestActionButtons";

// Mock useNavigate from react-router-dom
const mockNavigate = vi.fn();

vi.mock("react-router-dom", () => ({
  useNavigate: () => mockNavigate,
}));

describe("InspectionRequestActionButtons", () => {
  const defaultProps = {
    testRequestId: "req-abc123",
    lotId: "lot-xyz789",
  };

  beforeEach(() => {
    mockNavigate.mockClear();
  });

  describe("PENDING status", () => {
    it("renders 'Ghi nhận kết quả' text button", () => {
      render(
        <InspectionRequestActionButtons
          {...defaultProps}
          status="PENDING"
        />,
      );

      const button = screen.getByRole("button", { name: /ghi nhận kết quả/i });
      expect(button).toBeInTheDocument();
      expect(button).toHaveTextContent("Ghi nhận kết quả");
    });

    it("navigates to results page on click", () => {
      render(
        <InspectionRequestActionButtons
          {...defaultProps}
          status="PENDING"
        />,
      );

      fireEvent.click(screen.getByRole("button", { name: /ghi nhận kết quả/i }));

      expect(mockNavigate).toHaveBeenCalledWith(
        "/production-lots/lot-xyz789/inspection-requests/req-abc123/results",
      );
    });
  });

  describe("PASSED status", () => {
    it("renders Eye icon button with 'Xem chi tiết' tooltip", () => {
      render(
        <InspectionRequestActionButtons
          {...defaultProps}
          status="PASSED"
        />,
      );

      const button = screen.getByRole("button", { name: /xem chi tiết/i });
      expect(button).toBeInTheDocument();
      // Eye icon should be rendered (svg inside button)
      expect(button.querySelector("svg")).toBeInTheDocument();
    });

    it("navigates to results page on click", () => {
      render(
        <InspectionRequestActionButtons
          {...defaultProps}
          status="PASSED"
        />,
      );

      fireEvent.click(screen.getByRole("button", { name: /xem chi tiết/i }));

      expect(mockNavigate).toHaveBeenCalledWith(
        "/production-lots/lot-xyz789/inspection-requests/req-abc123/results",
      );
    });
  });

  describe("FAILED status", () => {
    it("renders Eye icon button with 'Xem chi tiết' tooltip", () => {
      render(
        <InspectionRequestActionButtons
          {...defaultProps}
          status="FAILED"
        />,
      );

      const button = screen.getByRole("button", { name: /xem chi tiết/i });
      expect(button).toBeInTheDocument();
      expect(button.querySelector("svg")).toBeInTheDocument();
    });

    it("navigates to results page on click", () => {
      render(
        <InspectionRequestActionButtons
          {...defaultProps}
          status="FAILED"
        />,
      );

      fireEvent.click(screen.getByRole("button", { name: /xem chi tiết/i }));

      expect(mockNavigate).toHaveBeenCalledWith(
        "/production-lots/lot-xyz789/inspection-requests/req-abc123/results",
      );
    });
  });

  describe("CANCELLED status", () => {
    it("renders nothing (no action available)", () => {
      const { container } = render(
        <InspectionRequestActionButtons
          {...defaultProps}
          status="CANCELLED"
        />,
      );

      expect(container.innerHTML).toBe("");
      expect(screen.queryByRole("button")).not.toBeInTheDocument();
    });
  });

  describe("navigation URL construction", () => {
    it("constructs correct URL with lotId and testRequestId", () => {
      render(
        <InspectionRequestActionButtons
          testRequestId="test-456"
          lotId="lot-100"
          status="PENDING"
        />,
      );

      fireEvent.click(screen.getByRole("button", { name: /ghi nhận kết quả/i }));

      expect(mockNavigate).toHaveBeenCalledWith(
        "/production-lots/lot-100/inspection-requests/test-456/results",
      );
    });
  });
});
