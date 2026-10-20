# 일정·태그 공개 범위(visibility) 규칙

도메인 SOT: 저장소 루트 `schedule_domain_design_document(1).md`  
구현 SOT: **schedule-svc** (`Tag`, `Schedule`, `ScheduleAccessService`) + **frontend 캘린더** (`features/calendar`)

---

## 1. 태그 공개 여부 변경 (소유자)

캘린더 설정(내 태그)에서 **공개 / 비공개** 토글 → `PUT /tags/{tagId}/visibility` → `TagService.updateVisibility`.

| 조건 | 결과 |
|------|------|
| 태그 소유자 + `USER` 타입 | 변경 가능 (`PRIVATE` ↔ `SHARED`, UI 「공개」는 `PUBLIC` 요청 → 서버 `SHARED`) |
| 타인 태그 / `followed` | 토글 비활성, 「공개 여부는 태그 소유자만…」 |
| `ETC` 태그 | **403** — 변경 불가 |
| API 실패 | 「태그 공개 여부를 변경할 수 없습니다.」 |

---

## 2. DB·도메인 `visibility` 값

`tags.visibility`, `schedules.visibility` 컬럼 — `VARCHAR(16)`, CHECK 제약.

| 값 | 의미 (요약) |
|----|-------------|
| `PRIVATE` | 소유자(및 스케줄은 참여자) 중심의 비공개 |
| `SHARED` | 지정 공유 대상(`tag_members` / 태그 접근) |
| `PUBLIC` | 누구나(비로그인 포함) 태그·스케줄 자체 접근 가능 |

태그와 스케줄의 `visibility`는 **독립**이다. 태그만 `PUBLIC`이어도 스케줄은 `PRIVATE`일 수 있다.

---

## 3. 태그(`tags`) 규칙

### 3.1 타입

| `type` | 설명 |
|--------|------|
| `USER` | 사용자가 만든 일반 태그 |
| `ETC` | 소유자당 하나, 미분류 일정용 시스템 태그 (`기타`) |

### 3.2 `ETC` 태그 (변경 불가)

| 항목 | 정책 |
|------|------|
| visibility | **`PRIVATE` 고정** |
| 공개·공유 전환 | **불가** |
| 삭제·이름 변경 | **불가** (서버·UI에서 USER 태그만 대상) |
| 생성 | 일정에 태그 없으면 서버가 소유자 `ETC` 자동 연결 |

### 3.3 `USER` 태그 — 생성 (`POST /tags`)

| FE 요청 | 서버 저장 |
|---------|-----------|
| `visibility: "PRIVATE"` (또는 생략) | `PRIVATE` |
| `visibility: "public"` → body `PUBLIC` | 서버가 **`SHARED`로 저장** (`PUBLIC` 요청은 SHARED로 정규화) |
| `visibility: "private"` | `PRIVATE` |

**의도:** UI의 「공개」는 DB의 `SHARED` 또는 `PUBLIC`에 가깝고, 생성 시 `PUBLIC` 문자열을내도 **실제로는 `SHARED`** 가 된다.

### 3.4 `USER` 태그 — 수정

| 동작 | schedule-svc | FE 캘린더 |
|------|-----------|-----------|
| 이름 변경 | API 없음 | 없음 |
| **visibility 변경** | `PUT /tags/{tagId}/visibility` — 소유자만 | `updateCalendarTagVisibility` |
| **삭제** | `DELETE /tags/{tagId}` — 소유자·`USER`만 (`ETC` 불가). 연결 일정에서 태그 제거 후 태그 0개면 **일정 소유자 `ETC`** 연결 | 소유 `USER`: API 삭제. `followed`: 구독 해제 |
| `tag_members` 추가 | API 없음 | 없음 |

### 3.5 태그 구독·접근 (`tag_members`, `ScheduleAccessService.canAccessTag`)

`tag_members` 한 행 = 사용자가 그 태그를 **구독**한다 (`hidden` 으로 숨김).

| 행이 생기는 때 | |
|----------------|--|
| 태그 생성 | 소유자 행 — DB 트리거(`trg_tags_owner_member`)가 만든다 |
| `POST /tags/{id}/follow` | `SHARED`·`PUBLIC` 태그 구독 (`PRIVATE` 는 403) |
| `POST /tags/{id}/members` | 소유자가 멤버 추가 |
| 자동 가입 | 게스트: `guestCalendar.subscribeTags`. 공지 같은 모두의 태그도 이 행으로만 보인다 (기본 노출 설정 없음) |

