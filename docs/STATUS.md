# Status

**Updated:** 2026-09-01 · **Branch:** `claude/android-workout-scheduler-app-e70u8m`
**CI:** compiled clean on `1864d63`, but real-device testing found the player was
completely stuck (layout bug, see below). Fix pushed; not yet re-verified on device.

Read this first, then `CLAUDE.md` for the background that does not change.

---

## Where the project is

| Mốc | Nội dung | Trạng thái |
|-----|----------|------------|
| M1 | Khung dự án, Gradle, Compose, điều hướng, CI ra APK | ✅ Xong |
| M2 | Tầng dữ liệu + toàn bộ nội dung (63 động tác, 7 buổi tập, chương trình tuần) | ✅ Xong |
| M3 | Minh hoạ động tác | 🔁 Đổi hướng — xem bên dưới |
| M4 | Trình phát buổi tập, nhịp tabata, giọng đếm tiếng Việt | 🟡 Build xanh, **chưa ai chạy thật trên máy** |
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

M4 được viết trong phiên trước và CI xanh trên lần build đầu tiên — nghĩa là API đúng
chữ ký, nhưng **build xanh không có nghĩa là chạy đúng**: người dùng cài thử và bị kẹt
cứng ngay màn hình CHUẨN BỊ, không tiếng, không phản hồi. Xem "Lỗi đã sửa" bên dưới —
đã tìm ra và vá, nhưng **bản vá này chưa được người dùng xác nhận chạy đúng**.

1. **Người dùng cần cài lại và thử lại từ đầu.** Bấm Bắt đầu, xem có chạm được hàng nút
   Play/Tạm dừng/Chuyển bài ở đáy màn hình không, đồng hồ có đếm không, có tiếng không.
2. **`videoUrl` toàn bộ đang trống.** Một lượt `WebSearch` để tìm link thật đã đụng giới
   hạn phiên (`session limit`, reset 04:20 UTC) trước khi tìm được kết quả nào. Nút
   "Xem video hướng dẫn" sẽ không hiện cho tới khi có link — điều này đúng, không phải
   lỗi.

### Lỗi đã sửa: `fillMaxSize()` lồng trong `Column` đẩy nút Play ra khỏi màn hình

Ảnh chụp máy thật cho thấy màn hình CHUẨN BỊ đứng yên, không nút, không tiếng.
`WorkoutPlayerScreen` có cấu trúc `Column { TopBar(); Column(fillMaxSize()){...}; Controls() }`
— trong Compose, `fillMaxSize()` trên một phần tử **giữa** của `Column` chiếm đúng bằng
chiều cao của `Column` cha (bằng cả màn hình), *không phải* "phần còn lại sau top bar".
Vì vậy tổng chiều cao ba phần tử vượt màn hình, và `Controls()` (chứa nút Play) bị đẩy
lọt hẳn ra ngoài đáy — tồn tại trong cây layout nhưng không ai chạm tới được. Nút Play
không bao giờ được bấm → `start()` không bao giờ chạy → đồng hồ đứng yên, không cue nào
phát ra. Một lỗi bố cục duy nhất giải thích trọn cả hai triệu chứng.

**Đã sửa** bằng `Modifier.weight(1f)` thay cho `fillMaxSize()` trên Column giữa — đây
mới là cách đúng để nói "chiếm phần còn lại sau các anh chị em không có weight". Đã có
comment tại chỗ giải thích, đừng "dọn dẹp" nó về `fillMaxSize()` vì trông có vẻ tương
đương.

**Bài học chung cho những màn hình sau:** bất cứ `Column`/`Row` nào có cấu trúc
header-cố-định + nội-dung-lấp-đầy + footer-cố-định đều phải dùng `weight(1f)` cho phần
lấp đầy, không bao giờ `fillMaxSize()`. Đây là lỗi không thể bắt được bằng compile hay
test tĩnh trong môi trường này — chỉ lộ ra khi chạy thật trên máy, đúng như giới hạn đã
ghi ở `CLAUDE.md`.

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

1. **Người dùng cài APK mới, bấm "Bắt đầu", báo lại kết quả.** Cụ thể cần biết: đồng
   hồ có chạy đúng nhịp và đúng giây không; âm thanh tabata có phát không (một số máy
   OEM xử lý `AudioAttributes.USAGE_ASSISTANCE_SONIFICATION` khác nhau khi máy đang ở
   chế độ im lặng/rung); giọng đọc tiếng Việt của TextToSpeech có kêu không (nhiều máy
   Android thiếu sẵn gói dữ liệu giọng vi-VN — `VoiceCues` lặng lẽ bỏ qua khi vậy, cần
   biết máy này có rơi vào trường hợp đó); vòng đếm ngược, rung, và nút chuyển bài có
   phản hồi đúng không. Đây là việc chặn mọi thứ khác.
2. **Tìm link video thật khi phiên WebSearch hết hạn** (reset 04:20 UTC theo thông báo
   lúc bị chặn) — ưu tiên nhóm bài gối trước vì đó là nơi sai kỹ thuật nguy hiểm nhất:
   `kn_spanish_squat`, `kn_step_down`, `kn_tibialis_raise`, cộng `lo_goblet_squat`,
   `co_plank`, `ca_mountain_climber`. Chỉ điền `videoUrl` bằng URL thật lấy được từ kết
   quả tìm kiếm — không bao giờ tự bịa link.
3. **Audio-focus ducking** (D-008) — chưa làm. Khi cue phát, xin
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
