import api from './axios'

export interface Note {
  uuid?: string
  title: string
  content: string
  author_uuid?: string
  authorUuid?: string
  created_at?: string
  createdAt?: string
  updated_at?: string
  updatedAt?: string
}

type NoteListResponse =
  | { results: Record<string, unknown>[] }
  | Record<string, unknown>[]

const mapNote = (note: Record<string, unknown>): Note => ({
  uuid: typeof note.uuid === 'string' ? note.uuid : undefined,
  title: typeof note.title === 'string' ? note.title : '',
  content: typeof note.content === 'string' ? note.content : '',
  author_uuid:
    typeof note.author_uuid === 'string'
      ? note.author_uuid
      : typeof note.authorUuid === 'string'
        ? note.authorUuid
        : undefined,
  authorUuid:
    typeof note.authorUuid === 'string'
      ? note.authorUuid
      : typeof note.author_uuid === 'string'
        ? note.author_uuid
        : undefined,
  created_at:
    typeof note.created_at === 'string'
      ? note.created_at
      : typeof note.createdAt === 'string'
        ? note.createdAt
        : undefined,
  createdAt:
    typeof note.createdAt === 'string'
      ? note.createdAt
      : typeof note.created_at === 'string'
        ? note.created_at
        : undefined,
  updated_at:
    typeof note.updated_at === 'string'
      ? note.updated_at
      : typeof note.updatedAt === 'string'
        ? note.updatedAt
        : undefined,
  updatedAt:
    typeof note.updatedAt === 'string'
      ? note.updatedAt
      : typeof note.updated_at === 'string'
        ? note.updated_at
        : undefined,
})

export const noteAPI = {
  getNoteList: async (params?: {
    page?: number
    size?: number
    sort?: string
    is_asc?: boolean
    searchBy?: 'title' | 'content'
    keyword?: string
  }) => {
    const response = await api.get<NoteListResponse>('/notes', {
      params: {
        page: params?.page ?? 0,
        size: params?.size ?? 20,
        sort: params?.sort ?? 'created_at',
        is_asc: params?.is_asc ?? false,
        searchBy: params?.searchBy,
        keyword: params?.keyword,
      },
    })
    const results = Array.isArray(response.data) ? response.data : response.data.results ?? []
    return {
      ...(Array.isArray(response.data) ? {} : response.data),
      results: results.map(mapNote),
    }
  },
  getNote: async (uuid: string) => {
    const response = await api.get<Record<string, unknown>>(`/notes/${uuid}`)
    return mapNote(response.data)
  },
  createNote: async (data: { title: string; content: string }) => {
    const response = await api.post<string | void>('/notes', data)
    return response.data
  },
  updateNote: async (uuid: string, data: { title: string; content: string }) => {
    await api.put(`/notes/${uuid}`, data)
  },
  deleteNote: async (uuid: string) => {
    await api.delete(`/notes/${uuid}`)
  },
}
