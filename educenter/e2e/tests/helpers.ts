import { expect, type Page } from "@playwright/test";

export async function login(page: Page, email: string, password = "Demo@123") {
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Mật khẩu").fill(password);
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page).not.toHaveURL(/\/login/);
}

export const uid = () => Date.now().toString(36).slice(-5).toUpperCase();
