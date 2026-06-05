import api from './axios'

export interface BookmarkItem {
  stage?: string
  type?: string
  number?: number
  problem_uuid?: string
  problemUuid?: string
  title?: string
  summary?: string
  created_at?: string
  createdAt?: string
}

export const bookmarkAPI = {
  getBookmarkList: async (userUuid: string, params?: { page?: number; size?: number; sort?: string; is_asc?: boolean }) => {
    const response = await api.get<{ results: BookmarkItem[] }>(`/users/${userUuid}/bookmarks`, {
      params: {
        page: params?.page ?? 0,
        size: params?.size ?? 20,
        sort: params?.sort ?? 'created_at',
        is_asc: params?.is_asc ?? false,
      },
    })
    return {
      ...response.data,
      results: (response.data.results ?? []).map((item) => {
        const problemUuid = item.problem_uuid ?? item.problemUuid
        return {
          ...item,
          problem_uuid: problemUuid,
          problemUuid,
        }
      }),
    }
  },
  createBookmark: async (problemUuid: string) => {
    await api.post(`/problems/${problemUuid}/bookmark`)
  },
  deleteBookmark: async (problemUuid: string) => {
    await api.delete(`/problems/${problemUuid}/bookmark`)
  },
}
