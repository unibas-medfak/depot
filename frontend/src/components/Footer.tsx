import { useEffect, useState } from 'react'
import type { ServiceInfo } from '../types'

export function Footer() {
  const [info, setInfo] = useState<ServiceInfo | null>(null)

  useEffect(() => {
    let cancelled = false
    fetch('/info')
      .then((r) => (r.ok ? r.json() : null))
      .then((data: ServiceInfo | null) => {
        if (!cancelled) setInfo(data)
      })
      .catch(() => {
        /* keep footer hidden if /info is unreachable */
      })
    return () => {
      cancelled = true
    }
  }, [])

  if (!info) return null

  return (
    <footer className="app-footer">
      <span className="version">depot {info.version}</span>
      <span className="sep">·</span>
      <a href={info.github} target="_blank" rel="noreferrer noopener">GitHub</a>
      <span className="sep">·</span>
      <a href={info.swagger} target="_blank" rel="noreferrer noopener">Swagger</a>
    </footer>
  )
}
