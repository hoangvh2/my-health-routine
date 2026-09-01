# Status

**Updated:** 2026-09-01 · **Branch:** `claude/android-workout-scheduler-app-e70u8m`
**CI:** clean through the previous push (layout fix). This round's commit (audio-route
fix, ducking, video links, Schedule screen) not yet built — see "Do this next".

Read this first, then `CLAUDE.md` for the background that does not change.

---

## Where the project is

| Mốc | Nội dung | Trạng thái |
|-----|----------|------------|
| M1 | Khung dự án, Gradle, Compose, điều hướng, CI ra APK | ✅ Xong |
| M2 | Tầng dữ liệu + toàn bộ nội dung (63 động tác, 7 buổi tập, chương trình tuần) | ✅ Xong |
| M3 | Minh hoạ động tác | 🔁 Đổi hướng — liên kết video, không tự vẽ. Xem D-008. |
| M4 | Trình phát buổi tập, nhịp tabata, giọng đếm tiếng Việt | 🟡 Chạy được trên máy thật (nút bấm được, giọng đọc nghe được), nhịp trống vừa vá — **chưa xác nhận lại** |
| M5 | Lịch tuần, kéo thả đổi ngày, báo thức và nhắc nhở | 🟡 Xem tuần read-only vừa xong; kéo thả + báo thức chưa làm |
| M6 | Tiến trình, biểu đồ, Room, sao lưu JSON | ⬜ Chưa bắt đầu |
| M7 | Hoàn thiện: tiếng Anh, tiếp cận, bản release | ⬜ Chưa bắt đầu |

## Vòng phản hồi thật đã chạy hai lần trên M4 — đây là bằng chứng quy trình push→CI→cài→báo lỗi→vá hoạt động

1. Build đầu tiên: xanh trên CI nhưng **kẹt cứng ở màn hình CHUẨN BỊ** — nút Play bị đẩy
   ra ngoài màn hình do `fillMaxSize()` lồng trong `Column` (đã vá, xem lịch sử commit).
2. Build thứ hai (đã vá lỗi trên): nút bấm được, đồng hồ chạy, **giọng đọc nghe được
   nhưng nhịp trống tabata thì im lặng.**

### Lỗi vừa vá: nhịp trống dùng sai luồng âm thanh Android

`BeatEngine` dùng `AudioAttributes.USAGE_ASSISTANCE_SONIFICATION` — luồng này trên phần
lớn máy Android đi theo **âm lượng chuông/thông báo**, và bị tắt tiếng ở chế độ rung/im
lặng. `TextToSpeech` mặc định phát qua `STREAM_MUSIC` (âm lượng media) — luồng khác hẳn,
không bị ảnh hưởng bởi chế độ rung. Ảnh chụp máy người dùng có icon rung ở thanh trạng
thái, khớp hoàn toàn với giả thuyết này.

**Đã sửa:** đổi sang `USAGE_MEDIA` + `CONTENT_TYPE_MUSIC` — cùng luồng với giọng đọc.
Đồng thời thêm `PlaybackFocus` (audio-focus ducking, D-08 mục còn treo): xin
`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` **một lần cho cả buổi tập** khi mở màn hình trình
phát, không xin theo từng tiếng bíp — xin theo từng tiếng sẽ làm nhạc nền của người dùng
giật lên xuống liên tục mỗi giây trong 3 giây đếm ngược, khó chịu hơn là không ducking.

**Chưa được xác nhận chạy đúng trên máy thật** — đây là việc ưu tiên nhất, xem "Do this
next".

## Video hướng dẫn — đã có 6 link, KHÔNG PHẢI đã xem qua

Phiên `WebSearch` trước bị chặn (`session limit`); phiên này (05:55 UTC, sau mốc reset
04:20 UTC) đã tìm và gắn `videoUrl` cho: `kn_spanish_squat`, `kn_step_down`,
`kn_tibialis_raise`, `lo_goblet_squat`, `co_plank`, `ca_mountain_climber`. Chọn theo
**tên tiêu đề và tên kênh** (ưu tiên kênh vật lý trị liệu/rehab có tên riêng cho nhóm bài
gối) — **không hề xem nội dung video**, vì không có khả năng phát video. Người dùng nên
tự mở thử vài link để xác nhận trước khi tin tưởng hoàn toàn. 57 động tác còn lại vẫn
chưa có link — nút "Xem video hướng dẫn" ẩn với những bài đó, đúng như thiết kế.

## Lịch tuần (M5, phần đầu) — mới, chưa build-verify

