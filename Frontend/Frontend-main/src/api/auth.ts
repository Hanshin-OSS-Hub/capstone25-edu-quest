import api from './axios'
import { decodeJwtUuid } from '../utils/jwt'

export interface LoginRequest {
  id: string
  password: string
}

export interface LoginResponse {
  accessToken: string
}

export interface UserProfile {
  uuid: string
  id?: string
  user_id?: string
  userId?: string
  email?: string
  birth: string
  nickname: string
  point?: number
  role: string
  is_locked: boolean
  isLocked?: boolean
  profile?: string
  profile_image_url?: string
  profileImageUrl?: string
  profile_image?: string
  profileImage?: string
  profile_url?: string
  profileUrl?: string
  avatar_url?: string
  avatarUrl?: string
  wallet?: {
    uuid: string
    balance: number
  }
}

export interface UserListItem {
  uuid: string
  id: string
  email: string
  nickname: string
}

export interface UpdateLoginIdRequest {
  newLoginId: string
  currentPassword: string
}

export interface UpdatePasswordRequest {
  currentPassword: string
  newPassword: string
  newPasswordConfirm: string
}

type UserListResponse =
  | {
      results: UserListItem[]
      page?: number
      size?: number
      sort?: string
      is_asc?: boolean
    }
  | UserListItem[]

type RoleListResponse =
  | { results: { uuid: string; name: string }[] }
  | { uuid: string; name: string }[]

const mapUserProfile = (profile: UserProfile): UserProfile => {
  const userId = profile.user_id ?? profile.userId ?? profile.id
  const isLocked = profile.is_locked ?? profile.isLocked ?? false
  const profileImageUrl = profile.profile_image_url ?? profile.profileImageUrl
  const profileImage = profile.profile_image ?? profile.profileImage
  const profileUrl = profile.profile_url ?? profile.profileUrl
  const avatarUrl = profile.avatar_url ?? profile.avatarUrl

  return {
    ...profile,
    id: profile.id ?? userId,
    user_id: userId,
    userId,
    is_locked: isLocked,
    isLocked,
    profile_image_url: profileImageUrl,
    profileImageUrl,
    profile_image: profileImage,
    profileImage,
    profile_url: profileUrl,
    profileUrl,
    avatar_url: avatarUrl,
    avatarUrl,
  }
}

export const authAPI = {
  signIn: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await api.post<LoginResponse>('/auth/sign-in', data, { skipAuth: true })
    return response.data
  },
  signUp: async (data: FormData): Promise<void> => {
    await api.post('/sign-up', data, { skipAuth: true })
  },
  findId: async (data: { email: string }): Promise<void> => {
    await api.post('/auth/find-id', data, { skipAuth: true })
  },
  findPassword: async (data: { email: string; id: string }): Promise<void> => {
    await api.post('/auth/find-password', data, { skipAuth: true })
  },
  resetPassword: async (data: { token: string; new_password: string }): Promise<void> => {
    await api.put('/auth/reset-password', data, { skipAuth: true })
  },
  refresh: async (): Promise<LoginResponse> => {
    const response = await api.post<LoginResponse>('/auth/refresh', {}, { skipAuth: true })
    return response.data
  },
  logout: async (): Promise<void> => {
    await api.post('/auth/logout', {})
  },
}

export const userAPI = {
  getProfile: async (uuid: string): Promise<UserProfile> => {
    const response = await api.get<UserProfile>(`/users/${uuid}`)
    return mapUserProfile(response.data)
  },
  getUserList: async (params?: { page?: number; size?: number; sort?: string; is_asc?: boolean }) => {
    const response = await api.get<UserListResponse>('/users', {
      params: {
        page: params?.page ?? 0,
        size: params?.size ?? 20,
        sort: params?.sort ?? 'created_at',
        is_asc: params?.is_asc ?? false,
      },
    })
    if (Array.isArray(response.data)) {
      return {
        results: response.data,
      }
    }

    return {
      ...response.data,
      results: Array.isArray(response.data.results) ? response.data.results : [],
    }
  },
  getUuidById: async (id: string): Promise<{ uuid: string }> => {
    const response = await api.get<{ uuid: string }>(`/users/${id}/uuid`)
    return response.data
  },
  updateProfile: async (uuid: string, data: FormData): Promise<void> => {
    await api.put(`/users/${uuid}`, data)
  },
  updateLoginId: async (data: UpdateLoginIdRequest): Promise<void> => {
    await api.patch('/users/me/login-id', data)
  },
  updatePassword: async (data: UpdatePasswordRequest): Promise<void> => {
    await api.patch('/users/me/password', data)
  },
  updateRole: async (uuid: string, roleUuid: string): Promise<void> => {
    await api.put(`/users/${uuid}/role`, { role_uuid: roleUuid })
  },
  lockUser: async (uuid: string): Promise<void> => {
    await api.put(`/users/${uuid}/lock`)
  },
  getRoles: async (): Promise<{ uuid: string; name: string }[]> => {
    const response = await api.get<RoleListResponse>('/users/roles')
    return Array.isArray(response.data) ? response.data : response.data.results ?? []
  },
  deleteUser: async (uuid: string): Promise<void> => {
    await api.delete(`/users/${uuid}`)
  },
}

export const resolveProfileUuid = async (options: {
  accessToken?: string | null
  storedUuid?: string | null
  userId?: string | null
}) => {
  if (options.storedUuid) {
    return options.storedUuid
  }

  if (options.accessToken) {
    const uuidFromToken = decodeJwtUuid(options.accessToken)
    if (uuidFromToken) {
      return uuidFromToken
    }
  }

  if (options.userId) {
    const response = await userAPI.getUuidById(options.userId)
    return response.uuid
  }

  return null
}
