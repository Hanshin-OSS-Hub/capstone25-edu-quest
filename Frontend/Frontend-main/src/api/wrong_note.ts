import api from './axios'

export interface WrongNote {
  uuid: string
  id?: number
  problem_id?: number
  problemId?: number
  problem_uuid?: string
  problemUuid?: string
  problem_summary?: string
  problemSummary?: string
  user_uuid?: string
  userUuid?: string
  wrong_answer: string
  wrongAnswer?: string
  feedback?: string
  is_reviewed: boolean
  isReviewed?: boolean
  last_submitted_at?: string
  lastSubmittedAt?: string
  created_at: string
  createdAt?: string
  updated_at?: string
  updatedAt?: string
}

type WrongNoteListResponse =
  | { results: Record<string, unknown>[] }
  | Record<string, unknown>[]

const decodeHtmlText = (value?: string) => {
  if (!value) return undefined

  let decoded = value

  for (let i = 0; i < 3; i += 1) {
    decoded = decoded
      .replaceAll('&amp;', '&')
      .replaceAll('&quot;', '"')
      .replaceAll('&#34;', '"')
      .replaceAll('&#39;', "'")
      .replaceAll('&lt;', '<')
      .replaceAll('&gt;', '>')
      .replaceAll('&#96;', '`')
      .replace(/&#(x?[0-9a-fA-F]+);/g, (_, rawCode: string) => {
        const isHex = rawCode.startsWith('x') || rawCode.startsWith('X')
        const codePoint = Number.parseInt(isHex ? rawCode.slice(1) : rawCode, isHex ? 16 : 10)
        return Number.isFinite(codePoint) ? String.fromCodePoint(codePoint) : `&#${rawCode};`
      })
  }

  return decoded.replace(/^\s*aiExplanation\s*:\s*/, '').trim()
}

const mapWrongNote = (note: Record<string, unknown>): WrongNote => {
  const problemId =
    typeof note.problemId === 'number'
      ? note.problemId
      : typeof note.problem_id === 'number'
        ? note.problem_id
        : undefined
  const userUuid =
    typeof note.userUuid === 'string'
      ? note.userUuid
      : typeof note.user_uuid === 'string'
        ? note.user_uuid
        : undefined
  const problemUuid =
    typeof note.problemUuid === 'string'
      ? note.problemUuid
      : typeof note.problem_uuid === 'string'
        ? note.problem_uuid
        : undefined
  const problemSummary =
    typeof note.problemSummary === 'string'
      ? note.problemSummary
      : typeof note.problem_summary === 'string'
        ? note.problem_summary
        : undefined
  const wrongAnswer =
    typeof note.wrongAnswer === 'string'
      ? note.wrongAnswer
      : typeof note.wrong_answer === 'string'
        ? note.wrong_answer
        : ''
  const isReviewed = typeof note.isReviewed === 'boolean' ? note.isReviewed : Boolean(note.is_reviewed)
  const lastSubmittedAt =
    typeof note.lastSubmittedAt === 'string'
      ? note.lastSubmittedAt
      : typeof note.last_submitted_at === 'string'
        ? note.last_submitted_at
        : undefined
  const createdAt =
    typeof note.createdAt === 'string'
      ? note.createdAt
      : typeof note.created_at === 'string'
        ? note.created_at
        : ''
  const updatedAt =
    typeof note.updatedAt === 'string'
      ? note.updatedAt
      : typeof note.updated_at === 'string'
        ? note.updated_at
        : undefined

  return {
    uuid: typeof note.uuid === 'string' ? note.uuid : '',
    id: typeof note.id === 'number' ? note.id : undefined,
    problem_id: problemId,
    problemId,
    problem_uuid: problemUuid,
    problemUuid,
    problem_summary: decodeHtmlText(problemSummary),
    problemSummary: decodeHtmlText(problemSummary),
    user_uuid: userUuid,
    userUuid,
    wrong_answer: decodeHtmlText(wrongAnswer) ?? '',
    wrongAnswer: decodeHtmlText(wrongAnswer),
    feedback: decodeHtmlText(typeof note.feedback === 'string' ? note.feedback : undefined),
    is_reviewed: isReviewed,
    isReviewed,
    last_submitted_at: lastSubmittedAt,
    lastSubmittedAt,
    created_at: createdAt,
    createdAt,
    updated_at: updatedAt,
    updatedAt,
  }
}

export const wrongNoteAPI = {
  getWrongNote: async (uuid: string) => {
    const response = await api.get<Record<string, unknown>>(`/wrong-notes/${uuid}`)
    return mapWrongNote(response.data)
  },
  getWrongNoteList: async (params?: { page?: number; size?: number; sort?: string; is_asc?: boolean }) => {
    const response = await api.get<WrongNoteListResponse>('/wrong-notes', {
      params: {
        page: params?.page ?? 0,
        size: params?.size ?? 20,
        sort: params?.sort ?? 'created_at',
        is_asc: params?.is_asc ?? false,
      },
    })
    const results = Array.isArray(response.data) ? response.data : response.data.results ?? []
    return {
      ...(Array.isArray(response.data) ? {} : response.data),
      results: results.map(mapWrongNote),
    }
  },
  getUserWrongNotes: async (userUuid: string, params?: { page?: number; size?: number; sort?: string; is_asc?: boolean }) => {
    const response = await api.get<WrongNoteListResponse>(`/wrong-notes/users/${userUuid}`, {
      params: {
        page: params?.page ?? 0,
        size: params?.size ?? 20,
        sort: params?.sort ?? 'created_at',
        is_asc: params?.is_asc ?? false,
      },
    })
    const results = Array.isArray(response.data) ? response.data : response.data.results ?? []
    return {
      ...(Array.isArray(response.data) ? {} : response.data),
      results: results.map(mapWrongNote),
    }
  },
  deleteWrongNote: async (uuid: string) => {
    await api.delete(`/wrong-notes/${uuid}`)
  },
  requestAiFeedback: async (uuid: string) => {
    const response = await api.put<Record<string, unknown>>(`/wrong-notes/${uuid}/ai-feedback`)
    return mapWrongNote(response.data)
  },
}
