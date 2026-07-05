package com.capturemate.app.feature.memo

import androidx.lifecycle.ViewModel
import com.capturemate.app.domain.model.LifeInfo
import com.capturemate.app.domain.model.Memo
import com.capturemate.app.domain.model.MemoCategory
import com.capturemate.app.domain.model.MemoGroup
import com.capturemate.app.domain.model.PlaceInfo
import com.capturemate.app.domain.model.ScheduleInfo
import com.capturemate.app.domain.model.Screenshot
import com.capturemate.app.domain.model.ShopInfo
import com.capturemate.app.domain.model.StudyInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val DAY_MS = 24L * 60L * 60L * 1000L

class MemoViewModel : ViewModel() {
    private val now = System.currentTimeMillis()

    private val _unconfirmedMemos = MutableStateFlow(sampleUnconfirmedMemos(now))
    val unconfirmedMemos = _unconfirmedMemos.asStateFlow()

    private val _confirmedMemos = MutableStateFlow(sampleConfirmedMemos(now))
    val confirmedMemos = _confirmedMemos.asStateFlow()

    private val _memoGroups = MutableStateFlow<List<MemoGroup>>(emptyList())
    val memoGroups = _memoGroups.asStateFlow()

    fun saveMemo(id: String, remindDays: Int? = null) {
        val memo = _unconfirmedMemos.value.find { it.id == id } ?: return

        _unconfirmedMemos.value = _unconfirmedMemos.value.filterNot { it.id == id }
        _confirmedMemos.value = _confirmedMemos.value + memo.copy(
            isConfirmed = true,
            remindAt = remindDays?.let {
                System.currentTimeMillis() + it * DAY_MS
            },
        )
    }

    fun dismissMemo(id: String) {
        _unconfirmedMemos.value = _unconfirmedMemos.value.filterNot { it.id == id }
    }

    fun snoozeMemo(id: String) {
        _unconfirmedMemos.value = _unconfirmedMemos.value.map { memo ->
            if (memo.id == id) memo.copy(createdAt = System.currentTimeMillis() + 3 * DAY_MS) else memo
        }
    }

    fun updateMemo(id: String, patch: MemoPatch) {
        _unconfirmedMemos.value = _unconfirmedMemos.value.map { it.applyPatch(id, patch) }
        _confirmedMemos.value = _confirmedMemos.value.map { it.applyPatch(id, patch) }
    }

    fun deleteMemo(id: String) {
        _unconfirmedMemos.value = _unconfirmedMemos.value.filterNot { it.id == id }
        _confirmedMemos.value = _confirmedMemos.value.filterNot { it.id == id }
    }

    fun mergeMemo(newId: String, existingId: String) {
        val newMemo = _unconfirmedMemos.value.find { it.id == newId } ?: return
        _confirmedMemos.value = _confirmedMemos.value.map { memo ->
            if (memo.id == existingId) {
                memo.copy(
                    screenshots = memo.screenshots + newMemo.screenshots,
                    screenshotCount = (memo.screenshotCount ?: memo.screenshots.size) + newMemo.screenshots.size,
                )
            } else {
                memo
            }
        }
        _unconfirmedMemos.value = _unconfirmedMemos.value.filterNot { it.id == newId }
    }

    fun groupMemos(ids: List<String>) {
        val selected = _unconfirmedMemos.value.filter { it.id in ids }
        if (selected.size < 2) return

        val primary = selected.first()
        _memoGroups.value = _memoGroups.value + MemoGroup(
            id = "group-${System.currentTimeMillis()}",
            title = primary.title,
            category = primary.category,
            memos = selected,
            summary = selected.joinToString(" / ") { it.summary },
        )
        _unconfirmedMemos.value = _unconfirmedMemos.value.filterNot { it.id in ids }
        _confirmedMemos.value = _confirmedMemos.value + primary.copy(
            id = "grouped-${primary.id}",
            title = primary.title,
            summary = selected.joinToString(" / ") { it.summary },
            screenshots = selected.flatMap { it.screenshots },
            screenshotCount = selected.sumOf { it.screenshotCount ?: it.screenshots.size },
            isConfirmed = true,
        )
    }

    private fun Memo.applyPatch(id: String, patch: MemoPatch): Memo {
        if (this.id != id) return this
        return copy(
            title = patch.title ?: title,
            summary = patch.summary ?: summary,
            category = patch.category ?: category,
        )
    }

}

data class MemoPatch(
    val title: String? = null,
    val summary: String? = null,
    val category: MemoCategory? = null,
)

