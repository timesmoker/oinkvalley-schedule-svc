# schedule-svc

Spring Boot **일정(스케줄)·태그 REST API**다. PostgreSQL에 `tags`, `tag_members`, `schedules`, `schedule_tags`, `schedule_participants`, `user_hidden_tags` 를 두고, **JWT(HS256)** 로 상태 없이 인증한다. 클라이언트는 **`Authorization: Bearer <token>`** 만내며, 쿠키에 실린 토큰은 이 서비스에서 직접 읽지 않는다.

의존성·JDK·플러그인 버전은 **[build.gradle](build.gradle)** 을 본다.

**단일 진실 소스(SOT):** 배포·운영에서 쓰는 값의 기준은 **무조건 `infra` 폴더**(Helm values, 매니페스트, 환경 변수 정의 등)에 있다. 이 저장소의 `application.properties` 와 여기 문서는 편의·개발용 설명이며, 충돌하면 **`infra` 쪽이 정답이다.**

도메인 상세: `schedule_domain_design_document(1).md`  
공개 범위·UI 규칙: **[VISIBILITY_RULES.md](VISIBILITY_RULES.md)**

## 목차

- [빠른 시작](#빠른-시작)
- [설정](#설정)
- [프로젝트 구조](#프로젝트-구조)
- [HTTP API](#http-api)

## 빠른 시작

- **JDK 21**, 저장소에 포함된 **Gradle Wrapper** (`./gradlew`) 를 쓴다.
- **빌드:** `./gradlew bootJar`
- **로컬 실행:** `./gradlew bootRun` — DB·JWT 등은 아래 [설정](#설정)을 맞춘다.
- **DB 시드(캘린더):** `../oinkvalley-db/setup-calendar-dev.sh` 또는 `--k8s`
- **k3s 재배포:** 로컬 `k3s-reload.sh` (`.gitignore` — 머신별 경로)

## 설정

런타임은 **`src/main/resources/application.properties`** 를 따른다. **DB 연결은 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` 를 반드시 준다(기본값 없음).** 로컬 `bootRun` 도 동일하게 환경 변수를 맞춘다.

**SOT 재확인:** 클러스터·배포에 실제로 쓰는 키·값은 **`infra` 폴더**를 따른다(이 절의 표는 이름·역할 참고용).

| 환경 변수 | 바인딩(요지) | 설명 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | JDBC URL (**필수**) |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | DB 사용자 (**필수**) |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | DB 비밀번호 (**필수**) |
| `SPRING_DATASOURCE_HIKARI_SCHEMA` | `spring.datasource.hikari.schema` | 스키마 (예: `oinkvalley_core`) |
| `SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA` | `spring.jpa.properties.hibernate.default_schema` | Hibernate 기본 스키마 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `spring.jpa.hibernate.ddl-auto` | 예: `validate` (기본값 `validate`) |
| `JWT_SECRET` | `jwt.secret` | HS256 검증용 비밀키. **auth-svc 와 동일 값** |
| `CALENDAR_DEFAULT_VISIBLE_TAG_IDS` | `calendar.default-visible-tag-ids` | 쉼표 구분, 캘린더 기본 노출 태그 ID |
| `SERVICE_PROFILE_GRPC_TARGET` | `spring.grpc.client.channel.profile.target` | profile-svc gRPC (ClusterIP) |

**JWT:** 이 서비스는 토큰을 **검증만** 한다. `sub` 는 사용자 ID(숫자 문자열), `roles` 는 `ROLE_` 접두사 없이 JWT에 담기고 필터에서 스프링 규약에 맞게 변환한다.

## 프로젝트 구조

| 경로 | 역할 |
| --- | --- |
| `controller/` | `ScheduleController`, `TagController`, `HealthController`, `ScheduleExceptionHandler` |
| `service/` | 도메인 로직, `ScheduleMapper`, `CalendarTimeUtil` |
| `client/` | `UserProfileClient` → profile-svc gRPC |
| `dto/schedule/`, `dto/tag/` | 요청·응답 레코드 |
| `db/domain/` | JPA 엔티티 |
| `db/repository/` | Spring Data JPA |
| `security/` | JWT 검증 (`JwtUtil`, `JwtAuthenticationFilter`) |
| `config/` | `SecurityConfig`, `CalendarProperties` |

## HTTP API

베이스 URL·리버스 프록시 접두 경로는 이 저장소에서 고정하지 않는다.

### 보안 (구현 요약)

- Spring Security, **무상태**(세션 미사용), **CSRF 비활성화**.
- JWT는 **`Authorization: Bearer <token>`** 만 처리한다.
- **규칙 (`SecurityConfig`):**
  1. `GET /health` → 허용.
  2. `GET /schedules` → 허용(비로그인은 **PUBLIC** 일정만, `ScheduleAccessService`·`scope` 로 필터).
  3. 나머지 → 인증 필요.

### 엔드포인트

| 메서드 | 경로 | 인증 | 비고 |
| --- | --- | --- | --- |
| GET | `/health` | 불필요 | |
| GET | `/schedules?from&to&scope&tagIds` | 선택 | `scope=visible`(기본) \| `mine`. 비로그인 PUBLIC만 |
| POST | `/schedules` | 필요 | 참여자 이메일 → `schedule_participants` |
| PUT | `/schedules/{id}` | 필요 | 소유자만 |
| DELETE | `/schedules/{id}` | 필요 | 소유자만 |
| GET | `/tags` | 필요 | |
| GET | `/tags/discover?email=` | 필요 | SHARED/PUBLIC USER 태그 |
| POST | `/tags` | 필요 | |
| PUT | `/tags/{id}` | 필요 | 이름 변경 |
| PUT | `/tags/{id}/visibility` | 필요 | |
| DELETE | `/tags/{id}` | 필요 | USER 태그, 소유자 |
| PUT | `/tags/hidden` | 필요 | |
| PUT | `/tags/{id}/hidden` | 필요 | |
| POST | `/tags/{id}/follow` | 필요 | SHARED → `tag_members` |
| DELETE | `/tags/{id}/follow` | 필요 | |
| POST | `/tags/{id}/members` | 필요 | 소유자, 이메일로 멤버 추가 |
| DELETE | `/tags/{id}/members/{userId}` | 필요 | 소유자 |

JSON 은 **camelCase** 다. 필드·공개 규칙 상세는 **[VISIBILITY_RULES.md](VISIBILITY_RULES.md)** 를 본다.
