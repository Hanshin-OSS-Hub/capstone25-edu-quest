export interface DecodedJwtPayload {
  uuid?: string
  role?: string
  sub?: string
  [key: string]: unknown
}

const decodeBase64Url = (value: string) => {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), '=')

  return decodeURIComponent(
    atob(padded)
      .split('')
      .map((char) => `%${(`00${char.charCodeAt(0).toString(16)}`).slice(-2)}`)
      .join('')
  )
}

export const decodeJwtPayload = (token: string): DecodedJwtPayload | null => {
  try {
    const [, payload] = token.split('.')

    if (!payload) {
      return null
    }

    return JSON.parse(decodeBase64Url(payload)) as DecodedJwtPayload
  } catch {
    return null
  }
}

export const decodeJwtUuid = (token: string): string | null => {
  const payload = decodeJwtPayload(token)
  return typeof payload?.uuid === 'string' ? payload.uuid : null
}

export const decodeJwtRole = (token: string): string | null => {
  const payload = decodeJwtPayload(token)
  return typeof payload?.role === 'string' ? payload.role : null
}
