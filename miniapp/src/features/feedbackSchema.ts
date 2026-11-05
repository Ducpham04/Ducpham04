import { z } from "zod";
import { ALLOWED_TYPES, MAX_IMAGE_BYTES, MAX_IMAGES } from "../components/ImagePicker";

/** Khớp với validation phía backend (CreateFeedbackRequest). */
export const feedbackSchema = z.object({
  category: z.string().min(1, "Vui lòng chọn chuyên mục"),
  title: z.string().trim().min(5, "Tiêu đề từ 5–150 ký tự").max(150, "Tiêu đề từ 5–150 ký tự"),
  content: z.string().trim().min(10, "Nội dung từ 10–2000 ký tự").max(2000, "Nội dung từ 10–2000 ký tự"),
  address: z.string().trim().max(255, "Địa chỉ tối đa 255 ký tự").optional().or(z.literal("")),
});

export type FeedbackFormValues = z.infer<typeof feedbackSchema>;

/** Kiểm tra ảnh trước khi upload. Trả về thông báo lỗi hoặc null. */
export function validateImages(current: File[], incoming: File[]): { accepted: File[]; error: string | null } {
  let error: string | null = null;
  const accepted: File[] = [];
  for (const f of incoming) {
    if (current.length + accepted.length >= MAX_IMAGES) {
      error = `Tối đa ${MAX_IMAGES} ảnh`;
      break;
    }
    if (!ALLOWED_TYPES.includes(f.type)) {
      error = "Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP";
      continue;
    }
    if (f.size > MAX_IMAGE_BYTES) {
      error = "Mỗi ảnh tối đa 5MB";
      continue;
    }
    accepted.push(f);
  }
  return { accepted, error };
}
