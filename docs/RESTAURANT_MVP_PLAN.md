# 맛집 메모 MVP 구현 계획

## 목표

스크린샷 OCR 텍스트에서 `capturemate-ai`가 맛집 정보를 분석하고, CaptureMate Android 앱은 분석 결과를 저장/조회하여 다음 화면을 제공한다.

- 맛집 상세 화면: AI 분석 결과, 장소 정보, 메뉴, 가격, 1인 예상 금액, 태그, 추천 액션 표시
- 메모함 맛집 화면: 저장된 맛집 메모를 지도 위에 마커로 표시 + 주소/동네가 겹치는 맛집들을 `성수동 맛집` 같은 그룹 카드로 묶어 표시
- 그룹 상세 화면: 그룹에 속한 맛집 메모 리스트 표시
- 맛집 메모 상세 진입: 리스트 또는 지도 마커에서 맛집 상세 화면으로 이동

## 책임 분리

### capturemate-ai 책임

- OCR 텍스트에서 맛집 카테고리 판별
- 가게명, 주소, 메뉴, 가격, 요약, 태그, 특징, 추천 액션 추출
- 추출 정보가 부족할 때 지도/검색 API를 호출해 보강
- 주소 기반 위도/경도 좌표 보강
- 주소 또는 장소 유사도 기반 동네 그룹 산출
- 앱에서 바로 렌더링 가능한 정규화된 응답 반환

### CaptureMate Android 책임

- OCR 및 민감정보 마스킹 후 `capturemate-ai` 분석 API 호출
- 맛집 분석 결과를 Room DB에 저장
- 맛집 상세/지도/그룹/리스트 UI 구현
- 지도 SDK 또는 지도 WebView/Static Map 연동
- 저장된 맛집 메모 조회, 추가, 상세 이동, 새로고침 상태 처리

## MVP 범위

### 포함

- 맛집 카테고리 분석 결과 저장
- 상세 화면 UI 구현
- 지도 화면에 저장된 맛집 메모 마커 표시
- 지도 마커 클릭 시 맛집 카드 또는 상세 이동
- 동네 그룹 카드 생성 및 그룹별 맛집 리스트 표시
- 가게명만 있을 때 AI 서버가 외부 지도/검색 API로 장소 정보 보강

### 제외

- 사용자가 직접 장소를 검색해 새 맛집을 생성하는 풀 플로우
- 지도 위 클러스터링 고도화
- 다중 지도 사업자 동시 지원
- 서버 기반 실시간 동기화
- 리뷰/평점 크롤링 고도화

## 추천 지도 API 선택

MVP는 Android 앱 구현 난이도와 국내 장소 데이터 품질을 기준으로 카카오맵을 1순위로 둔다.

1. 카카오맵
   - 국내 주소/장소 검색 품질이 좋음
   - 맛집/상호명 검색과 주소 좌표 변환에 적합
   - 앱 키 관리 필요

2. 네이버맵
   - 국내 지도 품질이 좋음
   - 장소 검색 API와 지도 SDK 정책 확인 필요

3. Google Maps
   - Android SDK 안정성이 좋음
   - 국내 소상공인/메뉴 정보 보강에는 상대적으로 불리할 수 있음

결정 기준:

- `capturemate-ai`가 장소 보강 API를 담당한다면 Android 앱은 지도 표시만 필요하므로 지도 SDK 선택 부담이 줄어든다.
- Android MVP에서는 `latitude`, `longitude`, `placeName`, `address`가 이미 저장되어 있다는 전제로 UI를 먼저 완성한다.

## AI 분석 API 계약

### 요청

```json
{
  "captureId": "local-capture-id",
  "categoryHint": "restaurant",
  "maskedText": "성수동 브런치 카페 ...",
  "locale": "ko-KR"
}
```

### 응답

