import { auth } from './firebase.js'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''

export async function apiFetch(path, options = {}, devSession = null) {
  const headers = new Headers(options.headers || {})
  headers.set('Content-Type', 'application/json')

  if (auth?.currentUser) {
    headers.set('Authorization', `Bearer ${await auth.currentUser.getIdToken()}`)
  } else if (devSession) {
    headers.set('X-Dev-Role', devSession.role)
    headers.set('X-Dev-User', devSession.uid)
  }

  const response = await fetch(`${apiBaseUrl}${path}`, { ...options, headers })
  const body = await response.json().catch(() => null)

  if (!response.ok) {
    const details = body?.details?.length ? ` ${body.details.join(' ')}` : ''
    throw new Error(`${body?.message || 'Não foi possível concluir a operação.'}${details}`)
  }
  return body
}

