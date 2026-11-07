import { api, ApiError, buildUrl, tokenStore } from "./client";

describe("api client", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    tokenStore.clear();
  });

  it("builds query strings and skips empty values", () => {
    expect(buildUrl("/students", { q: "Hân", status: undefined, page: 0, empty: "" })).toBe("/api/v1/students?q=H%C3%A2n&page=0");
  });

  it("maps problem details to ApiError with field errors", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ detail: "Dữ liệu chưa hợp lệ", errors: { fullName: "Bắt buộc" } }), { status: 400 }));
    const err = (await api("/students", { method: "POST", body: {} }).catch((e) => e)) as ApiError;
    expect(err).toBeInstanceOf(ApiError);
    expect(err.status).toBe(400);
    expect(err.message).toBe("Dữ liệu chưa hợp lệ");
    expect(err.errors?.fullName).toBe("Bắt buộc");
  });

  it("refreshes the session once on 401 and retries", async () => {
    tokenStore.setTokens("old-access", "refresh-1");
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response("", { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ accessToken: "new-access", refreshToken: "refresh-2" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ ok: true }), { status: 200 }));
    const res = await api<{ ok: boolean }>("/auth/me");
    expect(res.ok).toBe(true);
    expect(fetchMock).toHaveBeenCalledTimes(3);
    const retryHeaders = fetchMock.mock.calls[2][1]!.headers as Headers;
    expect(retryHeaders.get("Authorization")).toBe("Bearer new-access");
    expect(tokenStore.refreshToken()).toBe("refresh-2");
  });

  it("reports network failures in Vietnamese", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new TypeError("Failed to fetch"));
    await expect(api("/x")).rejects.toThrow("Không kết nối được máy chủ");
  });
});
