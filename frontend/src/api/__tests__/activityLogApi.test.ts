import { describe, expect, it } from "vitest";
import { getActivityLogApiError } from "../activityLogApi";

describe("activityLogApi", () => {
  it("đọc đúng ApiResult lỗi khi responseType blob được dùng cho export", async () => {
    const error = {
      response: {
        data: new Blob([
          JSON.stringify({ message: "Không có nhật ký hoạt động trong phạm vi lọc." }),
        ], { type: "application/json" }),
      },
    };

    await expect(getActivityLogApiError(error, "Lỗi mặc định"))
      .resolves.toBe("Không có nhật ký hoạt động trong phạm vi lọc.");
  });

  it("dùng fallback khi blob lỗi không chứa JSON hợp lệ", async () => {
    const error = { response: { data: new Blob(["not-json"]) } };
    await expect(getActivityLogApiError(error, "Lỗi mặc định"))
      .resolves.toBe("Lỗi mặc định");
  });
});
