import { reactive } from 'vue'
import { authAPI, resolveProfileUuid, userAPI, type UserProfile } from '../api/auth'
import { syncUnityAccessToken } from '../utils/unityAuthBridge'
import { decodeJwtRole } from '../utils/jwt'

const AUTH_PREVIEW_STORAGE_KEY = 'authPreviewMode'
const AUTH_PREVIEW_ACCESS_TOKEN = 'preview-mode-token'
const AUTH_USER_UUID_STORAGE_KEY = 'authUserUuid'
const AUTH_USER_ID_STORAGE_KEY = 'authUserId'
const PUBLIC_PATHS = new Set(['/', '/login', '/signup', '/terms', '/privacy', '/service-intro'])

export interface AuthUser {
  uuid: string
  user_id: string
  nickname: string
  role: string
  birth: string
  is_locked: boolean
  balance?: number
}

interface AuthState {
  user: AuthUser | null
  accessToken: string | null
  isLoggedIn: boolean
  isAuthReady: boolean
}

const state = reactive<AuthState>({
  user: null,
  accessToken: typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null,
  isLoggedIn: typeof window !== 'undefined' ? Boolean(localStorage.getItem('accessToken')) : false,
  isAuthReady: false,
})

let restorePromise: Promise<void> | null = null

const mapProfile = (profile: UserProfile, fallbackRole?: string | null): AuthUser => ({
  uuid: profile.uuid,
  user_id: profile.user_id ?? profile.id ?? '',
  nickname: profile.nickname,
  birth: profile.birth,
  role: profile.role || fallbackRole || 'user',
  is_locked: profile.is_locked,
  balance: profile.wallet?.balance ?? profile.point ?? 0,
})

const setAccessToken = (token: string | null) => {
  state.accessToken = token
  state.isLoggedIn = Boolean(token)

  if (typeof window === 'undefined') {
    return
  }

  if (token) {
    localStorage.setItem('accessToken', token)
  } else {
    localStorage.removeItem('accessToken')
  }

  syncUnityAccessToken(token)
}

const setStoredIdentity = (user: Pick<AuthUser, 'uuid' | 'user_id'> | null) => {
  if (typeof window === 'undefined') {
    return
  }

  if (user) {
    localStorage.setItem(AUTH_USER_UUID_STORAGE_KEY, user.uuid)
    localStorage.setItem(AUTH_USER_ID_STORAGE_KEY, user.user_id)
    return
  }

  localStorage.removeItem(AUTH_USER_UUID_STORAGE_KEY)
  localStorage.removeItem(AUTH_USER_ID_STORAGE_KEY)
}

const getPreviewUser = (): AuthUser => ({
  uuid: 'preview-user-uuid',
  user_id: 'preview_user',
  nickname: 'Preview User',
  birth: '2000-01-01',
  role: 'user',
  is_locked: false,
  balance: 0,
})

const isPreviewEnabled = () =>
  typeof window !== 'undefined' && localStorage.getItem(AUTH_PREVIEW_STORAGE_KEY) === 'true'

const applyPreviewSession = () => {
  state.user = getPreviewUser()
  state.accessToken = AUTH_PREVIEW_ACCESS_TOKEN
  state.isLoggedIn = true
  state.isAuthReady = true
  setStoredIdentity(state.user)
}

const applyProfile = async (token: string) => {
  const storedUuid = typeof window !== 'undefined' ? localStorage.getItem(AUTH_USER_UUID_STORAGE_KEY) : null
  const storedUserId = typeof window !== 'undefined' ? localStorage.getItem(AUTH_USER_ID_STORAGE_KEY) : null
  const uuid = await resolveProfileUuid({
    accessToken: token,
    storedUuid,
    userId: storedUserId,
  })

  if (!uuid) {
    throw new Error('invalid access token')
  }

  const profile = await userAPI.getProfile(uuid)
  const mappedProfile = mapProfile(profile, decodeJwtRole(token))
  state.user = mappedProfile
  setAccessToken(token)
  setStoredIdentity(mappedProfile)
}

const clearAuth = () => {
  state.user = null
  setAccessToken(null)
  setStoredIdentity(null)

  if (typeof window !== 'undefined') {
    localStorage.removeItem(AUTH_PREVIEW_STORAGE_KEY)
  }
}

const restoreAuth = async (pathname?: string) => {
  if (restorePromise) {
    return restorePromise
  }

  restorePromise = (async () => {
    try {
      const storedToken = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null
      const isPublicPath = pathname ? PUBLIC_PATHS.has(pathname) : false

      // Avoid re-validating the same healthy session on every route change.
      if (
        state.isAuthReady &&
        state.user &&
        state.accessToken &&
        storedToken &&
        state.accessToken === storedToken &&
        !isPreviewEnabled()
      ) {
        return
      }

      if (isPreviewEnabled()) {
        applyPreviewSession()
        return
      }

      if (storedToken) {
        try {
          await applyProfile(storedToken)
          return
        } catch {
          if (isPublicPath) {
            clearAuth()
            return
          }
        }
      }

      if (isPublicPath) {
        clearAuth()
        return
      }

      const refreshResponse = await authAPI.refresh()
      if (refreshResponse.accessToken) {
        await applyProfile(refreshResponse.accessToken)
        return
      }

      clearAuth()
    } catch {
      clearAuth()
    } finally {
      state.isAuthReady = true
      restorePromise = null
    }
  })()

  return restorePromise
}

const loginSuccess = (payload: { user: AuthUser; accessToken: string }) => {
  if (typeof window !== 'undefined') {
    localStorage.removeItem(AUTH_PREVIEW_STORAGE_KEY)
  }

  state.user = payload.user
  setAccessToken(payload.accessToken)
  setStoredIdentity(payload.user)
  state.isAuthReady = true
}

const enterPreviewMode = () => {
  if (typeof window !== 'undefined') {
    localStorage.removeItem('accessToken')
    localStorage.setItem(AUTH_PREVIEW_STORAGE_KEY, 'true')
  }

  applyPreviewSession()
}

const logout = () => {
  clearAuth()
  state.isAuthReady = true
}

export const useAuthStore = () => ({
  state,
  restoreAuth,
  loginSuccess,
  enterPreviewMode,
  logout,
})
