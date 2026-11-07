import { addDays, formatDate, formatMoney, formatMoneyShort, initials, isoDayOfWeek, scheduleSummary, startOfWeek } from "./format";

describe("format", () => {
  it("formats VND", () => {
    expect(formatMoney(1250000)).toBe("1.250.000 ₫");
    expect(formatMoney(null)).toBe("0 ₫");
    expect(formatMoneyShort(34_350_000)).toBe("34,4 tr");
  });

  it("formats ISO dates without timezone drift", () => {
    expect(formatDate("2026-11-05")).toBe("05/11/2026");
    expect(formatDate(null)).toBe("—");
  });

  it("computes week days (T2 = 1, CN = 7)", () => {
    expect(isoDayOfWeek("2026-11-02")).toBe(1);
    expect(isoDayOfWeek("2026-11-08")).toBe(7);
    expect(startOfWeek("2026-11-05")).toBe("2026-11-02");
    expect(addDays("2026-12-31", 1)).toBe("2027-01-01");
  });

  it("summarises weekly schedules", () => {
    expect(scheduleSummary([
      { dayOfWeek: 5, startTime: "18:00:00", endTime: "19:30:00" },
      { dayOfWeek: 1, startTime: "18:00:00", endTime: "19:30:00" },
      { dayOfWeek: 3, startTime: "18:00:00", endTime: "19:30:00" },
    ])).toBe("T2·T4·T6 18:00–19:30");
    expect(scheduleSummary([])).toBe("Chưa có lịch");
  });

  it("builds initials from Vietnamese names", () => {
    expect(initials("Nguyễn Thị Lan")).toBe("NL");
    expect(initials("David")).toBe("D");
  });
});
