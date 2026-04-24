import { useEffect, useState } from 'react'
import type { TokenInfo } from './types'

const TOKEN_KEY = 'depot.token'

const unauthorizedListeners = new Set<() => void>()

export function notifyUnauthorized() {
  unauthorizedListeners.forEach((fn) => fn())
}

export function onUnauthorized(fn: () => void) {
  unauthorizedListeners.add(fn)
  return () => {
    unauthorizedListeners.delete(fn)
  }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

function b64urlDecode(s: string): string {
  const padded = s + '==='.slice((s.length + 3) % 4)
  const binary = atob(padded.replace(/-/g, '+').replace(/_/g, '/'))
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}

export function decodeToken(token: string): TokenInfo | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const data = JSON.parse(b64urlDecode(payload))
    if (
      typeof data.tenant !== 'string' ||
      typeof data.realm !== 'string' ||
      typeof data.sub !== 'string' ||
      typeof data.mode !== 'string' ||
      typeof data.exp !== 'number'
    ) {
      return null
    }
    return {
      tenant: data.tenant,
      realm: data.realm,
      subject: data.sub,
      mode: data.mode,
      exp: data.exp,
    }
  } catch {
    return null
  }
}

export function isExpired(info: TokenInfo): boolean {
  return info.exp * 1000 <= Date.now()
}

interface AuthState {
  token: string | null
  info: TokenInfo | null
}

function loadInitial(): AuthState {
  const token = getToken()
  if (!token) return { token: null, info: null }
  const info = decodeToken(token)
  if (!info || isExpired(info)) {
    localStorage.removeItem(TOKEN_KEY)
    return { token: null, info: null }
  }
  return { token, info }
}

export function useAuth() {
  const [state, setState] = useState<AuthState>(loadInitial)

  useEffect(() => {
    return onUnauthorized(() => {
      localStorage.removeItem(TOKEN_KEY)
      setState({ token: null, info: null })
    })
  }, [])

  return {
    token: state.token,
    info: state.info,
    login: (token: string) => {
      const info = decodeToken(token)
      if (!info || isExpired(info)) return false
      localStorage.setItem(TOKEN_KEY, token)
      setState({ token, info })
      return true
    },
    logout: () => {
      localStorage.removeItem(TOKEN_KEY)
      setState({ token: null, info: null })
    },
  }
}
