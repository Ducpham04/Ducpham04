import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { api, ApiError } from "@/api/client";
import type { Student } from "@/api/types";
import { Button, Input, Select, Sheet, Textarea } from "@/components/ui";
import { todayIso } from "@/lib/format";

export const phoneSchema = z.string().trim().regex(/^(0\d{9})?$/, "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0");

const schema = z.object({
  fullName: z.string().trim().min(2, "Vui lòng nhập họ tên").max(120),
  dob: z.string().optional(),
  gender: z.enum(["", "MALE", "FEMALE", "OTHER"]).optional(),
  phone: phoneSchema.optional(),
  email: z.union([z.literal(""), z.string().trim().email("Email không hợp lệ")]).optional(),
  school: z.string().max(150).optional(),
  level: z.string().max(60).optional(),
  address: z.string().max(255).optional(),
  status: z.enum(["TRIAL", "ACTIVE", "ON_HOLD", "DROPPED"]),
  joinedAt: z.string().min(1, "Vui lòng chọn ngày nhập học"),
  note: z.string().max(1000).optional(),
  guardianName: z.string().max(120).optional(),
  guardianRelationship: z.string().max(30).optional(),
  guardianPhone: phoneSchema.optional(),
  guardianEmail: z.union([z.literal(""), z.string().trim().email("Email không hợp lệ")]).optional(),
});
export type StudentFormValues = z.infer<typeof schema>;

const nz = (v?: string) => (v && v.trim() ? v.trim() : null);

export function StudentFormSheet({ open, onClose, student, onSaved }: { open: boolean; onClose: () => void; student?: Student; onSaved?: (s: Student) => void }) {
  const qc = useQueryClient();
  const editing = !!student;
  const { register, handleSubmit, setError, reset, formState: { errors } } = useForm<StudentFormValues>({
    resolver: zodResolver(schema),
    values: student
      ? {
          fullName: student.fullName, dob: student.dob ?? "", gender: student.gender ?? "", phone: student.phone ?? "", email: student.email ?? "",
          school: student.school ?? "", level: student.level ?? "", address: student.address ?? "", status: student.status, joinedAt: student.joinedAt, note: student.note ?? "",
        }
      : { fullName: "", status: "ACTIVE", joinedAt: todayIso(), gender: "", phone: "", email: "", guardianPhone: "", guardianEmail: "", guardianRelationship: "Mẹ" },
  });

  const mutation = useMutation({
    mutationFn: async (v: StudentFormValues) => {
      const body = {
        fullName: v.fullName, dob: nz(v.dob), gender: nz(v.gender), phone: nz(v.phone), email: nz(v.email), school: nz(v.school),
        level: nz(v.level), address: nz(v.address), status: v.status, joinedAt: v.joinedAt, note: nz(v.note),
      };
      const saved = editing
        ? await api<Student>(`/students/${student!.id}`, { method: "PUT", body })
        : await api<Student>("/students", { method: "POST", body });
      if (!editing && nz(v.guardianName)) {
        await api(`/students/${saved.id}/guardians`, {
          method: "POST",
          body: { fullName: v.guardianName, relationship: nz(v.guardianRelationship), phone: nz(v.guardianPhone), email: nz(v.guardianEmail), zaloPhone: nz(v.guardianPhone), primary: true },
        });
      }
      return saved;
    },
    onSuccess: (saved) => {
      qc.invalidateQueries({ queryKey: ["students"] });
      qc.invalidateQueries({ queryKey: ["student", String(saved.id)] });
      toast.success(editing ? "Đã cập nhật hồ sơ học viên" : `Đã thêm học viên ${saved.code}`);
      reset();
      onClose();
      onSaved?.(saved);
    },
    onError: (e) => {
      if (e instanceof ApiError && e.errors) {
        Object.entries(e.errors).forEach(([k, m]) => setError(k as keyof StudentFormValues, { message: m }));
      }
      toast.error(e.message);
    },
  });

  return (
    <Sheet
      open={open}
      onClose={onClose}
      title={editing ? "Sửa hồ sơ học viên" : "Thêm học viên"}
      description={editing ? student!.code : "Mã học viên được tạo tự động"}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Hủy</Button>
        <Button type="submit" form="student-form" loading={mutation.isPending}>{editing ? "Lưu thay đổi" : "Thêm học viên"}</Button>
      </>}
    >
      <form id="student-form" noValidate onSubmit={handleSubmit((v) => mutation.mutate(v))} className="grid gap-4 sm:grid-cols-2">
        <Input label="Họ và tên" required wrapperClassName="sm:col-span-2" error={errors.fullName?.message} {...register("fullName")} />
        <Input label="Ngày sinh" type="date" error={errors.dob?.message} {...register("dob")} />
        <Select label="Giới tính" {...register("gender")}>
          <option value="">—</option><option value="FEMALE">Nữ</option><option value="MALE">Nam</option><option value="OTHER">Khác</option>
        </Select>
        <Input label="Số điện thoại" inputMode="tel" error={errors.phone?.message} {...register("phone")} />
        <Input label="Email" type="email" error={errors.email?.message} {...register("email")} />
        <Input label="Trường" {...register("school")} />
        <Input label="Trình độ đầu vào" placeholder="VD: A2, IELTS 4.5" {...register("level")} />
        <Select label="Trạng thái" required {...register("status")}>
          <option value="ACTIVE">Đang học</option><option value="TRIAL">Học thử</option><option value="ON_HOLD">Bảo lưu</option><option value="DROPPED">Đã nghỉ</option>
        </Select>
        <Input label="Ngày nhập học" type="date" required error={errors.joinedAt?.message} {...register("joinedAt")} />
        <Input label="Địa chỉ" wrapperClassName="sm:col-span-2" {...register("address")} />
        <Textarea label="Ghi chú" wrapperClassName="sm:col-span-2" rows={2} {...register("note")} />
        {!editing && (
          <fieldset className="grid gap-4 border-t border-line pt-4 sm:col-span-2 sm:grid-cols-2">
            <legend className="mb-1 text-[15px] font-semibold">Phụ huynh liên hệ chính</legend>
            <Input label="Họ tên phụ huynh" wrapperClassName="sm:col-span-2" {...register("guardianName")} />
            <Select label="Quan hệ" {...register("guardianRelationship")}>
              <option>Mẹ</option><option>Bố</option><option>Ông/Bà</option><option>Anh/Chị</option><option>Khác</option>
            </Select>
            <Input label="SĐT / Zalo" inputMode="tel" error={errors.guardianPhone?.message} {...register("guardianPhone")} />
            <Input label="Email nhận nhắc nhở" type="email" wrapperClassName="sm:col-span-2" error={errors.guardianEmail?.message} {...register("guardianEmail")} />
          </fieldset>
        )}
      </form>
    </Sheet>
  );
}
