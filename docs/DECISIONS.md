# Decision log

Short entries. Each one records something a future session would otherwise waste time
rediscovering or, worse, quietly undo.

---

## D-001 — The APK is built by CI, not here

**Date:** 2026-09-01 · **Status:** accepted

`dl.google.com` returns 403 through the container's egress proxy, and it hosts the
Android SDK, the Android Gradle Plugin, and every AndroidX artifact. `maven.google.com`
only redirects there. Maven Central, `services.gradle.org` and `plugins.gradle.org` are
reachable.

The user chose to build via GitHub Actions rather than ask an administrator to unblock
the host. `.github/workflows/android-build.yml` produces a debug APK as a run artifact.

**Consequence:** Android code is type-checked only in CI. Push early, read the logs.

## D-002 — Plugins are declared per module, never at the root

**Date:** 2026-09-01 · **Status:** accepted

A conventional root `plugins { alias(libs.plugins.android.application) apply false }`
would make the root project resolve the Android Gradle Plugin, which fails here (D-001)
and takes `:core` down with it.

Instead the root build file is empty, each module declares its own plugins, and
`org.gradle.configureondemand=true` keeps `:core:test` from configuring `:app`. This is
what makes a real test loop possible in the dev container.

**Do not "tidy" the root build file by adding a plugins block.**

The build therefore prints a Kotlin warning that the plugin is loaded in both `:app`
and `:core` with explicit versions, advising the root-plugins arrangement. That advice
is correct in general and wrong here — the warning is accepted, not a defect to fix.

## D-003 — No Room until session logging exists

**Date:** 2026-09-01 · **Status:** accepted

Everything persisted so far is a handful of settings, and the programme content is
read-only. DataStore covers that with no annotation processing, no KSP, and no extra
way for the first CI build to fail. Room arrives with M6, when there are session logs
and body metrics to store.

## D-004 — No dependency-injection framework

**Date:** 2026-09-01 · **Status:** accepted

`AppContainer` in `VhHealthApp.kt` builds the two repositories by hand. The graph is
two objects deep. Hilt would add annotation processing and a build step for no benefit
at this size.

## D-005 — Content lives in `:core` resources, not in `app/assets`

**Date:** 2026-09-01 · **Status:** accepted

Putting `exercises.json` and `program.json` in `core/src/main/resources/content/` means
`ContentTest` validates the exact files that ship. A dangling exercise id fails
`./gradlew :core:test` in seconds instead of crashing the session player mid-workout.
Java resources are packaged into the APK and readable through the class loader on
Android, so the app reads them the same way.

## D-006 — Audio is synthesised, media is linked, never bundled

**Date:** 2026-09-01 · **Status:** accepted

The tabata beat is generated at runtime with `AudioTrack`, so it stays locked to the
timer instead of drifting the way a separate MP3 would, adds nothing to the APK, and
raises no licensing question. Demo videos are links or files the user attaches. No
copyrighted audio or video is ever committed to this repository.

## D-007 — The knee programme loads the knee, it does not spare it

**Date:** 2026-09-01 · **Status:** accepted · **Supersedes an earlier draft**

The first plan read the user's answer as an injury and prescribed avoidance: no
plyometrics for four weeks, restricted squat depth, back off on any pain. The user
clarified: the knees ache only after a lot of walking or running, and the root cause is
sitting all day. That is a capacity problem, and the treatment is close to the opposite.

Encoded in `KneeLoadPolicy`, `ImpactPolicy`, and `RunVolumeGuard`:

- Strength load is never reduced. When the knee signal goes red, impact volume drops
  30% and the weights stay exactly where they are.
- Impact returns from week 3 in small in-place hops, before running volume climbs.
- Walking and running volume rises at most 10% per week, enforced not suggested.
- Full squat depth, no artificial range limit.

A future session must not soften this back into a rest-and-avoid programme.

## D-008 — Media stays linked or self-supplied; the player itself is built

**Date:** 2026-09-01 · **Status:** accepted

The user asked me to source exercise photos/video from the internet and bundle them
into the app, on the grounds that personal use makes copyright a non-issue. That
premise is wrong — downloading commercial stock content (the concrete example was a
paid illustration pack) and compiling it into the APK is copying it, not viewing it
privately, and it contradicts D-006. Declined, and explained why rather than silently
reinterpreting the request.

What shipped instead, all consistent with D-006:

- `Exercise.videoUrl` (already in the content model) is the hook for layer 2 — an
  external link opened via `ACTION_VIEW`, never an embedded/downloaded file. No URLs
  are populated yet: a `WebSearch` pass to curate them hit its session quota before
  it produced results, and a session cannot verify a video's content by watching it,
  only by source reputation — so a future pass must say so explicitly, and must
  never fabricate a URL it hasn't gotten as an actual search result.
- The tabata beat and every player cue are synthesised PCM (`audio/BeatEngine.kt`),
  not a file — D-006 extended to the player rather than relaxed for it.
- No bundled "background music" track. If the user plays their own music in another
  app, that is the intended source for it; a future pass can make cues request
  transient audio focus so a beep politely ducks it rather than fighting it, but
  that is not yet implemented.

Do not revisit the "just download some images, it's personal use" framing — it was
considered and rejected once already; re-litigating it wastes a session.
