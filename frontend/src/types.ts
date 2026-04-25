export type FileType = 'FILE' | 'FOLDER'

export interface FileDto {
  name: string
  type: FileType
  size: number
  modified: string
  hash: string
}

export interface TokenInfo {
  tenant: string
  realm: string
  subject: string
  mode: string
  exp: number
}

export interface ServiceInfo {
  version: string
  github: string
  swagger: string
}
