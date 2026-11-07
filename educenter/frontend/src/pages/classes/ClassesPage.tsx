import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";
import { Plus } from "lucide-react";
import { api } from "@/api/client";
import type { ClassListItem, ClassStatus } from "@/api/types";
import { Button, Card, EmptyState, ErrorState, ListSkeleton, PageHeader, SearchInput, StatusChip, Tabs } from "@/components/ui";
import { CLASS_STATUS } from "@/lib/labels";
import { formatDate, scheduleSummary } from "@/lib/format";
import { useAuth } from "@/lib/auth";
import { ClassFormSheet } from "./ClassForm";

type Filter = "ALL" | ClassStatus;

function Capacity({ n, max }: { n: number; max: number }) {
  const pct = Math.min(100, Math.round((n / max) * 100));
  return (
    <div className="w-20">
      <p className="tabular text-sm"><span className="font-semibold">{n}</span><span className="text-ink-2">/{max}</span></p>
      <div className="mt-1 h-1 rounded-full bg-surface-2" aria-hidden><div className={pct >= 100 ? "h-1 rounded-full bg-warn" : "h-1 rounded-full bg-primary"} style={{ width: `${pct}%` }} /></div>
    </div>
  );
}

export default function ClassesPage() {
  const { hasRole } = useAuth();
  const canEdit = hasRole("CENTER_ADMIN", "STAFF");
  const navigate = useNavigate();
  const [q, setQ] = useState("");
  const [status, setStatus] = useState<Filter>("ALL");
  const [creating, setCreating] = useState(false);
  const query = useQuery({
    queryKey: ["classes", q, status],
    queryFn: () => api<ClassListItem[]>("/classes", { query: { q, status: status === "ALL" ? undefined : status } }),
  });

  return (
    <>
      <PageHeader title={canEdit ? "Lớp học" : "Lớp của tôi"} description="Sĩ số, giáo viên, phòng và lịch học theo tuần"
        actions={canEdit && <Button icon={<Plus className="size-4" />} onClick={() => setCreating(true)}>Tạo lớp</Button>} />
      <Card>
        <div className="flex flex-col gap-3 px-4 pt-4 sm:px-5">
          <SearchInput value={q} onChange={setQ} placeholder="Tên hoặc mã lớp" />
          <Tabs<Filter> value={status} onChange={setStatus} items={[
            { value: "ALL", label: "Tất cả" }, { value: "ONGOING", label: "Đang học" }, { value: "PLANNED", label: "Sắp mở" }, { value: "FINISHED", label: "Đã kết thúc" },
          ]} />
        </div>
        {query.isLoading ? <ListSkeleton /> : query.isError ? <ErrorState error={query.error} onRetry={() => query.refetch()} /> : !query.data!.length ? (
          <EmptyState title="Chưa có lớp nào" description={canEdit ? "Tạo lớp, chọn giáo viên, phòng rồi đặt lịch tuần." : "Bạn chưa được phân công lớp nào."}
            action={canEdit && <Button icon={<Plus className="size-4" />} onClick={() => setCreating(true)}>Tạo lớp</Button>} />
        ) : (
          <>
            <table className="hidden w-full text-left text-sm lg:table">
              <thead className="bg-surface-2 text-[13px] text-ink-2">
                <tr>
                  <th className="px-5 py-2.5 font-medium">Lớp</th>
                  <th className="px-3 py-2.5 font-medium">Giáo viên · Phòng</th>
                  <th className="px-3 py-2.5 font-medium">Lịch học</th>
                  <th className="px-3 py-2.5 font-medium">Sĩ số</th>
                  <th className="px-5 py-2.5 font-medium">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {query.data!.map((c) => (
                  <tr key={c.id} className="cursor-pointer hover:bg-surface-2" onClick={() => navigate(`/app/classes/${c.id}`)}>
                    <td className="px-5 py-3">
                      <Link to={`/app/classes/${c.id}`} onClick={(e) => e.stopPropagation()} className="font-medium hover:text-primary">{c.name}</Link>
                      <p className="text-[12.5px] text-ink-2">{c.code} · {c.courseName}</p>
                    </td>
                    <td className="px-3 py-3"><p>{c.teacherName ?? <span className="text-ink-3">Chưa phân công</span>}</p><p className="text-[12.5px] text-ink-2">{c.roomName ?? "Chưa xếp phòng"}</p></td>
                    <td className="tabular px-3 py-3"><p>{scheduleSummary(c.schedules)}</p><p className="text-[12.5px] text-ink-2">{formatDate(c.startDate)} → {formatDate(c.endDate)}</p></td>
                    <td className="px-3 py-3"><Capacity n={c.enrolledCount} max={c.maxStudents} /></td>
                    <td className="px-5 py-3"><StatusChip map={CLASS_STATUS} value={c.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
            <ul className="divide-y divide-line border-t border-line lg:hidden">
              {query.data!.map((c) => (
                <li key={c.id}>
                  <Link to={`/app/classes/${c.id}`} className="flex items-center gap-3 px-4 py-3 active:bg-surface-2">
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-medium">{c.name}</p>
                      <p className="tabular truncate text-[12.5px] text-ink-2">{scheduleSummary(c.schedules)}</p>
                      <p className="truncate text-[12.5px] text-ink-2">{c.teacherName ?? "Chưa phân công"} · {c.roomName ?? "—"}</p>
                    </div>
                    <div className="flex flex-col items-end gap-1.5"><StatusChip map={CLASS_STATUS} value={c.status} /><span className="tabular text-[13px] text-ink-2">{c.enrolledCount}/{c.maxStudents} HV</span></div>
                  </Link>
                </li>
              ))}
            </ul>
          </>
        )}
      </Card>
      {canEdit && <ClassFormSheet open={creating} onClose={() => setCreating(false)} onSaved={(c) => navigate(`/app/classes/${c.id}?tab=schedule`)} />}
    </>
  );
}
