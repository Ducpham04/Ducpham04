const VN_TZ = 'Asia/Ho_Chi_Minh'

export function formatVnd(n: number): string {
  return new Intl.NumberFormat('vi-VN').format(n) + ' ₫'
}

/** Backend trả LocalDateTime "YYYY-MM-DDTHH:mm:ss" theo giờ Việt Nam – xử lý như chuỗi giờ tường. */
export function parts(dt: string) {
  const [d, t = '00:00'] = dt.split('T')
  const [y, m, day] = d.split('-').map(Number)
  const [hh, mm] = t.split(':').map(Number)
  return { y, m, d: day, hh, mm, date: d, minutes: hh * 60 + mm }
}

export function hhmm(dt: string): string {
  return dt.includes('T') ? dt.split('T')[1].slice(0, 5) : dt.slice(0, 5)
}

export function ddmmyyyy(date: string): string {
  const [y, m, d] = date.slice(0, 10).split('-')
  return `${d}/${m}/${y}`
}

const WEEKDAY_SHORT = ['CN', 'Th 2', 'Th 3', 'Th 4', 'Th 5', 'Th 6', 'Th 7']
const WEEKDAY_LONG = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy']
export const DAY_NAMES = ['Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy', 'Chủ nhật'] // ISO 1..7

function utc(date: string) {
  const [y, m, d] = date.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d))
}

export function weekdayShort(date: string) {
  return WEEKDAY_SHORT[utc(date).getUTCDay()]
}

export function weekdayLong(date: string) {
  return WEEKDAY_LONG[utc(date).getUTCDay()]
}

/** 1 = Thứ hai ... 7 = Chủ nhật */
export function isoDow(date: string) {
  const d = utc(date).getUTCDay()
  return d === 0 ? 7 : d
}

export function addDays(date: string, n: number): string {
  const x = utc(date)
  x.setUTCDate(x.getUTCDate() + n)
  return x.toISOString().slice(0, 10)
}

export function startOfWeek(date: string): string {
  return addDays(date, 1 - isoDow(date))
}

export function longDate(date: string) {
  return `${weekdayLong(date)}, ${ddmmyyyy(date)}`
}

export function dateTimeLabel(dt: string) {
  const p = parts(dt)
  return `${hhmm(dt)} · ${weekdayShort(p.date)}, ${ddmmyyyy(p.date)}`
}

/** Thời điểm hiện tại theo giờ Việt Nam, bất kể múi giờ trình duyệt. */
export function vnNow(): { date: string; minutes: number; iso: string } {
  const f = new Intl.DateTimeFormat('en-CA', {
    timeZone: VN_TZ,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
  const p = Object.fromEntries(f.formatToParts(new Date()).map((x) => [x.type, x.value]))
  const hour = p.hour === '24' ? '00' : p.hour
  const date = `${p.year}-${p.month}-${p.day}`
  return { date, minutes: Number(hour) * 60 + Number(p.minute), iso: `${date}T${hour}:${p.minute}:00` }
}

export function minutesToHHMM(min: number): string {
  const h = Math.floor(min / 60)
  const m = min % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

export function durationLabel(min: number): string {
  if (min < 60) return `${min} phút`
  const h = Math.floor(min / 60)
  const m = min % 60
  return m ? `${h} giờ ${m} phút` : `${h} giờ`
}

export function percent(v: number): string {
  return `${(v * 100).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%`
}

const PHONE_RE = /^0(3|5|7|8|9)\d{8}$/
export function normalizePhone(raw: string): string | null {
  let p = raw.replace(/[\s.\-()]/g, '')
  if (p.startsWith('+84')) p = '0' + p.slice(3)
  else if (p.startsWith('84') && p.length === 11) p = '0' + p.slice(2)
  return PHONE_RE.test(p) ? p : null
}

export function slugify(input: string): string {
  return input
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '')
    .slice(0, 60)
}

/** Chọn chữ trắng/đen trên nền màu thương hiệu (độ sáng tương đối WCAG). */
export function onColor(hex: string): string {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex)
  if (!m) return '#ffffff'
  const n = parseInt(m[1], 16)
  const ch = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map((c) => {
    const s = c / 255
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4
  })
  const L = 0.2126 * ch[0] + 0.7152 * ch[1] + 0.0722 * ch[2]
  return L > 0.4 ? '#1a2421' : '#ffffff'
}

export function initials(name: string): string {
  const words = name.replace(/^(BS\.|ThS\.|TS\.)\s*/i, '').trim().split(/\s+/)
  const last = words[words.length - 1] ?? ''
  const prev = words.length > 1 ? words[words.length - 2] : ''
  return ((prev[0] ?? '') + (last[0] ?? '')).toUpperCase()
}
