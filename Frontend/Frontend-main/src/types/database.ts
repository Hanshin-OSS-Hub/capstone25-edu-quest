// src/types/database.ts

export interface User {
  uuid: string;
  id: string;
  email?: string;
  birth: string;
  nickname: string;
  point: number;
  role: string;
  is_locked: boolean;
  profile?: string;
}

export interface Notice {
  uuid: string;
  title: string;
  content: string;
  created_at: string;
  updated_at?: string;
}

export interface Community {
  uuid: string;
  title: string;
  content: string;
  created_at: string;
  updated_at?: string;
  author: string;
  answers_count?: number;
}

export interface IncorrectNote {
  uuid: string;
  user_uuid: string;
  problem_uuid: string;
  wrong_answer: string;
  correct_answer: string;
  ai_explanation: string;
  is_reviewed: boolean;
  created_at: string;
  reviewed_at?: string;
}

export interface Bookmark {
  stage: string;
  type: string;
  number: number;
}

export interface StageProgress {
  stage: string;
  total_question_count: number;
  clear: number[];
}

// Request interfaces
export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
}

export interface LoginRequest {
  id: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
}

export interface NoteRequest {
  title: string;
  content: string;
}

export interface Note extends NoteRequest {
  uuid: string;
  created_at: string;
  updated_at?: string;
}

export interface StageClearRequest {
  stage_uuid: string;
  problem_uuid: string;
}