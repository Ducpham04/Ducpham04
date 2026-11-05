import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { authApi, meApi } from "../api/endpoints";
import { errorMessage, setSessionExpiredHandler, tokenStore } from "../api/client";
import type { User } from "../api/types";
import { zalo } from "../zalo/bridge";

type Status = "loading" | "ready" | "error";

interface AuthContextValue {
  status: Status;
  user: User | null;
  error: string | null;
  login: () => Promise<void>;
  logout: () => Promise<void>;
  setUser: (u: User) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * Luồng: có token → GET /me (tự refresh nếu hết hạn) → nếu thất bại thì
 * zmp-sdk getAccessToken() → POST /auth/zalo → lưu JWT.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<Status>("loading");
  const [user, setUser] = useState<User | null>(null);
  const [error, setError] = useState<string | null>(null);
  const inflight = useRef<Promise<void> | null>(null);

  const loginWithZalo = useCallback(async () => {
    const zaloToken = await zalo.getAccessToken();
    const res = await authApi.loginZalo(zaloToken);
    tokenStore.save(res);
    setUser(res.user);
  }, []);

  const login = useCallback(() => {
    if (inflight.current) return inflight.current;
    setStatus("loading");
    setError(null);
    inflight.current = (async () => {
      try {
        if (tokenStore.access) {
          try {
            setUser(await meApi.get());
            setStatus("ready");
            return;
          } catch {
            tokenStore.clear();
          }
        }
        await loginWithZalo();
        setStatus("ready");
      } catch (e) {
        setError(errorMessage(e));
        setStatus("error");
      } finally {
        inflight.current = null;
      }
    })();
    return inflight.current;
  }, [loginWithZalo]);

  const logout = useCallback(async () => {
    const rt = tokenStore.refresh;
    tokenStore.clear();
    setUser(null);
    if (rt) await authApi.logout(rt).catch(() => undefined);
    await login();
  }, [login]);

  useEffect(() => {
    setSessionExpiredHandler(() => void login());
    void login();
    return () => setSessionExpiredHandler(null);
  }, [login]);

  const value = useMemo(() => ({ status, user, error, login, logout, setUser }), [status, user, error, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