private fun sampleUnconfirmedMemos(now: Long): List<Memo> =
    listOf(
        Memo(
            id = "u1",
            title = "서울대 빅데이터 캠프 모집",
            summary = "7월 10일 마감, 서울대 39동. 학부생 대상 4주 과정, 장학금 지급",
            category = MemoCategory.Schedule,
            screenshots = listOf(Screenshot("s1", ""), Screenshot("s2", "")),
            createdAt = now,
            screenshotCount = 2,
            dDay = 11,
            recommendedAction = "7월 10일 마감 알림 설정",
            scheduleInfo = ScheduleInfo(
                eventTitle = "서울대 빅데이터 캠프 모집",
                date = "2026년 7월 10일 (금)",
                deadline = "2026년 7월 10일 23:59",
                location = "서울대학교 39동 강의실",
                notes = "학부생 대상 · 장학금 월 50만원 · 4주 과정",
            ),
        ),
        Memo(
            id = "u2",
            title = "정보처리기사 실기 공부법",
            summary = "기출문제 반복, SQL·프로그래밍 문제 유형, 시험 직전 암기 포인트를 정리한 자료",
            category = MemoCategory.Study,
            screenshots = listOf(Screenshot("s3", ""), Screenshot("s4", "")),
            createdAt = now,
            screenshotCount = 5,
            keyPoints = listOf("기출문제 반복", "SQL 실습", "프로그래밍 위주"),
            recommendedAction = "3일 후 복습 알림 설정 권장",
            studyInfo = StudyInfo(
                keyPoints = listOf(
                    "기출문제 반복 풀이에서 패턴 파악이 핵심",
                    "SQL, 프로그래밍, SW공학 위주로 시간 배분",
                    "시험 직전 암기 포인트를 시험 2주 전 다시 암기",
                    "실기 환경 직접 설정해 프로그래밍 실습 필수",
                ),
                reviewSchedule = "3일 후",
            ),
        ),
        Memo(
            id = "u5",
            title = "SQL 핵심 개념 정리",
            summary = "JOIN, GROUP BY, 서브쿼리 중심으로 실기 대비 정리",
            category = MemoCategory.Study,
            screenshots = listOf(Screenshot("s7", "")),
            createdAt = now,
            screenshotCount = 1,
            keyPoints = listOf("JOIN 유형 구분", "집계 함수 복습", "서브쿼리 패턴"),
            studyInfo = StudyInfo(
                keyPoints = listOf("JOIN 유형 구분", "집계 함수 복습", "서브쿼리 패턴"),
                reviewSchedule = "3일 후",
            ),
        ),
        Memo(
            id = "u3",
            title = "청년 교통비 지원 신청 안내",
            summary = "만 19~34세 대상, 월 최대 6만원 지원. 7월 15일까지 온라인 신청",
            category = MemoCategory.Life,
            screenshots = listOf(Screenshot("s5", "")),
            createdAt = now - DAY_MS,
            screenshotCount = 1,
            dDay = 16,
            recommendedAction = "7월 15일 신청 마감 알림 설정",
            lifeInfo = LifeInfo(
                benefit = "월 최대 6만원 교통비 지원 (연 72만원)",
                target = "만 19~34세 서울 거주 청년",
                applyMethod = "복지로 앱 또는 주민센터 방문 신청",
                deadline = "2026년 7월 15일",
            ),
        ),
        Memo(
            id = "u4",
            title = "을지로 힙바 3선",
            summary = "힙한 분위기 을지로 골목 바 3곳. 예약 불가, 웨이팅 필수",
            category = MemoCategory.Place,
            screenshots = listOf(Screenshot("s6", "")),
            createdAt = now - DAY_MS,
            screenshotCount = 1,
            placeInfo = PlaceInfo(
                placeName = "을지로 힙바 3선",
                address = "서울특별시 중구 을지로 일대",
                menu = listOf("생맥주 6,000원", "하이볼 9,000원", "안주 12,000원~"),
                priceRange = "1인 25,000~35,000원",
                tags = listOf("회식", "친구", "분위기"),
            ),
        ),
    )

