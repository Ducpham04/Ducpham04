import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Pencil, Plus } from "lucide-react";
import { api } from "@/api/client";
import { useCourses, useRooms } from "@/api/hooks";
import type { Course, Room } from "@/api/types";
import { Badge, Button, Card, CardHeader, EmptyState, Input, ListSkeleton, PageHeader, Sheet, Textarea } from "@/components/ui";
import { formatMoney } from "@/lib/format";

const courseSchema = z.object({
  code: z.string().trim().regex(/^[A-Za-z0-9._-]{2,20}$/, "Mã 2–20 ký tự, không dấu, không khoảng trắng"),
  name: z.string().trim().min(2, "Vui lòng nhập tên khóa học"),
  level: z.string().optional(),
  totalSessions: z.string(),
  tuitionFee: z.string().refine((v) => Number(v.replace(/\D/g, "")) >= 0, "Học phí không hợp lệ"),
  description: z.string().max(1000).optional(),
  active: z.boolean(),
});
const roomSchema = z.object({
  name: z.string().trim().min(1, "Vui lòng nhập tên phòng"),
  capacity: z.string().refine((v) => Number(v) >= 1, "Sức chứa tối thiểu 1"),
  active: z.boolean(),
});

export default function CatalogPage() {
  const courses = useCourses();
  const rooms = useRooms();
  const [course, setCourse] = useState<Course | "new" | null>(null);
  const [room, setRoom] = useState<Room | "new" | null>(null);
  return (
    <>
      <PageHeader title="Khóa học & phòng học" description="Danh mục dùng khi tạo lớp và gói học phí" />
      <div className="grid gap-5 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <CardHeader title="Khóa học" action={<Button size="sm" icon={<Plus className="size-4" />} onClick={() => setCourse("new")}>Thêm khóa</Button>} />
          {courses.isLoading ? <ListSkeleton /> : !courses.data?.length ? <EmptyState title="Chưa có khóa học" /> : (
            <ul className="divide-y divide-line border-t border-line">
              {courses.data.map((c) => (
                <li key={c.id} className="flex items-center gap-3 px-4 py-3 sm:px-5">
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium">{c.name}</p>
                    <p className="truncate text-[12.5px] text-ink-2">{c.code}{c.level && ` · ${c.level}`} · {c.totalSessions} buổi</p>
                  </div>
                  {!c.active && <Badge>Ngừng mở</Badge>}
                  <p className="tabular text-sm font-semibold">{formatMoney(c.tuitionFee)}</p>
                  <Button size="sm" variant="ghost" aria-label={`Sửa ${c.name}`} icon={<Pencil className="size-4" />} onClick={() => setCourse(c)} />
                </li>
              ))}
            </ul>
          )}
        </Card>
        <Card className="lg:col-span-2">
          <CardHeader title="Phòng học" action={<Button size="sm" icon={<Plus className="size-4" />} onClick={() => setRoom("new")}>Thêm phòng</Button>} />
          {rooms.isLoading ? <ListSkeleton /> : !rooms.data?.length ? <EmptyState title="Chưa có phòng học" /> : (
            <ul className="divide-y divide-line border-t border-line">
              {rooms.data.map((r) => (
                <li key={r.id} className="flex items-center gap-3 px-4 py-3 sm:px-5">
                  <p className="flex-1 font-medium">{r.name}</p>
                  {!r.active && <Badge>Tạm đóng</Badge>}
                  <p className="tabular text-sm text-ink-2">{r.capacity} chỗ</p>
                  <Button size="sm" variant="ghost" aria-label={`Sửa ${r.name}`} icon={<Pencil className="size-4" />} onClick={() => setRoom(r)} />
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
      <CourseSheet value={course} onClose={() => setCourse(null)} />
      <RoomSheet value={room} onClose={() => setRoom(null)} />
    </>
  );
}

function CourseSheet({ value, onClose }: { value: Course | "new" | null; onClose: () => void }) {
  const qc = useQueryClient();
  const c = value && value !== "new" ? value : null;
  const { register, handleSubmit, formState: { errors } } = useForm<z.infer<typeof courseSchema>>({
    resolver: zodResolver(courseSchema),
    values: { code: c?.code ?? "", name: c?.name ?? "", level: c?.level ?? "", totalSessions: String(c?.totalSessions ?? 24), tuitionFee: String(c?.tuitionFee ?? ""), description: c?.description ?? "", active: c?.active ?? true },
  });
  const m = useMutation({
    mutationFn: (v: z.infer<typeof courseSchema>) => {
      const body = { ...v, totalSessions: Number(v.totalSessions) || 0, tuitionFee: Number(v.tuitionFee.replace(/\D/g, "")) || 0, level: v.level || null, description: v.description || null };
      return c ? api(`/courses/${c.id}`, { method: "PUT", body }) : api("/courses", { method: "POST", body });
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["courses"] }); toast.success("Đã lưu khóa học"); onClose(); },
    onError: (e) => toast.error(e.message),
  });
  return (
    <Sheet open={!!value} onClose={onClose} title={c ? "Sửa khóa học" : "Thêm khóa học"}
      footer={<><Button variant="secondary" onClick={onClose}>Hủy</Button><Button type="submit" form="course-form" loading={m.isPending}>Lưu</Button></>}>
      <form id="course-form" noValidate onSubmit={handleSubmit((v) => m.mutate(v))} className="grid gap-4 sm:grid-cols-2">
        <Input label="Mã khóa" required error={errors.code?.message} {...register("code")} />
        <Input label="Trình độ" placeholder="VD: A2–B1" {...register("level")} />
        <Input label="Tên khóa học" required wrapperClassName="sm:col-span-2" error={errors.name?.message} {...register("name")} />
        <Input label="Số buổi" type="number" min={0} {...register("totalSessions")} />
        <Input label="Học phí niêm yết (₫)" inputMode="numeric" error={errors.tuitionFee?.message} {...register("tuitionFee")} />
        <Textarea label="Mô tả" rows={3} wrapperClassName="sm:col-span-2" {...register("description")} />
        <label className="flex items-center gap-2.5 text-sm sm:col-span-2"><input type="checkbox" className="size-5 accent-[var(--primary)]" {...register("active")} />Đang tuyển sinh</label>
      </form>
    </Sheet>
  );
}

function RoomSheet({ value, onClose }: { value: Room | "new" | null; onClose: () => void }) {
  const qc = useQueryClient();
  const r = value && value !== "new" ? value : null;
  const { register, handleSubmit, formState: { errors } } = useForm<z.infer<typeof roomSchema>>({
    resolver: zodResolver(roomSchema),
    values: { name: r?.name ?? "", capacity: String(r?.capacity ?? 20), active: r?.active ?? true },
  });
  const m = useMutation({
    mutationFn: (v: z.infer<typeof roomSchema>) => {
      const body = { ...v, capacity: Number(v.capacity) };
      return r ? api(`/rooms/${r.id}`, { method: "PUT", body }) : api("/rooms", { method: "POST", body });
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["rooms"] }); toast.success("Đã lưu phòng học"); onClose(); },
    onError: (e) => toast.error(e.message),
  });
  return (
    <Sheet open={!!value} onClose={onClose} title={r ? "Sửa phòng học" : "Thêm phòng học"}
      footer={<><Button variant="secondary" onClick={onClose}>Hủy</Button><Button type="submit" form="room-form" loading={m.isPending}>Lưu</Button></>}>
      <form id="room-form" noValidate onSubmit={handleSubmit((v) => m.mutate(v))} className="grid gap-4">
        <Input label="Tên phòng" required error={errors.name?.message} {...register("name")} />
        <Input label="Sức chứa" type="number" min={1} error={errors.capacity?.message} {...register("capacity")} />
        <label className="flex items-center gap-2.5 text-sm"><input type="checkbox" className="size-5 accent-[var(--primary)]" {...register("active")} />Đang sử dụng</label>
      </form>
    </Sheet>
  );
}
