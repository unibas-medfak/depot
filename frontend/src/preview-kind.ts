export type PreviewKind = 'image' | 'audio' | 'video' | 'text' | 'binary'

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

export function previewKind(filename: string): PreviewKind {
  const dot = filename.lastIndexOf('.')
  if (dot < 0) return 'binary'
  const ext = filename.slice(dot + 1).toLowerCase()
  return EXT_TO_KIND[ext] ?? 'binary'
}
