import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  ChevronRight,
  File,
  FileText,
  Film,
  Folder,
  Home,
  Image,
  Music,
} from 'lucide-react'
import { listFiles } from '../api'
import type { FileDto } from '../types'
import { kindFromFilename } from '../preview-kind'

function entryIcon(entry: FileDto) {
  if (entry.type === 'FOLDER') return <Folder size={18} />
  switch (kindFromFilename(entry.name)) {
    case 'image':
      return <Image size={18} />
    case 'audio':
      return <Music size={18} />
    case 'video':
      return <Film size={18} />
    case 'text':
    case 'pdf':
      return <FileText size={18} />
    default:
      return <File size={18} />
  }
}

function formatSize(n: number) {
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  if (n < 1024 * 1024 * 1024) return `${(n / 1024 / 1024).toFixed(1)} MB`
  return `${(n / 1024 / 1024 / 1024).toFixed(2)} GB`
}

const dateFormatter = new Intl.DateTimeFormat(undefined, {
  dateStyle: 'short',
  timeStyle: 'short',
})

function formatDate(iso: string) {
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? '' : dateFormatter.format(d)
}

function sortEntries(entries: FileDto[]): FileDto[] {
  return [...entries].sort((a, b) => {
    if (a.type !== b.type) return a.type === 'FOLDER' ? -1 : 1
    return a.name.localeCompare(b.name)
  })
}

export function Browser() {
  const params = useParams()
  const path = params['*'] ?? ''
  const [entries, setEntries] = useState<FileDto[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setEntries(null)
    setError(null)

    listFiles(path)
      .then((data) => {
        if (!cancelled) setEntries(sortEntries(data))
      })
      .catch((e: Error) => {
        if (!cancelled) setError(e.message)
      })

    return () => {
      cancelled = true
    }
  }, [path])

  const segments = path.split('/').filter(Boolean)

  return (
    <div className="browser">
      <nav className="breadcrumb">
        <Link to="/browse" className="crumb">
          <Home size={16} />
        </Link>
        {segments.map((seg, i) => {
          const to = '/browse/' + segments.slice(0, i + 1).join('/')
          return (
            <span key={to} className="crumb-group">
              <ChevronRight size={14} />
              <Link to={to} className="crumb">
                {seg}
              </Link>
            </span>
          )
        })}
      </nav>

      {error && <p className="error">{error}</p>}
      {!entries && !error && <p className="muted">Loading…</p>}
      {entries && entries.length === 0 && (
        <p className="muted">This folder is empty.</p>
      )}
      {entries && entries.length > 0 && (
        <ul className="entries">
          {entries.map((e) => {
            const childPath = path ? `${path}/${e.name}` : e.name
            const to =
              e.type === 'FOLDER'
                ? `/browse/${childPath}`
                : `/view/${childPath}`
            return (
              <li key={e.name}>
                <Link to={to} className="entry">
                  <span className="entry-icon">{entryIcon(e)}</span>
                  <span className="entry-name">{e.name}</span>
                  <span className="entry-modified">{formatDate(e.modified)}</span>
                  <span className="entry-size">
                    {e.type === 'FILE' ? formatSize(e.size) : ''}
                  </span>
                </Link>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
