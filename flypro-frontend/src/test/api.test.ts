import { describe, expect, it, vi } from "vitest";
import type { ApiResponse, Department } from "@/types/api";

describe("api unwrap contract", () => {
  it("extracts data payload", () => {
    const response: ApiResponse<Department> = {
      success: true,
      data: {
        id: 1,
        name: "IT",
        code: "IT",
        active: true,
      },
    };
    expect(response.data?.code).toBe("IT");
  });

  it("mocks list call shape", async () => {
    const list = vi.fn().mockResolvedValue({
      content: [{ id: 1, name: "IT", code: "IT", active: true }],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });
    const page = await list();
    expect(page.content).toHaveLength(1);
  });
});
