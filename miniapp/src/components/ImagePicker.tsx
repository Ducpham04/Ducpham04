import { ImagePlus, X } from "lucide-react";
import { useEffect, useMemo } from "react";

export const MAX_IMAGES = 5;
export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
export const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp"];

interface Props {
  files: File[];
  onAdd: () => void;
  onRemove: (index: number) => void;
  error?: string;
}

export function ImagePicker({ files, onAdd, onRemove, error }: Props) {
  const previews = useMemo(() => files.map((f) => URL.createObjectURL(f)), [files]);
  useEffect(() => () => previews.forEach((u) => URL.revokeObjectURL(u)), [previews]);

  return (
    <div>
      <div className="grid grid-cols-3 gap-2">
        {previews.map((src, i) => (
          <div key={src} className="relative aspect-square rounded-lg overflow-hidden bg-gray-100">
            <img src={src} alt={`Ảnh ${i + 1}`} className="w-full h-full object-cover" />
            <button type="button" aria-label={`Xoá ảnh ${i + 1}`} onClick={() => onRemove(i)}
              className="absolute top-1 right-1 w-7 h-7 rounded-full bg-black/60 text-white flex items-center justify-center border-0">
              <X size={16} />
            </button>
          </div>
        ))}
        {files.length < MAX_IMAGES && (
          <button type="button" onClick={onAdd} data-testid="add-image"
            className="aspect-square rounded-lg border-2 border-dashed border-line bg-white text-ink-muted flex flex-col items-center justify-center gap-1">
            <ImagePlus size={28} />
            <span className="text-xs">Thêm ảnh</span>
          </button>
        )}
      </div>
      <div className="flex justify-between text-xs mt-1">
        <span className="text-danger">{error}</span>
        <span className="text-ink-muted">{files.length}/{MAX_IMAGES}</span>
      </div>
    </div>
  );
}
