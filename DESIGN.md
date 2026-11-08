# LUMA Tech — Thiết kế giao diện

Trạng thái: kế hoạch được chốt giữa agent chính và agent phản biện theo yêu cầu người dùng, ngày 08/11/2026. Chưa nghiệm thu giao diện render. Chi tiết thảo luận: `tai-lieu/20_PhanBienGiaoDien.md`.

## Sản phẩm và nguyên tắc

Website bán điện thoại/laptop tiếng Việt chạy localhost, Java 21, Spring Boot 4.x, Thymeleaf, Bootstrap 5.3 và JavaScript thuần. Hai khu vực cửa hàng và quản trị dùng chung hệ token. LUMA Tech là tên làm việc của bản demo, chưa phải thương hiệu được người dùng xác nhận.

Cửa hàng giúp chọn đúng máy và SKU, thấy đúng giá/tồn và hoàn tất đơn. Checkout ưu tiên địa chỉ người nhận, điểm giao và tổng tiền rõ ràng. Quản trị ưu tiên hàng chờ và trạng thái nghiệp vụ. Nội dung không tự khẳng định chứng nhận, số khách, cam kết giao hàng, mức giảm hoặc đánh giá không có dữ liệu chứng minh.

## Token giao diện

| Token CSS đề nghị | Giá trị | Vai trò |
|---|---|---|
| `--luma-bg` | `#f6f7f9` | Nền trang |
| `--luma-surface` | `#ffffff` | Form, bảng, vùng nội dung |
| `--luma-ink` | `#15283d` | Chữ chính, tiêu đề, logo chữ |
| `--luma-muted` | `#596879` | Chữ phụ có ý nghĩa |
| `--luma-accent` | `#087f76` | Nút chính, link có nhấn mạnh |
| `--luma-accent-hover` | `#06665f` | Hover/active nút chính |
| `--luma-accent-soft` | `#e7f3f0` | Nền nhấn phụ |
| `--luma-border` | `#dce2e8` | Phân tách vùng nội dung |
| `--luma-danger` | `#b42318` | Lỗi, thao tác hủy/xóa |
| `--luma-warning-ink` | `#7a4e00` | Chữ cảnh báo |
| `--luma-warning-bg` | `#fff3cd` | Nền cảnh báo |
| `--luma-space-1/2/3/4/6/8/12/16` | `4/8/12/16/24/32/48/64px` | Nhịp khoảng cách |
| `--luma-radius-control` | `8px` | Input, nút |
| `--luma-radius-panel` | `12px` | Panel, card sản phẩm |
| `--luma-content-max` | `1280px` | Chiều rộng cửa hàng |

Font: `system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif`, hỗ trợ tiếng Việt. Nội dung/form 16px, line-height khoảng 1.5; metadata 14px; tiêu đề trang 28–40px theo viewport; giá 20–24px. Mobile input tối thiểu 16px. Giá và số liệu bảng dùng tabular numbers khi có. Màu trạng thái luôn có nhãn chữ, không dựa màu để truyền đạt ý nghĩa.

Ảnh máy là nội dung chính: dùng đúng model có nguồn, tỉ lệ ảnh nhất quán và `object-fit: contain`. Không thay ảnh sản phẩm thật bằng biểu tượng hoặc hình minh họa máy chung. Chưa có ảnh thì dùng placeholder ghi rõ, không giả làm sản phẩm đúng model.

## Bố cục và thành phần

- Cửa hàng: header logo chữ, tìm kiếm nổi bật, tài khoản/giỏ; nav Điện thoại và Laptop. Mobile search tách hàng, nút menu có tên truy cập được.
- Hero tĩnh gọn, khoảng 280–340px desktop; nội dung và ảnh đủ ngắn trên mobile để danh mục/sản phẩm xuất hiện sớm. Không carousel tự chạy.
- Grid sản phẩm: một cột dưới 390px; hai cột từ 390px; desktop tăng tối đa bốn cột theo vùng nội dung còn lại. Card chứa tên model, cấu hình tóm tắt, giá, trạng thái tồn và liên kết chi tiết. Text dài xuống dòng.
- Bootstrap cung cấp form, grid, offcanvas, modal, tab, pagination và table. CSS dự án điều chỉnh token và mật độ; không thêm thư viện UI frontend riêng. Nút chính nền teal; nút phụ viền; thao tác nguy hiểm màu đỏ với nội dung xác nhận cụ thể.
- Quản trị: sidebar desktop, offcanvas mobile; thanh tiêu đề chứa tên trang và thao tác chính. Bảng có tìm/lọc/phân trang, header rõ, vùng cuộn khi thật sự cần; mobile hiển thị các trường quyết định xử lý trước.
- Mỗi form có label, hint cần thiết, lỗi theo trường và dữ liệu được giữ lại sau submit lỗi. Thông báo thành công gắn với thao tác đã được server chấp nhận.

## Quy tắc theo màn hình

