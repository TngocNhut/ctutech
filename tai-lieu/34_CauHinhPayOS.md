# Cấu hình thanh toán PayOS

Website sử dụng PayOS **link thanh toán** để nhận tiền đơn hàng; không tạo lệnh chi hoặc tự động chuyển tiền hoàn.

1. Tạo tài khoản và kênh thanh toán tại my.payos.vn; lấy Client ID, API key và checksum key của kênh đó. Không gửi các khóa qua chat hay đưa vào mã nguồn.
2. Đưa ứng dụng lên địa chỉ HTTPS công khai. Một reverse proxy hoặc đường hầm HTTPS cần chuyển tiếp đến ứng dụng; `localhost` trên máy khách không phải địa chỉ công khai. Bảo vệ quản trị bằng HTTPS và giới hạn truy cập phù hợp trước khi dùng thông tin thật.
3. Đăng nhập quản trị, mở **Cấu hình PayOS**, nhập URL gốc HTTPS (không có đường dẫn), ba thông tin kết nối và chọn bật. Lưu, sau đó khởi động lại ứng dụng để áp dụng. Các khóa không được hiển thị lại; để trống trường khóa để giữ giá trị đang cấu hình.
4. Trên cổng PayOS, đăng ký webhook `https://<ten-mien>/api/payos/webhook`, thay tên miền bằng URL công khai thực tế. Xác nhận webhook và tạo một giao dịch thử theo quy trình của PayOS trước khi nhận thanh toán chính thức.
5. Khi PayOS đã sẵn sàng, khách chọn PayOS ở bước đặt hàng. Website thử tạo link sau khi tạo đơn; trang thanh toán cung cấp nút mở link hoặc thử tạo lại nếu kết nối chưa thành công. Trạng thái thanh toán chỉ thay đổi sau webhook có chữ ký hợp lệ hoặc đối soát API; trang trở về không xác nhận tiền tự động.

Các giá trị do giao diện quản trị lưu nằm trong `config/payos-local.properties`, được loại khỏi Git và giới hạn quyền hệ thống tệp theo tài khoản chạy ứng dụng; chúng là **văn bản thuần**, vì vậy chỉ người vận hành tin cậy mới được truy cập máy chủ hoặc bản sao lưu. Nếu khóa đã lộ, đổi khóa trên PayOS và cấu hình lại. Lưu cấu hình yêu cầu khởi động lại ứng dụng; việc khởi động lại làm kết thúc phiên đăng nhập hiện tại. Nếu còn giao dịch PayOS chờ xử lý hoặc đối soát, hãy giải quyết trước khi thay đổi cấu hình.

Tham khảo: https://payos.vn/docs/api/ và https://payos.vn/docs/tich-hop-webhook/kiem-tra-du-lieu-voi-signature/ .

## Nếu chạy trên Render

Render Free không lưu bền vững tệp cấu hình. Màn hình quản trị trên Render chỉ hiển thị trạng thái và hướng dẫn; hãy đặt `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`, `PAYOS_MODE` và `PUBLIC_URL` trong Environment của Web Service theo [hướng dẫn Render](35_TrienKhaiRender.md). Không nhập khóa vào GitHub hoặc chat.