private fun sampleConfirmedMemos(now: Long): List<Memo> =
    listOf(
        Memo(
            id = "c1",
            title = "부천국제영화제 주요 일정",
            summary = "7월 3일~13일 개최, 주요 상영작과 예매 오픈 일정 캡처",
            category = MemoCategory.Schedule,
            screenshots = listOf(Screenshot("sc1", ""), Screenshot("sc2", "")),
            isConfirmed = true,
            createdAt = now - 2 * DAY_MS,
            dDay = 4,
            screenshotCount = 2,
            detectedEvent = ScheduleInfo(
                eventTitle = "부천국제영화제 개막",
                date = "2026년 7월 3일 (금)",
                time = "오후 7:00",
                location = "부천시청 잔디광장",
                notes = "예매 오픈 6월 27일 · 야외 상영 무료 입장",
            ),
            scheduleInfo = ScheduleInfo(
                eventTitle = "부천국제영화제 개막",
                date = "2026년 7월 3일 (금)",
                time = "오후 7:00",
                location = "부천시청 잔디광장",
                notes = "예매 오픈 6월 27일 · 야외 상영 무료 입장",
            ),
        ),
        Memo(
            id = "c2",
            title = "알고리즘 스터디 OT 자료",
            summary = "매주 화·목 오후 8시 디스코드. BFS/DFS → DP → 그리디 순서",
            category = MemoCategory.Study,
            screenshots = listOf(Screenshot("sc3", "")),
            isConfirmed = true,
            createdAt = now - 5 * DAY_MS,
            keyPoints = listOf(
                "화·목 오후 8시 디스코드 채널 접속",
                "BFS/DFS → DP → 그리디 → 분할정복 순",
                "매주 백준 3문제 필수 풀이",
            ),
            studyInfo = StudyInfo(
                keyPoints = listOf(
                    "화·목 오후 8시 디스코드 채널 접속",
                    "BFS/DFS → DP → 그리디 → 분할정복 순",
                    "매주 백준 3문제 필수 풀이",
                ),
                reviewSchedule = "7일 후",
            ),
        ),
        Memo(
            id = "c3",
            title = "청년내일저축계좌 신청",
            summary = "근로·사업소득 있는 만 19~34세. 매달 10만원 저축 시 3배 매칭",
            category = MemoCategory.Life,
            screenshots = listOf(Screenshot("sc4", "")),
            isConfirmed = true,
            createdAt = now - 3 * DAY_MS,
            lifeInfo = LifeInfo(
                benefit = "월 10만원 저축 시 정부 매칭 30만원 → 3년 후 1,440만원",
                target = "만 19~34세 근로·사업소득 있는 청년",
                applyMethod = "복지로 또는 행복e음 사이트",
                deadline = "2026년 7월 31일",
            ),
        ),
        Memo(
            id = "c4",
            title = "성수동 브런치 카페",
            summary = "에그베네딕트 맛집. 주말 웨이팅 40분, 평일 바로 입장 가능",
            category = MemoCategory.Place,
            screenshots = listOf(Screenshot("sc5", "")),
            isConfirmed = true,
            createdAt = now - 7 * DAY_MS,
            placeInfo = PlaceInfo(
                placeName = "카페 오월의 종",
                address = "서울 성동구 성수이로 77",
                menu = listOf("에그베네딕트 18,000원", "아메리카노 6,000원", "크로와상 5,500원"),
                priceRange = "1인 20,000~30,000원",
                tags = listOf("데이트", "브런치", "혼밥"),
            ),
        ),
        Memo(
            id = "c6",
            title = "분기 회고 회의 자료",
            summary = "다음 분기 목표와 액션 아이템을 팀별로 정리한 캡처",
            category = MemoCategory.Work,
            screenshots = listOf(Screenshot("sc7", "")),
            isConfirmed = true,
            createdAt = now - 4 * DAY_MS,
        ),
        Memo(
            id = "c7",
            title = "Compose Navigation 샘플",
            summary = "NavHost, route, ViewModel 상태 연결 방식 메모",
            category = MemoCategory.Development,
            screenshots = listOf(Screenshot("sc8", "")),
            isConfirmed = true,
            createdAt = now - 6 * DAY_MS,
            keyPoints = listOf("route를 문자열로 분리", "StateFlow를 collectAsState로 구독", "상세 화면은 선택 id 기반"),
        ),
        Memo(
            id = "c5",
            title = "나이키 에어맥스 2025 신상",
            summary = "7월 한정 발매, 정가 189,000원. 나이키 앱 선착순 드로우",
            category = MemoCategory.Purchase,
            screenshots = listOf(Screenshot("sc6", "")),
            isConfirmed = true,
            createdAt = now - DAY_MS,
            shopInfo = ShopInfo(
                productName = "나이키 에어맥스 2025",
                price = "189,000원",
                seller = "나이키 공식 앱",
            ),
        ),
    )
