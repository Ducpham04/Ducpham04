import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { api } from "@/api/client";
import type { SessionView } from "@/api/types";
import { Button, Card, EmptyState, ErrorState, ListSkeleton, PageHeader } from "@/components/ui";
import { SessionRow } from "@/components/domain/SessionRow";
import { addDays, DAY_LONG, formatDate, isoDayOfWeek, todayIso } from "@/lib/format";
import { useAuth } from "@/lib/auth";

/** "Hôm nay" (giáo viên) và "Điểm danh" (giáo vụ): danh sách buổi học theo ngày + 7 ngày tới. */
export default function SessionsDayPage() {
  const { user } = useAuth();
  const isTeacher = user?.role === "TEACHER";
  const [date, setDate] = useState(todayIso());
  const today = todayIso();
  const day = useQuery({ queryKey: ["sessions", date, date], queryFn: () => api<SessionView[]>("/sessions", { query: { from: date, to: date } }) });
  const upcoming = useQuery({
    queryKey: ["sessions", addDays(today, 1), addDays(today, 7)],
    queryFn: () => api<SessionView[]>("/sessions", { query: { from: addDays(today, 1), to: addDays(today, 7) } }),
    enabled: isTeacher,
  });

  const label = date === today ? "Hôm nay" : date === addDays(today, -1) ? "Hôm qua" : DAY_LONG[isoDayOfWeek(date)];
  const pending = day.data?.filter((s) => s.status === "SCHEDULED" && s.date <= today).length ?? 0;

  return (
    <>
      <PageHeader
        title={isTeacher ? "Buổi dạy hôm nay" : "Điểm danh"}
        description={isTeacher ? `${DAY_LONG[isoDayOfWeek(today)]}, ${formatDate(today)}` : "Chọn buổi học để điểm danh hoặc xem lại"}
      />
      <Card>
        <div className="flex items-center justify-between gap-2 border-b border-line px-3 py-2 sm:px-4">
          <Button variant="ghost" size="sm" onClick={() => setDate(addDays(date, -1))} aria-label="Ngày trước" icon={<ChevronLeft className="size-4" />} />
          <div className="text-center">
            <p className="text-[15px] font-semibold">{label}</p>
            <p className="tabular text-[12.5px] text-ink-2">{formatDate(date)}{pending > 0 && ` · ${pending} buổi chưa điểm danh`}</p>
          </div>
          <Button variant="ghost" size="sm" onClick={() => setDate(addDays(date, 1))} aria-label="Ngày sau" icon={<ChevronRight className="size-4" />} />
        </div>
        {day.isLoading ? <ListSkeleton rows={3} /> : day.isError ? <ErrorState error={day.error} onRetry={() => day.refetch()} /> : day.data!.length ? (
          <ul className="divide-y divide-line">{day.data!.map((s) => <SessionRow key={s.id} s={s} showTeacher={!isTeacher} showAction={s.date <= today} />)}</ul>
        ) : (
          <EmptyState title="Không có buổi học" description="Ngày này không có lớp nào trên lịch." action={date !== today ? <Button variant="secondary" size="sm" onClick={() => setDate(today)}>Về hôm nay</Button> : undefined} />
        )}
      </Card>

      {isTeacher && (
        <section className="mt-8">
          <h2 className="mb-3 text-lg font-semibold">7 ngày tới</h2>
          <Card>
            {upcoming.isLoading ? <ListSkeleton rows={4} /> : upcoming.data?.length ? (
              <ul className="divide-y divide-line">
                {upcoming.data.map((s, i, arr) => (
                  <div key={s.id}>
                    {(i === 0 || arr[i - 1].date !== s.date) && (
                      <p className="bg-surface-2 px-4 py-1.5 text-[12.5px] font-medium text-ink-2 sm:px-5">{DAY_LONG[isoDayOfWeek(s.date)]}, {formatDate(s.date)}</p>
                    )}
                    <SessionRow s={s} showTeacher={false} showAction={false} />
                  </div>
                ))}
              </ul>
            ) : <EmptyState title="Không có buổi dạy trong 7 ngày tới" />}
          </Card>
        </section>
      )}
    </>
  );
}
