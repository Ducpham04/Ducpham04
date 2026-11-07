import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, refreshAccessToken, tokens } from './api'
import type { Me, TokenResponse } from './types'

interface AuthState {
  user: Me | null
  ready: boolean
  login: (email: string, password: string) => Promise<Me>
  acceptTokens: (t: TokenResponse) => Me
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const qc = useQueryClient()
  const [user, setUser] = useState<Me | null>(() => (tokens.refresh ? tokens.cachedUser() : null))
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let alive = true
    ;(async () => {
      if (tokens.refresh) {
        const t = await refreshAccessToken()
        if (alive) setUser(t ? tokens.cachedUser() : null)
      }
      if (alive) setReady(true)
    })()
    const onLogout = () => setUser(null)
    window.addEventListener('bookeasy:logout', onLogout)
    return () => {
      alive = false
      window.removeEventListener('bookeasy:logout', onLogout)
    }
  }, [])

  const acceptTokens = useCallback((t: TokenResponse) => {
    tokens.save(t)
    setUser(t.user)
    return t.user
  }, [])

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await api.post<TokenResponse>('/auth/login', { email, password })
      return acceptTokens(res.data)
    },
    [acceptTokens],
  )

  const logout = useCallback(async () => {
    const rt = tokens.refresh
    tokens.clear()
    setUser(null)
    qc.clear()
    if (rt) await api.post('/auth/logout', { refreshToken: rt }).catch(() => undefined)
  }, [qc])

  const value = useMemo(() => ({ user, ready, login, acceptTokens, logout }), [user, ready, login, acceptTokens, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

export function homeFor(user: Me): string {
  return user.role === 'SUPER_ADMIN' ? '/admin' : '/app'
}
