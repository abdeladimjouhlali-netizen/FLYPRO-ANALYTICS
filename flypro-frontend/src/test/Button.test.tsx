import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { Button } from "@/components/ui/Button";
import { cn } from "@/utils/cn";

describe("cn utility", () => {
  it("merges class names", () => {
    expect(cn("px-2", "px-4")).toContain("px-4");
  });
});

describe("Button", () => {
  it("renders label", () => {
    render(<Button>Enregistrer</Button>);
    expect(screen.getByRole("button", { name: "Enregistrer" })).toBeInTheDocument();
  });
});
