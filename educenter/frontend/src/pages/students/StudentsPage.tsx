import { useState } from "react";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Plus } from "lucide-react";
import { api } from "@/api/client";
import type { Page, StudentListItem, StudentStatus } from "@/api/types";
import { Button, Card, EmptyState, ErrorState, ListSkeleton, PageHeader, Pagination, SearchInput, StatusChip, Tabs } from "@/components/ui";
import { STUDENT_STATUS } from "@/lib/labels";
import { formatDate } from "@/lib/format";
import { StudentFormSheet } from "./StudentForm";

type Filter = "ALL" | StudentStatus;

export default function StudentsPage() {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const q = params.get("q") ?? "";
  const status = (params.get("status") as Filter) ?? "ALL";
  const page = Number(params.get("page") ?? 0);
  const [creating, setCreating] = useState(false);

  const update = (patch: Record<string, string | null>) => {
    const next = new URLSearchParams(params);
    Object.entries(patch).forEach(([k, v]) => (v === null || v === "" ? next.delete(k) : next.set(k, v)));
    setParams(next, { replace: true });
  };

  const query = useQuery({
    queryKey: ["students", q, status, page],
    queryFn: () => api<Page<StudentListItem>>("/students", { query: { q, status: status === "ALL" ? undefined : status, page, size: 20 } }),
    placeholderData: keepPreviousData,
  });

  return (
    <>
      <PageHeader title="Học viên" description="Hồ sơ, phụ huynh và trạng thái học tập" actions={<Button icon={<Plus className="size-4" />} onClick={() => setCreating(true)}>Thêm học viên</Button>} />
      <Card>
        <div className="flex flex-col gap-3 px-4 pt-4 sm:px-5">
          <SearchInput value={q} onChange={(v) => update({ q: v, page: null })} placeholder="Tên, SĐT hoặc mã học viên" />
          <Tabs<Filter>
            value={status}
            onChange={(v) => update({ status: v === "ALL" ? null : v, page: null })}
            items={[
              { value: "ALL", label: "Tất cả" },
              { value: "ACTIVE", label: "Đang học" },
              { value: "TRIAL", label: "Học thử" },
              { value: "ON_HOLD", label: "Bảo lưu" },
              { value: "DROPPED", label: "Đã nghỉ" },
            ]}
          />
        </div>
        {query.isLoading ? <ListSkeleton rows={8} /> : query.isError ? <ErrorState error={query.error} onRetry={() => query.refetch()} /> : query.data!.items.length === 0 ? (
          <EmptyState
            title={q ? `Không tìm thấy học viên khớp "${q}"` : "Chưa có học viên"}
            description={q ? "Thử tìm bằng số điện thoại hoặc mã HV." : "Thêm học viên đầu tiên để bắt đầu xếp lớp."}
            action={!q && <Button onClick={() => setCreating(true)} icon={<Plus className="size-4" />}>Thêm học viên</Button>}
          />
        ) : (
          <>
            <table className="hidden w-full text-left text-sm md:table">
              <thead className="bg-surface-2 text-[13px] text-ink-2">
                <tr>
                  <th className="px-5 py-2.5 font-medium">Học viên</th>
                  <th className="px-3 py-2.5 font-medium">Phụ huynh</th>
                  <th className="px-3 py-2.5 font-medium">Ngày nhập học</th>
                  <th className="px-5 py-2.5 font-medium">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {query.data!.items.map((s) => (
                  <tr key={s.id} className="cursor-pointer hover:bg-surface-2" onClick={() => navigate(`/app/students/${s.id}`)}>
                    <td className="px-5 py-3">
                      <Link to={`/app/students/${s.id}`} className="font-medium text-ink hover:text-primary" onClick={(e) => e.stopPropagation()}>{s.fullName}</Link>
                      <p className="tabular text-[12.5px] text-ink-2">{s.code}{s.phone && ` · ${s.phone}`}</p>
                    </td>
                    <td className="px-3 py-3">
                      {s.primaryGuardian ? <><p>{s.primaryGuardian}</p><p className="tabular text-[12.5px] text-ink-2">{s.guardianPhone}</p></> : <span className="text-ink-3">—</span>}
                    </td>
                    <td className="tabular px-3 py-3 text-ink-2">{formatDate(s.joinedAt)}</td>
                    <td className="px-5 py-3"><StatusChip map={STUDENT_STATUS} value={s.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
            <ul className="divide-y divide-line border-t border-line md:hidden">
              {query.data!.items.map((s) => (
                <li key={s.id}>
                  <Link to={`/app/students/${s.id}`} className="flex items-center gap-3 px-4 py-3 active:bg-surface-2">
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-medium">{s.fullName}</p>
                      <p className="tabular truncate text-[12.5px] text-ink-2">{s.code} · {s.phone ?? s.guardianPhone ?? "chưa có SĐT"}</p>
                    </div>
                    <StatusChip map={STUDENT_STATUS} value={s.status} />
                  </Link>
                </li>
              ))}
            </ul>
            <Pagination page={query.data!.page} totalPages={query.data!.totalPages} totalItems={query.data!.totalItems} onChange={(p) => update({ page: String(p) })} />
          </>
        )}
      </Card>
      <StudentFormSheet open={creating} onClose={() => setCreating(false)} onSaved={(s) => navigate(`/app/students/${s.id}`)} />
    </>
  );
}