태그 **접근** (일정에 태그 붙이기 등):

| 조건 | 접근 |
|------|------|
| `owner_id` = 본인 | O |
| `visibility = PUBLIC` | O |
| `visibility = SHARED` 이고 `tag_members`에 본인 | O |
| 그 외 | X |

`GET /tags` 목록 = 구독 중인 태그 (`TagRepository.findListedForUser`, 남의 `PRIVATE` 태그 제외). 구독하지 않은 태그 찾기는 §3.7.

### 3.6 태그 목록 정렬

| 기준 | 설명 |
|------|------|
| 1순위 | `last_used_at` 내림차순 (일정에 태그 연결 시 갱신) |
| 2순위 | `last_used_at` 없음 → `created_at` 내림차순 |

`GET /tags` 응답 배열 순서가 FE 왼쪽 필터·설정·색 순서(`orderedTagIds`)에 그대로 쓰인다.

### 3.7 이메일로 태그 찾기 (`GET /tags/discover?email=`)

| 항목 | 설명 |
|------|------|
| 대상 | 해당 이메일 `users`의 `USER` 태그 중 `SHARED`·`PUBLIC`만 |
| 비공개 | `PRIVATE`(ETC 포함)는 검색 결과에 없음 |
| FE 「공개」 | 생성 시 `SHARED`로 저장되므로 검색에 포함됨 |
| 소유자 닉네임 | **profile-svc** `GET /profiles?ids=` (FE·board와 동일, schedule-svc는 DB 직접 조회 안 함) |

### 3.8 태그 숨김 (`tag_members.hidden`)

| API | 설명 |
|-----|------|
| `PUT /tags/hidden` | 숨김 태그 ID 목록 전체 교체 (구독 중인 태그에만 적용) |
| `PUT /tags/{tagId}/hidden?hidden=true\|false` | 태그 하나 숨김/표시. 구독하지 않은 태그면 404 |

- 숨긴 태그는 왼쪽 필터에서 빠지고, **달력 목록(`GET /schedules`)에서도 그 태그로 들어오는 일정이 빠진다** (§4.2). 내 태그를 숨기면 내 일정도 빠진다.
- 구독을 해제하면 행이 지워지므로 숨김 상태도 사라진다.

### 3.9 일정 중복 표시

| 구간 | 동작 |
|------|------|
| `GET /schedules` | 일정 ID당 **한 번만** 반환 (태그 필터에 여러 태그가 매칭돼도 중복 없음) |
| FE | 응답을 `id` 기준으로 한 번 더 dedupe |

---

## 4. 스케줄(`schedules`) 규칙

### 4.1 생성 (`POST /schedules`)

| 필드 | 기본값 |
|------|--------|
| `visibility` | 생략 시 `PRIVATE` |
| `tags` | 비어 있으면 소유자 `ETC` 태그 ID 자동 연결 |

### 4.1.1 삭제 (`DELETE /schedules/{scheduleId}`)

| 항목 | 정책 |
|------|------|
| 권한 | **일정 소유자만** |
| 태그 | 일정 row 삭제 시 `schedule_tags`는 FK CASCADE로 함께 제거 (별도 ETC 이동 없음) |

### 4.1.2 태그 삭제 시 일정 처리

| 상황 | 처리 |
|------|------|
| 삭제된 태그만 연결된 일정 | 해당 태그 연결 제거 후 **일정 `owner_id`의 `ETC`** 자동 연결 |
| 다른 태그도 있는 일정 | 삭제 태그 연결만 제거 |
| `ETC` 태그 | 삭제 불가 |

### 4.2 달력 목록 조회 (`GET /schedules` → `ScheduleRepository.findVisibleIds` / `findPublicIds`)

일정 전체를 읽고 하나씩 권한을 확인하지 않는다. **열쇠에서 출발해 이번 기간 일정만** 찾는다 (쿼리 1번).

| 사용자 | 보이는 일정 |
|--------|-------------|
| 비로그인 | 기간 안의 모든 `PUBLIC` 일정 |
| 로그인 — 내 일정 | 붙은 태그를 전부 숨기지 않은 내 일정 (태그를 모두 숨기면 내 일정도 숨겨짐) |
| 로그인 — 구독 태그 | `tag_members`에서 숨기지 않은 태그(남의 태그는 `PRIVATE` 아닐 때)에 붙은, 남의 `SHARED`/`PUBLIC` 일정 |
| 로그인 — 참여 일정 | `schedule_participants`에 내가 있는 일정 (숨김과 무관) |

