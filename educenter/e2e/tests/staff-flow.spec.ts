import { expect, test } from "@playwright/test";
import { login, uid } from "./helpers";

test("giáo vụ: thêm học viên → ghi danh → lập hóa đơn → thu tiền một phần", async ({ page }) => {
  const name = `Học Viên E2E ${uid()}`;
  await login(page, "giaovu@hoasen.edu.vn");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Chào");
  await expect(page.getByText("Doanh thu tháng này")).toBeVisible();

  // Thêm học viên kèm phụ huynh
  await page.getByRole("navigation", { name: "Điều hướng chính" }).getByRole("link", { name: "Học viên" }).click();
  await page.getByRole("button", { name: "Thêm học viên" }).first().click();
  const sheet = page.getByRole("dialog", { name: "Thêm học viên" });
  await sheet.getByLabel("Họ và tên").fill(name);
  await sheet.getByLabel("Số điện thoại").fill("0912345678");
  await sheet.getByLabel("Họ tên phụ huynh").fill("Phụ Huynh E2E");
  await sheet.getByLabel("SĐT / Zalo").fill("0987654321");
  await sheet.getByRole("button", { name: "Thêm học viên" }).click();
  await expect(page.getByRole("heading", { level: 1, name })).toBeVisible();
  await expect(page.getByText("Phụ Huynh E2E")).toBeVisible();

  // Ghi danh vào lớp đang học
  await page.getByRole("navigation", { name: "Điều hướng chính" }).getByRole("link", { name: "Lớp học" }).click();
  await page.getByRole("link", { name: "TOEIC 650+ – Tối 2-4-6" }).first().click();
  await page.getByRole("button", { name: "Ghi danh" }).click();
  const enroll = page.getByRole("dialog");
  await enroll.getByRole("searchbox").fill(name);
  await enroll.getByRole("listitem").filter({ hasText: name }).getByRole("button", { name: "Thêm" }).click();
  await expect(page.getByText(`Đã ghi danh ${name}`)).toBeVisible();
  await enroll.getByRole("button", { name: "Đóng" }).click();
  await expect(page.getByRole("link", { name })).toBeVisible();

  // Lập hóa đơn từ hồ sơ học viên
  await page.getByRole("link", { name }).click();
  await page.getByRole("button", { name: "Lập hóa đơn" }).first().click();
  const inv = page.getByRole("dialog", { name: "Lập hóa đơn" });
  await inv.getByLabel("Gói học phí").selectOption({ label: "TOEIC 650+ – trọn khóa · 5.400.000 ₫" });
  await inv.getByRole("button", { name: "Lập hóa đơn" }).click();
  await expect(page.getByText(/Đã lập hóa đơn HD/)).toBeVisible();

  // Mở hóa đơn, thu 2.000.000 ₫
  await page.getByRole("tab", { name: "Học phí" }).click();
  await page.getByRole("link", { name: /TOEIC 650\+ – trọn khóa/ }).first().click();
  await expect(page.getByAltText(/Mã QR chuyển/)).toBeVisible();
  await page.getByLabel("Số tiền (₫)").fill("2000000");
  await page.getByRole("button", { name: "Ghi nhận & tạo phiếu thu" }).click();
  await expect(page.getByText(/Đã ghi nhận 2\.000\.000 ₫ – phiếu thu PT/)).toBeVisible();
  await expect(page.getByText("Thu một phần")).toBeVisible();
  await expect(page.getByRole("button", { name: "Phiếu thu PDF" })).toBeVisible();
});

test("lịch: phát hiện trùng phòng khi đặt lịch lớp mới", async ({ page }) => {
  const code = `E2E-${uid()}`;
  await login(page, "chulan@hoasen.edu.vn");
  await page.goto("/app/classes");
  await page.getByRole("button", { name: "Tạo lớp" }).first().click();
  const sheet = page.getByRole("dialog", { name: "Tạo lớp mới" });
  await sheet.getByLabel("Khóa học").selectOption({ label: "TOEIC 650+" });
  await sheet.getByLabel("Mã lớp").fill(code);
  await sheet.getByLabel("Tên lớp").fill(`Lớp ${code}`);
  await sheet.getByLabel("Phòng học").selectOption({ label: "P201 (24 chỗ)" });
  await sheet.getByLabel("Sĩ số tối đa").fill("20");
  await sheet.getByRole("button", { name: "Tạo lớp" }).click();
  await expect(page.getByRole("heading", { level: 1, name: `Lớp ${code}` })).toBeVisible();
  await expect(page.getByRole("tab", { name: "Lịch tuần" })).toHaveAttribute("aria-selected", "true");
  await page.getByRole("button", { name: "Thêm khung giờ" }).click();
  // Thứ 2 19:45 trùng phòng P201 với lớp TOEIC-A2
  await page.getByRole("radio", { name: "Thứ 2" }).click();
  await page.getByLabel("Bắt đầu").fill("20:00");
  await page.getByLabel("Kết thúc").fill("21:00");
  await expect(page.getByText(/Trùng phòng với lớp TOEIC-A2/)).toBeVisible();
  await expect(page.getByRole("button", { name: "Lưu lịch học" })).toBeDisabled();
  // Đổi sang Chủ nhật → hết trùng, lưu được
  await page.getByRole("radio", { name: "Chủ nhật" }).click();
  await expect(page.getByText("Không trùng lịch")).toBeVisible();
  await page.getByRole("button", { name: "Lưu lịch học" }).click();
  await expect(page.getByText(/Đã lưu lịch và sinh/)).toBeVisible();
  // Dọn dẹp: hủy lớp để lần chạy sau không bị trùng lịch
  await page.getByRole("button", { name: "Sửa lớp" }).click();
  const edit = page.getByRole("dialog", { name: "Sửa thông tin lớp" });
  await edit.getByLabel("Trạng thái").selectOption({ label: "Đã hủy" });
  await edit.getByRole("button", { name: "Lưu thay đổi" }).click();
  await expect(page.getByText("Đã cập nhật lớp")).toBeVisible();
});

test("báo cáo & cài đặt tải được", async ({ page }) => {
  await login(page, "chulan@hoasen.edu.vn");
  await page.goto("/app/reports");
  await expect(page.getByText("Chuyên cần theo lớp")).toBeVisible();
  const download = page.waitForEvent("download");
  await page.getByRole("button", { name: "Xuất Excel" }).first().click();
  expect((await download).suggestedFilename()).toMatch(/doanh-thu-\d{4}\.xlsx/);
  await page.goto("/app/settings");
  await expect(page.getByLabel("Ngân hàng")).toHaveValue("VCB");
});
