import { INVOICE_STATUS, invoiceStatusKey } from "./labels";

describe("labels", () => {
  it("shows overdue over raw status", () => {
    expect(invoiceStatusKey({ status: "PARTIAL", overdue: true })).toBe("OVERDUE");
    expect(invoiceStatusKey({ status: "PAID", overdue: false })).toBe("PAID");
    expect(INVOICE_STATUS.OVERDUE.label).toBe("Quá hạn");
  });
});
