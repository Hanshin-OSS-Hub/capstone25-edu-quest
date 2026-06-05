# EduQuest

EduQuest는 코딩 학습을 게임 흐름과 연결한 캡스톤 프로젝트입니다. 웹 프론트엔드, Spring Boot 백엔드, Unity WebGL 게임 화면이 함께 동작합니다.

## 구성

- `Frontend/Frontend-main`: Vue 3 + Vite 프론트엔드
- `backend`: Spring Boot 백엔드
- `Frontend/Frontend-main/public/unity/EduQuest`: 프론트에서 서빙하는 Unity WebGL 빌드 파일
- `backend/src/main/resources/db/migration`: Flyway DB migration

## 필요한 프로그램

- Node.js 20 이상 권장
- npm
- Docker, Docker Compose
- Java 25
- Unity 2022.3.62f3 권장
- Git LFS

Git LFS는 Unity WebGL의 큰 `.data` 파일을 받기 위해 필요합니다.

```bash
git lfs install
git lfs pull
```

## 업로드하면 안 되는 파일

아래 파일은 개인 키, DB 비밀번호, API 키가 들어갈 수 있으므로 Git에 올리지 않습니다.

```text
.env
.env.*
prod.env
env
backend/env
backend/prod.env
Frontend/Frontend-main/.env
backend/garage-data/
backend/garage-meta/
backend/.gradle/
```

이미 노출된 API 키는 Git 기록에서 지웠더라도 폐기하고 새 키로 교체해야 합니다.

## 백엔드 환경변수

백엔드는 `backend/prod.env` 또는 실행 환경의 환경변수를 사용합니다. 실제 값은 팀원별로 따로 공유하고, Git에는 올리지 않습니다.

`backend/prod.env` 예시:

```env
SPRING_PROFILES_ACTIVE=prod

MYSQL_ROOT_PASSWORD=change-me
MYSQL_DATABASE=eduquest
MYSQL_USER=eduquest
MYSQL_PASSWORD=change-me

SPRING_DATASOURCE_URL=jdbc:mariadb://db:3306/eduquest?useSSL=false&serverTimezone=UTC&useGSSAPI=false&restrictedAuth=mysql_native_password&characterEncoding=UTF-8&useUnicode=true
SPRING_DATASOURCE_USERNAME=eduquest
SPRING_DATASOURCE_PASSWORD=change-me

JWT_SECRET_KEY=change-me-to-a-long-random-secret

ADMIN_ID=admin
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=change-me

EMAIL_ADDRESS=your-gmail@example.com
EMAIL_PASSWORD=your-app-password

OPENAI_API_KEY=your-ai-api-key

AWS_S3_ACCESS_KEY=garage-access-key
AWS_S3_SECRET_KEY=garage-secret-key
AWS_S3_ENDPOINT=http://garage:3900
AWS_S3_BUCKET=eduquest
AWS_S3_PRESIGNED_URL_EXPIRATION=PT10H

PISTON_EXECUTE_URL=http://piston:2000/api/v2/execute
PISTON_EXECUTE_SCHEME=http

FRONTEND_URL=http://localhost:3000
BACKEND_URL=http://localhost:8080
MAIL_TOKEN_EXPIRATION_MILLIS=600000
```

로컬에서 Garage/S3를 완전히 설정하지 않아도, 현재 회원가입은 프로필 이미지 업로드 실패 시 기본 프로필로 계속 진행됩니다. 단, 프로필 이미지 파일 형식이 잘못된 경우는 실패할 수 있습니다.

## 프론트엔드 환경변수

프론트는 `Frontend/Frontend-main/.env`를 사용합니다. Git에 올리지 않습니다.

예시:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

## 실행 방법

### 1. 백엔드 실행

```bash
cd backend
docker compose --env-file prod.env up --build
```

백엔드는 기본적으로 `http://localhost:8080`에서 실행됩니다.

주의:
- `prod.env`가 없으면 컨테이너가 정상 실행되지 않을 수 있습니다.
- Flyway migration은 백엔드 시작 시 자동 적용됩니다.
- Java 25 toolchain이 필요합니다.

### 2. 프론트엔드 실행

```bash
cd Frontend/Frontend-main
npm install
npm run dev
```

프론트는 기본적으로 `http://localhost:3000` 또는 Vite가 표시하는 포트에서 실행됩니다.

빌드:

```bash
npm run build
```

### 3. Unity WebGL 반영

Unity에서 WebGL 빌드를 만든 뒤 아래 폴더에 결과물을 반영합니다.

```text
Frontend/Frontend-main/public/unity/EduQuest
```

프론트에서 게임 페이지를 열면 이 Unity WebGL 빌드를 iframe으로 로드합니다.

Unity WebGL 인증 전달 흐름:

1. `GamePage.vue`가 Unity iframe에 `EDUQUEST_AUTH` 메시지를 보냅니다.
2. `public/unity/EduQuest/index.html`이 메시지를 받아 Unity의 `WebAuthBridge.ReceiveAuthPayload`로 전달합니다.
3. Unity의 `EduQuestApiManager`가 accessToken, userUuid, apiBaseUrl을 저장합니다.
4. Unity에서 문제 제출, 힌트, 코인, 북마크 API를 호출합니다.