`ScheduleScreen` thay thế placeholder: hiện đủ 7 ngày trong `program.json`, buổi tập mỗi
ngày, chip 4 khối (Làm quen/Tăng tải/Đỉnh khối/Giảm tải) tô sáng khối hiện tại, đánh dấu
"Hôm nay". Read-only — **chưa có** kéo thả đổi ngày, chưa có đánh dấu ngày nghỉ, chưa có
nút bắt đầu buổi tập từ đây (vẫn phải qua tab Hôm nay). Đây là quyết định phạm vi có chủ
đích, không phải thiếu sót — xem "Việc cố tình chưa làm".

Logic "đang ở tuần thứ mấy" được rút ra khỏi `TodayViewModel` (trước đó tính riêng lẻ,
không test được) thành `Progression.weekNumber(startEpochDay, todayEpochDay)` trong
`:core`, có test riêng (biên 6/7/27/28 ngày, và trường hợp ngày bắt đầu ở tương lai).
Today và Schedule giờ dùng chung một hàm — không thể lệch nhau về "tuần mấy" nữa.

## Đã kiểm chứng thật (test chạy được ở đây)

`:core` — **59 unit test, tất cả pass.** Chạy `./gradlew :core:test`.

- `TimelineEngineTest`, `SleepLinkTest`, `TimelineClockTest` — lịch neo, co giãn, vị trí
  hiện tại trong ngày.
- `ProgramRulesTest` — chu kỳ 4 tuần (**+ `weekNumber` mới**), trần 10%/tuần, đèn giao
  thông gối, mở va đập từ tuần 3.
- `ContentTest` — nội dung 63 động tác + 7 buổi tập hợp lệ, đúng với chính file sẽ ship.
- `SessionBuilderTest` — bộ máy trình phát: tổng giây khớp `Workout.estimatedSeconds`,
  REST xem trước đúng bài kế tiếp, `cursorAt` đúng tại mọi mốc, `isCountIn` chỉ sáng ở
  3 giây cuối.

**`:app` — chưa compile-verify trong phiên này.** Cả BeatEngine, PlaybackFocus và
ScheduleScreen đều mới, CI cho commit này chưa chạy khi ghi dòng này.

## Do this next

1. **Push, đọc CI, rồi người dùng cài lại và thử buổi tập thật.** Cần biết: nhịp trống
   tabata có nghe được không (đặc biệt lúc máy đang ở chế độ rung — đúng kịch bản gây ra
   bug lần trước); nếu đang mở nhạc nền (Spotify/YouTube Music), nhạc có tự hạ nhỏ khi
   có tiếng bíp rồi trả lại không (audio-focus ducking mới thêm); tab Lịch tuần hiện
   đúng 7 ngày và đúng khối hiện tại không.
2. **Mở thử vài link trong 6 video đã gắn**, xác nhận chúng thật sự đúng động tác và
   chất lượng chấp nhận được — chưa ai xem qua nội dung, chỉ chọn theo tên/kênh.
3. **M5 phần còn lại:** báo thức 04:35/19:45 (`AlarmManager`), ba lần nhắc nghỉ bàn giấy
   trong ngày (`WorkManager`), kéo thả đổi ngày trên Lịch tuần, đánh dấu ngày nghỉ. Đây
   là mảng rủi ro cao hơn hẳn mọi thứ đã làm — hành vi nền/báo thức khác nhau rất nhiều
   giữa các hãng Android (Doze, tối ưu pin theo OEM) và **không thể kiểm chứng gì từ
   trong môi trường này**, chỉ suy luận đúng API là chưa đủ. Đừng làm dồn nhiều thứ
   cùng lúc — làm từng mảnh nhỏ, để người dùng xác nhận từng bước trên máy thật.
4. **Tìm nốt link video** cho các bài gối còn lại (`kn_calf_raise_seated`,
   `kn_calf_raise_standing`, `kn_monster_walk`, `kn_clamshell`, `kn_side_abduction`,
   `kn_ham_slide`, `kn_copenhagen_short`, `kn_tke`) khi có phiên WebSearch mới — vẫn theo
   nguyên tắc chỉ dùng URL thật lấy từ kết quả tìm kiếm.

## Things deliberately left undone

- **No ViewModels on the Library and Settings screens.** They read the repository flow
  directly. Fine at this size; revisit if either grows real state. Schedule follows the
  same convention.
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
  change of direction (see D-008), not an oversight.
- **Schedule is read-only.** No drag-and-drop, no marking a day off, no starting a
  workout from this screen. Deliberately scoped down to the safe, testable slice this
  round; see "Do this next" #3 for why the rest is deferred rather than rushed.
