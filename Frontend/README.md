# EduQuest

EduQuest는 초등학생을 대상으로 코딩 학습을 제공하는 Unity WebGL 기반 교육용 게임 플랫폼입니다.  
프론트엔드는 Vue 3 기반 웹 애플리케이션으로 학습 게임 화면, 스테이지 진행률, 오답 노트, 북마크, 커뮤니티, 마이페이지, 관리자 기능을 제공합니다.  
백엔드는 Spring Boot 기반 API 서버로 회원 관리, JWT 인증, 스테이지/문제 관리, 코드 제출 및 채점, 오답 노트, AI 피드백, 커뮤니티, 메모, 파일 업로드 기능을 담당합니다.

---

## 프로젝트 구성

```text
EduQuest/
  Frontend-main/   Vue 3 + TypeScript + Vite 프론트엔드
  backend-dev/     Spring Boot + MariaDB 백엔드
```

---

## 주요 기능

### 사용자 기능

- 회원가입 및 로그인
- JWT 기반 인증 상태 유지
- Access Token 만료 시 Refresh Token으로 재발급
- 마이페이지에서 프로필, 학습 요약, 보유 코인 확인
- 비밀번호 변경
- Unity WebGL 학습 게임 실행
- 스테이지 목록 및 진행률 확인
- 문제 풀이 및 코드 제출
- 힌트 사용
- 오답 노트 조회
- AI 오답 피드백 요청
- 북마크한 문제 확인
- 커뮤니티 질문/답변 작성
- 빠른 메모 작성 및 수정

### 관리자 기능

- 사용자 목록 조회
- 사용자 권한 변경
- 사용자 잠금 처리
- 사용자 삭제
- 스테이지 생성, 수정, 삭제
- 문제 생성, 삭제
- 공지/메모성 게시글 생성, 수정, 삭제

---

## 기술 스택

### Frontend

- Vue 3
- TypeScript
- Vite
- Vue Router
- Axios
- Tailwind CSS
- Docker
- Nginx

### Backend

- Java 25
- Spring Boot 4
- Spring Security
- Spring Data JPA
- MyBatis
- QueryDSL
- Flyway
- MariaDB
- JWT
- Spring AI
- Gemini OpenAI-compatible API
- Garage S3-compatible Object Storage
- Piston Code Execution Engine
- Docker
- Docker Compose

---

## 실행 환경

### Frontend

- Node.js 20 이상 권장
- npm

### Backend

- Java 25
- Docker Desktop
- MariaDB
- Gradle Wrapper
- Garage
- Piston

---

## Frontend 로컬 실행

프론트엔드 폴더로 이동합니다.

```bash
cd Frontend-main
```

의존성을 설치합니다.

```bash
npm install
```

프로젝트 루트에 `.env` 파일을 생성합니다.

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

개발 서버를 실행합니다.

```bash
npm run dev
```

현재 `vite.config.ts` 기준 개발 서버 주소는 다음과 같습니다.

```text
http://localhost:3000
```

---

## Frontend 스크립트

```bash
npm run dev      # 개발 서버 실행
npm run build    # TypeScript 검사 후 프로덕션 빌드
npm run preview  # 빌드 결과 미리보기
npm run lint     # ESLint 검사
```

---

## Backend 로컬 실행

백엔드 폴더로 이동합니다.

```bash
cd backend-dev
```

필요한 환경 변수를 설정합니다.  
개발 환경에서는 프로젝트 루트에 `.env` 파일을 둘 수 있습니다.

```env
SPRING_PROFILES_ACTIVE=dev

SPRING_DATASOURCE_URL=jdbc:mariadb://localhost:3306/eduquest?useSSL=false&serverTimezone=UTC&useGSSAPI=false&restrictedAuth=mysql_native_password&characterEncoding=UTF-8&useUnicode=true
SPRING_DATASOURCE_USERNAME=eduquest
SPRING_DATASOURCE_PASSWORD=your_password

JWT_SECRET_KEY=your_jwt_secret_key

ADMIN_ID=admin
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=admin_password

FRONTEND_URL=http://localhost:3000
BACKEND_URL=http://localhost:8080

EMAIL_ADDRESS=your_email@gmail.com
EMAIL_PASSWORD=your_email_app_password

OPENAI_API_KEY=your_gemini_or_openai_compatible_key

PISTON_EXECUTE_URL=http://localhost:2000/api/v2/execute
PISTON_EXECUTE_SCHEME=http

AWS_S3_ACCESS_KEY=your_garage_access_key
AWS_S3_SECRET_KEY=your_garage_secret_key
AWS_S3_ENDPOINT=http://localhost:3900
AWS_S3_BUCKET=eduquest-bucket
```

