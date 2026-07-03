# CaptureMate 안드로이드

CaptureMate 안드로이드 앱 저장소입니다. 캡처 이미지에서 텍스트를 추출하고, 민감정보를 로컬에서 마스킹한 뒤, 필요한 분석 요청만 AI 서버로 보냅니다.

## 저장소 구조

프로젝트는 두 개의 저장소로 나눠 관리합니다.

```text
captureMate
  안드로이드 앱
  로컬 OCR
  민감정보 마스킹
  Room 로컬 DB
  알림 및 앱 화면

capturemate-ai
  AI 분석 서버
  요약
  카테고리 분류
  추천 액션 생성
```

이 저장소에는 안드로이드 앱 코드만 둡니다. 서버에서 실행되는 AI 분석 코드는 `capturemate-ai` 저장소에서 관리합니다.

## 주요 기술

- Kotlin
- Jetpack Compose
- Room
- Retrofit, OkHttp
- kotlinx.serialization
- WorkManager
- Google ML Kit Text Recognition

## 로컬 설정

`local.properties`는 각자 로컬 환경에 맞게 생성합니다. Android Studio로 프로젝트를 열면 보통 자동으로 생성됩니다.

```properties
sdk.dir=로컬 Android SDK 경로
```

`local.properties`는 커밋하지 않습니다.

## AI 서버 연동

앱은 기본적으로 로컬 AI 서버를 호출하도록 설정되어 있습니다.

```text
http://10.0.2.2:8001/
```

안드로이드 에뮬레이터에서 `10.0.2.2`는 개발 PC의 `localhost`를 의미합니다. 실제 기기에서 테스트할 때는 같은 네트워크에 있는 개발 PC의 IP 주소로 바꿔야 합니다.

AI 서버 실행은 `capturemate-ai` 저장소에서 진행합니다.

```bash
py -m pip install -r requirements.txt
py -m uvicorn app.main:app --reload --port 8001
```

## 실행

Android Studio에서 프로젝트를 열고 `app` 구성을 실행합니다.

명령어로 테스트할 경우:

```bash
.\gradlew.bat testDebugUnitTest
```

## 데이터 처리 원칙

- 원본 캡처 이미지는 서버로 보내지 않습니다.
- 원본 OCR 텍스트는 로컬 Room DB에만 저장합니다.
- 이메일, 전화번호, 계좌번호 같은 민감정보는 로컬에서 마스킹합니다.
- AI 서버에는 마스킹된 텍스트만 보냅니다.

