import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { RiskBadge, RiskFactors } from "./components";

describe("RiskBadge", () => {
  it("shows the level and the score as a percentage", () => {
    render(<RiskBadge level="HIGH" score={0.934} />);
    expect(screen.getByText("HIGH 93%")).toBeDefined();
  });

  it("handles events recorded before scoring existed", () => {
    render(<RiskBadge level={undefined} />);
    expect(screen.getByText("not scored")).toBeDefined();
  });
});

describe("RiskFactors", () => {
  it("renders labels as text, never as HTML", () => {
    const { container } = render(
      <RiskFactors factors={[{ feature: "x", label: "<img src=x onerror=alert(1)>", value: 1.2 }]} />,
    );
    expect(container.querySelector("img")).toBeNull();
    expect(screen.getByText("<img src=x onerror=alert(1)>")).toBeDefined();
  });

  it("scales bars to the strongest factor and colours them by direction", () => {
    const { container } = render(
      <RiskFactors
        factors={[
          { feature: "a", label: "Raises", value: 2 },
          { feature: "b", label: "Lowers", value: -1 },
        ]}
      />,
    );
    const bars = container.querySelectorAll<HTMLElement>(".factor-bar");
    expect(bars[0]?.style.width).toBe("100%");
    expect(bars[0]?.className).toContain("risk");
    expect(bars[1]?.style.width).toBe("50%");
    expect(bars[1]?.className).toContain("safe");
  });
});
