import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api, refreshSession, tokenStore } from "@/api/client";
import type { Me, Role, TokenResponse } from "@/api/types";

interface AuthState {
  user: Me | null;
  ready: boolean;
  login: (email: string, password: string) => Promise<Me>;
  logout: () => Promise<void>;
  hasRole: (...roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Me | null>(null);
  const [ready, setReady] = useState(false);
  const queryClient = useQueryClient();

  useEffect(() => {
    tokenStore.onExpired(() => {
      setUser(null);
      queryClient.clear();
    });
    (async () => {
      if (tokenStore.refreshToken() && (await refreshSession())) {
        try {
          setUser(await api<Me>("/auth/me"));
        } catch {
          tokenStore.clear();
        }
      }
      setReady(true);
    })();
  }, [queryClient]);

  const login = useCallback(async (email: string, password: string) => {
    const res = await api<TokenResponse>("/auth/login", { method: "POST", body: { email, password } });
    tokenStore.setTokens(res.accessToken, res.refreshToken);
    setUser(res.user);
    return res.user;
  }, []);

  const logout = useCallback(async () => {
    const refresh = tokenStore.refreshToken();
    if (refresh) {
      try {
        await api("/auth/logout", { method: "POST", body: { refreshToken: refresh } });
      } catch { /* bỏ qua */ }
    }
    tokenStore.clear();
    setUser(null);
    queryClient.clear();
  }, [queryClient]);

  const hasRole = useCallback((...roles: Role[]) => !!user && roles.includes(user.role), [user]);

  const value = useMemo(() => ({ user, ready, login, logout, hasRole }), [user, ready, login, logout, hasRole]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

export function homePathFor(role: Role): string {
  switch (role) {
    case "SUPER_ADMIN": return "/app/tenants";
    case "STUDENT": return "/portal";
    case "TEACHER": return "/app/today";
    default: return "/app/dashboard";
  }
}
