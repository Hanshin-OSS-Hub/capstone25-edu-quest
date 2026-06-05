export interface Wallet {
  uuid: string
  balance: number
  created_at: string
  createdAt?: string
  updated_at?: string
  updatedAt?: string
}

export interface WalletHistory {
  uuid: string
  amount: number
  reason?: string
  created_at: string
  createdAt?: string
  type: 'INCOME' | 'EXPENSE'
}

export interface UpdateWalletRequest {
  amount: number
  reason?: string
}

const unsupportedWalletAPI = () =>
  Promise.reject(new Error('TODO: backend-dev has no /wallet controller yet.'))

export const walletAPI = {
  // TODO: backend-dev.zip 기준 /wallet Controller가 없어 화면에서 직접 호출하지 않습니다.
  getWallet: async (): Promise<Wallet> => unsupportedWalletAPI(),

  // TODO: backend-dev.zip 기준 /wallet Controller가 없어 화면에서 직접 호출하지 않습니다.
  getBalance: async (): Promise<{ balance: number }> => unsupportedWalletAPI(),

  // TODO: backend-dev.zip 기준 /wallet Controller가 없어 화면에서 직접 호출하지 않습니다.
  getHistory: async (
    params?: { page?: number; size?: number; type?: 'INCOME' | 'EXPENSE' }
  ): Promise<{ results: WalletHistory[]; page: number; size: number }> => {
    void params
    return unsupportedWalletAPI()
  },

  // TODO: backend-dev.zip 기준 /wallet Controller가 없어 화면에서 직접 호출하지 않습니다.
  addBalance: async (data: UpdateWalletRequest): Promise<Wallet> => {
    void data
    return unsupportedWalletAPI()
  },

  // TODO: backend-dev.zip 기준 /wallet Controller가 없어 화면에서 직접 호출하지 않습니다.
  subtractBalance: async (data: UpdateWalletRequest): Promise<Wallet> => {
    void data
    return unsupportedWalletAPI()
  },
}
