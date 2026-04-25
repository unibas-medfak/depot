import { getToken, notifyUnauthorized } from './auth'
import type { FileDto } from './types'

async function apiFetch(url: string): Promise<Response> {
  const token = getToken()
  const res = await fetch(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (res.status === 401 || res.status === 403) {
    notifyUnauthorized()
    throw new Error('Session expired. Please log in again.')
  }
  if (!res.ok) {
    throw new Error(`Request failed (${res.status})`)
  }
  return res
}

export async function listFiles(path: string): Promise<FileDto[]> {
  const res = await apiFetch(`/list?path=${encodeURIComponent(path)}`)
  return res.json()
}

export async function getFileBlob(file: string): Promise<Blob> {
  const res = await apiFetch(`/get?file=${encodeURIComponent(file)}`)
  return res.blob()
}
