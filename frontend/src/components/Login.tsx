import { useState } from 'react'
import type { FormEvent } from 'react'
import { decodeToken, isExpired } from '../auth'

interface Props {
  onLogin: (token: string) => boolean
}

export function Login({ onLogin }: Props) {
  const [value, setValue] = useState('')
  const [error, setError] = useState<string | null>(null)

  function submit(e: FormEvent) {
    e.preventDefault()
    const token = value.trim().replace(/^Bearer\s+/i, '')
    const info = decodeToken(token)
    if (!info) {
      setError("That doesn't look like a depot token.")
      return
    }
    if (isExpired(info)) {
      setError('This token has expired.')
      return
    }
    if (!onLogin(token)) {
      setError('Could not accept this token.')
    }
  }

  return (
    <div className="login">
      <div className="login-card">
        <h1>depot</h1>
        <p>Paste your access token to continue.</p>
        <form onSubmit={submit}>
          <textarea
            value={value}
            onChange={(e) => {
              setValue(e.target.value)
              setError(null)
            }}
            placeholder="eyJhbGciOiJIUzI1NiIs..."
            rows={6}
            spellCheck={false}
            autoFocus
          />
          {error && <p className="error">{error}</p>}
          <button type="submit" disabled={!value.trim()}>
            Continue
          </button>
        </form>
      </div>
    </div>
  )
}
