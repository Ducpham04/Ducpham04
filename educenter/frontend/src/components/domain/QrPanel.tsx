import { useState } from "react";
import { Check, Copy } from "lucide-react";
import type { VietQr } from "@/api/types";
import { Card, CardBody, CardHeader } from "@/components/ui";
import { formatMoney } from "@/lib/format";

function CopyRow({ label, value, display }: { label: string; value: string; display?: string }) {
  const [copied, setCopied] = useState(false);
  return (
    <div className="flex items-center justify-between gap-3 py-2">
      <div className="min-w-0">
        <p className="text-[12.5px] text-ink-2">{label}</p>
        <p className="tabular truncate text-sm font-medium">{display ?? value}</p>
      </div>
      <button
        type="button"
        onClick={async () => { try { await navigator.clipboard.writeText(value); setCopied(true); setTimeout(() => setCopied(false), 1500); } catch { /* ignore */ } }}
        className="grid size-9 shrink-0 place-items-center rounded-lg text-ink-2 hover:bg-surface-2 hover:text-ink"
        aria-label={`Sao chép ${label}`}
      >
        {copied ? <Check className="size-4 text-ok" /> : <Copy className="size-4" />}
      </button>
    </div>
  );
}

export function QrPanel({ qr, title = "Chuyển khoản bằng VietQR" }: { qr: VietQr; title?: string }) {
  return (
    <Card>
      <CardHeader title={title} description="Quét bằng app ngân hàng bất kỳ – số tiền và nội dung được điền sẵn" />
      <CardBody>
        <div className="mx-auto w-full max-w-[240px] rounded-xl border border-line bg-white p-2">
          <img src={qr.imageUrl} alt={`Mã QR chuyển ${formatMoney(qr.amount)} tới ${qr.bankName}`} className="aspect-square w-full" loading="lazy" />
        </div>
        <div className="mt-4 divide-y divide-line">
          <CopyRow label="Ngân hàng" value={qr.bankName} />
          <CopyRow label="Số tài khoản" value={qr.accountNo} />
          <CopyRow label="Chủ tài khoản" value={qr.accountName ?? ""} />
          <CopyRow label="Số tiền" value={String(qr.amount)} display={formatMoney(qr.amount)} />
          <CopyRow label="Nội dung chuyển khoản" value={qr.note} />
        </div>
      </CardBody>
    </Card>
  );
}
