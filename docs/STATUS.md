# Status

**Updated:** 2026-09-01 · **Branch:** `claude/android-workout-scheduler-app-e70u8m`
**CI:** green through `6dba6f2` (dashboard redesign) · APK artifact `vh-health-debug-apk`.
The session-player commit below has not been pushed/built yet — see "Do this next".

Read this first, then `CLAUDE.md` for the background that does not change.

---

## Where the project is

| Mốc | Nội dung | Trạng thái |
|-----|----------|------------|
| M1 | Khung dự án, Gradle, Compose, điều hướng, CI ra APK | ✅ Xong |
| M2 | Tầng dữ liệu + toàn bộ nội dung (63 động tác, 7 buổi tập, chương trình tuần) | ✅ Xong |
| M3 | Minh hoạ động tác | 🔁 Đổi hướng — xem bên dưới |
| M4 | Trình phát buổi tập, nhịp tabata, giọng đếm tiếng Việt | 🟡 Vừa viết xong, **chưa build, chưa chạy thật** |
| M5 | Lịch tuần, kéo thả đổi ngày, báo thức và nhắc nhở | ⬜ Chưa bắt đầu |
| M6 | Tiến trình, biểu đồ, Room, sao lưu JSON | ⬜ Chưa bắt đầu |
| M7 | Hoàn thiện: tiếng Anh, tiếp cận, bản release | ⬜ Chưa bắt đầu |

**M3 đổi hướng, không phải bị bỏ.** Kế hoạch ban đầu là tự vẽ hoạt hình vector khung
xương (đã có prototype Python/Pillow chứng minh động học thuận/ngược đúng, xem lịch sử
hội thoại). Người dùng sau đó đổi ý, muốn dùng ảnh/video 3D thật từ mạng. Việc **tải về
và đóng gói cứng nội dung có bản quyền vào app đã bị từ chối** — xem `DECISIONS.md`
(D-008), đây là ranh giới đã được quyết định, đừng lật lại. Hướng thay thế: liên kết
video ngoài (`Exercise.videoUrl`, đã có sẵn trong model, hiện toàn bộ đang `null`) và
văn bản kỹ thuật (`cues`) đã có sẵn trong nội dung — trình phát M4 dùng cả hai.

## Điều CHƯA xong ở M4 — nói rõ để không ai lầm

M4 vừa được viết trong phiên này (`WorkoutPlayerViewModel`, `WorkoutPlayerScreen`,
`BeatEngine`, `VoiceCues`, `SessionBuilder`) nhưng:

1. **`:app` chưa được compile lại từ khi thêm code này.** CI cho commit này chưa chạy.
   Rất có thể có lỗi kiểu/API tương tự lần trước (`item` vs `items`) — kiểm tra Actions
   trước khi làm bất cứ gì khác.
2. **Chưa ai bấm nút "Bắt đầu" trên máy thật.** Toàn bộ hành vi thời gian thực — đồng
   hồ đếm, chuyển pha, âm thanh, rung — mới chỉ được suy luận đúng trên giấy, không
   phải quan sát.
3. **`videoUrl` toàn bộ đang trống.** Một lượt `WebSearch` để tìm link thật đã đụng giới
   hạn phiên (`session limit`) trước khi tìm được kết quả nào. Nút "Xem video hướng dẫn"
   sẽ không hiện cho tới khi có link — điều này đúng, không phải lỗi.

## Đã kiểm chứng thật (test chạy được ở đây)

`:core` — **55 unit test, tất cả pass.** Chạy `./gradlew :core:test`.

- `TimelineEngineTest`, `SleepLinkTest`, `TimelineClockTest` — lịch neo, co giãn, vị trí
  hiện tại trong ngày (đã dùng để sửa layout theo phản hồi "quá nhiều chữ").
- `ProgramRulesTest` — chu kỳ 4 tuần, trần 10%/tuần, đèn giao thông gối, mở va đập từ
  tuần 3.
- `ContentTest` — nội dung 63 động tác + 7 buổi tập hợp lệ, đúng với chính file sẽ ship.
- `SessionBuilderTest` — bộ máy trình phát: tổng giây khớp `Workout.estimatedSeconds`
  cho mọi buổi tập thật trong `program.json`, REST xem trước đúng bài kế tiếp qua ranh
  giới item/vòng/khối, `cursorAt` đúng tại mọi mốc kể cả biên, `isCountIn` chỉ sáng ở
  3 giây cuối chứ không phải lúc bằng 0.

**`:app` — chưa có gì được compile-verify trong phiên này.** Xem mục "CHƯA xong" ở trên.

## Do this next

1. **Push commit này rồi đọc CI.** `git push`, mở tab Actions, đọc `Build APK`. Sửa lỗi
   Kotlin/Compose nếu có — quy trình y hệt lần trước (push → đọc log → sửa → lặp).
   Nghi vấn cao nhất: chữ ký API Compose Material3/Canvas trong
   `WorkoutPlayerScreen.kt` (chưa từng compile).
2. **Người dùng bấm "Bắt đầu" trên máy thật**, báo lại: đồng hồ có chạy đúng nhịp
   không, âm thanh tabata có phát không (đặc biệt: máy có tắt tiếng hệ thống ảnh hưởng
   gì không — `AudioAttributes.USAGE_ASSISTANCE_SONIFICATION` có thể bị một số máy xử
   lý khác nhau), giọng đọc tiếng Việt của TextToSpeech có cài sẵn trên máy không (một
   số máy Android thiếu gói dữ liệu giọng vi-VN, `VoiceCues` lặng lẽ bỏ qua khi vậy —
   cần biết máy người dùng có rơi vào trường hợp này không).
3. **Tìm link video thật khi phiên WebSearch hết hạn** (reset 04:20 UTC theo thông báo
   lúc bị chặn) — ưu tiên nhóm bài gối trước vì đó là nơi sai kỹ thuật nguy hiểm nhất:
   `kn_spanish_squat`, `kn_step_down`, `kn_tibialis_raise`, cộng `lo_goblet_squat`,
   `co_plank`, `ca_mountain_climber`. Chỉ điền `videoUrl` bằng URL thật lấy được từ kết
   quả tìm kiếm — không bao giờ tự bịa link.
4. **Audio-focus ducking** (D-008) — chưa làm. Khi cue phát, xin
   `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` để nhạc nền của người dùng tự hạ nhỏ rồi trả
   lại, thay vì im lặng hoàn toàn hoặc chồng tiếng.

## Things deliberately left undone

- **No ViewModels on the Library and Settings screens.** They read the repository flow
  directly. Fine at this size; revisit if either grows real state.
- **27 of the 63 exercises are unused by the programme.** That is the substitution pool
  for the "easier/harder" swaps and for later four-week blocks — not dead content.
  `ContentTest` guards that the pool never swallows more than half the library.
- **Vietnamese strings are inline, not in `strings.xml`.** Moving them is M7, when the
  English translation lands. Doing it now would be churn for no gain.
- **No Room.** See `docs/DECISIONS.md` (D-003).
- **Player state does not survive process death.** Killing the app mid-workout loses
  progress; resuming always restarts the session from PREPARE. Acceptable for a first
  cut — revisit if it turns out to matter in practice.
- **No hand-drawn/vector illustration layer.** Deliberately dropped per the user's own
  change of direction (see M3 note above), not an oversight.