## 주요 기능

- 로그인, 회원가입
- 관리자 문제/스테이지 관리
- Unity WebGL 전투 문제 풀이
- 일반 문제 제출 및 백엔드 채점 polling
- 보스 로컬 문제
- 힌트 사용 및 코인 차감
- 스테이지 클리어 보상
- 코인 UI 서버 동기화
- 오답노트 자동 생성 및 AI 피드백
- 오답노트 삭제, 다시 풀기
- 복습 문제 목록
- 웹/Unity 북마크
- 공지사항
- 홈 대시보드

## 북마크와 복습

- 웹 문제 화면에서 북마크 추가/해제가 가능합니다.
- Unity 전투 화면에서도 일반 백엔드 문제는 북마크 버튼을 사용할 수 있습니다.
- 보스 로컬 문제는 북마크 대상이 아닙니다.
- 북마크 목록은 문제 번호보다 문제 요약을 우선 표시합니다.

## 오답노트

- 백엔드 채점 결과가 오답이면 오답노트가 자동 생성 또는 업데이트됩니다.
- 같은 사용자와 같은 문제 조합은 중복 생성되지 않도록 처리합니다.
- 사용자가 오답노트를 삭제할 수 있습니다.
- AI 피드백 API 키나 AI 서비스 상태가 잘못되면 503 응답과 안내 메시지를 반환합니다.

## 문제 풀이 흐름

일반 백엔드 문제:

1. Unity 또는 웹에서 문제를 풉니다.
2. `POST /api/v1/problems/{problemUuid}/submissions`로 제출합니다.
3. `GET /api/v1/problems/evaluations/{submissionUuid}`를 polling합니다.
4. 정답이면 진행도, 복습, 보상 흐름이 반영됩니다.
5. 오답이면 오답노트 생성 이벤트가 발행됩니다.

보스 로컬 문제:

- `boss_hanoi`
- `boss_python_spell`
- `boss_python_condition`

이 문제들은 Unity 자체 문제 생성 및 채점 흐름을 유지합니다. 백엔드 제출/채점으로 바꾸지 않습니다.

## 배포/업로드 주의사항

현재 원본 작업 폴더에서 Git 루트가 사용자 홈 폴더로 잡힐 수 있습니다.

```bash
git rev-parse --show-toplevel
```

결과가 프로젝트 폴더가 아니라 `/Users/...` 홈 폴더라면, 그 위치에서 절대 아래 명령을 실행하지 마세요.

```bash
git add .
git commit
git push
```

안전한 업로드 방법:

1. 별도 업로드용 폴더를 만듭니다.
2. GitHub 저장소를 새 폴더에 clone합니다.
3. 원본 EduQuest 파일을 rsync로 복사하되 환경파일과 로컬 데이터는 제외합니다.
4. 새 clone 폴더에서만 Git 작업을 합니다.

예시:

```bash
mkdir -p ~/Desktop/GitUpload
cd ~/Desktop/GitUpload
git clone https://github.com/Capstone-EduQuest/Unity.git EduQuest_upload
cd EduQuest_upload

rsync -av --delete \
  --exclude='.git' \
  --exclude='.env' \
  --exclude='.env.*' \
  --exclude='prod.env' \
  --exclude='env' \
  --exclude='backend/env' \
  --exclude='backend/prod.env' \
  --exclude='Frontend/Frontend-main/.env' \
  --exclude='backend/garage-data/' \
  --exclude='backend/garage-meta/' \
  --exclude='backend/.gradle/' \
  /path/to/EduQuest/ ./
```

커밋 전 반드시 확인합니다.

```bash
git status --short
git diff --cached --name-only
```

환경파일, Garage 메타데이터, 개인 폴더가 보이면 커밋하지 않습니다.

## 자주 발생하는 문제

### 회원가입이 S3/Garage 오류로 실패하는 경우

현재는 프로필 이미지 업로드 실패가 회원가입 전체 실패로 이어지지 않도록 처리되어 있습니다. 그래도 이미지가 꼭 필요한 기능을 테스트하려면 Garage bucket/key 설정을 확인해야 합니다.

### AI 피드백이 503을 반환하는 경우

`OPENAI_API_KEY` 값 또는 AI 서비스 상태를 확인하세요. 이미 GitHub에 올라간 적 있는 키는 폐기하고 새 키를 사용해야 합니다.

### 백엔드 compileJava가 Java 25를 찾지 못하는 경우

백엔드 Gradle 설정은 Java 25 toolchain을 요구합니다. Java 25를 설치하거나 Gradle toolchain download repository를 설정해야 합니다.

### Unity WebGL 변경이 웹에 반영되지 않는 경우

Unity에서 WebGL을 다시 빌드한 뒤 `Frontend/Frontend-main/public/unity/EduQuest`에 새 빌드 결과를 반영해야 합니다. 브라우저 캐시 또는 UnityCache도 확인하세요.
