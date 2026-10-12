import { describe, expect, it } from "vitest";
import { z } from "zod";

const loginSchema = z.object({
  username: z.string().min(1),
  password: z.string().min(1),
});

describe("login validation", () => {
  it("rejects empty credentials", () => {
    const result = loginSchema.safeParse({ username: "", password: "" });
    expect(result.success).toBe(false);
  });

  it("accepts valid credentials", () => {
    const result = loginSchema.safeParse({ username: "admin", password: "Admin@123" });
    expect(result.success).toBe(true);
  });
});