| Trang | Quy tắc chốt |
|---|---|
| Trang chủ | Hero biên tập gọn, lối vào danh mục/nhu cầu, sản phẩm nổi bật từ dữ liệu; không tự thêm claim bán hàng. |
| Danh sách | Bộ lọc sidebar desktop/offcanvas mobile, chip lọc đang chọn, số kết quả và sort; giữ query khi tải lại. Rỗng có xóa lọc. |
| Chi tiết | Gallery và khối mua hai cột desktop/một cột mobile; chọn SKU hiển thị giá/tồn tương ứng. SKU hết hàng có thông báo và lựa chọn khác. Thông số, đánh giá, 3D nằm phía sau nội dung mua chính. |
| Giỏ | SKU đầy đủ, số lượng, đơn giá và tổng; lỗi tồn gắn dòng; giỏ rỗng có đường về danh mục. |
| Checkout | Hai cột desktop, một cột mobile. Nhập tay luôn hiện. Mở bản đồ bằng nút, có xác nhận pin riêng. Có người nhận, điện thoại, địa chỉ chi tiết, ghi chú, COD/PayOS và tổng tiền. Tóm tắt sticky chỉ trên desktop; mobile không để vùng cố định che form. |
| PayOS | Hiển thị mã đơn, tiền, hạn và trạng thái server; có chờ xác minh, đã thu, thu thiếu, thu dư/cần đối soát, hết hạn, hủy và chờ hoàn. Return URL chỉ mở trang kiểm tra. Lỗi mạng cho thử lại cùng đơn. |
| Tài khoản | Menu phụ gọn; hồ sơ, địa chỉ, bảo mật và đơn hàng. Địa chỉ mặc định có nhãn. Xác minh/reset có trạng thái token hết hạn hoặc đã dùng. |
| Đơn hàng | Snapshot hàng/giá/địa chỉ; timeline dọc; ba nhãn riêng cho đơn, thanh toán, vận chuyển. Hủy/trả chỉ hiện khi hợp lệ nhưng server vẫn kiểm tra. |
| Vận chuyển/trả | Một kiện giao đi mỗi đơn demo. Hàng chờ có người phụ trách/hạn. Chi tiết có SKU, số lượng dự kiến, thực nhận, nhập lại và timeline. Ghi thu COD riêng giao thành công; nhận hàng trả riêng hoàn tiền. |
| Chat | Nhãn bot/nhân viên và trạng thái bàn giao rõ. Lỗi gửi có thử lại. Mobile chat không che thao tác mua và mở thành vùng đọc được. |
| Báo cáo | Bộ lọc kỳ, định nghĩa chỉ số, bảng và biểu đồ; tiền thực thu tách doanh thu. Không có dữ liệu dùng trạng thái rỗng, không tạo số giả để lấp biểu đồ. |

## Khả năng truy cập và trạng thái

- Focus bàn phím rõ, vùng chạm thao tác chính khoảng 44px, DOM theo thứ tự đọc; link/nút đúng ngữ nghĩa. Modal/offcanvas quản lý focus và trả focus khi đóng.
- Nút icon có accessible name; ảnh sản phẩm có alt model; ảnh trang trí alt rỗng. Lỗi nối với input qua `aria-describedby`, input lỗi dùng `aria-invalid`.
- Thông báo giỏ/thanh toán thay đổi dùng vùng live phù hợp, không đọc lại toàn trang. Xác nhận pin có phương án tìm/chọn địa chỉ bằng bàn phím.
- Tất cả màn hình có loading, empty, input error, lỗi mạng và nội dung dài. Quyền truy cập, trạng thái hết hàng/hết hạn và thao tác đã xử lý có phản hồi rõ.
- Chuyển động ngắn 120–180ms nếu giúp phản hồi; tôn trọng `prefers-reduced-motion`. Nội dung hiện mặc định, không phụ thuộc animation để xuất hiện.
- Thông tin kỹ thuật/thiếu key chi tiết ở quản trị hoặc tài liệu cấu hình; phía khách có thông báo dễ hiểu và thao tác phục hồi. Khóa bí mật không đưa vào HTML/JavaScript.

## Cổng kiểm tra trước nghiệm thu

1. Chụp và xem thật các trang chính ở 1440px, 390px; thêm 360px cho catalog, chi tiết và checkout. Kiểm tra zoom/font lớn, text dài, bảng, menu, modal và khung chat.
2. Thử chọn SKU hết hàng/còn hàng, thêm/xóa giỏ, lỗi form và giữ dữ liệu, bộ lọc không có kết quả, lỗi mạng và hết hạn thanh toán.
3. Thử Google Maps thật: tìm địa chỉ Việt Nam, mở bản đồ, chọn/sửa/xác nhận pin và ghi lại điểm giao. Nhập tay là phục hồi; không thay bằng chứng API đã chạy.
4. Thử PayOS thật khi cấu hình sẵn sàng: tạo link, server xác minh khoản nhận, webhook lặp và trang chờ; ghi riêng bằng chứng thật với mô phỏng.
5. Review hình ảnh và tương tác là bước tiếp theo sau triển khai; tài liệu thiết kế này không tự chứng minh API, trạng thái nghiệp vụ hoặc giao diện đã chạy đúng.


## Cập nhật theo yêu cầu người dùng — 08/11/2026

Trang chủ giữ thế giới màu/font hiện tại, có mục Danh mục sản phẩm với ba lối vào ĐIỆN THOẠI, LAPTOP, PHỤ KIỆN. Trang chủ và kết quả tìm chung không hiện bộ lọc/sắp xếp. Chỉ trang đã chọn danh mục có bộ lọc, hãng/dòng phù hợp danh mục; dòng phụ thuộc hãng, phụ kiện có loại phụ kiện và không hiện RAM/CPU/bộ nhớ. Danh sách chung dùng bốn cột desktop; cấu trúc danh mục có sidebar và ba cột. Đã bỏ toàn bộ tìm kiếm giọng nói theo yêu cầu. Nhân viên có mục quản lí riêng dành admin, nhân viên dùng điều hướng vận hành giới hạn.