- 로그인 사용자에게 `PUBLIC` 일정은 **태그를 구독(멤버)해야** 보인다. 구독하지 않은 `PUBLIC` 일정은 달력에 나오지 않는다.
- `scope=mine`: 위 결과 중 소유·참여 일정만. `tagIds`: 위 결과 중 해당 태그가 붙은 일정만.
- 기간 검색: `schedule_tags`·`schedule_participants`에 일정 `start_time`/`end_time` 사본이 있고 DB 트리거가 맞춘다 (migration 0014). 인덱스 0015.
- 단건 조회 API는 없다 (수정·삭제는 소유자만).

「나의 일정」과 「볼 수 있는 일정」 구분은 설계 문서 §11 참고.

---

## 5. 프론트엔드 캘린더 UI 규칙

### 5.1 API `visibility` → UI 표시

```text
TagResponse.visibility === "PUBLIC" | "SHARED"  →  UI "public" (공개 토글 ON)
그 외 (PRIVATE 등)                              →  UI "private"
```

`SHARED`도 화면에서는 **공개**로 묶어서 표시한다.

### 5.2 설정 패널 — 토글·버튼 가능 여부

| 태그 종류 | 공개/비공개 토글 | 삭제(휴지통) | 숨기기 |
|-----------|------------------|--------------|--------|
| `builtin` (UI 기본 태그) | 미표시 | 미표시 | 미표시 |
| `source: "followed"` (구독한 남의 태그) | 미표시 | 구독 해제 | 가능 |
| 본인 소유 `USER` | 표시 | **삭제**(API) | 가능 |
| 본인 소유 `ETC` | 미표시 | **미표시** | 가능 |
| 타인 소유 (가져오지 않음) | 미표시 | **미표시** | 접근 가능 시만 |

`canManageTag`: `!builtin && source !== "followed" && ownerId === currentUserId`

### 5.3 설정 패널 — 사용자 메시지

| 메시지 | 조건 |
|--------|------|
| 공개 여부는 태그 소유자만 바꿀 수 있습니다. | `!canManage(tag)` 인데 토글 시도 |
| **태그 공개 여부를 변경할 수 없습니다.** | API 실패 또는 `ETC` 등 |
| 태그 공개 여부를 변경했습니다. | 소유자 `USER` 태그 변경 성공 |
| 왼쪽 태그 목록에서 숨겼습니다. / 다시 표시 | 숨김 API 성공/실패 |
| 비공개 태그는 추가할 수 없습니다. | 태그 찾기에서 follow 실패 |

### 5.4 태그 생성 (사이드바·설정)

- `createCalendarTag(name, "private")` — 새 태그는 **비공개(`PRIVATE`)** 로만 생성.
- UI에서 생성 시 **공개로 만들기**는 설정 패널 토글에 의존 → 현재는 §1과 동일 이유로 불가.

---

## 6. API 요약 (schedule-svc)

| 메서드 | 경로 | visibility 관련 |
|--------|------|-----------------|
| GET | `/tags` | 구독 중인 태그 목록 (`hidden` 포함) |
| POST | `/tags` | 생성 시만 `{ name, visibility? }` |
| PUT | `/tags/hidden` | 숨김만 (visibility 무관) |
| PUT | `/tags/{tagId}/hidden` | 숨김만 |
| PUT | `/tags/{tagId}/visibility` | 소유자 `USER` 태그 공개 범위 변경 |
| GET | `/schedules` | 조회 시 접근 규칙 적용 |
| POST | `/schedules` | `{ visibility?, tags?(최대 20), participantEmails?(최대 50), ... }` |

---

## 7. 추후 권장

1. `tag_members` API — `SHARED` 전환 시 실제 공유 대상 지정.
2. 진짜 `PUBLIC` 태그 정책 — 생성·수정 시 `PUBLIC` vs `SHARED` 정규화 재검토.

---

## 8. 관련 파일

| 영역 | 파일 |
|------|------|
| 도메인 설계 | `schedule_domain_design_document(1).md` §8–10 |
| 서버 접근 | `schedule_svc/db/repository/ScheduleRepository.java` (목록), `ScheduleAccessService.java` (태그) |
| 서버 태그 | `schedule_svc/service/TagService.java` |
| FE 훅 | `frontend/.../hooks/useCalendarTags.ts` (`setTagVisibility`) |
| FE 메시지 | `frontend/.../components/CalendarSettingsPanel.tsx` |
| FE 권한 | `frontend/.../components/settings/calendarSettingsUtils.ts` (`canManageTag`) |
