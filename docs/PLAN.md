# Kế hoạch đã duyệt — VH. Health

Bản kế hoạch người dùng đã duyệt ngày 01.09.2026. Bản web tương ứng:
<https://claude.ai/code/artifact/c0f5b27a-18b4-4425-9aa0-bc80b910fc8a>

Đây là nguồn sự thật cho nội dung sản phẩm. Muốn đổi giáo án hay cấu trúc lịch thì sửa
ở đây trước, rồi mới sửa mã.

---

## Người dùng

Nhân viên văn phòng, ngủ 20:30–04:30 mỗi ngày. Có thảm, dây kháng lực và tạ đơn.

**Gối:** không chấn thương. Yếu đi vì ngồi nhiều, chỉ nhức nhẹ sau khi đi bộ hoặc chạy
nhiều. Đây là vấn đề sức chịu tải, và cách chữa là tăng tải có kiểm soát chứ không phải
kiêng cữ. Xem `DECISIONS.md` (D-007) — đừng đảo ngược hướng này.

## Ba mục tiêu

| Mục tiêu | Cách app hỗ trợ | Chỉ số |
|---|---|---|
| Giảm cân lành mạnh | 6 buổi/tuần trộn sức mạnh và cardio để giữ cơ khi giảm mỡ | −0,3 → 0,7 kg/tuần |
| Săn chắc, ít mỡ bụng | Sức mạnh toàn thân + core 3 buổi + 1 buổi tabata | Vòng eo đo hàng tuần |
| Gối khoẻ để chạy | 3 buổi/tuần có phần gối, tạ đơn tăng dần, trần 10%/tuần | Mức tạ, km/tuần, tín hiệu nhức |

Không có giảm mỡ cục bộ. Mỡ bụng giảm khi tổng mỡ giảm; các bài core làm bụng săn và
khoẻ, phần giảm mỡ đến từ thâm hụt calo, đạm đủ và ngủ đủ.

## Lịch ngày thường (T2–T6)

105 phút từ lúc mở mắt tới lúc sẵn sàng đi làm.

| Giờ | Khối | Thời lượng | Sàn tối thiểu | Ưu tiên |
|---|---|---|---|---|
| 04:30 | Dậy, vệ sinh cá nhân, uống 400 ml nước | 10′ | 5′ | ESSENTIAL |
| 04:40 | Khởi động & mobility | 12′ | 6′ | ESSENTIAL |
| 04:52 | Buổi tập chính | 33′ | 12′ | HIGH |
| 05:25 | Giãn cơ tĩnh & thở | 10′ | 5′ | ESSENTIAL |
| 05:35 | Tắm & vệ sinh sau tập | 20′ | 10′ | HIGH |
| 05:55 | Bữa sáng nhiều đạm | 20′ | 10′ | NORMAL |
| 06:15 | Sẵn sàng đi làm | — | | |

Trong ngày: 3 lần nghỉ bàn giấy 3 phút lúc 09:30 / 14:00 / 16:30, có nhiệm vụ riêng cho
gối (duỗi gối siết tứ đầu, mở hông, nhón bắp chân).

Buổi tối, neo ngược từ giờ ngủ: 19:45 hạ nhiệt 15′ · 20:00 đệm 15′ · 20:15 chuẩn bị ngủ
15′ · 20:30 ngủ.

## Lịch co giãn

Không mốc giờ nào ghi cứng. Mỗi khối chỉ mang thời lượng; điểm neo sinh ra giờ cụ thể.

- **Neo theo giờ bắt đầu** — mặc định. Đổi 04:30 → 05:00 thì cả chuỗi dịch 30 phút.
- **Neo theo giờ phải xong** — nhập "6h15 phải xong", app tính ngược ra giờ báo thức.
- **Neo theo khung giờ có hạn** — nhập "sáng nay chỉ có 40 phút", app tự co buổi tập.