백엔드를 실행합니다.

```bash
./gradlew bootRun
```

Windows 환경에서는 다음 명령을 사용할 수 있습니다.

```bash
gradlew.bat bootRun
```

백엔드 기본 주소는 다음과 같습니다.

```text
http://localhost:8080
```

API 기본 prefix는 다음과 같습니다.

```text
/api/v1
```

---

## Docker Compose 실행

백엔드 폴더로 이동합니다.

```bash
cd backend-dev
```

`prod.env` 파일을 생성합니다.

```env
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=eduquest
MYSQL_USER=eduquest
MYSQL_PASSWORD=your_password

SPRING_DATASOURCE_URL=jdbc:mariadb://db:3306/eduquest?useSSL=false&serverTimezone=UTC&useGSSAPI=false&restrictedAuth=mysql_native_password&characterEncoding=UTF-8&useUnicode=true
SPRING_DATASOURCE_USERNAME=eduquest
SPRING_DATASOURCE_PASSWORD=your_password

JWT_SECRET_KEY=your_jwt_secret_key

ADMIN_ID=admin
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=admin_password

FRONTEND_URL=http://localhost:3000
BACKEND_URL=http://localhost:8080

EMAIL_ADDRESS=your_email@gmail.com
EMAIL_PASSWORD=your_email_app_password

OPENAI_API_KEY=your_gemini_or_openai_compatible_key

PISTON_EXECUTE_URL=http://localhost:2000/api/v2/execute
PISTON_EXECUTE_SCHEME=http

AWS_S3_ACCESS_KEY=your_garage_access_key
AWS_S3_SECRET_KEY=your_garage_secret_key
AWS_S3_ENDPOINT=http://garage:3900
AWS_S3_BUCKET=eduquest-bucket
```

Docker Compose로 백엔드, MariaDB, Garage를 실행합니다.

```bash
docker compose --env-file prod.env up -d --build
```

실행되는 주요 포트는 다음과 같습니다.

| 서비스 | 포트 | 설명 |
| --- | --- | --- |
| Backend | `8080` | Spring Boot API 서버 |
| MariaDB | `3306` | 데이터베이스 |
| Garage S3 API | `3900` | S3-compatible 파일 저장소 |
| Garage RPC | `3901` | Garage 내부 통신 |

---

## Frontend Docker 빌드

프론트엔드 폴더로 이동합니다.

```bash
cd Frontend-main
```

이미지를 빌드합니다.

```bash
docker build -t eduquest-frontend:latest .
```

컨테이너를 실행합니다.

```bash
docker run -d -p 3000:80 --name eduquest-frontend eduquest-frontend:latest
```

브라우저에서 접속합니다.

```text
http://localhost:3000
```

---

## 주요 라우트

| 경로 | 설명 | 인증 |
| --- | --- | --- |
| `/` | 랜딩 페이지 | 비로그인 |
| `/login` | 로그인 | 비로그인 |
| `/signup` | 회원가입 | 비로그인 |
| `/home` | 홈 대시보드 | 필요 |
| `/game` | Unity 학습 게임 | 필요 |
| `/stage` | 스테이지 목록 | 필요 |
| `/progress` | 학습 진행률 | 필요 |
| `/review` | 복습 | 필요 |
| `/bookmark` | 북마크 | 필요 |
| `/incorrect-note` | 오답 노트 | 필요 |
| `/community` | 커뮤니티 | 필요 |
| `/notice` | 공지사항 | 필요 |
| `/mypage` | 마이페이지 | 필요 |
| `/admin` | 관리자 페이지 | 관리자 |
| `/unity` | `/game`으로 리다이렉트 | 필요 |

---

## 주요 API

