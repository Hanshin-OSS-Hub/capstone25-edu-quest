import api from './axios'

export interface CommunityUser {
  uuid?: string
  nickname?: string
}

export interface CommunityPost {
  uuid: string
  title: string
  content?: string
  created_at: string
  createdAt?: string
  user?: CommunityUser
  member?: CommunityUser
  is_adopt?: boolean
  isAdopt?: boolean
  adopted_answer?: string | null
  adoptedAnswer?: string | null
}

export interface CommunityAnswer {
  uuid: string
  content: string
  created_at: string
  createdAt?: string
  is_adopt?: boolean
  isAdopt?: boolean
  user?: CommunityUser
  member?: CommunityUser
}

type CommunityPostListResponse =
  | { results: CommunityPost[] }
  | CommunityPost[]

const defaultListParams = {
  page: 0,
  size: 20,
  sort: 'created_at',
  is_asc: false,
}

const defaultAnswerListParams = {
  page: 0,
  size: 20,
  is_asc: true,
}

const mapCommunityPost = (post: CommunityPost): CommunityPost => {
  const createdAt = post.created_at ?? post.createdAt ?? ''
  const isAdopt = post.is_adopt ?? post.isAdopt
  const adoptedAnswer = post.adopted_answer ?? post.adoptedAnswer ?? null

  return {
    ...post,
    created_at: createdAt,
    createdAt,
    is_adopt: isAdopt,
    isAdopt,
    adopted_answer: adoptedAnswer,
    adoptedAnswer,
  }
}

const mapCommunityAnswer = (answer: CommunityAnswer): CommunityAnswer => {
  const createdAt = answer.created_at ?? answer.createdAt ?? ''
  const isAdopt = answer.is_adopt ?? answer.isAdopt

  return {
    ...answer,
    created_at: createdAt,
    createdAt,
    is_adopt: isAdopt,
    isAdopt,
  }
}

export const communityPostAPI = {
  getPostList: async (params?: {
    page?: number
    size?: number
    sort?: string
    is_asc?: boolean
    searchBy?: 'title' | 'content' | 'nickname'
    keyword?: string
  }) => {
    const response = await api.get<CommunityPostListResponse>('/questions', {
      params: { ...defaultListParams, ...params },
    })
    const results = Array.isArray(response.data) ? response.data : response.data.results ?? []
    return { results: results.map(mapCommunityPost) }
  },
  getPost: async (uuid: string) => {
    const response = await api.get<CommunityPost>(`/questions/${uuid}`)
    return mapCommunityPost(response.data)
  },
  createPost: async (data: { title: string; content: string }) => {
    const response = await api.post('/questions', data)
    return response.data
  },
  deletePost: async (uuid: string) => {
    await api.delete(`/questions/${uuid}`)
  },
}

export const communityAnswerAPI = {
  getAnswerList: async (questionUuid: string, params?: { page?: number; size?: number; is_asc?: boolean }) => {
    const response = await api.get<{ results: CommunityAnswer[] }>(`/questions/${questionUuid}/answers`, {
      params: { ...defaultAnswerListParams, ...params },
    })
    return {
      ...response.data,
      results: (response.data.results ?? []).map(mapCommunityAnswer),
    }
  },
  createAnswer: async (questionUuid: string, data: { content: string }) => {
    const response = await api.post(`/question/${questionUuid}/answers`, data)
    return response.data
  },
  deleteAnswer: async (uuid: string) => {
    await api.delete(`/answers/${uuid}`)
  },
  adoptAnswer: async (uuid: string) => {
    const response = await api.post(`/answers/${uuid}/adopt`)
    return response.data
  },
}
