import api from './axios'

export interface PagedResponse<T> {
  results: T[]
  page?: number
  size?: number
  sort?: string
  is_asc?: boolean
  isAsc?: boolean
}

export interface Stage {
  uuid: string
  title: string
  number: number
  reward: number
  created_at?: string
  createdAt?: string
  updated_at?: string
  updatedAt?: string
}

export interface ProblemBlock {
  order?: number
  number?: number
  code: string
}

export interface ProblemBlockPayload {
  answer?: number[]
  blocks?: ProblemBlock[]
}

export interface ProblemDetail {
  uuid: string
  stage?: string
  stage_uuid?: string
  stageUuid?: string
  stageTitle?: string
  stageNumber?: number
  type: string
  number: number
  summary: string
  example?: string
  expectedOutput?: string
  block?: string | ProblemBlockPayload
  hints?: HintResponse[]
}

export interface HintResponse {
  level: number
  point: number
  content: string
}

export interface SubmissionResponse {
  uuid: string
}

export interface EvaluationResponse {
  status: 'pending' | 'completed'
  result: boolean | null
}

export interface CreateProblemRequest {
  stage_uuid: string
  type: 'typing' | 'ordering' | 'final'
  number: number
  summary: string
  example: string
  expectedOutput: string
  block: {
    answer: number[]
    blocks: { order: number; code: string }[]
  }
  hints: HintResponse[]
}

export type UpdateProblemRequest = CreateProblemRequest

export interface ProgressResponse {
  results: {
    stage: string | number
    stageNumber?: number
    stage_number?: number
    total_question_count?: number
    totalQuestionCount?: number
    clear: number[]
  }[]
}

type StagePayload = {
  title: string
  number: number
  reward: number
}

type MaybePagedResponse<T> =
  | PagedResponse<T>
  | T[]

const stageListDefaults = {
  page: 0,
  size: 20,
  sort: 'created_at',
  is_asc: true,
}

const problemListDefaults = {
  page: 0,
  size: 20,
  sort: 'created_at',
  is_asc: true,
}

const normalizePagedResponse = <T>(data: MaybePagedResponse<T>) => {
  if (Array.isArray(data)) {
    return {
      results: data,
    }
  }

  return {
    ...data,
    results: Array.isArray(data.results) ? data.results : [],
  }
}

const decodeHtmlText = (value?: string) => {
  if (!value) return value

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

  return decoded
}

const mapHint = (hint: HintResponse): HintResponse => ({
  ...hint,
  content: decodeHtmlText(hint.content) ?? '',
})

const mapStage = (stage: Stage): Stage => {
  const createdAt = stage.created_at ?? stage.createdAt
  const updatedAt = stage.updated_at ?? stage.updatedAt

  return {
    ...stage,
    created_at: createdAt,
    createdAt,
    updated_at: updatedAt,
    updatedAt,
  }
}

const mapProblem = (problem: ProblemDetail): ProblemDetail => {
  const stageUuid = problem.stage_uuid ?? problem.stageUuid

  return {
    ...problem,
    stage_uuid: stageUuid,
    stageUuid,
    summary: decodeHtmlText(problem.summary) ?? '',
    example: decodeHtmlText(problem.example),
    expectedOutput: decodeHtmlText(problem.expectedOutput),
    hints: (problem.hints ?? []).map(mapHint),
  }
}

const normalizeStagePayload = (data: StagePayload) => {
  const title = data.title.trim()
  const number = Number(data.number)
  const reward = Number(data.reward)

  if (!title) {
    throw new Error('스테이지 제목을 입력해 주세요.')
  }

  if (!Number.isFinite(number) || number < 1) {
    throw new Error('스테이지 번호는 1 이상의 숫자여야 합니다.')
  }

  if (!Number.isFinite(reward) || reward < 0) {
    throw new Error('보상 코인은 0 이상의 숫자여야 합니다.')
  }

  return {
    title,
    number: Math.trunc(number),
    reward: Math.trunc(reward),
  }
}

export const stageAPI = {
  getStageList: async (params?: { page?: number; size?: number; sort?: string; is_asc?: boolean }) => {
    const response = await api.get<MaybePagedResponse<Stage>>('/stages', {
      params: { ...stageListDefaults, ...params },
    })
    const normalized = normalizePagedResponse(response.data)
    return {
      ...normalized,
      results: normalized.results.map(mapStage),
    }
  },
  getStage: async (uuid: string) => {
    const response = await api.get<Stage>(`/stages/${uuid}`)
    return mapStage(response.data)
  },
  createStage: async (data: StagePayload) => {
    const response = await api.post<Stage>('/stages', normalizeStagePayload(data))
    return response.data
  },
  updateStage: async (uuid: string, data: StagePayload) => {
    await api.put(`/stages/${uuid}`, normalizeStagePayload(data))
  },
  deleteStage: async (uuid: string) => {
    await api.delete(`/stages/${uuid}`)
  },
}

export const problemAPI = {
  getProblemList: async (params?: { page?: number; size?: number; sort?: string; is_asc?: boolean }) => {
    const response = await api.get<MaybePagedResponse<ProblemDetail>>('/problems', {
      params: { ...problemListDefaults, ...params },
    })
    const normalized = normalizePagedResponse(response.data)
    return {
      ...normalized,
      results: normalized.results.map(mapProblem),
    }
  },
  getProblemsByStage: async (stageNumber: number) => {
    const response = await api.get<MaybePagedResponse<ProblemDetail>>('/problems', {
      params: { stage_number: stageNumber },
    })
    return normalizePagedResponse(response.data).results.map(mapProblem)
  },
  getProblem: async (uuid: string) => {
    const response = await api.get<ProblemDetail>(`/problems/${uuid}`)
    return mapProblem(response.data)
  },
  getReviewProblems: async (userUuid: string) => {
    const response = await api.get<MaybePagedResponse<ProblemDetail>>(`/users/${userUuid}/review-problems`)
    const normalized = normalizePagedResponse(response.data)
    return {
      ...normalized,
      results: normalized.results.map(mapProblem),
    }
  },
  createProblem: async (data: CreateProblemRequest) => {
    const response = await api.post('/problems', data)
    return response.data
  },
  updateProblem: async (uuid: string, data: UpdateProblemRequest) => {
    await api.put(`/problems/${uuid}`, data)
  },
  deleteProblem: async (uuid: string) => {
    await api.delete(`/problems/${uuid}`)
  },
}

export const submissionAPI = {
  submitProblem: async (problemUuid: string, answer: string) => {
    const response = await api.post<SubmissionResponse>(`/problems/${problemUuid}/submissions`, { answer })
    return response.data
  },
  getEvaluation: async (submissionUuid: string) => {
    const response = await api.get<EvaluationResponse>(`/problems/evaluations/${submissionUuid}`)
    return response.data
  },
}

export const hintAPI = {
  getHint: async (problemUuid: string, level: number) => {
    const response = await api.get<HintResponse>(`/problems/${problemUuid}/hint`, {
      params: { level },
    })
    return mapHint(response.data)
  },
}

export const progressAPI = {
  getProgress: async (userUuid: string) => {
    const response = await api.get<ProgressResponse>(`/users/${userUuid}/progress`)
    return response.data
  },
}
