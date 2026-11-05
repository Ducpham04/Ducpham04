import { Inbox, RotateCw, TriangleAlert } from "lucide-react";
import { Button } from "zmp-ui";

export function Skeleton({ className = "" }: { className?: string }) {
  return <div className={`skeleton ${className}`} aria-hidden="true" />;
}

export function ListSkeleton({ rows = 3 }: { rows?: number }) {
  return (
    <div className="space-y-3" data-testid="loading">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="card p-3 flex gap-3">
          <Skeleton className="w-16 h-16 shrink-0" />
          <div className="flex-1 space-y-2 py-1">
            <Skeleton className="h-4 w-4/5" />
            <Skeleton className="h-3 w-1/2" />
          </div>
        </div>
      ))}
    </div>
  );
}

export function EmptyState({ title, description, action }: { title: string; description?: string; action?: { label: string; onClick: () => void } }) {
  return (
    <div className="flex flex-col items-center text-center py-10 px-6" data-testid="empty">
      <div className="w-16 h-16 rounded-full bg-primary-soft text-primary flex items-center justify-center mb-3">
        <Inbox size={32} />
      </div>
      <div className="font-semibold text-base">{title}</div>
      {description && <div className="text-sm text-ink-muted mt-1">{description}</div>}
      {action && (
        <Button size="small" className="mt-4" onClick={action.onClick}>
          {action.label}
        </Button>
      )}
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="flex flex-col items-center text-center py-10 px-6" role="alert" data-testid="error">
      <div className="w-16 h-16 rounded-full bg-red-50 text-danger flex items-center justify-center mb-3">
        <TriangleAlert size={32} />
      </div>
      <div className="font-semibold">Không tải được dữ liệu</div>
      <div className="text-sm text-ink-muted mt-1">{message}</div>
      {onRetry && (
        <Button size="small" variant="secondary" className="mt-4" onClick={onRetry} prefixIcon={<RotateCw size={20} />}>
          Thử lại
        </Button>
      )}
    </div>
  );
}
