# Status

**Updated:** 2026-09-01 · **Branch:** `claude/android-workout-scheduler-app-e70u8m`
**CI:** green on `1f3c798` (M5 + M6), first try — `Core tests` and `Build APK` both
passed, APK produced (`Assemble debug APK`, 2m55s, 38/38 tasks). **None of it has run
on a real device yet** — that is the next step, and it is a bigger one than usual: see
"Real risk in this round" before assuming a clean compile means it works.

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

`:core` — **94 unit test, tất cả pass.** Chạy `./gradlew :core:test`.

Mới trong phiên này: `ReminderScheduleTest`, `ReminderContentTest` (giờ nhắc đúng kế
hoạch 04:30/09:30/14:00/16:30/19:45, ranh giới nửa đêm, nội dung không rỗng cho mọi
buổi tập), `LoadAdjustmentTest` (chỉ cardio bị co giãn, khối gối/sức mạnh giữ nguyên
byte-identical, sàn không về 0/âm), `StreakTest`, `ProgressRecordsTest` (round-trip
JSON thật cho cả 3 loại bản ghi — đây là cơ chế `ProgressRepository` dựa vào, không
cách nào khác để xác nhận nó trong container này), cộng 2 test mới cho
`KneeLoadPolicy.impactFactorFor`.

**`:app` — compile-verify qua CI, không phải trong container này** (không thể, xem
CLAUDE.md). `Build APK` chạy `1f3c798` xanh: `BUILD SUCCESSFUL in 2m 55s`, 38/38 task,
APK 17.3MB đóng gói và upload thành công. Ba cảnh báo deprecation trong log, không phải
lỗi — hai cái có từ trước (`Icons.Filled.ShowChart`, `Icons.Filled.OpenInNew`), một cái
mới từ vòng này (`LocalLifecycleOwner` import sai gói) đã sửa ngay, gộp vào cùng lần
push này. Trước khi push đã tự rà soát thủ công (đóng ngoặc, tên package, tên hàm/tham
số khớp chữ ký) — lần này việc rà soát khớp với kết quả build thật, nhưng đó là may mắn
có xác nhận, không phải điều nên trông cậy ở vòng sau: build thật trên CI vẫn luôn là
bằng chứng duy nhất đáng tin.

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

**Push + CI đã xong, xanh cả hai workflow** — không cần lặp lại bước đó. Việc còn lại
toàn bộ nằm trên máy thật, không cách nào làm thay được từ container này:

1. **Cài lại và bật "Bật nhắc nhở" trong Cài đặt.** Cần biết: hộp thoại xin quyền
   thông báo có hiện không, công tắc có tự tắt lại khi từ chối không, nút "Cấp quyền
   báo đúng giờ" có dẫn đúng màn hình hệ thống không, và — quan trọng nhất vì đây là lý
   do M5 tồn tại — **thông báo có hiện rõ ràng trên đồng hồ thông minh không**, hay chỉ
   hiện trên điện thoại.
2. **Hoàn thành một buổi tập gối/chạy thật** (ví dụ `w_zone2_knee`), xác nhận màn hình
   chọn tín hiệu gối hiện đúng lúc, chọn xong quay lại đúng, rồi mở tab Tiến trình xem
   chuỗi ngày/tín hiệu gối/buổi tập vừa xong có hiện đúng không.
3. **Thử nhập cân nặng/vòng eo** ở tab Tiến trình, xác nhận lưu và hiện lại đúng.
4. **Chờ qua 04:30/09:30/14:00/16:30/19:45 một lần thật** (hoặc chỉnh giờ máy để test
   nhanh) để biết `AlarmManager` có bắn đúng giờ trên máy cụ thể của người dùng không —
   đây là phần duy nhất không cách nào rút ngắn được.

## Things deliberately left undone

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
