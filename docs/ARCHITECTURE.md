# CaptureMate Android Architecture

## MVP 원칙

원본 스크린샷 이미지는 서버로 보내지 않는다.
앱은 온디바이스 OCR로 텍스트를 추출하고, 민감정보를 로컬에서 마스킹한 뒤 필요한 텍스트만 백엔드 API로 전송한다.

## 현재 앱 구조

```text
app/src/main/java/com/capturemate/app
├─ core
│  ├─ ai              # OCR, 온디바이스 AI 경계
│  ├─ privacy         # 민감정보 탐지/마스킹
│  └─ di              # AppContainer 기반 DI
├─ data
│  ├─ local           # Room DB, Dao, Entity
│  ├─ remote          # Retrofit API, DTO
│  └─ repository      # Repository 구현체
├─ domain
│  ├─ model           # 앱 공통 도메인 모델
│  └─ repository      # Repository 인터페이스
├─ feature
│  └─ home            # 화면 단위 기능
└─ ui
   └─ theme           # Compose theme
```

## 팀별 담당 경계

- OCR/마스킹 담당: `core/ai`, `core/privacy`, `data/local`
- 일정 담당: `feature/calendar`, Google sign-in, Calendar intent/API 연동
- 학습/생활정보 담당: `feature/memo`, `data/local/entity/MemoEntity`, 알림 예약
- 맛집 담당: `feature/restaurant`, 지도 intent/API, 위치 권한
- 서버/API 담당: `data/remote`, DTO, Repository sync 로직

## 데이터 흐름

1. 사용자가 스크린샷 선택
2. `OcrTextExtractor`가 로컬에서 텍스트 추출
3. `SensitiveTextMasker`가 이메일, 전화번호, 계좌/식별번호 후보 마스킹
4. 원본 텍스트와 이미지 URI는 Room DB에 로컬 저장
5. `maskedText`만 `CaptureMateApi`로 전송
6. 서버 분석 결과를 메모/액션 후보로 저장
7. 일정, 알림, 지도 등 실행 기능으로 연결

## 인증 흐름

1. 사용자가 첫 화면에서 Google 로그인을 선택
2. 앱이 AndroidX Credential Manager로 Google ID token을 받음
3. 앱이 ID token과 사용자 기본 정보를 로컬 세션으로 변환
4. 토큰과 사용자 기본 정보는 `EncryptedSharedPreferences`에 저장
5. 홈 화면은 저장된 로컬 세션을 구독해 로그인 상태를 표시
6. 로그아웃 시 Google Credential 상태와 로컬 세션을 함께 삭제

Google Credential Manager는 현재 Google refresh token을 앱에 직접 제공하지 않는다.
따라서 Google 로그인은 ID token 중심으로 저장하고, 카카오 등 다른 provider가 access token
또는 refresh token을 제공하면 같은 `AuthSession` 모델의 provider token 필드에 저장한다.

## 공통 기술 선택

- UI: Jetpack Compose
- DI: `AppContainer` 수동 DI
- Local DB: Room
- Network: Retrofit + OkHttp + kotlinx.serialization
- OCR: Google ML Kit Text Recognition, Korean recognizer 포함
- Background/Notification: WorkManager 기준
- Login: AndroidX Credential Manager + Google ID

현재 AGP 9 템플릿에서 Hilt Gradle plugin이 `Android BaseExtension not found` 오류를 내므로,
MVP 시작점은 빌드 안정성을 우선해 수동 DI로 고정했다.
Hilt 호환 버전이 확정되면 `core/di`만 교체하는 방식으로 전환한다.

## 구현 규칙

- ViewModel은 feature 패키지 안에 둔다.
- 화면에서 Retrofit/Room을 직접 호출하지 않는다.
- 서버에는 원본 이미지 URI, raw OCR text, 갤러리 경로를 보내지 않는다.
- DTO는 `data/remote/dto`, Entity는 `data/local/entity`, UI state는 각 feature 안에 둔다.
- 기능 간 공유 모델이 필요할 때만 `domain/model`로 승격한다.
