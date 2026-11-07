import { Search } from "lucide-react";
import { useEffect, useState } from "react";

export function SearchInput({ value, onChange, placeholder = "Tìm kiếm…", label = "Tìm kiếm" }: { value: string; onChange: (v: string) => void; placeholder?: string; label?: string }) {
  const [local, setLocal] = useState(value);
  useEffect(() => setLocal(value), [value]);
  useEffect(() => {
    const t = setTimeout(() => local !== value && onChange(local), 300);
    return () => clearTimeout(t);
  }, [local, value, onChange]);
  return (
    <div className="relative w-full sm:max-w-xs">
      <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ink-3" aria-hidden />
      <input
        type="search"
        aria-label={label}
        value={local}
        onChange={(e) => setLocal(e.target.value)}
        placeholder={placeholder}
        className="h-11 w-full rounded-lg border border-line-strong bg-surface pl-9 pr-3 text-ink placeholder:text-ink-3 focus:border-primary focus:outline-none focus:ring-[3px] focus:ring-primary/20 sm:h-10"
      />
    </div>
  );
}
