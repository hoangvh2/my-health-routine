# Status

**Updated:** 2026-09-01 · **Branch:** `claude/android-workout-scheduler-app-e70u8m`
**CI:** both workflows green on `dfa1480` · APK artifact `vh-health-debug-apk` (17 MB)

Read this first, then `CLAUDE.md` for the background that does not change.

---

## Where the project is

| Mốc | Nội dung | Trạng thái |
|-----|----------|------------|
| M1 | Khung dự án, Gradle, Compose, điều hướng, CI ra APK | ✅ Xong |
| M2 | Tầng dữ liệu + toàn bộ nội dung (63 động tác, 7 buổi tập, chương trình tuần) | ✅ Xong |
| M3 | Bộ hoạt hình động tác + trang chi tiết đầy đủ | ⬜ Chưa bắt đầu |
| M4 | Trình phát buổi tập, nhịp tabata, giọng đếm tiếng Việt | ⬜ Chưa bắt đầu |
| M5 | Lịch tuần, kéo thả đổi ngày, báo thức và nhắc nhở | ⬜ Chưa bắt đầu |
| M6 | Tiến trình, biểu đồ, Room, sao lưu JSON | ⬜ Chưa bắt đầu |
| M7 | Hoàn thiện: tiếng Anh, tiếp cận, bản release | ⬜ Chưa bắt đầu |

**The APK exists and is installable.** The first CI type-check of `:app` reported three
errors, all the same mistake (`import androidx.compose.foundation.lazy.item` — `item` is
a `LazyListScope` member, not a top-level function). Fixed in `dfa1480`, and the next run
produced the APK.

What is **not** verified is runtime behaviour: nobody has installed and opened the app
yet. Layout and Vietnamese text wrapping have never been seen on a real screen. Ask the
user what it looks like before building on top of it.

## What actually works today

- **`:core` — 30 unit tests, all passing.** Run `./gradlew :core:test`.
  - The timeline anchored at 04:30 lays out exactly as `docs/PLAN.md` promises
    (04:30 · 04:40 · 04:52 · 05:25 · 05:35 · 05:55 → 06:15).
  - Moving the anchor to 05:00 shifts every block by exactly 30 minutes.
  - `FinishBy` works backwards; `Window` compresses; warm-up and cool-down are never
    dropped; overflow is reported rather than hidden.
  - Programme rules: 4-week phases, the 10%/week impact cap, the knee traffic light,
    plyometrics gated to week 3+.
  - The bundled content is validated against the real JSON files.
- **`:app`** — Today screen (live timeline + anchor controls + "chỉ có N phút"),
  exercise library with filters and coaching text, settings for both anchors. Schedule
  and Progress are honest placeholders that name their milestone.

## Do this next

1. **Get the user's eyes on the running app.** They install the APK from the latest
   `Build APK` run and say what the Today screen actually looks like on their phone.
   This is the only outstanding thing blocking M3.
2. **M3 — the exercise animator.** A Compose `Canvas` that draws a jointed figure from
   keyframes. Design sketch: a `Pose` is a map of joint → angle; an `Animation` is a
   list of `Pose` plus timings; the drawing code interpolates between them. Each
   exercise's `animation` field already names its keyframe set (it currently equals the
   exercise id). Start with five: `lo_goblet_squat`, `kn_spanish_squat`,
   `kn_step_down`, `co_plank`, `ca_mountain_climber`.
3. **M4 — session player and audio.** `AudioTrack` PCM synthesis for the tabata beat,
   `TextToSpeech` for the Vietnamese cues. The workout structure it plays is already
   in `core/src/main/resources/content/program.json`.

## Open questions for the user

- Which knee-focused exercises they can actually do with the dumbbells they own — the
  onboarding screen that asks for available weights is not built yet (part of M5).
- Whether to publish demo video links (layer 2 of the media plan) or wait until they
  can record their own (layer 3).

## Things deliberately left undone

- **No ViewModels on the Library and Settings screens.** They read the repository flow
  directly. Fine at this size; revisit if either grows real state.
- **27 of the 63 exercises are unused by the programme.** That is the substitution pool
  for the "easier/harder" swaps and for later four-week blocks — not dead content.
  `ContentTest` guards that the pool never swallows more than half the library.
- **Vietnamese strings are inline, not in `strings.xml`.** Moving them is M7, when the
  English translation lands. Doing it now would be churn for no gain.
- **No Room.** See `docs/DECISIONS.md` (D-003).
