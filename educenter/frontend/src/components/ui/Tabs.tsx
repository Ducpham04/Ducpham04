import { cn } from "@/lib/cn";

export function Tabs<T extends string>({ value, onChange, items, className }: {
  value: T;
  onChange: (v: T) => void;
  items: { value: T; label: string; count?: number }[];
  className?: string;
}) {
  return (
    <div role="tablist" className={cn("-mx-4 flex gap-1 overflow-x-auto border-b border-line px-4 [scrollbar-width:none] sm:mx-0 sm:px-0", className)}>
      {items.map((it) => {
        const active = it.value === value;
        return (
          <button
            key={it.value}
            role="tab"
            aria-selected={active}
            onClick={() => onChange(it.value)}
            className={cn(
              "relative -mb-px flex h-11 shrink-0 items-center gap-1.5 border-b-2 px-3 text-sm font-medium transition-colors duration-[120ms]",
              active ? "border-primary text-primary" : "border-transparent text-ink-2 hover:text-ink",
            )}
          >
            {it.label}
            {it.count !== undefined && (
              <span className={cn("tabular rounded-full px-1.5 text-[12px]", active ? "bg-primary-soft" : "bg-surface-2")}>{it.count}</span>
            )}
          </button>
        );
      })}
    </div>
  );
}