Khi co: bỏ theo thứ tự ưu tiên thấp trước (bỏ khối dài nhất trong mỗi mức trước), rồi
trả lại phút thừa theo ưu tiên cao trước. Khởi động và giãn cơ không bao giờ bị bỏ hẳn.
Nút "chỉ có 10 phút" là trường hợp cực đoan của cùng cơ chế này, không phải buổi tập
viết riêng.

Buổi tối neo riêng vào giờ ngủ. Mặc định giờ ngủ = giờ dậy − 8 tiếng; người dùng gỡ
liên kết được, khi đó app chỉ báo con số thật chứ không tự dời.

## Lịch tuần

| Ngày | Buổi chính | Thời lượng | RPE |
|---|---|---|---|
| T2 | Sức mạnh toàn thân A | 30′ | 6–7 |
| T3 | Chạy–đi bộ Zone 2 + 6′ chăm sóc gối | 32′ | 4–5 |
| T4 | Tabata va đập thấp + Core | 28′ | 8–9 |
| T5 | Thân dưới + Prehab gối | 32′ | 6–7 |
| T6 | Chạy biến tốc + Core + gối | 30′ | 7–8 |
| T7 | Chạy–đi bộ dài + Mobility đầy đủ | 50′ | 4–5 |
| CN | Hồi phục chủ động | 25′ | 2–3 |

Chu kỳ 4 tuần: Làm quen (×1,00) → Tăng tải (×1,10) → Đỉnh (×1,20) → Giảm tải (×0,60).

## Quy tắc gối

Bốn nhóm cơ ưu tiên: tứ đầu đùi, mông nhỡ, cơ dép và bắp chân, chày trước.
Bài chính: step-up có tạ, split squat, Spanish squat, hip thrust, RDL một chân, nhón
bắp chân ngồi và đứng, nhón cẳng chân trước, monster walk. Nhịp hạ chậm 3 giây.

Va đập: tuần 1–2 giữ thấp, từ tuần 3 thêm bật nhỏ tại chỗ.
Khối lượng đi bộ và chạy: tăng tối đa 10%/tuần, app chặn cứng.

Tín hiệu sau buổi đi bộ hoặc chạy dài:

| Tín hiệu | Tuần kế tiếp |
|---|---|
| Không nhức, hoặc tan trong 1 giờ | Tăng 10% theo kế hoạch |
| Còn nhức sang hôm sau, dưới 3/10 | Giữ nguyên, không cộng thêm |
| Nhức trên 3/10 hoặc quá 24 giờ | Giảm 30% khối lượng đi/chạy, **giữ nguyên phần tạ** |

Hai tuần đỏ liên tiếp → app khuyên đi khám.

## Ba lớp minh hoạ động tác

1. **Hoạt hình dựng sẵn, offline.** Hình người 2D vẽ bằng Compose Canvas theo khung
   hình góc khớp. Luôn có, không cần mạng, sạch bản quyền.
2. **Video demo trực tuyến.** Liên kết tuyển chọn cho từng động tác.
3. **Video của người dùng.** File MP4 tự quay, lưu nội bộ, phát bằng ExoPlayer.

## Âm thanh

Nhịp trống tabata tổng hợp thời gian thực bằng `AudioTrack` (128–150 BPM), khớp tuyệt
đối với đồng hồ. Cấu trúc 20 giây gắng sức / 10 giây nghỉ × 8 vòng = 1 block 4 phút.
Giọng đếm tiếng Việt qua `TextToSpeech`. Dùng lại bộ này làm máy đếm nhịp chân 170–180
khi chạy bộ. Không đóng gói nhạc có bản quyền.

## Lộ trình

M1 khung dự án + CI · M2 dữ liệu + nội dung · M3 hoạt hình động tác · M4 trình phát +
âm thanh · M5 lịch + nhắc nhở · M6 tiến trình + sao lưu · M7 hoàn thiện.

## Lưu ý sức khoẻ

Giáo án tập luyện chung, không phải tư vấn y tế. App hiển thị cảnh báo tương tự ở màn
hình khởi tạo.
