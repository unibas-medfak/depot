import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Download } from 'lucide-react'
import { getFileBlob } from '../api'
import { previewKind } from '../preview-kind'

export function Preview() {
  const params = useParams()
  const path = params['*'] ?? ''
  const [url, setUrl] = useState<string | null>(null)
  const [text, setText] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const name = path.split('/').pop() ?? path
  const kind = previewKind(name)
  const parentPath = path.includes('/')
    ? path.slice(0, path.lastIndexOf('/'))
    : ''
  const backTo = parentPath ? `/browse/${parentPath}` : '/browse'

  useEffect(() => {
    let revokeUrl: string | null = null
    let cancelled = false
    setUrl(null)
    setText(null)
    setError(null)

    getFileBlob(path)
      .then(async (blob) => {
        if (cancelled) return
        if (kind === 'text') {
          const content = await blob.text()
          if (cancelled) return
          setText(content)
        }
        const objectUrl = URL.createObjectURL(blob)
        revokeUrl = objectUrl
        if (!cancelled) setUrl(objectUrl)
      })
      .catch((e: Error) => {
        if (!cancelled) setError(e.message)
      })

    return () => {
      cancelled = true
      if (revokeUrl) URL.revokeObjectURL(revokeUrl)
    }
  }, [path, kind])

  return (
    <div className="preview">
      <nav className="preview-nav">
        <Link to={backTo} className="back">
          <ArrowLeft size={16} /> Back
        </Link>
        <h2 className="preview-title">{name}</h2>
        {url && (
          <a href={url} download={name} className="download">
            <Download size={16} /> Download
          </a>
        )}
      </nav>

      {error && <p className="error">{error}</p>}
      {!url && !error && <p className="muted">Loading…</p>}

      <div className="preview-body">
        {url && kind === 'image' && (
          <img src={url} alt={name} className="preview-image" />
        )}
        {url && kind === 'audio' && (
          <audio controls src={url} className="preview-audio" />
        )}
        {url && kind === 'video' && (
          <video controls src={url} className="preview-video" />
        )}
        {text !== null && <pre className="preview-text">{text}</pre>}
        {url && kind === 'binary' && (
          <p className="muted">
            This file type can't be previewed. Use the download button above.
          </p>
        )}
      </div>
    </div>
  )
}
