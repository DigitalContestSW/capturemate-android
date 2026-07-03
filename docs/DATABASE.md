# Database Plan

## Local Room DB

원본 스크린샷 관련 데이터는 로컬 DB를 기본 저장소로 둔다.

### `captures`

- `id`: 로컬 캡처 ID
- `localImageUri`: 갤러리 URI 또는 앱 내부 복사본 URI
- `rawTextLocalOnly`: OCR 원문, 서버 전송 금지
- `maskedText`: 마스킹 후 서버 전송 가능 텍스트
- `category`: 서버/로컬 분류 결과
- `capturedAt`: 스크린샷 촬영 추정 시각
- `createdAt`: 앱 저장 시각

### `memos`

- `id`: 로컬 메모 ID
- `captureId`: 연결된 캡처 ID
- `serverMemoId`: 서버 저장 메모 ID
- `title`: 카드 제목
- `summary`: 요약
- `category`: 일정, 학습, 생활정보, 맛집, 채용 등
- `recommendedAction`: 추천 액션
- `reminderAt`: 알림 예약 시각
- `createdAt`, `updatedAt`: 생성/수정 시각

## Server DB

서버 DB는 원본 이미지 없이 사용자의 실행 흐름과 분석 결과만 저장한다.

### 필수 후보 테이블

- `users`: Google 로그인 사용자
- `captures`: 로컬 캡처와 매핑되는 서버 분석 단위, `masked_text`만 저장
- `memos`: 요약 카드와 카테고리
- `actions`: 일정 등록, 할 일, 지도 열기, 알림 등 추천/실행 액션
- `reminders`: 서버 기반 리마인드가 필요할 경우
- `feedback_events`: 사용자가 추천을 저장/삭제/실행했는지 기록

### 카테고리별 확장 테이블

- `calendar_items`: 마감일, 일정 제목, 시작/종료 시각, 외부 캘린더 이벤트 ID
- `study_items`: 시험일, 복습 카드, 과목, 중요도
- `places`: 장소명, 주소, 위도/경도, 지도 provider ID
- `job_posts`: 회사명, 직무, 마감일, 지원 URL

## 서버로 보내면 안 되는 데이터

- 원본 스크린샷 이미지
- 로컬 파일 경로
- 마스킹 전 OCR 원문
- 전화번호, 이메일, 계좌번호 등 원문 민감정보
