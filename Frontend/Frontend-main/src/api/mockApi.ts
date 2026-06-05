const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));
const noteStorageKey = 'eduquest-floating-notes';

export interface LoginRequest {
  user_id: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  member: {
    uuid: string;
    user_id: string;
    nickname: string;
    birth: string;
    role: string;
    is_locked: boolean;
  };
}

export interface NoteItem {
  id: string;
  title: string;
  content: string;
  updatedAt: string;
}

export interface QuickStat {
  label: string;
  value: string;
}

export interface StageQuest {
  title: string;
  status: string;
  badge: string;
}

export interface WrongNote {
  id: number;
  question: string;
  wrongAnswer: string;
}

export interface BackpackItem {
  icon: string;
  title: string;
  type: 'bookmark' | 'note';
}

export interface CommunityItem {
  label: string;
  title: string;
  status: string;
}

export interface ProgressStage {
  uuid: string;
  number: number;
  title: string;
  reward: string;
  clearCount: number;
  totalCount: number;
  isCleared: boolean;
}

export const authMock = {
  signIn: async (data: LoginRequest): Promise<LoginResponse> => {
    await delay(400);
    return {
      accessToken: 'mock-access-token',
      refreshToken: 'mock-refresh-token',
      member: {
        uuid: 'mock-user-uuid',
        user_id: data.user_id,
        nickname: 'User',
        birth: '2000-01-01',
        role: 'student',
        is_locked: false,
      },
    };
  },

  signUp: async (): Promise<void> => {
    await delay(400);
    return;
  },
};

export const noteApi = {
  fetchNotes: async (): Promise<NoteItem[]> => {
    await delay(250);
    const stored = localStorage.getItem(noteStorageKey);
    if (stored) {
      try {
        return JSON.parse(stored) as NoteItem[];
      } catch {
        return [];
      }
    }
    return [];
  },

  saveNotes: async (notes: NoteItem[]): Promise<void> => {
    await delay(150);
    localStorage.setItem(noteStorageKey, JSON.stringify(notes));
  },
};

export const homeApi = {
  fetchQuickStats: async (): Promise<QuickStat[]> => {
    await delay(200);
    return [];
  },

  fetchStageQuests: async (): Promise<StageQuest[]> => {
    await delay(250);
    return [];
  },

  fetchWrongNotes: async (): Promise<WrongNote[]> => {
    await delay(250);
    return [];
  },

  fetchBackpackItems: async (): Promise<BackpackItem[]> => {
    await delay(250);
    return [];
  },

  fetchCommunityItems: async (): Promise<CommunityItem[]> => {
    await delay(250);
    return [];
  },
};

export const progressApi = {
  fetchProgressStages: async (): Promise<ProgressStage[]> => {
    await delay(400);
    return [];
  },
};

// New API functions based on Postman spec
import type {
  User,
  Notice,
  Community,
  IncorrectNote,
  Bookmark,
  StageProgress,
  LoginResponse as NewLoginResponse,
  NoteRequest,
  Note,
} from '../types/database';

// New API exports
export const authApi = {
  register: async (): Promise<void> => {
    await delay(400);
  },

  login: async (): Promise<NewLoginResponse> => {
    await delay(400);
    return { accessToken: 'mock-access-token' };
  },
};

export const userApi = {
  getProfile: async (): Promise<User> => {
    await delay(200);
    throw new Error('Not implemented');
  },
};

export const notesApiNew = {
  getNotes: async (): Promise<Note[]> => {
    await delay(250);
    return [];
  },

  createNote: async (data: NoteRequest): Promise<Note> => {
    await delay(250);
    const newNote: Note = {
      uuid: `note-${Date.now()}`,
      ...data,
      created_at: new Date().toISOString(),
    };
    return newNote;
  },
};

export const stagesApi = {
  getProgress: async (): Promise<StageProgress[]> => {
    await delay(300);
    return [];
  },

  clearStage: async (): Promise<void> => {
    await delay(300);
  },
};

export const boardApi = {
  getNotices: async (): Promise<Notice[]> => {
    await delay(250);
    return [];
  },

  getCommunity: async (): Promise<Community[]> => {
    await delay(250);
    return [];
  },
};

export const studyApi = {
  getIncorrectNotes: async (): Promise<IncorrectNote[]> => {
    await delay(250);
    return [];
  },

  getBookmarks: async (): Promise<Bookmark[]> => {
    await delay(250);
    return [];
  },
};

// Dashboard API functions
export const dashboardApi = {
  fetchRecentNotices: async (): Promise<Notice[]> => {
    await delay(250);
    return [];
  },

  fetchRecentPosts: async (): Promise<Community[]> => {
    await delay(250);
    return [];
  },

  fetchUserProgress: async (): Promise<{ level: number; exp: number; expMax: number; clearedStages: number; totalStages: number }> => {
    await delay(300);
    return {
      level: 1,
      exp: 0,
      expMax: 100,
      clearedStages: 0,
      totalStages: 0,
    };
  },
};
