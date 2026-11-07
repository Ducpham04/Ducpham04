import { Link } from "react-router-dom";
import type { Invoice } from "@/api/types";
import { formatDate, formatMoney } from "@/lib/format";
import { INVOICE_STATUS, invoiceStatusKey } from "@/lib/labels";
import { StatusChip } from "@/components/ui";

export function InvoiceList({ invoices, showStudent = true, linkTo = (i: Invoice) => `/app/billing/invoices/${i.id}` }: { invoices: Invoice[]; showStudent?: boolean; linkTo?: ((i: Invoice) => string) | null }) {
  return (
    <ul className="divide-y divide-line">
      {invoices.map((i) => {
        const content = (
          <>
            <div className="min-w-0 flex-1">
              <p className="truncate font-medium">{showStudent ? i.studentName : i.description}</p>
              <p className="tabular truncate text-[12.5px] text-ink-2">
                {i.code} · {showStudent ? i.description : `hạn ${formatDate(i.dueDate)}`}{showStudent && ` · hạn ${formatDate(i.dueDate)}`}
              </p>
            </div>
            <div className="tabular text-right">
              <p className="font-semibold">{formatMoney(i.status === "PAID" || i.status === "CANCELLED" ? i.total : i.remaining)}</p>
              <p className="text-[12px] text-ink-2">{i.status === "PAID" ? "đã thu" : i.status === "CANCELLED" ? "đã hủy" : `còn lại / ${formatMoney(i.total)}`}</p>
            </div>
            <div className="hidden w-28 justify-end sm:flex"><StatusChip map={INVOICE_STATUS} value={invoiceStatusKey(i)} /></div>
          </>
        );
        return (
          <li key={i.id}>
            {linkTo ? (
              <Link to={linkTo(i)} className="flex items-center gap-3 px-4 py-3 hover:bg-surface-2 sm:px-5">{content}</Link>
            ) : <div className="flex items-center gap-3 px-4 py-3 sm:px-5">{content}</div>}
          </li>
        );
      })}
    </ul>
  );
}