```json
{
  "category": "restaurant",
  "summary": "에그베네딕트 맛집. 주말 웨이팅 40분, 평일 바로 입장 가능",
  "restaurant": {
    "name": "카페 오월의 종",
    "address": "서울 성동구 성수이로 77",
    "roadAddress": "서울 성동구 성수이로 77",
    "neighborhood": "성수동",
    "latitude": 37.5441,
    "longitude": 127.0558,
    "mapProvider": "kakao",
    "mapProviderPlaceId": "123456789",
    "menus": [
      {
        "name": "에그베네딕트",
        "price": 18000
      },
      {
        "name": "아메리카노",
        "price": 6000
      },
      {
        "name": "크로와상",
        "price": 5500
      }
    ],
    "estimatedPricePerPersonMin": 20000,
    "estimatedPricePerPersonMax": 30000,
    "tags": ["데이트", "브런치", "혼밥"],
    "features": [
      "주말 웨이팅 가능성 높음",
      "브런치 메뉴 중심",
      "평일 방문 추천"
    ],
    "recommendedActions": [
      {
        "type": "visit_time",
        "title": "평일 오전 방문",
        "description": "웨이팅을 줄이려면 평일 오픈 시간대 방문을 추천"
      },
      {
        "type": "companion",
        "title": "데이트 또는 친구와 방문",
        "description": "브런치와 카페 메뉴가 있어 가벼운 약속에 적합"
      }
    ]
  },
  "group": {
    "id": "seongsu-restaurant",
    "title": "성수동 맛집",
    "neighborhood": "성수동"
  },
  "confidence": 0.86,
  "needsUserReview": false
}
```

### 보강 실패 시 허용 응답

```json
{
  "category": "restaurant",
  "summary": "가게명만 확인됨. 장소 정보 확인 필요",
  "restaurant": {
    "name": "카페 오월의 종",
    "address": null,
    "latitude": null,
    "longitude": null,
    "menus": [],
    "tags": [],
    "features": [],
    "recommendedActions": []
  },
  "confidence": 0.42,
  "needsUserReview": true
}
```

## 로컬 DB 설계

### `restaurant_memos`

맛집 상세 화면과 지도 마커에 필요한 정보를 저장한다.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | String | 로컬 맛집 메모 ID |
| `memoId` | String | 공통 메모 ID |
| `captureId` | String | 원본 캡처 ID |
| `name` | String | 가게명 |
| `summary` | String | AI 분석 요약 |
| `address` | String? | 주소 |
| `roadAddress` | String? | 도로명 주소 |
| `neighborhood` | String? | 동네명 |
| `latitude` | Double? | 위도 |
| `longitude` | Double? | 경도 |
| `mapProvider` | String? | kakao/naver/google |
| `mapProviderPlaceId` | String? | 지도 API 장소 ID |
| `estimatedPricePerPersonMin` | Int? | 1인 예상 최소 금액 |
| `estimatedPricePerPersonMax` | Int? | 1인 예상 최대 금액 |
| `confidence` | Double | 분석 신뢰도 |
| `needsUserReview` | Boolean | 사용자 확인 필요 여부 |
| `createdAt` | Long | 생성 시각 |
| `updatedAt` | Long | 수정 시각 |

### `restaurant_menus`

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | String | 메뉴 ID |
| `restaurantMemoId` | String | 맛집 메모 ID |
| `name` | String | 메뉴명 |
| `price` | Int? | 가격 |
| `currency` | String | 기본 `KRW` |
| `sortOrder` | Int | 노출 순서 |

### `restaurant_tags`

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | String | 태그 ID |
| `restaurantMemoId` | String | 맛집 메모 ID |
| `name` | String | 태그명 |

### `restaurant_features`

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | String | 특징 ID |
| `restaurantMemoId` | String | 맛집 메모 ID |
| `text` | String | 특징 문장 |
| `sortOrder` | Int | 노출 순서 |

### `restaurant_recommended_actions`

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | String | 추천 액션 ID |
| `restaurantMemoId` | String | 맛집 메모 ID |
| `type` | String | `visit_time`, `companion`, `reservation`, `budget` 등 |
| `title` | String | 액션 제목 |
| `description` | String | 액션 설명 |
| `sortOrder` | Int | 노출 순서 |

### `restaurant_groups`

