import { Info } from "lucide-react";
import { Button } from "zmp-ui";

/** Hiển thị khi người dùng từ chối quyền: giải thích lý do + cho phép thử lại. */
export function PermissionPrompt({ title, reason, onRetry, onDismiss }: { title: string; reason: string; onRetry: () => void; onDismiss?: () => void }) {
  return (
    <div className="rounded-card border border-amber-200 bg-amber-50 p-3 text-sm" role="status" data-testid="permission-denied">
      <div className="flex gap-2 items-start">
        <Info size={20} className="text-warning shrink-0" />
        <div className="flex-1">
          <div className="font-medium text-ink">{title}</div>
          <div className="text-ink-muted mt-0.5">{reason}</div>
          <div className="flex gap-2 mt-2">
            <Button size="small" onClick={onRetry}>Cho phép lại</Button>
            {onDismiss && <Button size="small" variant="tertiary" onClick={onDismiss}>Để sau</Button>}
          </div>
        </div>
      </div>
    </div>
  );
}
