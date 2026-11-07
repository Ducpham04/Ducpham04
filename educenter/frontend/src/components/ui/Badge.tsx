import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import type { Tone } from "@/lib/labels";

const tones: Record<Tone, string> = {
  neutral: "bg-surface-2 text-ink-2",
  primary: "bg-primary-soft text-primary-strong",
  ok: "bg-ok-soft text-ok",
  warn: "bg-warn-soft text-warn",
  danger: "bg-danger-soft text-danger",
  info: "bg-info-soft text-info",
};

/** Hình dạng chấm khác nhau theo tone để không phụ thuộc chỉ vào màu. */
const marks: Record<Tone, string> = {
  neutral: "rounded-full border border-current bg-transparent",
  primary: "rounded-full bg-current",
  ok: "rounded-full bg-current",
  warn: "rotate-45 rounded-[1px] bg-current",
  danger: "rounded-[1px] bg-current",
  info: "rounded-full border-2 border-current bg-transparent",
};

export function Badge({ tone = "neutral", children, className }: { tone?: Tone; children: ReactNode; className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-0.5 text-[12.5px] font-medium", tones[tone], className)}>
      <span aria-hidden className={cn("size-1.5 shrink-0", marks[tone])} />
      {children}
    </span>
  );
}

export function StatusChip<K extends string>({ map, value }: { map: Record<K, { label: string; tone: Tone }>; value: K }) {
  const item = map[value];
  if (!item) return null;
  return <Badge tone={item.tone}>{item.label}</Badge>;
}
