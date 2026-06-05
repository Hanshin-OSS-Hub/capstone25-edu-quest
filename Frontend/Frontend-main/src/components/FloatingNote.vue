<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { noteAPI, type Note } from '../api/note'
import { useAuthStore } from '../store/auth'

type EditorNote = Note & { clientId: string; isLocalOnly?: boolean }

const props = withDefaults(
  defineProps<{
    mode?: 'overlay' | 'panel'
  }>(),
  {
    mode: 'overlay',
  }
)

const auth = useAuthStore()
const isOpen = ref(props.mode === 'panel')
const isEditorOpen = ref(false)
const notes = ref<EditorNote[]>([])
const selectedClientId = ref('')
const titleInput = ref('')
const contentInput = ref('')
const isLoading = ref(false)
const isSyncingInputs = ref(false)
const syncError = ref('')
const saveTimers = new Map<string, ReturnType<typeof setTimeout>>()
const LOCAL_NOTE_STORAGE_KEY = 'eduquest-floating-note-drafts'
const DEFAULT_NOTE_TITLE = '빠른 메모'
const DEFAULT_NOTE_CONTENT = '메모 내용을 입력해 주세요.'

const isPanelMode = computed(() => props.mode === 'panel')
const activeNote = computed(
  () => notes.value.find((note) => note.clientId === selectedClientId.value) ?? null
)