### Auth / User

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/sign-up` | 회원가입 |
| `POST` | `/api/v1/auth/sign-in` | 로그인 |
| `POST` | `/api/v1/auth/logout` | 로그아웃 |
| `POST` | `/api/v1/auth/refresh` | Access Token 재발급 |
| `POST` | `/api/v1/auth/find-id` | 아이디 찾기 |
| `POST` | `/api/v1/auth/find-password` | 비밀번호 찾기 |
| `PUT` | `/api/v1/auth/reset-password` | 비밀번호 재설정 |
| `GET` | `/api/v1/users/{uuid}` | 사용자 프로필 조회 |
| `GET` | `/api/v1/users` | 사용자 목록 조회 |
| `PUT` | `/api/v1/users/{uuid}` | 사용자 정보 수정 |
| `PUT` | `/api/v1/users/{uuid}/role` | 사용자 권한 변경 |
| `PUT` | `/api/v1/users/{uuid}/lock` | 사용자 잠금 |
| `DELETE` | `/api/v1/users/{uuid}` | 사용자 삭제 |

### Learning

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/stages` | 스테이지 목록 조회 |
| `POST` | `/api/v1/stages` | 스테이지 생성 |
| `PUT` | `/api/v1/stages/{uuid}` | 스테이지 수정 |
| `DELETE` | `/api/v1/stages/{uuid}` | 스테이지 삭제 |
| `GET` | `/api/v1/problems` | 문제 목록 조회 |
| `GET` | `/api/v1/problems?stage_number={number}` | 스테이지별 문제 조회 |
| `GET` | `/api/v1/problems/{uuid}` | 문제 상세 조회 |
| `POST` | `/api/v1/problems` | 문제 생성 |
| `PUT` | `/api/v1/problems/{uuid}` | 문제 수정 |
| `DELETE` | `/api/v1/problems/{uuid}` | 문제 삭제 |
| `GET` | `/api/v1/problems/{uuid}/hint` | 힌트 조회 |
| `POST` | `/api/v1/problems/{problemUuid}/submissions` | 문제 답안 제출 |
| `GET` | `/api/v1/problems/evaluations/{submissionUuid}` | 채점 결과 조회 |

### Progress / Wrong Note / Bookmark

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/users/{userUuid}/progress` | 사용자 학습 진행률 조회 |
| `GET` | `/api/v1/wrong-notes` | 오답 노트 목록 조회 |
| `GET` | `/api/v1/wrong-notes/{uuid}` | 오답 노트 상세 조회 |
| `GET` | `/api/v1/wrong-notes/users/{uuid}` | 사용자별 오답 노트 조회 |
| `PUT` | `/api/v1/wrong-notes/{uuid}/ai-feedback` | AI 피드백 요청 |
| `DELETE` | `/api/v1/wrong-notes/{uuid}` | 오답 노트 삭제 |
| `POST` | `/api/v1/problems/{problemUuid}/bookmark` | 북마크 추가 |
| `DELETE` | `/api/v1/problems/{problemUuid}/bookmark` | 북마크 삭제 |
| `GET` | `/api/v1/users/{userUuid}/bookmarks` | 사용자 북마크 목록 조회 |

### Community / Note

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/questions` | 질문 목록 조회 |
| `POST` | `/api/v1/questions` | 질문 작성 |
| `GET` | `/api/v1/questions/{questionUuid}` | 질문 상세 조회 |
| `DELETE` | `/api/v1/questions/{questionUuid}` | 질문 삭제 |
| `GET` | `/api/v1/questions/{questionUuid}/answers` | 답변 목록 조회 |
| `POST` | `/api/v1/question/{questionUuid}/answers` | 답변 작성 |
| `DELETE` | `/api/v1/answers/{answerUuid}` | 답변 삭제 |
| `POST` | `/api/v1/answers/{answerUuid}/adopt` | 답변 채택 |
| `GET` | `/api/v1/notes` | 메모 목록 조회 |
| `POST` | `/api/v1/notes` | 메모 생성 |
| `GET` | `/api/v1/notes/{uuid}` | 메모 상세 조회 |
| `PUT` | `/api/v1/notes/{uuid}` | 메모 수정 |
| `DELETE` | `/api/v1/notes/{uuid}` | 메모 삭제 |

---

## Frontend 구조

```text
Frontend-main/
  src/
    api/          Axios API 모듈
    assets/       이미지 및 정적 리소스
    components/   공통 UI 컴포넌트
    pages/        페이지 컴포넌트
    router/       Vue Router 설정
    store/        인증 상태 관리
    types/        공통 타입 정의
    utils/        JWT, Unity 인증 브리지 유틸
    App.vue       루트 컴포넌트
    main.ts       앱 진입점

  public/
    unity_preview.mp4   랜딩/홈 미리보기 영상
    unity/EduQuest/     Unity WebGL 빌드 파일
```

---

## Backend 구조

```text
backend-dev/
  src/main/java/com/eduquest/backend/
    application/      유스케이스, 서비스 계층
    domain/           핵심 도메인 모델 및 도메인 서비스
    infrastructure/   JPA, Security, S3, Piston, AI, Mail 구현체
    presentation/     Controller, Request, Response DTO
    common/           공통 설정, 예외 처리

  src/main/resources/
    application.yml
    application-dev.yml
    application-prod.yml
    db/migration/     Flyway DB 마이그레이션
```

---

## 인증 흐름