AI가 내려준 그룹을 저장하거나 앱에서 주소 기반 fallback 그룹을 생성한다.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | String | 그룹 ID |
| `title` | String | 예: 성수동 맛집 |
| `neighborhood` | String | 예: 성수동 |
| `representativeLatitude` | Double? | 지도 표시 중심 위도 |
| `representativeLongitude` | Double? | 지도 표시 중심 경도 |
| `createdAt` | Long | 생성 시각 |
| `updatedAt` | Long | 수정 시각 |

### `restaurant_group_members`

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `groupId` | String | 그룹 ID |
| `restaurantMemoId` | String | 맛집 메모 ID |

## Android 패키지 구조

```text
app/src/main/java/com/capturemate/app
  data
    local
      dao
        RestaurantMemoDao.kt
        RestaurantGroupDao.kt
      entity
        RestaurantMemoEntity.kt
        RestaurantMenuEntity.kt
        RestaurantTagEntity.kt
        RestaurantFeatureEntity.kt
        RestaurantRecommendedActionEntity.kt
        RestaurantGroupEntity.kt
        RestaurantGroupMemberEntity.kt
    remote
      dto
        RestaurantAnalysisDto.kt
    repository
      RestaurantRepositoryImpl.kt
  domain
    model
      RestaurantMemo.kt
      RestaurantMenu.kt
      RestaurantGroup.kt
      RestaurantRecommendedAction.kt
    repository
      RestaurantRepository.kt
  feature
    restaurant
      RestaurantDetailScreen.kt
      RestaurantMapScreen.kt
      RestaurantGroupDetailScreen.kt
      RestaurantViewModel.kt
      RestaurantUiState.kt
```

## 화면별 구현

### 1. 맛집 상세 화면

목표: 첫 번째 레퍼런스 이미지처럼 분석 결과와 장소 정보를 카드 형태로 보여준다.

필수 UI:

- 상단 앱바: 뒤로가기, 제목, 더보기
- 원본 캡처 썸네일 영역
- AI 분석 결과 카드
  - 카테고리 칩
  - 요약
- 장소 정보 카드
  - 가게명
  - 주소
  - 지도 미리보기 또는 지도 연동 예정 placeholder
  - 메뉴 목록
  - 1인 예상 금액
  - 태그
  - 가게 특징
  - 추천 액션
- 리마인드 알림 카드

상태:

- `Loading`: DB 조회 중
- `Content`: 상세 정보 표시
- `NeedsReview`: 주소/좌표/메뉴 부족 시 확인 필요 배지 표시
- `Error`: 조회 실패 메시지와 재시도

### 2. 맛집 지도 화면 + 동네 그룹 카드

목표: 두 번째 레퍼런스 이미지처럼 메모함의 맛집 탭에서 저장된 장소를 지도에 표시한다.

필수 UI:

- 저장된 메모 수 / 이번 주 추가 수 요약
- 카테고리 필터 칩
- 지도 영역
- 지도 아래 저장된 장소/그룹 카드 리스트
- 하단 내비게이션 유지

지도 동작:

- 좌표가 있는 맛집 메모는 마커 표시
- 마커 클릭 시 하단에 맛집 카드 표시
- 카드 클릭 시 맛집 상세 화면 이동
- 좌표가 없는 맛집은 리스트에는 표시하되 지도에는 표시하지 않음

동네 그룹 카드:

목표: 주소의 동네가 겹치는 맛집이 있으면 `성수동 맛집` 카드로 묶는다.

그룹 생성 우선순위:

1. `capturemate-ai` 응답의 `group.id`, `group.title`, `group.neighborhood` 사용
2. 응답에 그룹이 없으면 앱에서 `neighborhood` 기준으로 fallback 그룹 생성
3. `neighborhood`가 없으면 주소 문자열에서 `동`, `가`, `로` 단위 추출 시도
4. 그래도 없으면 그룹 없이 개별 맛집 카드로 표시

그룹 카드 조건:

- 같은 `neighborhood`에 맛집 메모가 2개 이상이면 그룹 카드 표시
- 1개뿐이면 개별 맛집 카드로 표시
- 그룹 카드에는 대표 썸네일, 그룹명, 포함 장소 수, 대표 주소/동네 표시

