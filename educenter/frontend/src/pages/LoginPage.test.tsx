import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "@/lib/auth";
import { LoginPage } from "./LoginPage";

function setup() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <AuthProvider>
        <MemoryRouter><LoginPage /></MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  );
}

describe("LoginPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("validates required fields in Vietnamese", async () => {
    setup();
    await userEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));
    expect(await screen.findByText("Vui lòng nhập email")).toBeInTheDocument();
    expect(screen.getByText("Vui lòng nhập mật khẩu")).toBeInTheDocument();
  });

  it("fills demo account and shows server error", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ detail: "Email hoặc mật khẩu không đúng" }), { status: 401 }));
    setup();
    await userEvent.click(screen.getByRole("button", { name: /giaovu@hoasen\.edu\.vn/ }));
    expect(screen.getByLabelText("Email")).toHaveValue("giaovu@hoasen.edu.vn");
    await userEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Email hoặc mật khẩu không đúng");
  });
});
