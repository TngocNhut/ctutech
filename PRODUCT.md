# LUMA Tech

## Platform
Web responsive, chạy localhost trên Windows.

## Stack
Java 21, Spring Boot 4.x/MVC, Thymeleaf, Bootstrap 5.3, JavaScript thuần, Spring JDBC/JdbcClient, PostgreSQL V7, Flyway.

## Users and purpose
Khách mua điện thoại/laptop và quản trị cửa hàng. Thực hiện đầy đủ kế hoạch 12 bước trong chat: tài khoản, sản phẩm/SKU, kho, giỏ, checkout, COD/PayOS, địa chỉ Google Maps, hóa đơn, vận chuyển/trả/hoàn, hỗ trợ, báo cáo và các chức năng mở rộng.

## Constraints and evidence
Schema chuẩn: tai-lieu/ecom-web-db-v7.sql. Kiến trúc: tai-lieu/11_KeHoachKienTrucBaTang.md. Một kiện giao đi mỗi đơn. Tiền VND, giá/tồn mẫu phải được ghi rõ là mô phỏng. Tên LUMA Tech là tên làm việc do agent đề xuất, chưa phải thương hiệu người dùng xác nhận.

## Integration state
Người dùng xác nhận chưa có khóa PayOS/Google Maps, cho phép triển khai và hướng dẫn cấu hình sau. Không tuyên bố thanh toán hoặc bản đồ thật thành công trước khi kiểm thử với khóa thật. Khóa bí mật đặt trong config local được gitignore. Không đưa khóa vào chat, nhật ký hoặc source.