### 4. 그룹 상세 화면

목표: `성수동 맛집` 카드 클릭 시 해당 그룹에 저장된 맛집 메모 리스트를 보여준다.

필수 UI:

- 상단 앱바: 뒤로가기, 그룹명
- 지도 미니 영역 또는 그룹 중심 지도
- 맛집 메모 리스트
  - 썸네일
  - 가게명
  - 주소
  - 1인 예상 금액
- 맛집 카드 클릭 시 상세 화면 이동

## 구현 단계

### Phase 1. 데이터 계약과 모델 고정

- [ ] `capturemate-ai` 응답 DTO 확정
- [ ] Android `RestaurantAnalysisDto` 추가
- [ ] Domain model 추가
- [ ] Room Entity/Dao 추가
- [ ] 기존 `MemoEntity`와 맛집 상세 Entity 연결 방식 결정
- [ ] 분석 응답을 Room에 저장하는 mapper 작성

완료 기준:

- 샘플 JSON을 앱 DTO로 파싱 가능
- 샘플 맛집 데이터를 Room에 저장/조회 가능

### Phase 2. 맛집 상세 화면

- [ ] `RestaurantDetailScreen` Compose 구현
- [ ] 상세 ViewModel 상태 정의
- [ ] DB에서 `restaurantMemoId` 기준 상세 조회
- [ ] 메뉴/태그/특징/추천 액션 렌더링
- [ ] 데이터 부족 시 `확인 필요` 상태 표시
- [ ] 공통 네비게이션에 상세 route 추가

완료 기준:

- 샘플 DB 데이터로 첫 번째 레퍼런스 수준의 상세 화면 표시
- 메뉴 가격과 예상 금액이 원화 포맷으로 표시
- 뒤로가기와 더보기 버튼이 화면을 깨지 않음

### Phase 3. 맛집 지도 화면

- [ ] 지도 SDK 선택 및 키 설정 방식 결정
- [ ] 지도 Composable wrapper 작성
- [ ] 저장된 맛집 메모 좌표 목록 조회
- [ ] 마커 렌더링
- [ ] 마커 클릭 시 선택된 맛집 카드 표시
- [ ] 좌표 없는 맛집 fallback 리스트 표시
- [ ] 메모함 카테고리 필터에서 `맛집` 탭 연결

완료 기준:

- 좌표가 있는 맛집 메모가 지도에 마커로 표시
- 마커 또는 카드 클릭 시 상세 화면 이동
- 좌표 없는 맛집도 앱에서 사라지지 않음

### Phase 4. 동네 그룹

- [ ] `RestaurantGroupEntity`와 member 저장 로직 구현
- [ ] AI 응답 group 정보 저장
- [ ] group 정보가 없을 때 `neighborhood` 기반 fallback 그룹 생성
- [ ] 맛집 지도 화면 하단 리스트에 그룹 카드 표시
- [ ] 그룹 카드 클릭 route 추가
- [ ] `RestaurantGroupDetailScreen` 구현

완료 기준:

- 같은 동네 맛집 2개 이상이면 그룹 카드가 표시
- 그룹 상세에서 포함 맛집 리스트 확인 가능
- 리스트 맛집 클릭 시 상세 화면 이동

### Phase 5. 외부 장소 정보 보강

이 단계는 가능한 한 `capturemate-ai`에서 처리한다.

- [ ] 가게명만 있는 OCR 결과 감지
- [ ] 카카오/네이버/구글 장소 검색 API 호출
- [ ] 후보가 여러 개일 때 주소/지역 키워드로 랭킹
- [ ] 메뉴/가격/영업 관련 정보 수집 가능 범위 확인
- [ ] 보강 실패 시 `needsUserReview = true` 반환
- [ ] Android에서 보강 실패 상태 UI 표시

완료 기준:

- 가게명만 있는 샘플에서도 주소/좌표 보강 시도
- 보강 성공 시 지도에 마커 표시
- 보강 실패 시 상세 화면이 깨지지 않고 확인 필요 상태 표시

