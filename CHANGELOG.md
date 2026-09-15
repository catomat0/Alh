# Changelog

이 프로젝트의 모든 주요 변경사항은 이 파일에 기록됩니다.
버전 형식은 [Semantic Versioning](https://semver.org/) 기준.

## [1.0.0] - 2026-09-15

첫 릴리즈. Spring Boot 3.4+ 용 AOP 로깅 스타터.

### Added

- **MDC 요청 컨텍스트 (`alh.mdc.*`)**
  - 요청마다 `requestId`, `userId`, `method`, `uri` 자동 세팅 (`AlhMdcFilter`)
  - `X-Request-Id` 헤더 재사용 (whitelist 정규식 검증) / 응답 헤더로 되돌려보내기
  - `AlhUserIdResolver` 인터페이스로 Spring Security principal 커스텀 매핑 지원
  - URI/method/userId 길이 제한 + control char strip (log injection 방지)

- **AOP 자동 로깅 (`alh.logging.*`)**
  - 사용자 설정 pointcut 표현식 (`alh.logging.pointcut`) 매칭 메서드 자동 진입/종료 로그
  - 파라미터·소요시간·예외 포함
  - `AspectJExpressionPointcutAdvisor` + `MethodInterceptor` 기반
  - `spring-boot-starter-aop` 조건부 활성화

- **Slow method 감지**
  - `alh.logging.slow-threshold-ms` 초과 시 INFO → WARN 승격
  - `@LogSlow(thresholdMs=...)` 메서드/클래스별 override

- **민감정보 마스킹**
  - 키워드 기반: `password`, `token`, `authorization` 등 프로퍼티로 지정 (Map/JSON/`key=value` 문자열 재귀 처리)
  - PII 정규식: 이메일 (`a***@***.com`), 한국 휴대폰 (`010-****-5678`), 신용카드 (마지막 4자리 유지), 주민등록번호 (전체 마스킹)
  - 숫자 userId 는 그대로 유지 (예: `userId=42` 마스킹 안 됨)
  - `Pattern.quote()` 로 사용자 키워드 정규식 escape

- **로그 인젝션 방지**
  - 파라미터·예외 메시지·URI 내부 `\n`, `\r`, `\t`, ESC 자동 이스케이프
  - 컨트롤 문자 (`<0x20`, `0x7F`) 는 `?` 로 대체

- **커스텀 어노테이션**
  - `@LogExecution(logArgs, logResult)` — 전역 pointcut 밖이어도 강제 로깅
  - `@LogSlow(thresholdMs)` — 임계값 override
  - `@NoLog` — 로깅 완전 제외

- **Alert / Notification (`alh.alert.*`)**
  - Logback appender 로 붙어서 ERROR/WARN 이상 로그 → Slack/Discord 웹훅 자동 발송
  - AOP 밖 에러 (Spring 예외, DB, 네트워크 등) 도 캐치
  - 비동기 dispatch (bounded 1-thread executor, discard-oldest)
  - 레이트 리미트 (`rate-limit-per-minute`) — 장애 시 웹훅 폭탄 방지
  - 무한루프 가드 — 웹훅 실패는 `System.err` 로만 남김
  - MDC 필터링 (`include-mdc-keys`) — requestId/userId 등 필요한 키만 포함
  - 스택 트레이스 트림 (`max-stack-lines`) — 슬랙/디스코드 사이즈 제한 회피
  - 커스텀 `WebhookClient` 빈으로 Teams/PagerDuty 등 확장

- **Spring Boot AutoConfiguration**
  - `AlhMdcAutoConfiguration`, `AlhLoggingAutoConfiguration`, `AlhAlertAutoConfiguration`
  - `AutoConfiguration.imports` (Spring Boot 3 방식)
  - 모듈별 독립 `enabled` 프로퍼티로 통째로 opt-out 가능
  - 모든 사용자 커스터마이징 지점 `@ConditionalOnMissingBean`

### Distribution

- **JitPack primary** — 소비자 프로젝트가 개인/조직/외부 레포 어디서든 credential 없이 사용 가능
- **GitHub Packages** — private 배포 / 캐시 최적화 필요 시 옵션
- **GitHub Pages JavaDoc** — https://catomat0.github.io/Alh/
- **Apache License 2.0**
