# VH. Health — project memory

Android app: a workout schedule for someone who wakes at 04:30, works a desk job, and
wants to lose fat, firm up, and build knees strong enough to run on.

**Read `docs/STATUS.md` first.** It is the living state of the project and says exactly
what to do next. This file is the stable background that rarely changes.

---

## The one constraint that shapes everything

`dl.google.com` is **blocked** by the network policy of the Claude Code container.
The Android SDK, the Android Gradle Plugin and every AndroidX artifact live behind it.

```
curl -I https://dl.google.com/...           → CONNECT tunnel failed, 403
curl -L  https://maven.google.com/...       → redirects to dl.google.com, 403
curl -I  https://repo1.maven.org/maven2/    → 200
curl -I  https://plugins.gradle.org/m2/     → 200
```

So, in this container:

- `./gradlew :core:test` **works** and is the real feedback loop. Use it constantly.
- `./gradlew :app:...` **cannot work**. Do not try, and do not spend turns diagnosing it.
- The APK is built by GitHub Actions (`.github/workflows/android-build.yml`), and the
  user downloads it from the run's Artifacts.

Never route around the block, never disable TLS verification. If `:app` needs
verifying, push and read the CI logs.

## Layout

```
core/    plain Kotlin JVM. Domain model, the anchor/timeline engine, programme rules,
         content parsing. No Android imports — that is what keeps it testable here.
         Bundled content lives in core/src/main/resources/content/*.json.
app/     Android: Compose UI, DataStore, wiring. Depends on :core.
docs/    PLAN.md (approved plan) · STATUS.md (state) · DECISIONS.md (why)
```

Root `build.gradle.kts` is deliberately empty and plugins are declared per module, so
the root project never resolves the Android Gradle Plugin. Combined with
`org.gradle.configureondemand=true`, that is what lets `:core:test` run here at all.

## Commands

```bash
./gradlew :core:test              # the feedback loop — always run before committing
./gradlew :core:test --tests '*TimelineEngineTest*'
```

`:core` targets Java 17 bytecode but sets no Java toolchain, so it builds on whatever
JDK is present (21 locally, 17 in CI).

## The domain, in one paragraph

Nothing stores a wall-clock time for a block. A `DayBlock` knows only how long it
lasts, its floor (`minMinutes`), and its `Priority`. An `Anchor` — `StartAt`,
`FinishBy`, or `Window` — turns a list of blocks into a `Timeline` of real times.
Change the anchor and every time moves together; there is no second place holding
times that could drift out of sync. When a `Window` is too small, `TimelineEngine`
drops lowest-priority blocks first and then hands leftover minutes back highest-first.
`ESSENTIAL` blocks (warm-up, cool-down, personal care) are never dropped — they are
what protects the knees on a body that has just woken up.

## Conventions

- **User-facing text is Vietnamese.** Code, comments, commits, docs: English.
  (Block titles and content JSON are Vietnamese; moving them into resources is M7.)
- No DI framework, no annotation processing. `AppContainer` in `VhHealthApp.kt` wires
  things by hand.
- No Room yet — DataStore covers settings, and content is read-only. Room arrives with
  session logging at M6.
- Never bundle copyrighted audio or video. The tabata beat is synthesised at runtime;
  demo videos are links or files the user supplies.
- Content changes go in `core/src/main/resources/content/*.json` and are validated by
  `ContentTest`. Add an exercise there, not in Kotlin.

## Health-content rules that are not negotiable

The programme was approved with a specific clinical direction — see `docs/PLAN.md`.
The user's knees are **under-conditioned from sitting**, not injured; they ache only
after higher walking or running volume. That means:

- Load them, don't spare them. Strength work is never cut back when the knee signal
  goes red — only impact volume gives way (`KneeLoadPolicy`).
- Impact returns from week 3 (`ImpactPolicy.PLYOMETRICS_FROM_WEEK`); avoiding it
  forever would leave the tendons unprepared to run.
- Walking and running volume never rises more than 10% a week (`RunVolumeGuard`).

Do not "helpfully" soften these into a rest-and-avoid programme.
