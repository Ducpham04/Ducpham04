import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { api, ApiError } from "@/api/client";
import { useCourses, useRooms, useTeachers } from "@/api/hooks";
import type { ClassDetail } from "@/api/types";
import { Button, Input, Select, Sheet, Textarea } from "@/components/ui";
import { CLASS_STATUS } from "@/lib/labels";
import { todayIso } from "@/lib/format";

const schema = z.object({
  courseId: z.string().min(1, "Vui lòng chọn khóa học"),
  code: z.string().trim().regex(/^[A-Za-z0-9._-]{2,30}$/, "Mã lớp 2–30 ký tự, không dấu, không khoảng trắng"),
  name: z.string().trim().min(2, "Vui lòng nhập tên lớp").max(150),
  teacherId: z.string().optional(),
  roomId: z.string().optional(),
  maxStudents: z.coerce.number<string>().int().min(1, "Sĩ số tối thiểu 1").max(200),
  startDate: z.string().min(1, "Vui lòng chọn ngày bắt đầu"),
  endDate: z.string().optional(),
  status: z.enum(["PLANNED", "ONGOING", "FINISHED", "CANCELLED"]),
  note: z.string().max(1000).optional(),
}).refine((v) => !v.endDate || v.endDate >= v.startDate, { message: "Ngày kết thúc phải sau ngày bắt đầu", path: ["endDate"] });
type Values = z.input<typeof schema>;
type Output = z.output<typeof schema>;

export function ClassFormSheet({ open, onClose, value, onSaved }: { open: boolean; onClose: () => void; value?: ClassDetail; onSaved?: (c: ClassDetail) => void }) {
  const qc = useQueryClient();
  const courses = useCourses();
  const rooms = useRooms();
  const teachers = useTeachers();
  const { register, handleSubmit, setError, formState: { errors } } = useForm<Values, unknown, Output>({
    resolver: zodResolver(schema),
    values: value
      ? { courseId: String(value.courseId), code: value.code, name: value.name, teacherId: value.teacherId ? String(value.teacherId) : "", roomId: value.roomId ? String(value.roomId) : "",
          maxStudents: String(value.maxStudents), startDate: value.startDate, endDate: value.endDate ?? "", status: value.status, note: value.note ?? "" }
      : { courseId: "", code: "", name: "", teacherId: "", roomId: "", maxStudents: "20", startDate: todayIso(), endDate: "", status: "PLANNED", note: "" },
  });
  const m = useMutation({
    mutationFn: (v: Output) => {
      const body = { ...v, courseId: Number(v.courseId), teacherId: v.teacherId ? Number(v.teacherId) : null, roomId: v.roomId ? Number(v.roomId) : null, endDate: v.endDate || null, note: v.note || null };
      return value ? api<ClassDetail>(`/classes/${value.id}`, { method: "PUT", body }) : api<ClassDetail>("/classes", { method: "POST", body });
    },
    onSuccess: (c) => {
      qc.invalidateQueries({ queryKey: ["classes"] });
      qc.setQueryData(["class", String(c.id)], c);
      qc.invalidateQueries({ queryKey: ["sessions"] });
      toast.success(value ? "Đã cập nhật lớp" : `Đã tạo lớp ${c.code}. Tiếp theo: đặt lịch tuần.`);
      onClose();
      onSaved?.(c);
    },
    onError: (e) => {
      if (e instanceof ApiError && e.errors) Object.entries(e.errors).forEach(([k, msg]) => setError(k as keyof Values, { message: msg }));
      toast.error(e.message, { duration: 8000 });
    },
  });
  return (
    <Sheet open={open} onClose={onClose} title={value ? "Sửa thông tin lớp" : "Tạo lớp mới"}
      footer={<><Button variant="secondary" onClick={onClose}>Hủy</Button><Button type="submit" form="class-form" loading={m.isPending}>{value ? "Lưu thay đổi" : "Tạo lớp"}</Button></>}>
      <form id="class-form" noValidate onSubmit={handleSubmit((v) => m.mutate(v))} className="grid gap-4 sm:grid-cols-2">
        <Select label="Khóa học" required wrapperClassName="sm:col-span-2" error={errors.courseId?.message} {...register("courseId")}>
          <option value="">— Chọn khóa học —</option>
          {courses.data?.filter((c) => c.active || String(c.id) === String(value?.courseId)).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </Select>
        <Input label="Mã lớp" required placeholder="VD: IELTS-F-K14" error={errors.code?.message} {...register("code")} />
        <Input label="Tên lớp" required error={errors.name?.message} {...register("name")} />
        <Select label="Giáo viên" {...register("teacherId")}>
          <option value="">— Chưa phân công —</option>
          {teachers.data?.map((t) => <option key={t.id} value={t.id}>{t.fullName}</option>)}
        </Select>
        <Select label="Phòng học" {...register("roomId")}>
          <option value="">— Chưa xếp phòng —</option>
          {rooms.data?.filter((r) => r.active).map((r) => <option key={r.id} value={r.id}>{r.name} ({r.capacity} chỗ)</option>)}
        </Select>
        <Input label="Sĩ số tối đa" type="number" min={1} required error={errors.maxStudents?.message} {...register("maxStudents")} />
        <Select label="Trạng thái" {...register("status")}>
          {Object.entries(CLASS_STATUS).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
        </Select>
        <Input label="Ngày bắt đầu" type="date" required error={errors.startDate?.message} {...register("startDate")} />
        <Input label="Ngày kết thúc" type="date" error={errors.endDate?.message} {...register("endDate")} />
        <Textarea label="Ghi chú" rows={2} wrapperClassName="sm:col-span-2" {...register("note")} />
      </form>
    </Sheet>
  );
}
