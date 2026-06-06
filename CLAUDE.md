# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build
mvn clean package -DskipTests

# Run (requires .env at project root with DB credentials + DEEPSEEK_API_KEY)
mvn spring-boot:run
```

- Java 17, Spring Boot 4.0.6
- No tests exist yet — `-DskipTests` is always needed for now
- Requires running MySQL (port 3306, database `cueb_agent`) and Redis (port 6379)

## Architecture

Standard layered architecture: **Controller → Service → Mapper(MyBatis XML) → MySQL**

### Two API surfaces

| Surface | Controller | Base Path | Purpose |
|---|---|---|---|
| REST API | `ClassroomController` | `/api/classrooms` | Direct classroom status queries (no AI) |
| AI Agent | `ClassroomAgentController` | `/api/agent` | Natural-language chat powered by DeepSeek + tool calling |

### Classroom status determination (in `ClassroomService.buildStatus`)

Priority order:
1. **上课中** — a `course_schedule` row matches current weekday + week + period (highest)
2. **占用中 / 空闲** — an active `occupancy_report` whose `create_time + duration >= NOW()` is still valid
3. Fallback to the latest report today, or `"空闲"` if none exists

### AI tool functions (`ClassroomAgentService`)

Annotated with `@Tool` — the DeepSeek model decides when to call them:
- `queryClassrooms(classroomName?, building?, status?)` — filter/single lookup
- `reportOccupancy(classroomName, isOccupied, peopleCount, duration)` — submit occupancy report

These are wired into `ChatClient` via `.defaultTools(agentService)` in `ClassroomAgentController`.

### Multi-turn memory

`GuestChatHistoryService` stores conversation history in Redis under key `chat:guest:<token>`, TTL 30 min, max 6 rounds (12 messages). Clients call `GET /api/agent/guest-token` to get a token, then pass `X-Guest-Token` header to `/api/agent/chat/memory`.

### School period system (`PeriodUtil`)

- 10 periods/day: 8:00–21:30, mapped via `PERIOD_START[]` array
- `currentPeriod()` returns 0 outside class hours
- `currentWeek()` calculates teaching weeks from Sep 1 (the Monday on or before Sep 1 is week 1's start)

## Configuration

- `.env` at project root — loaded by `dotenv-java` in `CuebAgentDemoApplication.main()`, entries set as system properties
- `application.yml` resolves `${VAR_NAME}` placeholders from system properties
- Required env vars: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `DEEPSEEK_API_KEY`
- See `.env.example` for the template (though it references OpenAI vars — the actual `.env` uses DeepSeek)

## Database

Three tables with MyBatis mapper XML in `src/main/resources/mapper/`:
- `classroom` — name (unique), building, capacity
- `course_schedule` — week_day, start/end_period, start/end_week tied to classroom_id
- `occupancy_report` — is_occupied, people_count, duration (TIME), create_time

MyBatis config: `map-underscore-to-camel-case: true`, mapper XML location `classpath:mapper/*.xml`.

## Key Dependencies

- **Spring AI 2.0.0-M1** (`spring-ai-starter-model-deepseek`) — provides `ChatClient`, `@Tool` annotation, tool calling
- **springdoc-openapi 3.0.2** — Swagger UI at `/swagger-ui.html`
- **dotenv-java 3.2.0** — loads `.env` into system properties before Spring bootstraps