### Phase 6. 품질 점검

- [ ] DTO 파싱 테스트
- [ ] Room mapper 테스트
- [ ] Repository 조회 테스트
- [ ] 상세 화면 Preview 또는 Compose UI 테스트
- [ ] 지도 화면에서 좌표 null 케이스 테스트
- [ ] 그룹 생성 fallback 테스트
- [ ] 실제 기기/에뮬레이터에서 지도 SDK 렌더링 확인

완료 기준:

- 샘플 맛집 1개, 같은 동네 맛집 2개, 좌표 없는 맛집 1개 케이스가 모두 정상 표시
- 앱 재실행 후 저장된 맛집 메모가 유지됨

## 샘플 시나리오

### 시나리오 A: 상세 정보가 충분한 맛집

1. 사용자가 맛집 스크린샷 추가
2. 앱이 OCR 텍스트 추출
3. 앱이 민감정보 마스킹
4. 앱이 `capturemate-ai`에 분석 요청
5. AI가 맛집 상세 정보와 좌표 반환
6. 앱이 Room에 저장
7. 사용자가 맛집 상세 화면에서 요약, 메뉴, 가격, 태그 확인
8. 메모함 맛집 탭 지도에 마커 표시

### 시나리오 B: 가게명만 있는 맛집

1. OCR 텍스트에 가게명만 있음
2. AI가 장소 검색 API로 보강 시도
3. 후보가 명확하면 주소/좌표/기본 정보를 반환
4. 후보가 불명확하면 `needsUserReview = true` 반환
5. 앱은 상세 화면에 확인 필요 상태를 표시하고 지도 마커는 생략

### 시나리오 C: 성수동 맛집 그룹

1. 성수동 주소를 가진 맛집 메모가 2개 이상 저장됨
2. AI 또는 앱 fallback이 `성수동 맛집` 그룹 생성
3. 메모함 맛집 탭에 그룹 카드 표시
4. 그룹 카드 클릭 시 저장된 성수동 맛집 리스트 표시
5. 리스트에서 맛집 클릭 시 상세 화면 표시

## 구현 순서 요약

1. DTO/Entity/Dao/Repository부터 만든다.
2. 샘플 데이터 seed 또는 테스트 데이터로 상세 화면을 먼저 완성한다.
3. 지도 SDK 연동 후 좌표 있는 맛집만 마커로 표시한다.
4. 지도 하단 카드와 상세 이동을 연결한다.
5. 동네 그룹 저장/조회와 그룹 상세 화면을 붙인다.
6. 마지막으로 AI 보강 실패/불확실 상태를 UI에 반영한다.

## 리스크와 대응

| 리스크 | 대응 |
| --- | --- |
| 지도 SDK 키/정책 문제 | Android는 좌표 표시만 담당하고, 장소 검색은 AI 서버에서 처리 |
| 메뉴/가격 정보 수집 불가 | 메뉴가 없을 때 빈 상태 UI 제공, 예상 금액은 null 허용 |
| 장소 후보가 여러 개 | AI에서 confidence와 `needsUserReview` 반환 |
| 좌표 없는 데이터 | 지도에는 생략, 리스트에는 유지 |
| 동네 분류 오류 | AI 그룹 우선, 앱 fallback은 단순 규칙으로 제한 |
| 화면 복잡도 증가 | 상세, 지도, 그룹을 Phase 단위로 분리해 순차 구현 |

## MVP 완료 정의

- 맛집 OCR 분석 결과가 로컬 DB에 저장된다.
- 첫 번째 레퍼런스와 같은 맛집 상세 화면을 실제 저장 데이터로 렌더링한다.
- 두 번째 레퍼런스와 같은 메모함 맛집 지도 화면에서 저장된 맛집 마커와 카드를 표시한다.
- 같은 동네 맛집은 그룹 카드로 묶이고, 그룹 상세에서 리스트를 볼 수 있다.
- 가게명만 있는 경우에도 AI 서버 보강 결과 또는 확인 필요 상태를 앱에서 안정적으로 표시한다.
