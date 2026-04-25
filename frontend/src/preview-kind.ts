export type PreviewKind = 'image' | 'audio' | 'video' | 'text' | 'pdf' | 'binary'

const TEXT_APPLICATION_TYPES = new Set([
  'application/json',
  'application/xml',
  'application/yaml',
  'application/x-yaml',
  'application/javascript',
  'application/x-sh',
])

export function kindFromMimeType(mimeType: string): PreviewKind {
  const type = mimeType.split(';', 1)[0].trim().toLowerCase()
  if (type.startsWith('image/')) return 'image'
  if (type.startsWith('audio/')) return 'audio'
  if (type.startsWith('video/')) return 'video'
  if (type.startsWith('text/')) return 'text'
  if (type === 'application/pdf') return 'pdf'
  if (TEXT_APPLICATION_TYPES.has(type)) return 'text'
  return 'binary'
}

const EXT_TO_KIND: Record<string, PreviewKind> = {
  png: 'image',
  jpg: 'image',
  jpeg: 'image',
  gif: 'image',
  webp: 'image',
  svg: 'image',
  bmp: 'image',
  avif: 'image',

  mp3: 'audio',
  wav: 'audio',
  ogg: 'audio',
  m4a: 'audio',
  flac: 'audio',
  aac: 'audio',

  mp4: 'video',
  webm: 'video',
  mov: 'video',
  mkv: 'video',

  pdf: 'pdf',

  txt: 'text',
  md: 'text',
  json: 'text',
  xml: 'text',
  csv: 'text',
  tsv: 'text',
  log: 'text',
  yaml: 'text',
  yml: 'text',
  html: 'text',
  htm: 'text',
  css: 'text',
  js: 'text',
  ts: 'text',
}

export function kindFromFilename(filename: string): PreviewKind {
  const dot = filename.lastIndexOf('.')
  if (dot < 0) return 'binary'
  const ext = filename.slice(dot + 1).toLowerCase()
  return EXT_TO_KIND[ext] ?? 'binary'
}
