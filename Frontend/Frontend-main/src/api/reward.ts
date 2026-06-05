export interface RewardHistory {
  uuid: string
  stage_uuid: string
  stageUuid?: string
  amount: number
  created_at: string
  createdAt?: string
  stage?: {
    uuid: string
    title: string
    number: number
  }
}

const unsupportedRewardAPI = () =>
  Promise.reject(new Error('TODO: backend-dev has no /rewards controller yet.'))

export const rewardAPI = {
  // TODO: backend-dev.zip 기준 /rewards Controller가 없어 화면에서 직접 호출하지 않습니다.
  getRewardHistory: async (
    params?: { page?: number; size?: number }
  ): Promise<{ results: RewardHistory[]; page: number; size: number }> => {
    void params
    return unsupportedRewardAPI()
  },

  // TODO: backend-dev.zip 기준 /rewards Controller가 없어 화면에서 직접 호출하지 않습니다.
  getTotalReward: async (): Promise<{ total_reward: number }> => unsupportedRewardAPI(),

  // TODO: backend-dev.zip 기준 /rewards Controller가 없어 화면에서 직접 호출하지 않습니다.
  getStageReward: async (stageUuid: string): Promise<{ amount: number; is_completed: boolean }> => {
    void stageUuid
    return unsupportedRewardAPI()
  },
}
