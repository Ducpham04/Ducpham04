import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test("giáo viên điểm danh trên điện thoại", async ({ page }) => {
  await login(page, "david@hoasen.edu.vn");
  await expect(page).toHaveURL(/\/app\/today/);
  // Mở một buổi đã qua để điểm danh (lùi 1 ngày cho tới khi có buổi)
  for (let i = 0; i < 7; i++) {
    const btn = page.getByRole("link", { name: /Điểm danh|Xem/ }).first();
    if (await btn.isVisible().catch(() => false)) break;
    await page.getByRole("button", { name: "Ngày trước" }).click();
    await page.waitForTimeout(300);
  }
  await page.getByRole("link", { name: /Điểm danh|Xem/ }).first().click();
  await expect(page.getByRole("button", { name: "Lưu điểm danh" })).toBeVisible();
  const first = page.getByRole("radiogroup").first();
  await first.getByRole("radio", { name: "Đi muộn" }).click();
  await expect(first.getByRole("radio", { name: "Đi muộn" })).toHaveAttribute("aria-checked", "true");
  await page.getByRole("button", { name: "Lưu điểm danh" }).click();
  await expect(page.getByText(/Đã lưu điểm danh lớp/)).toBeVisible();
  // Giáo viên không vào được trang học phí
  await page.goto("/app/billing");
  await expect(page).toHaveURL(/\/app\/today/);
});

test("phụ huynh xem lịch, điểm danh và học phí", async ({ page }) => {
  await login(page, "phuhuynh@hoasen.edu.vn");
  await expect(page).toHaveURL(/\/portal/);
  await expect(page.getByRole("heading", { level: 1, name: "Nguyễn Gia Hân" })).toBeVisible();
  await page.getByRole("tab", { name: "Điểm danh" }).click();
  await expect(page.getByText(/Có mặt \d+\/\d+ buổi/)).toBeVisible();
  await page.getByRole("tab", { name: "Học phí" }).click();
  await expect(page.getByText(/Học phí Starters tháng/).first()).toBeVisible();
  await page.goto("/app/students");
  await expect(page).toHaveURL(/\/portal/);
});
