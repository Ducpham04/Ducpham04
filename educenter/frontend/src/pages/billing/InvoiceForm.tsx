import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { api } from "@/api/client";
import { useFeePlans } from "@/api/hooks";
import type { Invoice, Page, StudentEnrollment, StudentListItem } from "@/api/types";
import { Button, Input, Select, Sheet, Textarea } from "@/components/ui";
import { addDays, formatMoney, todayIso } from "@/lib/format";

const schema = z.object({
  studentId: z.string().min(1, "Vui lòng chọn học viên"),
  classId: z.string().optional(),
  feePlanId: z.string().optional(),
  description: z.string().max(255).optional(),
  amount: z.string().optional(),
  discount: z.string().optional(),
  dueDate: z.string().min(1, "Vui lòng chọn hạn thanh toán"),
  note: z.string().max(500).optional(),
}).refine((v) => !!v.feePlanId || (Number(v.amount?.replace(/\D/g, "")) >= 1000), { message: "Chọn gói học phí hoặc nhập số tiền (≥ 1.000 ₫)", path: ["amount"] });
type Values = z.infer<typeof schema>;

const num = (s?: string) => (s ? Number(s.replace(/\D/g, "")) || 0 : 0);

export function InvoiceFormSheet({ open, onClose, student, onCreated }: {
  open: boolean;
  onClose: () => void;
  student?: { id: number; fullName: string; code: string };
  onCreated?: (inv: Invoice) => void;
}) {
  const qc = useQueryClient();
  const plans = useFeePlans(open);
  const [studentQuery, setStudentQuery] = useState("");
  const students = useQuery({
    queryKey: ["students", "pick", studentQuery],
    queryFn: () => api<Page<StudentListItem>>("/students", { query: { q: studentQuery, size: 20 } }),
    enabled: open && !student,
  });
  const { register, handleSubmit, watch, setValue, reset, formState: { errors } } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { studentId: student ? String(student.id) : "", dueDate: addDays(todayIso(), 7), discount: "0" },
  });
  const studentId = watch("studentId");
  const enrollments = useQuery({
    queryKey: ["student", studentId, "enrollments"],
    queryFn: () => api<StudentEnrollment[]>(`/students/${studentId}/enrollments`),
    enabled: open && !!studentId,
  });
  const feePlanId = watch("feePlanId");
  const plan = plans.data?.find((p) => String(p.id) === feePlanId);
  useEffect(() => {
    if (plan) setValue("amount", String(plan.amount));
  }, [plan, setValue]);
  useEffect(() => {
    if (open) reset({ studentId: student ? String(student.id) : "", dueDate: addDays(todayIso(), 7), discount: "0" });
  }, [open, student, reset]);

  const total = Math.max(0, num(watch("amount")) - num(watch("discount")));

  const mutation = useMutation({
    mutationFn: (v: Values) => api<Invoice>("/invoices", {
      method: "POST",
      body: {
        studentId: Number(v.studentId),
        classId: v.classId ? Number(v.classId) : null,
        feePlanId: v.feePlanId ? Number(v.feePlanId) : null,
        description: v.description?.trim() || null,
        amount: num(v.amount) || null,
        discount: num(v.discount),
        dueDate: v.dueDate,
        note: v.note?.trim() || null,
      },
    }),
    onSuccess: (inv) => {
      qc.invalidateQueries({ queryKey: ["invoices"] });
      qc.invalidateQueries({ queryKey: ["student", String(inv.studentId)] });
      qc.invalidateQueries({ queryKey: ["debts"] });
      toast.success(`Đã lập hóa đơn ${inv.code}`);
      onClose();
      onCreated?.(inv);
    },
    onError: (e) => toast.error(e.message),
  });

  return (
    <Sheet open={open} onClose={onClose} title="Lập hóa đơn" description={student ? `${student.fullName} · ${student.code}` : undefined}
      footer={<><Button variant="secondary" onClick={onClose}>Hủy</Button><Button type="submit" form="invoice-form" loading={mutation.isPending}>Lập hóa đơn</Button></>}>
      <form id="invoice-form" noValidate onSubmit={handleSubmit((v) => mutation.mutate(v))} className="grid gap-4">
        {!student && (
          <>
            <Input label="Tìm học viên" placeholder="Nhập tên hoặc SĐT" value={studentQuery} onChange={(e) => setStudentQuery(e.target.value)} />
            <Select label="Học viên" required error={errors.studentId?.message} {...register("studentId")}>
              <option value="">— Chọn học viên —</option>
              {students.data?.items.map((s) => <option key={s.id} value={s.id}>{s.fullName} ({s.code})</option>)}
            </Select>
          </>
        )}
        <Select label="Lớp (không bắt buộc)" {...register("classId")}>
          <option value="">— Không gắn lớp —</option>
          {enrollments.data?.filter((e) => e.status === "ACTIVE").map((e) => <option key={e.classId} value={e.classId}>{e.classCode} – {e.className}</option>)}
        </Select>
        <Select label="Gói học phí" {...register("feePlanId")}>
          <option value="">— Nhập số tiền tự do —</option>
          {plans.data?.filter((p) => p.active).map((p) => <option key={p.id} value={p.id}>{p.name} · {formatMoney(p.amount)}</option>)}
        </Select>
        <Input label="Nội dung" placeholder={plan ? plan.name : "VD: Học phí tháng 11/2026"} {...register("description")} />
        <div className="grid grid-cols-2 gap-4">
          <Input label="Số tiền (₫)" inputMode="numeric" error={errors.amount?.message} {...register("amount")} />
          <Input label="Giảm giá (₫)" inputMode="numeric" {...register("discount")} />
        </div>
        <Input label="Hạn thanh toán" type="date" required error={errors.dueDate?.message} {...register("dueDate")} />
        <Textarea label="Ghi chú" rows={2} {...register("note")} />
        <div className="flex items-center justify-between rounded-lg bg-surface-2 px-4 py-3">
          <span className="text-sm text-ink-2">Phải thu</span>
          <span className="tabular text-lg font-bold">{formatMoney(total)}</span>
        </div>
      </form>
    </Sheet>
  );
}
