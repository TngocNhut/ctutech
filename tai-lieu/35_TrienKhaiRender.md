# Đưa CTUtech lên GitHub và Render

Hướng dẫn này dùng **GitHub private repository**, **Render Web Service Free**, **Render PostgreSQL Free** và pgAdmin trên Windows. Bạn tự thực hiện trên tài khoản của mình; không nhập mật khẩu hay khóa vào GitHub hoặc chat.

## Trước khi bắt đầu

- Có tài khoản GitHub, tài khoản Render và pgAdmin; cài Git cho Windows.
- PostgreSQL hiện tại đang chạy ở máy bạn. Nhớ tên database cần chuyển (`luma_shop`) và xác nhận có dữ liệu cần giữ.
- Render Free phù hợp để trình bày/kiểm thử, không đảm bảo dịch vụ và dữ liệu liên tục. Postgres Free hiện hết hạn sau 30 ngày, có 14 ngày gia hạn rồi có thể bị xóa; không có backup tự động. Web Service ngủ sau 15 phút không nhận truy cập và lưu trữ tệp cục bộ bị xóa khi restart/ngủ/redeploy.
- Khi restore bản sao `luma_shop`, **tài khoản và mật khẩu đang có cũng được sao chép**. Trước khi đưa ra Internet, đổi mật khẩu quản trị mặc định, khóa/xóa các tài khoản demo không cần thiết theo quy trình quản lý tài khoản và kiểm tra dữ liệu cá nhân. Chỉ tắt tạo demo trên Render không làm mất các tài khoản demo đã có trong bản sao.

## 1. Tạo repository trên GitHub

1. Vào GitHub → **New repository**.
2. Đặt tên, ví dụ `ctutech`, chọn **Private**. Tạo repository trống, không chọn tạo README, `.gitignore` hoặc license trên GitHub.
3. Mở PowerShell trong `C:\Users\Ngoc Nhat\Desktop\projweb` và chạy:

```powershell
git init
git branch -M main
git add .
git status --short
```

4. Kiểm tra danh sách trước khi commit. Không được có `config/application-local.properties`, `config/payos-local.properties`, `.local/`, file `.env`, file backup `.dump`/`.backup` hoặc khóa cá nhân. `.gitignore` đã loại chúng; đừng dùng `git add -f` với các tệp đó. Tài liệu cá nhân và báo cáo ở thư mục gốc, phần lớn `tai-lieu/`, cũng được loại khỏi Git; hai hướng dẫn Render/PayOS và các tệp JSON dữ liệu sản phẩm cần cho kiểm thử được giữ lại.
5. Commit và liên kết với repository vừa tạo (thay địa chỉ bằng URL của bạn):

```powershell
git commit -m "Prepare CTUtech for Render"
git remote add origin https://github.com/<ten-github>/ctutech.git
git push -u origin main
```

Nếu Git hỏi xác thực, làm theo Git Credential Manager hoặc hướng dẫn GitHub; không dán token vào source. Mã nguồn có nhiều ảnh sản phẩm nên push lần đầu có thể mất thời gian.

## 2. Tạo PostgreSQL trên Render

1. Trong Render Dashboard chọn **New → Postgres**.
2. Chọn gói **Free**, đặt tên; chọn region bạn dự định dùng cho Web Service. Chọn PostgreSQL **17** nếu Render cho phép, tương thích với cơ sở dữ liệu hiện tại.
3. Sau khi tạo, mở trang database và lấy thông tin kết nối. **External Database URL** dùng từ máy của bạn; địa chỉ/host nội bộ dùng cho Web Service cùng region. Không chia sẻ connection URL vì nó chứa mật khẩu.
4. Kiểm tra phần **IP Access Control**: nếu cần, cho phép địa chỉ IP công khai của máy bạn để pgAdmin kết nối. Dùng SSL theo yêu cầu Render. Không để database công khai rộng hơn mức cần thiết.

## 3. Sao chép database hiện tại bằng pgAdmin

Thực hiện khi database Render còn trống và trước khi deploy Web Service. Không restore đè lên database đã có dữ liệu cần giữ.

1. Trong pgAdmin, kết nối database `luma_shop` trên máy → nhấp phải database → **Backup**.
2. Chọn định dạng **Custom**, lưu file backup ở nơi riêng tư ngoài repository. Sao lưu schema và data, gồm cả `flyway_schema_history`. Không chia sẻ tệp vì nó chứa tài khoản, địa chỉ, đơn hàng và dữ liệu cá nhân.
3. Thêm kết nối server Render trong pgAdmin: lấy **host, port, database, username, password** từ phần kết nối bên ngoài của Render, không dán URL chứa mật khẩu vào chat. Chọn SSL mode theo hướng dẫn Render; thông thường `require` cho kết nối ngoài.
4. Chọn database Render vừa tạo → **Restore**, chọn backup Custom. Trong các tùy chọn pgAdmin/`pg_restore`, chọn **No owner** và **No privileges/ACL** để không cố tạo lại quyền của máy cũ. Không chọn `Clean/Drop` nếu database đích có dữ liệu cần giữ.
5. Kiểm tra kết quả Restore và nội dung, ví dụ số bảng, sản phẩm và bảng `flyway_schema_history`. Không chạy thêm file schema V7 bằng tay. Khi website chạy, Flyway sẽ kiểm tra lịch sử và chỉ chạy migration còn thiếu.