1. 사용자가 `/login`에서 아이디와 비밀번호를 입력합니다.
2. 프론트엔드는 `/api/v1/auth/sign-in`으로 로그인 요청을 보냅니다.
3. 백엔드는 인증 성공 시 Access Token을 발급합니다.
4. 프론트엔드는 Access Token을 `localStorage`에 저장합니다.
5. 이후 API 요청마다 `Authorization: Bearer {token}` 헤더를 자동으로 추가합니다.
6. API 응답이 `401`이면 `/api/v1/auth/refresh`로 Access Token 재발급을 시도합니다.
7. Refresh 실패 시 인증 정보를 삭제하고 로그인 페이지로 이동합니다.

---

## Unity 연동

- Unity WebGL 파일은 `Frontend-main/public/unity/EduQuest/` 아래에 위치합니다.
- 게임 화면은 `/game` 라우트에서 Unity iframe으로 렌더링됩니다.
- 프론트엔드는 Unity 쪽으로 API 서버 주소와 Access Token을 전달하는 브리지 유틸을 포함합니다.
- 홈/랜딩 미리보기 영상은 `public/unity_preview.mp4`를 사용합니다.

---

## 데이터베이스

백엔드는 Flyway를 사용하여 DB 마이그레이션을 관리합니다.

마이그레이션 파일 위치:

```text
backend-dev/src/main/resources/db/migration/
```

주요 테이블은 다음과 같습니다.

| 테이블 | 설명 |
| --- | --- |
| `member` | 사용자 계정 |
| `role` | 권한 |
| `user_role` | 사용자-권한 매핑 |
| `stage` | 학습 스테이지 |
| `problem` | 문제 |
| `hint` | 문제 힌트 |
| `hint_history` | 힌트 사용 이력 |
| `submission` | 문제 제출 |
| `evaluation` | 채점 결과 |
| `wrong_note` | 오답 노트 |
| `community_post` | 커뮤니티 질문 |
| `community_answer` | 커뮤니티 답변 |
| `bookmark` | 문제 북마크 |
| `note` | 빠른 메모 |
| `file` | 업로드 파일 |
| `wallet` | 사용자 지갑 |
| `wallet_history` | 코인 변동 이력 |
| `reward_history` | 보상 지급 이력 |

---

## AI 피드백

오답 노트 AI 피드백은 Spring AI 기반으로 동작합니다.  
현재 설정은 OpenAI-compatible API 형식을 사용하며, `application.yml`에서 Gemini API endpoint를 사용하도록 구성되어 있습니다.

필요 환경 변수:

```env
OPENAI_API_KEY=your_api_key
```

---

## 코드 채점

코드 제출 및 채점은 Piston API를 통해 처리합니다.  
백엔드는 제출된 답안을 Piston으로 전달하고, 실행 결과를 기반으로 채점 결과를 생성합니다.

필요 환경 변수:

```env
PISTON_EXECUTE_URL=http://localhost:2000/api/v2/execute
PISTON_EXECUTE_SCHEME=http
```

---

## 파일 업로드

프로필 이미지는 Garage S3-compatible Object Storage를 사용해 저장합니다.

필요 환경 변수:

```env
AWS_S3_ACCESS_KEY=your_access_key
AWS_S3_SECRET_KEY=your_secret_key
AWS_S3_ENDPOINT=http://localhost:3900
AWS_S3_BUCKET=eduquest-bucket
```

---

## 빌드 확인

### Frontend

```bash
cd Frontend-main
npm run build
```

### Backend

```bash
cd backend-dev
./gradlew clean build
```

Windows:

```bash
gradlew.bat clean build
```

---

## Git 관리 주의사항

다음 파일과 폴더는 GitHub에 올리지 않습니다.

```text
.env
prod.env
node_modules/
dist/
build/
.gradle/
garage-data/
garage-meta/
```

프론트엔드 `.gitignore`에는 다음 항목을 추가하는 것을 권장합니다.

```gitignore
.env
.env.local
.env.*.local
```

환경 변수 예시는 실제 비밀번호나 API Key 없이 `.env.example` 형태로 관리하는 것을 권장합니다.

---

## 개발 참고 사항

- 프론트엔드 개발 서버 포트는 `vite.config.ts` 기준 `3000`입니다.
- 백엔드 API 기본 주소는 `http://localhost:8080/api/v1`입니다.
- 백엔드 CORS 허용 주소는 `FRONTEND_URL` 환경 변수로 관리합니다.
- Docker Compose 실행 시 `prod.env` 값을 사용하려면 `docker compose --env-file prod.env up -d --build` 명령을 사용하는 것이 안전합니다.
- 현재 프론트엔드의 `reward.ts`, `wallet.ts`는 백엔드 컨트롤러가 아직 없어서 직접 호출하지 않도록 TODO 처리되어 있습니다.
- `dist/`, `node_modules/`는 빌드 결과물이거나 설치 결과물이므로 저장소에 올리지 않습니다.