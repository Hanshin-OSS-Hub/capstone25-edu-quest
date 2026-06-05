# Vue 3 Migration Report

## 1. `plan.md` Summary

- This migration prioritizes behavior preservation over redesign.
- Existing route paths, user flows, API semantics, auth handling, error handling, and UI intent were kept as the first-class constraints.
- Postman contracts were treated as the primary API reference when the React code and backend hints diverged.
- Risky mismatches were corrected only where the contract was clear; ambiguous areas remain as TODOs.

## 2. Analysis Result

### Frontend

- Build tool: Vite
- Language: TypeScript
- Previous framework: React 19 + React Router + Redux Toolkit
- Current framework: Vue 3 + Composition API + Vue Router + singleton reactive auth store
- Styling: existing Tailwind-based classes preserved
- Current auth persistence: `accessToken` in `localStorage`
- Refresh flow: migrated toward cookie-based refresh while keeping bearer injection and retry behavior

### Backend / ERD / Postman

- Backend zip contained Spring Boot skeleton plus migration SQL, but not enough controller/service code to serve as the sole contract source.
- ERD/migration SQL confirmed core domains: member, role, user_role, stage, problem, submission, evaluation, hint, wrong_note, bookmark, note, community_post, community_answer, wallet, reward history, file, reset_password_token.
- Postman exposed the operational contract for auth, learning, submission, progress, wrong notes, bookmarks, notes, and community.

## 3. Functional Consistency Table

| Feature | React Status | Vue Status | Contract Basis | Risk |
| --- | --- | --- | --- | --- |
| Login / logout | Real API | Preserved | Postman + current flow | High |
| Refresh token | React stored refresh locally in some paths | Switched to cookie-respecting refresh request/retry | Postman + `plan.md` | High |
| Sign-up multipart | Real API | Preserved with `profile` JSON blob + optional file | Postman | High |
| Profile read | Real API | Preserved | Postman + current UI fields | Medium |
| User admin list / role / lock | Real API | Preserved | Postman | Medium |
| Stage list | Real API | Preserved | Postman | Medium |
| Problem read / submit | Real API | Preserved, with `string | array` submit support | Postman | High |
| Hint lookup | Not surfaced strongly in React | Added safe contract-aware call path | Postman | Medium |
| Progress | Mixed mock/API usage | Connected to real progress API | Postman | Medium |
| Wrong note list | Static in React | Connected to real API | Postman | Medium |
| Bookmark list | Static in React | Connected to real API | Postman | Medium |
| Community list / detail / answer | React used plural path for create | Corrected to singular create + plural list | Postman | High |
| Notice / review / floating note | Static/mock | Preserved as static/mock | Current frontend behavior | Low |

## 4. Actual Changes

- Replaced React entrypoint with Vue `createApp`.
- Replaced React Router with Vue Router while preserving route paths.
- Replaced Redux auth slice with a singleton reactive auth store.
- Rebuilt common UI pieces as `.vue` SFCs.
- Reimplemented all page components as Vue Composition API SFCs.
- Updated API modules to respect key Postman contracts:
  - `PUT /auth/reset-password`
  - cookie-based `/auth/refresh`
  - community answer create on `/question/{uuid}/answers`
  - answer submission supports `string | number[]`
  - 204/201 flows no longer assume response bodies

## 5. Changed File Groups

### Added

- `src/App.vue`
- `src/main.ts`
- `src/env.d.ts`
- `src/router/index.ts`
- `src/store/auth.ts`
- `src/components/*.vue`
- `src/pages/*.vue`

### Updated

- `package.json`
- `package-lock.json`
- `vite.config.ts`
- `tsconfig.app.json`
- `eslint.config.js`
- `index.html`
- `src/api/*.ts`
- `src/store/index.ts`

### Removed

- React `.tsx` entry, component, page, and Redux slice files

## 6. Preserved Contracts

- Route paths: `/`, `/login`, `/signup`, `/game`, `/progress`, `/community`, `/mypage`, `/admin`, `/review`, `/notice`, `/incorrect-note`, `/bookmark`
- Query keys: `page`, `size`, `sort`, `is_asc`, `stage_number`, `level`
- Multipart structure for sign-up
- Bearer access token injection
- Cookie-enabled refresh request
- Community singular/plural path mismatch where defined by Postman
- Empty-body-safe handling for 201/204 success cases

## 7. TODO / Manual Verification Needed

- Profile update UI is not surfaced in the current frontend, so multipart profile-edit contract remains API-only.
- Problem create/update admin UI is not currently implemented in the migrated UI, though API helpers preserve the create JSON vs update form-data split.
- Home dashboard and floating notes still use mock/local behavior because the current frontend already relied on mock/local data there.
- Backend zip lacked controller implementations; if the running backend differs from Postman, manual endpoint verification is still needed.
- The workspace shows unrelated local changes such as `db/erd.erd`; these were not modified as part of the migration logic.

## 8. Verification Result

- `npm install`: passed
- `npm run lint`: passed
- `npx vue-tsc -b`: passed
- `npm run build`: passed
- Manual browser execution: not run in this session
- Manual API flow checks against live backend: not run in this session