const formatNoteDate = (value?: string) => {
  if (!value) {
    return '방금 전'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

const getNotePreview = (content: string) => {
  const preview = content.trim().replace(/\s+/g, ' ')
  return preview || '내용 없음'
}

const toEditorNote = (note: Note, index: number): EditorNote => ({
  ...note,
  clientId: note.uuid ?? `note-${index}-${Date.now()}`,
})

const getLocalNotes = (): EditorNote[] => {
  if (typeof window === 'undefined') {
    return []
  }

  const stored = window.localStorage.getItem(LOCAL_NOTE_STORAGE_KEY)
  if (!stored) {
    return []
  }

  try {
    const parsed = JSON.parse(stored) as EditorNote[]
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

const saveLocalNotes = (nextNotes = notes.value) => {
  if (typeof window === 'undefined') {
    return
  }

  const localNotes = nextNotes.filter((note) => note.isLocalOnly)
  window.localStorage.setItem(LOCAL_NOTE_STORAGE_KEY, JSON.stringify(localNotes))
}

const clearSaveTimer = (clientId: string) => {
  const timer = saveTimers.get(clientId)
  if (timer) {
    clearTimeout(timer)
    saveTimers.delete(clientId)
  }
}

const syncInputsFromNote = (note: EditorNote | null) => {
  isSyncingInputs.value = true
  titleInput.value = note?.title ?? ''
  contentInput.value = note?.content ?? ''
  queueMicrotask(() => {
    isSyncingInputs.value = false
  })
}

const selectNote = (clientId: string, openEditor = true) => {
  selectedClientId.value = clientId
  const target = notes.value.find((note) => note.clientId === clientId) ?? null
  syncInputsFromNote(target)
  isEditorOpen.value = openEditor
}

const closeEditor = () => {
  isEditorOpen.value = false
}

const createLocalNote = () => {
  const now = new Date().toISOString()
  const localNote: EditorNote = {
    clientId: `local-note-${Date.now()}`,
    title: `${DEFAULT_NOTE_TITLE} ${notes.value.length + 1}`,
    content: DEFAULT_NOTE_CONTENT,
    created_at: now,
    updated_at: now,
    isLocalOnly: true,
  }

  notes.value = [localNote, ...notes.value]
  saveLocalNotes()
  selectNote(localNote.clientId, false)
}

const loadNotes = async () => {
  if (!auth.state.isLoggedIn) {
    notes.value = []
    selectedClientId.value = ''
    isEditorOpen.value = false
    syncInputsFromNote(null)
    return
  }

  isLoading.value = true
  syncError.value = ''

  try {
    const response = await noteAPI.getNoteList({
      page: 0,
      size: 30,
      sort: 'updated_at',
      is_asc: false,
    })

    const mapped = [...getLocalNotes(), ...response.results.map(toEditorNote)]
    notes.value = mapped

    if (mapped.length > 0) {
      selectNote(mapped[0].clientId, false)
    } else {
      selectedClientId.value = ''
      isEditorOpen.value = false
      syncInputsFromNote(null)
    }
  } catch (error) {
    console.error('failed to load notes:', error)
    const localNotes = getLocalNotes()
    notes.value = localNotes
    if (localNotes.length > 0) {
      selectNote(localNotes[0].clientId, false)
    }
    syncError.value = '메모를 불러오지 못했습니다. 로컬 초안을 표시합니다.'
  } finally {
    isLoading.value = false
  }
}

const persistNote = (note: EditorNote) => {
  if (note.isLocalOnly || !note.uuid) {
    saveLocalNotes()
    return
  }

  if (!note.title.trim() || !note.content.trim()) {
    clearSaveTimer(note.clientId)
    return
  }

  const noteUuid = note.uuid
  clearSaveTimer(note.clientId)

  const timer = setTimeout(async () => {
    try {
      await noteAPI.updateNote(noteUuid, {
        title: note.title,
        content: note.content,
      })
    } catch (error) {
      console.error('failed to update note:', error)
      syncError.value = '메모 저장에 실패했습니다.'
    } finally {
      saveTimers.delete(note.clientId)
    }
  }, 350)

  saveTimers.set(note.clientId, timer)
}

const updateActiveNote = (fields: Partial<EditorNote>) => {
  const current = activeNote.value
  if (!current) {
    return
  }

  syncError.value = ''
  notes.value = notes.value.map((note) =>
    note.clientId === current.clientId
      ? { ...note, ...fields, updated_at: new Date().toISOString() }
      : note
  )

  const updated = notes.value.find((note) => note.clientId === current.clientId)
  if (updated) {
    persistNote(updated)
  }
}

const handleAddNote = async () => {
  if (!auth.state.isLoggedIn) {
    syncError.value = '로그인 후 메모를 사용할 수 있습니다.'
    return
  }

  syncError.value = ''

  try {
    await noteAPI.createNote({
      title: `${DEFAULT_NOTE_TITLE} ${notes.value.length + 1}`,
      content: DEFAULT_NOTE_CONTENT,
    })
    await loadNotes()
  } catch (error) {
    console.error('failed to create note:', error)
    createLocalNote()
    syncError.value = '서버에 메모를 생성하지 못해 로컬 초안으로 열었습니다.'
  }
}

const handleDeleteNote = async () => {
  const current = activeNote.value
  if (!current) {
    return
  }

  clearSaveTimer(current.clientId)
  syncError.value = ''

  if (current.isLocalOnly || !current.uuid) {
    notes.value = notes.value.filter((note) => note.clientId !== current.clientId)
    saveLocalNotes()

    if (notes.value.length > 0) {
      selectNote(notes.value[0].clientId, false)
    } else {
      selectedClientId.value = ''
      isEditorOpen.value = false
      syncInputsFromNote(null)
    }
    return
  }

  try {
    await noteAPI.deleteNote(current.uuid)
    isEditorOpen.value = false
    await loadNotes()
  } catch (error) {
    console.error('failed to delete note:', error)
    syncError.value = '메모를 삭제하지 못했습니다.'
  }
}

watch(titleInput, (value) => {
  if (isSyncingInputs.value || !activeNote.value) {
    return
  }

  updateActiveNote({ title: value })
})

watch(contentInput, (value) => {
  if (isSyncingInputs.value || !activeNote.value) {
    return
  }

  updateActiveNote({ content: value })
})

onMounted(async () => {
  await auth.restoreAuth(window.location.pathname)
  await loadNotes()
})

onBeforeUnmount(() => {
  saveTimers.forEach((timer) => clearTimeout(timer))
  saveTimers.clear()
})
</script>

<template>
  <div
    :class="
      isPanelMode
        ? 'luxe-panel flex h-full min-h-0 flex-col p-5'
        : 'fixed bottom-6 right-6 z-50 flex flex-col items-end gap-3'
    "
  >
    <div
      v-if="isOpen"
      :class="
        isPanelMode
          ? 'flex min-h-0 flex-1 flex-col'
          : 'luxe-panel flex h-[38rem] max-h-[calc(100vh-4rem)] w-[30rem] max-w-[92vw] flex-col p-5'
      "
    >
      <div class="mb-4 flex items-center justify-between">
        <div>
          <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-[#1A2A4F]/45">Quick Memo</p>
          <h2 class="mt-1 text-lg font-semibold text-[#1A2A4F]">{{ isEditorOpen ? '메모 수정' : '빠른 메모' }}</h2>
        </div>
        <button
          v-if="isEditorOpen"
          type="button"
          class="luxe-button-soft rounded-full px-3 py-1.5 text-sm font-medium"
          @click="closeEditor"
        >
          목록
        </button>
        <button
          v-else-if="!isPanelMode"
          type="button"
          class="luxe-button-soft rounded-full px-3 py-1.5 text-sm font-medium"
          @click="isOpen = false"
        >
          닫기
        </button>
      </div>

      <div v-if="!isEditorOpen" class="mb-4 flex items-center justify-between gap-3">
        <div>
          <p class="text-sm font-semibold text-[#1A2A4F]">내 노트 목록</p>
          <p class="mt-0.5 text-xs font-medium text-[#1A2A4F]/45">{{ notes.length }}개의 메모</p>
        </div>
        <button
          type="button"
          class="luxe-button-accent shrink-0 rounded-[16px] px-4 py-2 text-sm font-medium"
          @click="handleAddNote"
        >
          + 새 메모
        </button>
      </div>

      <div
        v-if="isLoading"
        class="luxe-card-soft flex flex-1 items-center justify-center p-6 text-sm font-medium text-[#1A2A4F]/60"
      >
        메모를 불러오는 중입니다.
      </div>

      <div v-else-if="isEditorOpen && activeNote" class="flex min-h-0 flex-1 flex-col gap-4">
        <div>
          <label class="mb-2 block text-[11px] font-semibold uppercase tracking-[0.18em] text-[#1A2A4F]/45">Title</label>
          <input
            v-model="titleInput"
            type="text"
            placeholder="메모 제목을 입력해 주세요"
            class="luxe-input w-full rounded-[18px] px-4 py-3 text-base font-semibold text-[#1A2A4F]"
          >
        </div>

        <div class="flex min-h-0 flex-1 flex-col">
          <label class="mb-2 block text-[11px] font-semibold uppercase tracking-[0.18em] text-[#1A2A4F]/45">Content</label>
          <textarea
            v-model="contentInput"
            placeholder="메모 내용을 입력해 주세요"
            class="luxe-input min-h-0 flex-1 resize-none rounded-[22px] p-4 text-sm font-medium text-[#1A2A4F]"
          />
        </div>

        <div class="flex justify-between gap-3">
          <button
            type="button"
            class="rounded-[16px] border border-[#d8b2b2] bg-[#fff3f1] px-4 py-2 text-sm font-medium text-[#9d5252]"
            @click="handleDeleteNote"
          >
            삭제
          </button>
          <p class="self-center text-xs font-medium text-[#1A2A4F]/45">입력 내용은 자동 저장됩니다.</p>
        </div>
      </div>

      <div v-else-if="notes.length > 0" class="flex min-h-0 flex-1 flex-col">
        <div class="min-h-0 flex-1 space-y-2 overflow-y-auto pr-1">
          <button
            v-for="note in notes"
            :key="note.clientId"
            type="button"
            class="w-full rounded-[18px] border p-3 text-left transition"
            :class="
              note.clientId === selectedClientId
                ? 'border-[#1A2A4F]/18 bg-[#1A2A4F] text-white shadow-[0_10px_20px_rgba(26,42,79,0.16)]'
                : 'border-[#1A2A4F]/10 bg-[#FFF8F4] text-[#1A2A4F] hover:bg-white'
            "
            @click="selectNote(note.clientId)"
          >
            <div class="flex items-start justify-between gap-3">
              <p class="line-clamp-1 text-sm font-semibold">
                {{ note.title || '제목 없음' }}
              </p>
              <span
                class="shrink-0 text-[11px] font-medium"
                :class="note.clientId === selectedClientId ? 'text-white/60' : 'text-[#1A2A4F]/45'"
              >
                {{ formatNoteDate(note.updated_at ?? note.created_at) }}
              </span>
            </div>
            <p
              class="mt-1 line-clamp-2 text-xs leading-5"
              :class="note.clientId === selectedClientId ? 'text-white/70' : 'text-[#1A2A4F]/58'"
            >
              {{ getNotePreview(note.content) }}
            </p>
          </button>
        </div>
      </div>

      <div
        v-else
        class="luxe-card-soft flex flex-1 flex-col items-center justify-center p-6 text-center"
      >
        <p class="text-sm font-medium text-[#1A2A4F]/65">아직 메모가 없습니다.</p>
        <button
          type="button"
          class="luxe-button-accent mt-4 rounded-[16px] px-4 py-2 text-sm font-medium"
          @click="handleAddNote"
        >
          첫 메모 만들기
        </button>
      </div>

      <p v-if="syncError" class="mt-3 text-xs font-medium text-red-600">{{ syncError }}</p>
    </div>

    <button
      v-if="!isPanelMode"
      type="button"
      class="luxe-button-accent inline-flex items-center justify-center rounded-full px-6 py-3 text-sm font-medium transition hover:translate-y-[-1px]"
      @click="isOpen = !isOpen"
    >
      {{ isOpen ? '메모 닫기' : '빠른 메모' }}
    </button>
  </div>
</template>
