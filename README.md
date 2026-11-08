# CTUtech — Website bán điện thoại, laptop và phụ kiện

Ứng dụng Spring Boot 4, Java 21, Thymeleaf và PostgreSQL. Dữ liệu cá nhân, tệp cấu hình trên máy và bản sao cơ sở dữ liệu không được đưa lên GitHub.

- [Hướng dẫn đưa mã lên GitHub và triển khai Render](tai-lieu/35_TrienKhaiRender.md)
- [Hướng dẫn cấu hình thanh toán PayOS](tai-lieu/34_CauHinhPayOS.md)

Chạy cục bộ trên Windows theo cấu hình máy hiện tại:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1
```

Kiểm thử dùng cơ sở dữ liệu `luma_test` riêng:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1
```

`application.properties` giữ cấu hình localhost; cấu hình riêng cho Render nằm trong `application-render.properties` và được bật bởi Dockerfile. Database đang dùng Flyway: khi chuyển dữ liệu cần giữ bảng `flyway_schema_history` đồng bộ với các migration trong source.
