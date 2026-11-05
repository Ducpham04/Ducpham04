import { MapPin, X } from "lucide-react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Button, useNavigate, useSnackbar } from "zmp-ui";
import { feedbackApi, meApi } from "../api/endpoints";
import { ApiError, errorMessage } from "../api/client";
import { AppPage } from "../components/AppPage";
import { ImagePicker, MAX_IMAGES } from "../components/ImagePicker";
import { PermissionPrompt } from "../components/PermissionPrompt";
import { feedbackSchema, validateImages, type FeedbackFormValues } from "../features/feedbackSchema";
import { zalo, ZaloError } from "../zalo/bridge";

const inputCls = "w-full rounded-lg border border-line bg-white px-3 py-2.5 text-[15px] outline-none focus:border-primary";

export default function FeedbackFormPage() {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const { openSnackbar } = useSnackbar();
  const cats = useQuery({ queryKey: ["feedback-categories"], queryFn: feedbackApi.categories });
  const [images, setImages] = useState<File[]>([]);
  const [imageError, setImageError] = useState<string>();
  const [location, setLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [locState, setLocState] = useState<"idle" | "loading" | "denied" | "error">("idle");

  const { register, handleSubmit, setError, formState: { errors } } = useForm<FeedbackFormValues>({
    resolver: zodResolver(feedbackSchema),
    defaultValues: { category: "", title: "", content: "", address: "" },
  });

  const submit = useMutation({
    mutationFn: (v: FeedbackFormValues) =>
      feedbackApi.create({ ...v, address: v.address || undefined, lat: location?.lat, lng: location?.lng }, images),
    onSuccess: (fb) => {
      void qc.invalidateQueries({ queryKey: ["feedbacks"] });
      openSnackbar({ text: `Đã gửi phản ánh ${fb.code}`, type: "success" });
      navigate(`/feedback/${fb.id}`, { replace: true });
    },
    onError: (e) => {
      if (e instanceof ApiError && e.errors) {
        Object.entries(e.errors).forEach(([k, msg]) => setError(k as keyof FeedbackFormValues, { message: msg }));
      }
      openSnackbar({ text: errorMessage(e), type: "error" });
    },
  });

  const addImages = async () => {
    try {
      const picked = await zalo.chooseImages(MAX_IMAGES - images.length);
      const { accepted, error } = validateImages(images, picked);
      setImages((prev) => [...prev, ...accepted]);
      setImageError(error ?? undefined);
    } catch (e) {
      if (!(e instanceof ZaloError && e.denied)) setImageError(errorMessage(e));
    }
  };

  const getLocation = async () => {
    setLocState("loading");
    try {
      const [token, zaloToken] = await Promise.all([zalo.getLocationToken(), zalo.getAccessToken()]);
      const u = await meApi.location(token, zaloToken);
      if (u.lastLat != null && u.lastLng != null) setLocation({ lat: Number(u.lastLat), lng: Number(u.lastLng) });
      setLocState("idle");
    } catch (e) {
      setLocState(e instanceof ZaloError && e.denied ? "denied" : "error");
    }
  };

  return (
    <AppPage title="Gửi phản ánh"
      footer={<Button fullWidth loading={submit.isPending} onClick={handleSubmit((v) => submit.mutate(v))} data-testid="submit-feedback">Gửi phản ánh</Button>}>
      <form className="space-y-4 pb-4" onSubmit={handleSubmit((v) => submit.mutate(v))} noValidate>
        <Field label="Chuyên mục" required error={errors.category?.message}>
          <select {...register("category")} className={inputCls} aria-label="Chuyên mục" disabled={cats.isPending}>
            <option value="">{cats.isPending ? "Đang tải…" : "— Chọn chuyên mục —"}</option>
            {cats.data?.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </Field>
        <Field label="Tiêu đề" required error={errors.title?.message}>
          <input {...register("title")} className={inputCls} placeholder="VD: Đèn đường hỏng trước số 10" aria-label="Tiêu đề" />
        </Field>
        <Field label="Nội dung" required error={errors.content?.message}>
          <textarea {...register("content")} rows={5} className={inputCls} placeholder="Mô tả chi tiết sự việc, thời gian, mức độ…" aria-label="Nội dung" />
        </Field>
        <Field label="Hình ảnh (tối đa 5 ảnh, ≤ 5MB)">
          <ImagePicker files={images} onAdd={addImages} onRemove={(i) => setImages((p) => p.filter((_, idx) => idx !== i))} error={imageError} />
        </Field>
        <Field label="Vị trí" error={errors.address?.message}>
          <div className="space-y-2">
            {location ? (
              <div className="flex items-center gap-2 text-sm bg-green-50 text-success rounded-lg px-3 py-2" data-testid="location-ok">
                <MapPin size={20} /> Đã đính kèm vị trí ({location.lat.toFixed(5)}, {location.lng.toFixed(5)})
                <button type="button" className="ml-auto bg-transparent border-0 text-ink-muted" onClick={() => setLocation(null)} aria-label="Bỏ vị trí"><X size={16} /></button>
              </div>
            ) : (
              <Button htmlType="button" variant="secondary" size="small" loading={locState === "loading"} prefixIcon={<MapPin size={20} />} onClick={getLocation}>
                Lấy vị trí hiện tại
              </Button>
            )}
            {locState === "denied" && (
              <PermissionPrompt title="Bạn chưa cho phép truy cập vị trí"
                reason="Vị trí giúp cán bộ đến đúng nơi nhanh hơn. Bạn vẫn có thể nhập địa chỉ thủ công bên dưới."
                onRetry={getLocation} onDismiss={() => setLocState("idle")} />
            )}
            {locState === "error" && <div className="text-xs text-danger">Không lấy được vị trí, vui lòng nhập địa chỉ.</div>}
            <input {...register("address")} className={inputCls} placeholder="Địa chỉ (không bắt buộc)" aria-label="Địa chỉ" />
          </div>
        </Field>
      </form>
    </AppPage>
  );
}

function Field({ label, required, error, children }: { label: string; required?: boolean; error?: string; children: React.ReactNode }) {
  return (
    <div>
      <div className="text-sm font-medium mb-1.5">{label}{required && <span className="text-danger"> *</span>}</div>
      {children}
      {error && <div className="text-xs text-danger mt-1" role="alert">{error}</div>}
    </div>
  );
}
