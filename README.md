# VH. Health

Ứng dụng Android lập lịch tập luyện cho người dậy 04:30 và đi làm giờ hành chính. Ba
đích: giảm cân lành mạnh, cơ thể săn chắc, và đôi gối đủ khoẻ để chạy bộ.

Chạy hoàn toàn offline. Không tài khoản, không máy chủ, không quảng cáo, không thu thập
dữ liệu.

## Lấy file APK

Vào tab **Actions** của repo, mở lần chạy **Build APK** mới nhất, tải
`vh-health-debug-apk` ở mục Artifacts, giải nén rồi cài file `.apk` vào điện thoại
(cần bật cho phép cài từ nguồn không xác định).

APK được build trên máy chủ GitHub vì môi trường phát triển không truy cập được
`dl.google.com`. Chi tiết ở `docs/DECISIONS.md`.

## Mở bằng Android Studio

```bash
git clone <repo> && cd my-health-routine
# Mở thư mục bằng Android Studio, bấm Sync rồi Run.
```

## Chạy test phần lõi

Không cần Android SDK:

```bash
./gradlew :core:test
```

Lệnh này kiểm tra cả engine lịch trình lẫn tính nhất quán của nội dung bài tập.

## Cấu trúc

| Thư mục | Nội dung |
|---|---|
| `core/` | Kotlin thuần: mô hình, engine điểm neo, quy tắc giáo án, nội dung JSON |
| `app/` | Android: giao diện Compose, DataStore |
| `docs/` | `PLAN.md` kế hoạch đã duyệt · `STATUS.md` tiến độ · `DECISIONS.md` quyết định |

## Lưu ý sức khoẻ

Đây là giáo án tập luyện chung, không phải tư vấn y tế và không thay thế bác sĩ. Nếu bạn
có bệnh tim mạch, huyết áp, từng chấn thương gối hay cột sống, hoặc thấy đau nhói, chóng
mặt, khó thở khi tập — dừng lại và đi khám trước khi theo lịch này.