Nếu chỉ cần database mới từ source, có thể bỏ qua bước restore: để database Render hoàn toàn trống và để Flyway tạo schema/dữ liệu khi web chạy lần đầu. Cách này **không có tài khoản quản trị được tạo sẵn** vì chế độ tạo tài khoản demo được tắt trên Render; hiện dự án chưa có quy trình khởi tạo admin an toàn cho database mới. Với yêu cầu giữ dữ liệu, nên restore bản sao đã bảo vệ như trên.

Chỉ dữ liệu trong PostgreSQL được restore. Ảnh/banner đã upload qua admin và chứng từ hoàn lưu ở `.local/` trên máy không nằm trong backup PostgreSQL; file tĩnh vốn trong source vẫn được GitHub/Render cung cấp. Render Free chưa có lưu trữ bền vững cho file upload.

## 4. Tạo Web Service trên Render

1. Chọn **New → Web Service**, kết nối GitHub, chọn repository `ctutech` và nhánh `main`.
2. Chọn **Docker** và gói **Free**, cùng region với database. Dockerfile và `application-render.properties` đã chuẩn bị sẵn; Render tự dùng Dockerfile. `render.yaml` là lựa chọn Blueprint tham khảo, không bắt buộc nếu tạo hai dịch vụ thủ công.
3. Trong **Environment**, thêm các biến (giá trị lấy từ dashboard PostgreSQL; không thêm dấu `< >`):

```text
DB_URL=jdbc:postgresql://<internal-host>:5432/<database-name>
DB_USER=<database-user>
DB_PASSWORD=<database-password>
PAYOS_MODE=DISABLED
```

`DB_URL` phải bắt đầu bằng `jdbc:postgresql://`, không dùng nguyên `postgresql://user:password@...` làm JDBC URL. Không đưa username/password vào URL nếu đã dùng hai biến riêng. Giữ mặc định port do Render cung cấp; Dockerfile bật profile `render`, bind `0.0.0.0` và tắt tạo tài khoản demo.
4. Tạo Web Service và đợi build/deploy. Mở `https://<ten-service>.onrender.com`; lần đầu hoặc sau khi dịch vụ ngủ có thể mất khoảng một phút.
5. Kiểm tra trang chủ, sản phẩm, đăng nhập tài khoản đã được bảo vệ, giỏ hàng và đơn hàng. Xem log Render nếu Flyway/DB không kết nối. Không đăng thông tin kết nối hoặc token trong ảnh chụp log.

## 5. Cấu hình kênh thu PayOS

1. Tạo kênh thu trên PayOS, lấy **Client ID, API key, checksum key**.
2. Chỉ sau khi website HTTPS hoạt động và bạn đã bảo vệ tài khoản quản trị, vào **Environment** của Web Service trong Render, thêm các biến:

```text
PUBLIC_URL=https://<ten-service>.onrender.com
PAYOS_CLIENT_ID=<client-id>
PAYOS_API_KEY=<api-key>
PAYOS_CHECKSUM_KEY=<checksum-key>
PAYOS_MODE=LIVE
```

Nhập giá trị trực tiếp trong Dashboard, không commit vào Git. Render redeploy khi thay đổi biến môi trường. Trên Render, màn hình **Cấu hình PayOS** chỉ hướng dẫn và hiển thị trạng thái; lưu khóa qua form local bị tắt vì ổ đĩa Render Free không bền vững.
3. Cấu hình webhook trên PayOS: `https://<ten-service>.onrender.com/api/payos/webhook`. Cấu hình PUBLIC_URL phải trùng domain truy cập thật. Sau khi xác minh webhook, kiểm tra với giao dịch giá trị nhỏ theo hướng dẫn PayOS. PayOS hiện không có sandbox riêng; giao dịch thử là giao dịch thật.
4. Website chỉ xác nhận tiền sau webhook có chữ ký hợp lệ hoặc đối soát; việc quay lại website không đủ để xác nhận. Không dùng dịch vụ Free cho hoạt động bán hàng thật cần webhook tin cậy, lưu trữ an toàn hoặc uptime liên tục.

## 6. Những giới hạn còn lại

- Chức năng gửi thư hiện chỉ ghi thư cục bộ trên máy chủ, chưa gửi email thật. Trên Render Free, file đó bị mất và người dùng không thể nhận email đặt lại mật khẩu; phải tích hợp dịch vụ email trước khi công bố rộng rãi.
- Ảnh upload, banner upload và chứng từ hoàn cần object storage hoặc lưu trữ bền vững trước khi dùng thật. Phiên đăng nhập, kể cả “Ghi nhớ tôi”, mất khi instance khởi động lại.
- Nếu database có người dùng hoặc đơn hàng thật, không dùng database Free quá thời gian hết hạn; chủ động backup và bảo vệ file backup. Hạn sử dụng và chính sách có thể thay đổi, kiểm tra lại tài liệu Render.
- Sau khi mọi thứ ổn, kiểm tra lại webhook thực tế, xử lý chậm do dịch vụ ngủ và các phiên cần đối soát trước khi nhận tiền.

Tài liệu chính thức: https://render.com/docs/free ; https://render.com/docs/web-services ; https://render.com/docs/postgresql-creating-connecting ; https://payos.vn/docs/moi-truong-test/ .
