# aop-log-helper

[![Release](https://img.shields.io/github/v/release/catomat0/Alh?sort=semver)](https://github.com/catomat0/Alh/releases)
[![JavaDoc](https://img.shields.io/badge/javadoc-latest-blue)](https://catomat0.github.io/Alh/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)

Spring Boot용 AOP 로깅 스타터. 요청 traceId(MDC) + 컨트롤러/서비스 자동 로깅 + Slow 감지 + 민감정보 마스킹을 자동 설정으로 제공합니다.

📖 **[JavaDoc API 레퍼런스](https://catomat0.github.io/Alh/)** · **[Releases](https://github.com/catomat0/Alh/releases)**

- **MDC 요청 컨텍스트** — 모든 요청마다 `requestId`, `userId`, `method`, `uri` 를 자동 세팅 (`X-Request-Id` 헤더 전파 지원)
- **자동 로깅 AOP** — 설정한 pointcut 표현식과 매칭되는 메서드는 진입/종료 자동 로그 (파라미터·소요시간·예외 포함)
- **Slow 감지** — 임계값(`slow-threshold-ms`) 초과 시 WARN 으로 로깅. 메서드마다 `@LogSlow(thresholdMs=...)` 로 override 가능
- **민감정보 마스킹** — `password`, `token`, `authorization` 등 지정 키워드는 로그에서 `***` 로 자동 대체 (Map/JSON/`key=value` 문자열 지원)
- **PII 마스킹** — 이메일, 카카오/한국 휴대폰 번호, 신용카드, 주민등록번호는 정규식 기반으로 자동 마스킹 (`alice@x.com` → `a***@***.com`, `010-1234-5678` → `010-****-5678`, 카드 마지막 4자리 유지, 주민번호 전체 마스킹). 숫자 userId 는 그대로 유지
- **로그 인젝션 방지** — 파라미터/예외 메시지/URI 내부 `\n`, `\r`, `\t`, ESC 는 자동 이스케이프
- **Alert / Notification** — `ERROR` (또는 `WARN`) 로그 발생 시 Slack/Discord 웹훅으로 자동 알림 (비동기 발송, 레이트 리미트, 재귀 방지, MDC 포함, 스택 트레이스 트림)
- **커스텀 어노테이션** — `@LogExecution` (강제 로깅), `@LogSlow` (임계값 오버라이드), `@NoLog` (제외)
- **완전한 on/off** — `alh.mdc.enabled=false` 또는 `alh.logging.enabled=false` 로 통째로 끄기
- **Spring Boot AutoConfiguration** — 빈 자동 등록, MDC/AOP 각각 독립적으로 opt-out 가능
- **모든 사용자 커스터마이징 지점 `@ConditionalOnMissingBean`** — 기본 빈은 사용자 빈으로 자유롭게 교체

---

## 1. 설치

**요약: 그냥 아래 두 줄 넣으면 끝.** 인증 세팅 필요 없고, 로컬/GitHub Actions/Jenkins/GitLab CI 어디서든 그대로 동작합니다.

```gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.catomat0:Alh:1.0.0'
}
```

**필수 런타임 의존성** (consumer 프로젝트에 이미 있어야 함):
- Java 17+
- Spring Boot 3.4+ (`spring-boot-starter-web`, `spring-boot-starter-aop`)

### 1-1. 왜 JitPack?

| 상황 | JitPack | GitHub Packages | Maven Central |
|---|---|---|---|
| 개인 레포에서 사용 | ✅ 세팅 0 | ⚠️ PAT 필요 | ✅ 세팅 0 |
| 다른 사용자 레포에서 사용 | ✅ 세팅 0 | ⚠️ PAT 필요 | ✅ 세팅 0 |
| 외부 조직 레포에서 사용 | ✅ 세팅 0 | ❌ 별도 PAT 발급 필요 | ✅ 세팅 0 |
| GitHub Actions | ✅ 그대로 됨 | ⚠️ secrets 세팅 필요 | ✅ 그대로 됨 |
| Jenkins/GitLab/CircleCI | ✅ 그대로 됨 | ⚠️ credential 저장 필요 | ✅ 그대로 됨 |

**JitPack 은 public 저장소에 대해 무인증 접근**을 제공합니다. Consumer 프로젝트가 개인이든 조직이든 CI 든 로컬이든, 위 두 줄만 있으면 그냥 동작합니다.

### 1-2. Maven 사용자

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.catomat0</groupId>
    <artifactId>Alh</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 1-3. 버전 지정 방법

| 표기 | 의미 |
|---|---|
| `1.0.0` | 특정 릴리즈 태그 (권장) |
| `main-SNAPSHOT` | main 브랜치 최신 커밋 (실험용, 캐시 30분) |
| `<커밋해시>` | 특정 커밋 (재현 가능) |

**첫 요청 시 JitPack 서버가 소스로부터 빌드**하기 때문에 30초~2분 지연이 있을 수 있고, 이후 요청은 캐시에서 즉시 응답합니다. CI 에서 처음 pull 할 때만 잠깐 느립니다.

### 1-4. CI/CD 스니펫

**GitHub Actions** — 아무 추가 세팅 불필요
```yaml
- uses: actions/checkout@v4
- uses: actions/setup-java@v4
  with:
    java-version: '17'
    distribution: 'temurin'
- run: ./gradlew build
```

**Jenkins / GitLab CI / CircleCI** — Consumer 의 표준 Java 빌드 스텝 그대로. Alh 때문에 추가 credential 세팅 필요 **없음**.

**Docker 빌드** — 그냥 됨.
```dockerfile
FROM gradle:8-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle build --no-daemon
```

---

## 1-B. (선택) GitHub Packages 로 사용하기

JitPack 은 첫 요청이 느릴 수 있어서 **CI 캐시 히트율을 최우선**으로 하고 싶거나, **완전 private 배포**가 필요한 경우 GitHub Packages 를 쓸 수 있습니다. 다만 소비자마다 PAT 세팅이 필요합니다.

<details>
<summary>펼쳐서 보기</summary>

**Step 1. GitHub PAT 발급**
[Settings → Developer settings → Personal access tokens (classic)](https://github.com/settings/tokens/new) — Scope: `read:packages` 만.

**Step 2. `~/.gradle/gradle.properties`** — 커밋 금지
```properties
gpr.user=<본인_github_username>
gpr.token=ghp_xxxxxxxxxxxxxxxxxxxxx
```

**Step 3. `build.gradle`**
```gradle
repositories {
    maven {
        url = uri('https://maven.pkg.github.com/catomat0/Alh')
        credentials {
            username = project.findProperty('gpr.user') ?: System.getenv('GITHUB_ACTOR')
            password = project.findProperty('gpr.token') ?: System.getenv('GITHUB_TOKEN')
        }
    }
}
```

**Step 4. CI 별 세팅**

- **GitHub Actions (같은 catomat0 계정 레포)** — workflow 에 `permissions: { packages: read }` 만 추가하면 자동 `GITHUB_TOKEN` 사용 가능
- **GitHub Actions (다른 계정/조직)** — 본인 PAT 을 secret 으로 등록 후 주입
  ```yaml
  - run: ./gradlew build
    env:
      GITHUB_ACTOR: ${{ github.actor }}
      GITHUB_TOKEN: ${{ secrets.GH_PACKAGES_READ_TOKEN }}
  ```
- **Jenkins/GitLab** — CI credential 저장소에 `gpr.user`/`gpr.token` 등록 후 build args 로 전달

**Q. 라이브러리를 임포트하면 소유자에게 내 정보/토큰이 흘러가나?**
아니오. PAT 은 GitHub Packages 서버 인증에만 쓰이고, 라이브러리 코드는 순수 JAR (외부 통신 로직 없음).

</details>

---

## 2. 최소 설정 예시

`application.yml` 에 아무것도 안 넣어도 기본값으로 동작합니다. 커스터마이징하려면:

```yaml
alh:
  mdc:
    enabled: true                # 기본 true — false 면 필터 미등록
    request-id-length: 8         # UUID 앞 N 자리 (1~32)
    header-name: X-Request-Id    # 상류에서 넘어온 traceId 재사용용 헤더
    response-header: true        # 응답 헤더에도 requestId 실어 내보내기

  logging:
    enabled: true
    pointcut: "execution(* com.myapp..*Service*.*(..)) || execution(* com.myapp..*Controller*.*(..))"
    log-args: true               # 파라미터 로깅
    log-result: false            # 반환값 로깅 (payload 큰 경우 부하 주의)
    slow-threshold-ms: 500       # 이 값 이상이면 WARN 으로 로깅
    mask-pii: true               # 이메일/휴대폰/카드/주민번호 정규식 마스킹 (기본 on)
    mask-keywords:
      - password
      - passwd
      - secret
      - token
      - accessToken
      - refreshToken
      - authorization
      - apiKey
      - creditCard
      - ssn
```

## 3. Logback 패턴 예시

MDC 키 (`requestId`, `userId`, `method`, `uri`) 를 로그 라인에 노출하려면:

```xml
<configuration>
    <property name="LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{requestId}] [userId=%X{userId}] [%X{method} %X{uri}] %logger{36} - %msg%n"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

## 4. 사용자 ID 로 MDC 채우기

Spring Security 를 쓰면 principal 을 꺼내 MDC 에 넣어야 로그가 유의미해집니다. `AlhUserIdResolver` 빈을 하나 등록하면 기본 `"anonymous"` 대신 사용됩니다:

```java
@Bean
AlhUserIdResolver alhUserIdResolver() {
    return request -> {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return String.valueOf(userId);
        }
        return "anonymous";
    };
}
```

## 5. 어노테이션 활용

### `@LogExecution` — 강제 로깅 (pointcut 밖이어도 무조건)

```java
@LogExecution(logArgs = true, logResult = true)
public Order placeOrder(OrderRequest request) {
    // 이 메서드는 alh.logging.pointcut 표현식과 상관없이 항상 로깅됨
}
```

### `@LogSlow` — 임계값 오버라이드

```java
@LogSlow(thresholdMs = 100)   // 100ms 초과 시 WARN
public List<Report> heavyQuery() { ... }
```

### `@NoLog` — 제외

```java
@NoLog
public String healthPing() { return "OK"; }
```

## 6. 로그 예시

```
2026-09-15 14:30:12.083 [http-nio-8080-exec-1] INFO  [a3f9c1] [userId=42] [POST /api/orders] c.g.c.a.l.AlhLoggingAspect - [OrderService] placeOrder([OrderRequest(item=book, password=***)]) - 87ms
2026-09-15 14:30:12.145 [http-nio-8080-exec-2] WARN  [b21ef0] [userId=42] [GET /api/reports] c.g.c.a.l.AlhLoggingAspect - [ReportService] heavyQuery([]) - 623ms (>= 500ms)
2026-09-15 14:30:12.200 [http-nio-8080-exec-3] ERROR [c94a2b] [userId=anonymous] [POST /api/login] c.g.c.a.l.AlhLoggingAspect - [AuthService] login([{email=a***@***.com, password=***}]) - 12ms - BadCredentialsException: invalid password
2026-09-15 14:30:12.410 [http-nio-8080-exec-4] INFO  [d31ba2] [userId=100] [POST /api/pay] c.g.c.a.l.AlhLoggingAspect - [PayService] charge([PayReq(card=****-****-****-1234, phone=010-****-5678)]) - 210ms
```

## 7. Alert / Notification (Slack, Discord)

로그 레벨이 임계값 (기본 `ERROR`) 이상이면 자동으로 웹훅으로 알림이 발송됩니다.
Logback appender 로 붙기 때문에 **AOP 밖에서 발생한 에러 (Spring 예외, DB, 네트워크 등) 도 모두 캐치**됩니다.

### 7-1. Slack

```yaml
alh:
  alert:
    enabled: true
    type: slack
    webhook-url: ${SLACK_WEBHOOK_URL}  # https://hooks.slack.com/services/... (환경변수로 주입 권장)
    threshold: ERROR                # ERROR | WARN
    include-mdc-keys: [requestId, userId, method, uri]
    include-stack-trace: true
    max-stack-lines: 20
    rate-limit-per-minute: 30       # 0 = 무제한
    connect-timeout-ms: 3000
    read-timeout-ms: 5000
```

발송 예시:
```
🚨 ERROR com.example.OrderService
requestId=a3f9c1 userId=42 method=POST uri=/api/orders
database timeout
```
```
java.sql.SQLException: connection refused
  at com.example.OrderService.placeOrder(OrderService.java:42)
  ... 17 more
```

### 7-2. Discord

```yaml
alh:
  alert:
    enabled: true
    type: discord
    webhook-url: ${DISCORD_WEBHOOK_URL}  # https://discord.com/api/webhooks/... (환경변수로 주입 권장)
    threshold: ERROR
```

### 7-3. 왜 이걸 라이브러리에서?

- **비동기 발송** — 로깅 스레드를 블록하지 않도록 별도 데몬 스레드에서 HTTP POST
- **레이트 리미트** — 장애 시 초당 수백 개 에러가 나도 웹훅 초당 30개로 제한 (기본값). 슬랙/디스코드 rate limit 회피
- **재귀 방지** — 웹훅 자체가 실패하면 `System.err` 로만 남김 (SLF4J 사용 시 무한 재귀)
- **MDC 필터링** — requestId/userId 만 뽑아서 알림. 불필요한 컨텍스트 배제
- **스택 트림** — `max-stack-lines` 이상은 잘라서 슬랙 메시지 사이즈 초과 방지

### 7-4. 커스텀 클라이언트

Teams, PagerDuty, 사내 aggregator 등을 쓰려면 `WebhookClient` 빈을 하나 정의하면 기본 Slack/Discord 를 대체합니다:

```java
@Bean
WebhookClient customWebhookClient(AlhAlertProperties props) {
    return event -> {
        // 사내 알림 서버로 전송
    };
}
```

`@ConditionalOnMissingBean` 이라 사용자 빈이 우선합니다.

---

## 8. 통째로 끄기

라이브러리는 붙여놨지만 특정 환경 (예: 성능 벤치) 에서 꺼야 하면:

```yaml
alh:
  mdc:
    enabled: false
  logging:
    enabled: false
  alert:
    enabled: false
```

각 모듈이 독립적이라 한쪽만 끄기도 가능합니다.

## 9. 커스터마이징 지점 (`@ConditionalOnMissingBean`)

같은 타입의 빈을 사용자 프로젝트에 정의하면 라이브러리 기본 빈을 덮어씁니다:

| 빈 타입 | 기본 구현 | 언제 override |
|---|---|---|
| `AlhUserIdResolver` | `"anonymous"` 반환 | Security principal 매핑 |
| `AlhMdcFilter` | 기본 필터 | 특수 헤더/MDC 키 추가 |
| `SensitiveMasker` | 프로퍼티 키워드 기반 | 도메인 특화 마스킹 로직 |
| `AlhLoggingAspect` | 기본 인터셉터 | 로그 포맷 자체 커스터마이징 |
| `WebhookClient` | Slack/Discord JDK HttpClient | Teams/PagerDuty/사내 aggregator |
| `AlertRateLimiter` | per-minute counter | sliding window / redis 기반 등 |

---

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).
