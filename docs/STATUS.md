# Status

**Updated:** 2026-09-01 · **Branch:** `claude/android-workout-scheduler-app-e70u8m`
**CI:** green on `2cda6e8` (the warm-up + thumbnail fix below), first try —
`BUILD SUCCESSFUL in 3m 21s`, 38/38 tasks, APK produced (18.0MB, up from 17.3MB —
consistent with Coil's library code being added). Only the one pre-existing, unrelated
`Icons.Filled.ShowChart` deprecation warning remains; the `OpenInNew` one is gone along
with the button that used it. **None of this has run on a real device yet** — see "Do
this next".

Read this first, then `CLAUDE.md` for the background that does not change.

---

## Where the project is

| Mốc | Nội dung | Trạng thái |
|-----|----------|------------|
| M1 | Khung dự án, Gradle, Compose, điều hướng, CI ra APK | ✅ Xong |
| M2 | Tầng dữ liệu + toàn bộ nội dung (63 động tác, 7 buổi tập, chương trình tuần) | ✅ Xong |
| M3 | Minh hoạ động tác | 🔁 Đổi hướng — liên kết video (14/63), không tự vẽ. Xem D-008. |
| M4 | Trình phát buổi tập, nhịp tabata, giọng đếm tiếng Việt | ✅ Xác nhận trên máy thật: nút bấm được, giọng đọc và nhịp trống đều nghe được. |
| M5 | Nhắc nhở qua thông báo (không báo thức) | 🟡 CI xanh (compile + đóng gói thật). **Chưa chạy trên máy thật.** |
| M6 | Tiến trình: chuỗi ngày, tín hiệu gối tác động thật lên buổi tập, cân nặng/vòng eo | 🟡 CI xanh (compile + đóng gói thật). **Chưa chạy trên máy thật.** |
| M7 | Hoàn thiện: tiếng Anh, tiếp cận, bản release | ⬜ Chưa bắt đầu |

## Phản hồi thật #3: thiếu khởi động, video ẩn sau nút bấm

Nguyên văn: *"tôi thấy bài tập luyện của bạn có lỗ hổng lớn về mặt khởi động trước khi
tập. ví dụ hôm nay là thứ 3, ngay khi nhấn bắt đầu thì vào ngay bài tập chạy nhẹ và
chạy nhanh, mà không hề có bài tập khởi động nào???? nếu có bài tập khởi động hoặc bài
tập đòi hỏi động tác, tôi mong đợi là hình ảnh hoặc video hướng dẫn xuất hiện bên dưới
đồng hồ đếm ngược luôn"*

### Lỗi 1 — không hề bịa, kiểm tra ra là thật: 6/7 buổi tập không có khởi động

`DayTemplates.morning()` đặt 12 phút "Khởi động & mobility" trên **lịch dashboard**, ghi
rõ "Không bao giờ bỏ hẳn: khớp còn cứng sau 8 tiếng nằm" — nhưng đó chỉ là một khối
*thời gian*, không tự động biến thành nội dung tập thật. Trình phát chạy trực tiếp
`Workout.blocks` từ `program.json`, và khối đó **không hề nối với nhau**. Kiểm tra thật:

| Buổi tập | Khối đầu tiên trước khi sửa |
|---|---|
| `w_strength_full_a` (T2) | Vòng sức mạnh 1 — vào tạ luôn |
| `w_zone2_knee` (T3) | Chạy – đi bộ xen kẽ — **đúng như người dùng báo cáo** |
| `w_tabata_core` (T4) | Tabata 1 — vào cường độ cao luôn |
| `w_lower_knee` (T5) | Sức mạnh thân dưới — vào tạ luôn |
| `w_intervals_core` (T6) | Có "Khởi động chạy" (đi bộ nhanh 5 phút) nhưng không phải mobility khớp |
| `w_long_easy` (T7) | Chạy – đi bộ dài — mobility có nhưng nằm ở **cuối**, không phải đầu |
| `w_recovery` (CN) | Yoga & mobility — **đây là buổi duy nhất đúng ngay từ đầu** |

**Đã sửa:** thêm 1 khối "Khởi động" dùng chung (8 động tác từ `MuscleGroup.WARM_UP`,
~4 phút, thứ tự đứng → tường → thảm để giảm số lần đổi tư thế) vào đầu 6/7 buổi tập —
`w_recovery` giữ nguyên vì đã đúng. Cập nhật lại trường `minutes` hiển thị cho 5 buổi
tập giờ dài hơn ~3–5 phút thật (không đổi cho `w_long_easy`/`w_recovery`, chênh lệch đã
nằm trong sai số cho phép).

**Chặn tái diễn:** thêm test `ContentTest > every workout opens with a genuine warm-up
movement` — khẳng định động tác đầu tiên của mọi buổi tập phải thuộc nhóm `WARM_UP`.
Nếu sau này thêm buổi tập mới mà quên khởi động, `./gradlew :core:test` báo lỗi ngay,
không phải chờ người dùng phát hiện trên máy thật lần nữa.

### Lỗi 2 (thật ra là thiếu tính năng) — hình minh hoạ giờ nằm ngay dưới đồng hồ đếm ngược

Trước đây: có `videoUrl` thì hiện nút "Xem video hướng dẫn", bấm vào mới rời app sang
trình duyệt. Giờ: `ExerciseThumbnail` hiện ảnh xem trước (thumbnail thật từ YouTube,
tải lúc chạy — không tải về, không đóng gói, vẫn đúng D-006/D-008, xem D-010) ngay dưới
vòng đếm ngược; bấm vào ảnh mới mở video ngoài, đúng như hành vi cũ.

**Nhưng: 63 động tác chỉ có 26 cái có `videoUrl`** (14 từ trước + 12 bài khởi động mới
tìm vòng này). 37 bài — gồm phần lớn nhóm thân dưới/thân trên/core không thuộc nhóm gối
— vẫn chưa có link, nghĩa là với những bài đó màn hình trình phát vẫn chỉ có chữ, không
có ảnh. Không phải lỗi (không có link thì không hiện ảnh, không hiện ảnh vỡ), nhưng đó
là còn thiếu, sẽ nêu lại trong "Việc cố tình chưa làm".

**Đã thêm quyền `INTERNET`** — trước giờ app hoàn toàn offline (mở link video là giao
Intent cho trình duyệt, không phải app tự tải gì). Đây là lần đầu app tự gọi mạng để
tải ảnh thumbnail. Vẫn không có tài khoản, không máy chủ riêng.

**CI xanh (xem "Đã kiểm chứng thật"), chưa có xác nhận nào trên máy thật.**

## M5 + M6, làm trong phiên này — chưa từng chạy, kể cả trên CI

Yêu cầu gốc: "triển khai M5 và M6. về m5 thì không cần báo thức. mọi thông báo, nhắc
nhở đề thực hiện qua Push Notification vì tôi dùng smartwatch nên cần thông báo rõ
ràng, đủ thông tin, dễ đọc nhất." Không có câu hỏi nào được hỏi lại — quyết định phạm
vi dưới đây là của phiên làm việc này, không phải do người dùng chốt riêng từng cái.

### M5 — Nhắc nhở

Năm mốc/ngày, tính ra từ `TimelineEngine`/`DayTemplates` chứ không hard-code
(`core/schedule/ReminderSchedule.kt`), nên nếu thời lượng khối tập đổi thì giờ nhắc tự
đổi theo, không lệch: bắt đầu buổi sáng (giờ dậy), ba lần nghỉ bàn giấy, hạ nhiệt buổi
tối (giờ bắt đầu khối hạ nhiệt buổi tối, tính lùi từ giờ ngủ).

- **Không phải báo thức.** Không `setAlarmClock()`, không chuông riêng, không toàn màn
  hình — `NotificationCompat` bình thường, 3 channel riêng (sáng/nghỉ giữa giờ/tối) để
  người dùng tắt riêng từng loại được từ Cài đặt hệ thống nếu muốn.
  Nội dung soạn riêng cho mặt đồng hồ thông minh: giờ + sự kiện lên trước tiên trong
  tiêu đề, một dòng thông tin — không có dòng thứ ba mà smartwatch không hiện được
  (`core/notify/ReminderContent.kt`).
- **Cơ chế:** `AlarmManager` một lần (exact, `setExactAndAllowWhileIdle`), tự đặt lại
  cho ngày mai ngay khi bắn — Android không còn API "lặp lại chính xác hàng ngày" nữa,
  đây là cách làm đúng hiện tại. Việc đặt lại xảy ra **trước** khi post thông báo, nên
  nếu post lỗi thì lịch ngày mai vẫn không mất (`notify/ReminderReceiver.kt`).
- `notify/BootReceiver.kt` đặt lại toàn bộ 5 lịch sau khi khởi động lại máy — alarm bị
  xoá sạch mỗi lần reboot, không có receiver này thì nhắc nhở âm thầm ngừng hẳn.
- Công tắc "Bật nhắc nhở" trong Cài đặt, xin quyền `POST_NOTIFICATIONS` (API 33+, hỏi
  trong app) và báo riêng khi thiếu quyền báo đúng giờ (API 31+, phải qua màn hình Cài
  đặt hệ thống — Android không có hộp thoại trong app cho quyền này).

### M6 — Tiến trình

- `ProgressRepository` (DataStore + JSON, không phải Room — xem D-009 sửa lại D-003).
  Ba loại bản ghi (`SessionLog`, `KneeCheckIn`, `BodyMetric`) khai báo trong `:core`
  (`core/progress/ProgressRecords.kt`) để có test round-trip JSON thật
  (`ProgressRecordsTest`), `:app` chỉ thêm phần lưu trữ DataStore xung quanh.
- **Tín hiệu gối giờ đã tác động thật lên buổi tập sau, không chỉ hiển thị số.** Đây
  là lỗ hổng ưu tiên cao nhất mà báo cáo audit trước đó nêu ra.
  `LoadAdjustment.applyCardioLoadFactor` co giãn riêng phần cardio của bài tập theo hệ
  số từ `KneeLoadPolicy.decide` (dựa trên tín hiệu gối gần nhất) — phần sức mạnh/gối
  giữ nguyên, đúng D-007. `WorkoutPlayerViewModel` áp dụng hệ số này **trước khi** dựng
  các bước buổi tập, nên bước CHUẨN BỊ đầu tiên đã đúng luôn, không nhảy số giữa chừng.
- Sau khi hoàn thành một buổi tập `tracksKneeSignal = true` (3 buổi đi bộ/chạy trong
  tuần), màn hình Hoàn thành hiện 3 nút chọn tín hiệu gối thay cho nút "Xong" — chọn
  xong là ghi nhận và thoát luôn, không có bước xác nhận thừa.
- Mỗi buổi tập hoàn thành tự ghi log (`WorkoutPlayerViewModel`, tại đúng thời điểm
  chuyển sang `isFinished`), có chống ghi trùng nếu màn hình bị vào lại.
- `ProgressScreen` mới: thẻ chuỗi ngày (`currentStreak` — "hôm nay chưa tập" không làm
  chuỗi thật của hôm qua về 0, xem `StreakTest`), thẻ tín hiệu gối + banner gợi ý đi
  khám khi 2 lần liên tiếp gần nhất đều nặng, form nhập cân nặng/vòng eo + danh sách 5
  lần gần nhất, danh sách buổi tập gần đây.

## Đã kiểm chứng thật (test chạy được ở đây)

`:core` — **102 unit test, tất cả pass.** Chạy `./gradlew :core:test`.

Từ vòng M5+M6 (đã CI-verify, xem dưới): `ReminderScheduleTest`, `ReminderContentTest`,
`LoadAdjustmentTest`, `StreakTest`, `ProgressRecordsTest`, cộng test cho
`KneeLoadPolicy.impactFactorFor`.

Mới trong vòng sửa lỗi khởi động (chưa CI-verify): test mới `every workout opens with
a genuine warm-up movement` trong `ContentTest` (khẳng định lỗi vừa sửa không tái
diễn), `each workout actually lasts roughly what it claims` vẫn pass sau khi cập nhật
`minutes`, `VideoThumbnailTest` (7 test — trích id từ 3 dạng URL YouTube, dừng đúng
trước query param thừa, URL không phải YouTube thì không ra thumbnail, và **mọi
`videoUrl` thật trong nội dung đã đóng gói đều ra được thumbnail** — chạy trên đúng 26
link thật, không phải dữ liệu giả).

**`:app` — compile-verify qua CI, không phải trong container này** (không thể, xem
CLAUDE.md). `Build APK` cho `620c0e0` (M5+M6) xanh. `Build APK` cho `2cda6e8` (khởi
động + thumbnail, đợt sửa vừa rồi) **cũng xanh, sạch từ lần đầu**: `Coil` (dependency
mới), quyền `INTERNET` (mới khai báo), `verticalScroll` lồng trong layout từng có bug
thật một lần (`fillMaxSize()`-trong-`Column`, dùng cách khác hẳn lần này —
`weight` + `verticalScroll`) — tất cả compile sạch, không cảnh báo mới. **Compile sạch
không chứng minh chạy đúng** — xem "Do this next".

## Real risk in this round

Lớn hơn hẳn các đợt trước — lần đầu tiên codebase này dùng: `AlarmManager`,
`BroadcastReceiver` (2 cái), `NotificationManager`/`NotificationCompat`,
`ActivityResultContracts.RequestPermission` (Compose permission launcher),
`LifecycleEventObserver` thủ công. Không cái nào có thể chạy thử trong container này —
kể cả compile. Rủi ro compile lỗi ở CI vòng đầu là thật, không phải chỉ hành vi runtime.

Ngoài compile, hành vi lúc chạy (không kiểm chứng được ở đây, chỉ suy luận đúng API):
Doze/tối ưu pin theo hãng có thể trễ hoặc bỏ alarm dù đã xin quyền exact; luồng xin
quyền `POST_NOTIFICATIONS` chỉ chạy thật trên máy Android 13+; thông báo có thực sự
hiện rõ ràng trên đồng hồ cụ thể của người dùng hay không phụ thuộc hãng đồng hồ và
app companion, ngoài tầm kiểm soát của app này.

## Do this next

1. **Push vòng sửa lỗi khởi động + thumbnail, đọc CI trước tiên.** Rủi ro compile thật
   (Coil lần đầu, `INTERNET` permission, layout đổi cấu trúc) — đừng giả định sạch.
2. **Mở lại đúng buổi thứ Ba (`w_zone2_knee`) trên máy thật**, xác nhận: khối "Khởi
   động" hiện ra đầu tiên với 8 động tác đúng thứ tự, ảnh thumbnail hiện dưới vòng đếm
   ngược cho các bài khởi động có link (không phải chữ suông), bấm vào ảnh mở đúng
   video, và cuộn được nếu nội dung dài hơn màn hình.
3. **Thử một buổi không có khởi động-video** (vd. giữa buổi tập gối cũ chưa có link) để
   xác nhận: không có ảnh thì không vỡ layout, không có khoảng trống kỳ lạ.
4. Sau đó mới quay lại các mục chưa xác nhận từ vòng M5+M6 trước — **chưa ai kiểm tra**:
   - Bật "Bật nhắc nhở": hộp thoại xin quyền, công tắc tự tắt khi từ chối, nút "Cấp
     quyền báo đúng giờ", và quan trọng nhất — **thông báo có hiện trên đồng hồ thông
     minh không**.
   - Màn hình chọn tín hiệu gối sau buổi `tracksKneeSignal`, tab Tiến trình (chuỗi
     ngày, banner gợi ý khám, nhập cân nặng/vòng eo).
   - Chờ qua giờ thật (hoặc chỉnh giờ máy) để biết `AlarmManager` có bắn đúng giờ trên
     máy cụ thể của người dùng không.

## Things deliberately left undone

- **37 of 63 exercises still have no `videoUrl`** — mostly lower/upper body and core
  work outside the knee-focus set. Their player screens show no thumbnail, by design
  (no link, no image) not by bug. Extending coverage is real remaining work, same
  caveat as always: chosen by title/channel only, never watched, never fabricated.
- **The shared warm-up block is identical for every workout**, not tailored per day
  (e.g. more ankle work before a run day, more shoulder before an upper-body day).
  Consistency over customization was the deliberate call this round — the person
  learns one routine instead of seven. Worth revisiting once the current fix is
  confirmed on a real device, not before.
- **`WorkoutItem.perSide` is still not read anywhere** (not new to this round — it was
  already unused dead data on 15 existing items before this fix). A `perSide` item
  produces exactly one WORK step of `workSeconds`, not two; the new warm-up block's
  `wu_leg_swing`/`wu_ankle_rock` follow that same existing convention rather than
  trying to fix it — a real gap, but pre-existing and out of scope for this round.
- **JSON backup/export.** Nêu trong kế hoạch M6 gốc, không làm vòng này — dữ liệu vẫn
  chỉ nằm trên máy, mất app là mất hết. Ưu tiên thấp hơn việc đóng lỗ hổng tín hiệu gối
  không tác động gì, nhưng vẫn là nợ thật, chưa trả.
- **Thông báo buổi sáng chưa có deep-link thẳng vào trình phát.** Chạm vào mở
  `MainActivity` như bình thường, người dùng tự bấm "Bắt đầu" ở tab Hôm nay — thiếu một
  bước, không phải lỗi.
- **"Sáng nay chỉ có N phút" (todayStartOverride) không kéo giờ nhắc nhở theo.** Giờ
  nhắc buổi sáng tính từ giờ dậy đã lưu, không phải override một-lần-cho-hôm-nay. Chỉnh
  giờ dậy/giờ ngủ *trong màn hình Cài đặt* thì có tự đặt lại lịch nhắc (nếu đang bật);
  chỉnh nhanh ở tab Hôm nay thì không. Quyết định phạm vi có chủ đích, không phải sót.
- **Không có biểu đồ cân nặng/vòng eo** — chỉ danh sách 5 lần gần nhất. Đủ để thấy xu
  hướng thô; biểu đồ thật sẽ cần thêm thư viện, chưa đáng đánh đổi ở quy mô dữ liệu này.
- **Form nhập cân nặng/vòng eo không tự điền lại giá trị đã lưu hôm nay.** Lưu lần hai
  trong cùng ngày vẫn đúng (đè lên bản ghi cũ, xem `ProgressRepository.logBodyMetric`),
  chỉ là ô nhập không tự hiện số đã lưu để sửa — bất tiện nhỏ, không phải lỗi dữ liệu.
- **No ViewModels on the Library screen.** It reads the repository flow directly. Fine
  at this size. Settings and Progress now have real derived state (permission flags,
  streak/clinician computation) and do use one each.
- **27 of the 63 exercises are unused by the programme.** Substitution pool for
  "easier/harder" swaps and later four-week blocks — not dead content.
- **Vietnamese strings are inline, not in `strings.xml`.** Moving them is M7.
- **No Room.** See D-009 (amends D-003).
- **Player state does not survive process death.** Killing the app mid-workout loses
  progress; resuming restarts from PREPARE.
- **No hand-drawn/vector illustration layer.** Dropped per D-008, not an oversight.
- **Schedule is read-only.** No drag-and-drop, no marking a day off, no starting a
  workout from that screen.
