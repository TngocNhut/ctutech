# Trạng thái triển khai — 08/11/2026

## Đăng nhập bằng email — cập nhật 09/11/2026

- Theo yêu cầu mới, tạm tắt đăng nhập Google, số điện thoại và bước gửi/xác nhận mã. Dữ liệu tài khoản và cấu hình cũ được giữ; các đường xác minh và Google bị chặn.
- Khách mới đăng ký bằng họ, tên, ngày sinh, email và mật khẩu; email không được gán trạng thái đã xác minh. Khách hoạt động có thể đặt hàng và đánh giá theo các điều kiện đơn hàng. Nhân viên do quản trị tạo có thể vận hành theo vai trò; chỉ quản trị quản lý tài khoản.
- Đã kiểm thử đăng ký, đăng nhập, giỏ, địa chỉ và đặt/hủy COD trên database test; không thực hiện thanh toán thật. Bộ 153 kiểm thử toàn hệ thống đạt (`.local/test-email-only-full.log`). Bản hiện chạy tại `http://localhost:8082`, PID 4932, log `.local/app-email-only.log`; các PID bên dưới là mốc trước.

## Cập nhật footer và trợ lý — 09/11/2026

- Đã bỏ chức năng so sánh cấu hình (đường dẫn, thao tác, template và logic phiên); danh sách đã xem và mua lại vẫn được giữ.
- Footer có các nhóm liên kết thực có và thông tin dự án học tập tại Trường Đại học Cần Thơ. Nút trợ lý cố định mở hộp hỗ trợ hiện có; khách chưa đăng nhập được hướng dẫn đăng nhập.
- 14 kiểm thử liên quan đạt (`.local/test-footer-chat.log`); browser desktop/mobile và kiểm thử gửi vào cuộc hỗ trợ trên database test đạt. Bản hiện chạy tại `http://localhost:8082`, PID 20884, log `.local/app-verify-locale.log`.

## Tài khoản, giao diện và địa chỉ — cập nhật 09/11/2026

- Bản nghiệm thu trước đó dùng PID 10724, log `.local/app-account-final.log`; V29 đã áp dụng thành công. Khởi tạo sạch 29 migration đạt ở `.local/qa-clean-account-release.log`. Đã chạy browser desktop/mobile cho đăng ký, giỏ, địa chỉ và hỗ trợ; OTP email/điện thoại đã được thử đầy đủ bằng dịch vụ giả lập trên database test, không gửi thật. Các tiến trình giả lập đã dừng.

- Đăng ký email/điện thoại dùng mã 6 số qua Twilio Verify sau khi cấu hình; Google OIDC tự tạo tài khoản mới khi email đã xác minh, không tự ghép email đang tồn tại. Hướng dẫn: `tai-lieu/36_CauHinhMaVaGoogle.md`. Chưa có thông tin cấu hình để kiểm thử gửi thật.
- Kiểm thử hợp đồng Twilio giả lập gồm cả email/SMS, mã sai, lỗi HTTP, JSON sai và trạng thái không hợp lệ đạt (`.local/test-verify-failure-modes.log`). Đã kiểm tra biến môi trường được tệp local chuyển vào ứng dụng bằng dữ liệu giả; thông tin đã có trong tệp local được giữ nguyên. Yêu cầu SMS dùng locale tiếng Việt; email dùng ngôn ngữ của mẫu SendGrid (`.local/test-verify-locale.log`). Gửi tới nhà cung cấp thật vẫn cần khóa và nghiệm thu thực tế.
- Flyway V29 hỗ trợ điện thoại đã xác minh và ngày sinh. Giỏ tự cập nhật số lượng; trang tài khoản, thông số, địa chỉ, hỗ trợ và thông báo được điều chỉnh. Dữ liệu địa giới và giấy phép: `tai-lieu/35_DuLieuDiaGioi.md`.
- Toàn bộ 151 kiểm thử đạt (`.local/test-full-auth-retry.log`); kiểm thử OTP bổ sung đạt (`.local/test-otp-release.log`). Bản sao database trước migration: `.local/pre-account-v29.dump`.

## Bổ sung mẫu sản phẩm — hiện hành

- Đã thêm 75 model vào mỗi danh mục; hiện có 105 điện thoại, 105 laptop, 90 phụ kiện và 611 SKU công khai. 225 model mới gồm 439 SKU và 1.095 ảnh nguồn, giá và tồn là dữ liệu trình diễn.
- Flyway V24–V26 đã áp dụng; đối chiếu toàn bộ giá và tồn SKU cũ không thay đổi. 146/146 kiểm thử đạt (`.local/test-more-catalogue-all.log`); khởi tạo database trống với 26 migration đạt (`.local/qa-clean-more.log`).
- Browser QA đạt cho 225 trang sản phẩm mới, ảnh và ba danh mục trên desktop/mobile (`.local/qa-more-catalogue.log`); lọc, phân trang và gallery đạt (`.local/qa-catalogue-more.log`). Ứng dụng local: http://localhost:8082. Chi tiết: `tai-lieu/34_MauSanPhamBoSung.md`.

## Mốc mở rộng nhân viên và danh mục trước đó (lịch sử)

- Bản nguồn đã đạt 145/145 kiểm thử toàn hệ thống, không bỏ qua: `.local/test-expansion-release.log`. Flyway V23 trên app/test.
- Đã thêm quản lí tài khoản nhân viên và quyền vận hành có kiểm tra HTTP/service; không cho nhân viên phân quyền/cấu hình/hoàn tiền. Khóa/đổi quyền thu hồi phiên.
- Đã bỏ toàn bộ UI/script giọng nói. Trang chủ có ba danh mục, không có lọc/sắp xếp. Danh mục có hãng/dòng tương ứng; phụ kiện có loại, không dùng thông số máy tính.
- 30 điện thoại +30 laptop +15 phụ kiện, 172 SKU và396 ảnh có nguồn/checksum. Card dùng ảnh đúng SKU màu; form admin phụ kiện chấp nhận trường không áp dụng, vẫn kiểm tra điện thoại/laptop.
- Khởi tạo sạch23 migration thành công ở `.local/qa-clean-expansion.log`. Browser cuối đã đạt: `.local/qa-expanded-release.log` (75 model, mọi ảnh SKU, ba danh mục, bộ lọc, desktop/mobile và không còn voice); `.local/qa-employees-release.log` (tạo/xác minh/quyền/khóa nhân viên); `.local/qa-3d-expansion.log` (3D và nhập tay).
- Bản đang chạy http://localhost:8082, JAR PID16692/session97677, log `.local/app-expansion-release.log`; xác minh PID/commandline trước khi dừng. Nhân viên QA đã khóa; không tạo giao dịch tiền từ đợt này.
- Smoke phụ kiện đạt: thêm/xóa giỏ khách, form quản trị không bắt nhập thông số máy tính; `.local/qa-accessory-release.log`. Ảnh nghiệm thu đã xem: `home-release-390.png`, `accessories-release-390.png`, `employees-390.png` trong `.local/qa`.
- Đối chiếu database đang phục vụ: điện thoại30/94SKU, laptop30/43SKU, phụ kiện15/35SKU; HTTP localhost200; `/js/voice.js` trả404. Không còn việc lập trình hoặc nghiệm thu bắt buộc của yêu cầu mở rộng đang treo.
- Chi tiết/nguồn: `tai-lieu/31_DanhMucMoRong.json`, `tai-lieu/32_NhanVienVaDanhMucMoRong.md`.

## Bản nghiệm thu trước khi mở rộng

Đã hoàn tất phạm vi bản đồ án local. Nội dung dưới phần “Lịch sử” là các checkpoint cũ, không đại diện trạng thái hiện hành.

- Stack Java21/Boot4.0.9/MVC/Thymeleaf/Bootstrap5.3/JDBC/PostgreSQL17.11, 64 bảng nghiệp vụ, Flyway V1–V20 trên app/test. Giữ nguyên schema thiết kế V7 và migration đã áp dụng.
- 138 kiểm thử toàn bộ ở `.local/test-final-acceptance.log`; gồm tài chính tính tay, analytics, OAuth service và giao thức authorization/token/JWT/state, bảo mật, đồng thời và rollback.
- Browser cuối: storefront/auth/admin 1440/390/360; reports/privacy/CSV/CSRF/quyền; GLB thực, voice giả lập và Maps contract. Theo dõi/thông báo có opt-in và choices/mua lại cũng đạt trên JAR cuối; fixture được dọn về ẩn/ngừng/tồn0, giỏ QA trống. Các bộ nghiệp vụ khác đã đạt ở các mốc lịch sử.
- Cài mới từ database trống thành công, 20 migration + seed và 5 route HTTP; `.local/qa-clean-startup-result.log`. Helper8083 đã dừng, không đụng database demo.
- App local http://localhost:8082, JAR cuối PID25200/session60695 tại mốc nghiệm thu; log `.local/app-final.log` (PID có thể thay đổi, xác minh trước dừng). Không deploy công khai.
- Đã bổ sung đầy đủ dashboard/analytics/OAuth, so sánh/đã xem/mua lại/3D/voice, banner/sự kiện, chat/tri thức/cấu hình cùng nhóm mua hàng/giao/trả/hoàn trước đó.
- PayOS/Maps/OAuth thật chưa có khóa và người dùng đã cho cấu hình sau. Không coi kiểm thử mô phỏng là giao dịch/provider thật; nhập tay/email local/COD hoạt động độc lập.
- Hướng dẫn tổng hợp: `tai-lieu/30_NghiemThu.md`; báo cáo28, OAuth29. Không còn hạng mục lập trình bắt buộc đang treo trong phạm vi local này.

## Lịch sử triển khai

## Cập nhật tiếp theo 08/11/2026

- V3__VaiTroUngDung áp dụng cho app/test; tạo vai trò khách/admin. Tài khoản demo admin@luma.local và khach@luma.local được tạo bằng Service + BCrypt; mật khẩu mẫu trong README. Cấu hình tắt seed và đổi mật khẩu seed có sẵn.
- Đăng ký có Bean Validation/CSRF, chuẩn hóa email, luôn cấp KhachHang. Token xác minh/reset ngẫu nhiên32byte, CSDL lưu hash, hạn30phút, một lần và thu hồi token cũ; local mailbox .local/mail sau commit. GET token không đổi trạng thái, POST xác nhận có CSRF.
- Login giao diện riêng, hồ sơ cá nhân; session hash trong PhienDangNhap; mỗi request kiểm tra thu hồi/hạn/trạng thái tài khoản/vai trò. Reset thu hồi toàn bộ phiên; logout thu hồi phiên hiện tại. Đã có hộp thư local, chưa gửi SMTP thật theo phạm vi local đã chốt.
- Chi tiết /san-pham/{slug}?sku=... hiển thị đúng SKU/giá/tồn/thông số và chặn SKU thuộc model khác. Card đã có liên kết. Ảnh vẫn placeholder và chưa có nút mua vì giỏ chưa triển khai.
- /quan-tri và /quan-tri/bien-the/{id}: admin sửa giá/tồn có lý do; Service kiểm quyền, khóa SKU/tồn, chặn dưới số giữ, ghi lịch sử giá/sổ biến động/log cùng transaction. Chưa phải full CRUD sản phẩm.
- scripts/test.ps1 tạo database luma_test riêng; 7 test tích hợp pass, 0 lỗi. Test kho rollback; auth dùng email UUID. Báo cáo target/surefire-reports. Test DB ghi không tác động demo.
- scripts/qa-browser.cjs đã pass trên Edge: home/detail/login/register ở1440/390/360px; admin login/profile/list/SKU-edit; bảng admin360px; logout. Chụp16ảnh .local/qa. Đã xem trực tiếp ảnh login390, admin1440, detail360; không tràn ngang, form và SKU readable. Media chưa có nên chưa nghiệm thu thẩm mỹ toàn site.
- Server8082 mới chạy bằng scripts/start-local.ps1 trong exec session33957, log .local/app.log; revalidate HTTP/process khi tiếp tục. Lệnh khởi động đã dùng JDK21 và socket temp .local/tmp. Process cũ24244 đã dừng sau kiểm tra commandline thuộc app.

## Cập nhật giỏ hàng và COD — 08/11/2026

- V4 thêm trần số lượng giỏ99; dùng unique SKU/giỏ đã có trong V7. V5 seed mã demo LUMA100. Migrations đã áp dụng cả luma_shop/luma_test, không sửa nội dung cũ.
- Giỏ anonymous trong HttpSession; đăng nhập gộp vào giỏ PostgreSQL rồi xóa session guest. Thêm/sửa/xóa có CSRF, kiểm SKU đang bán và tồn, tối đa50SKU/99máy mỗi dòng; giỏ không giữ tồn. Giới hạn một instance local đã ghi trong comment ponytail.
- Sổ địa chỉ thêm/xóa/default theo đúng chủ, tối đa20; chọn lại ở checkout. Nhập tay luôn khả dụng. Map module dùng PlaceAutocompleteElement/gmp-select, map + marker kéo/thả, vị trí thiết bị, xác nhận pin. Chỉ tải Google khi khách mở; thiếukey/lỗi có thông báo và nhập tay.
- BanDoService reverse Geocoding ở server trước transaction, yêu cầu countryVN; không tin country/PlaceID client; tự ghi nguồn/PlaceID Google/thời điểm/hạn30ngày. Chưa có key nên chưa có bằng chứng API thật; lỗi thiếukey không chấp nhận pin giả. Có browser-key/server-key/map-id cấu hình mẫu. Cache hết hạn được dọn khi đọc sổ địa chỉ/checkout, chưa có job dọn riêng.
- Checkout COD đọc lại giỏ và giá, khóa user→voucher→SKU/tồn theo mã tăng dần, giữ tồn + voucher + snapshot đơn/dòng + lịch sử cùng transaction. Phí demo từ cấu hình, fallback30.000VND. Chưa tích hợp chiến dịch giá SKU, vẫn đọc giá gốc.
- Có báo giá ưu đãi trước khi đặt (không giữ tồn/lượt), cấp đơn có SHA-256 nội dung chuẩn hóa + key theo user; replay cùng payload trả đơn cũ kể cả giỏ đã xóa, key cùng nhưng payload khác bị từ chối. Phân bổ voucher theo phần dư lớn nhất, số VND nguyên và tổng khớp.
- Đơn của khách có danh sách/chi tiết/snapshot/timeline tiếng Việt, cấu hình dễ đọc; quyền owner/admin kiểm ở Service. Hủy trước bàn giao nhả giữ và trả lượt đúng một lần. Job mỗi phút xử lý COD chờ xác nhận quá24giờ qua Service transaction riêng; trạng thái/điều kiện kiểm dưới khóa.
- 15 test tích hợp pass trên DB test riêng: thêm cả guest/gộp giỏ, COD replay, từ chối xem đơn người khác, rollback sau bước ghi đơn/dòng/giữ kho, hai khách tranh SKU cuối và voucher cuối, phân bổ nhiều dòng khớp tổng, địa chỉ ownership/thiếukey Maps, hết hạn/hủy chống lặp.
- Browser scripts/qa-checkout.cjs pass: guest thêm SKU→đăng ký→local email verify→login/gộp giỏ→lưu/chọn địa chỉ→Maps chưa cấu hình vẫn nhập tay→báo giá voucher→đặt COD→replay giữ nguyên đơn→xem/hủy/history. Tạo account QA riêng và hủy đơn/đăng xuất sau test; không chuyển tiền thật.
- QA desktop/mobile screenshot .local/qa/checkout-desktop.png, checkout-mobile.png, cod-order-mobile.png; đã xem render và sửa rawJSON/statuscode thành thông tin dễ đọc. scripts/qa-browser.cjs cũng pass lại các trang cũ ở1440/390/360px.
- Server hiện tại PID4584, exec session27560, log .local/app-latest.log, HTTP8082; revalidate khi tiếp tục. CácPID24244/11268/21976 là lịch sử đã dừng. Chưa complete goal vì còn PayOS, shipping/returns/refunds/invoices và các module kế hoạch khác.

## Cập nhật PayOS — 08/11/2026

- SDK chính thức vn.payos:payos-java:2.0.1 đã tải/build tương thích Boot4/JDK21. SDK dùng Jackson2 riêng; web Boot dùng Jackson3. Adapter verify rawString bằng SDK, không chuyển Webhook qua mapper Boot.
- V6 ứng dụng tạo sequence orderCode tối đa53bit và 2kênh LIVE/MOCK riêng. DatHangYeuCau thêm phương thức, defaultCOD tương thích; fingerprint chứa phương thức. Đơn/phiên PayOS tạo cùng transaction checkout, hạn15phút, mode cố định ở phiên. LIVE chỉ bật khi modeLIVE và đủ3khóa; mô phỏng được gắn nhãn rõ.
- ThanhToanService điều phối SDK ngoài transaction; ThanhToanGhiSoService xử lý transaction ngắn. Link claim một lần; timeout giữ cùngorderCode, query API để khôi phục, không cấp mã/phiên mới. Khi chưa biết link tồn tại thì tiếp tục đối soát/hủy; chưa tự retry create khi API trả definitive notfound.
- Webhook POST/api/payos/webhook là endpoint duy nhất miễnCSRF; không yêu cầu session. Giới hạn64KiB, SDK xác minh signature/envelope/code/currency, parse giờ ngân hàng theo Asia/Ho_Chi_Minh. Chỉ lưu dữ liệu tối thiểu không gồm thông tin tài khoản ngân hàng/khóa.
- Verified inbox ghi bền vững trước ACK; processor ghi receipt và trạng thái event cùng transaction. Duplicate bankreference không cộng hai lần; ref đổi amount/link chuyển đối soát và giữ cờ lỗi qua bước ghiLink. Unknown session giữ pending. Job đọc lại pending, query API, hủy link đã đóng/đối soát và kiểm hạn dưới khóa.
- Thứ tự khóa voucher→order→session→event/receipt→stock; customer checkout thêm userlock ở đầu. Đủ tiền chỉ nhận trong hạn; nhận đủ tạo accepted FK và snapshot hóa đơn V2/dòng, vẫn DANG_GIU chờ bàn giao. Thiếu không kéo dài hạn; dư tạo yêu cầu hoàn dư và chờ admin. Khoản thu thêm sau khi đã nhận đủ không phá DA_THU của đơn hợp lệ.
- Hủy trước bàn giao (kể cả đã đủ tiền) hoặc hết hạn nhả giữ/trả voucher đúng một lần, tạo yêu cầu hoàn theo receipt. Thu sau đóng không mở lại đơn. Tổng pending+completed+failed refund được giữ thận trọng không vượt receipt; failed refund cần admin retry riêng, chưa có UI. Hoàn đang là hàng chờ, không tuyên bố đã chuyển tiền. Invoice snapshot đã lưu, chưa có PDF.
- 28test pass:15cũ +10payment integration +3SDK contracts. Bao gồm HMAC độc lập/SDK reject sửa amount/USD; SDK tạo/get/cancel với HTTPserver cục bộ và response signature; HTTP webhook không session/CSRF nhận signed payload, reject chữ ký sai/oversize; replay; thiếu→đủ; dư; trễ/hủy; receipt conflict; rollback receipt/invoice nhưng giữ inbox đểretry; API query cùng ref; timeout cùngcode và không giữtransaction khiSDKcall. Không có giao dịch PayOS thật trong các test.
- Browser QA `qa-checkout.cjs --payos-mock` pass: chọnmock, ghi thiếu rồi đủ, trạng thái đã thu, replay cùngđơn, hủy tạo yêu cầu hoàn. COD và QA storefront/auth/admin cũng pass. Đã xem ảnh payos-mock-mobile.png. Chưa có chứng cứ khóa/live webhook/API chuyển tiền thật hoặc Maps thật.
- start-local.ps1 nay package rồi chạy executableJAR để test/build nguồn không làm template thay đổi giữa lúc backend cũ còn chạy. Chạy lại script khi cập nhật source; dừng JAR của dự án trước khi package trênWindows. App bind127.0.0.1:8082.
- Bản cuối hiện chạy bằng JAR PID16640, exec session58898, log .local/app-payments-final.log. Đã chạy lại `qa-checkout.cjs --payos-mock` trên đúng JAR này và pass. Revalidate PID/commandline/HTTP trước khi tiếp tục; PID4584/1660 là lịch sử đã dừng.
- Còn cần admin tiếp nhận đơn, vận đơn/xuất kho/thu COD, trả hàng/nhận lại/hoàn tiền/chứng từ, PDF và các module CSKH/dashboard/mở rộng ở danh sách tổng. Không hoàn thành goal chỉ vì payment mock pass. Khi đổi code Java, dừng JAR trước khi package; không dừng PostgreSQL chung nếu vẫn đang dùng.

## Giao nhận/PDF — đã kiểm tra 08/11/2026

- V7 ứng dụng seed giao nhận nội bộ và kênh COD mô phỏng; đã áp dụng app/test. Java compile/package thành công.
- Admin đơn/chi tiết/xác nhận địa chỉ, tạo một vận đơn + manifest + cước gốc (unknown khác 0), bàn giao xuất kho và sổ biến động cùng transaction; sự kiện UUID chống lặp, chặn chuyển lùi; giao lại không xuất kho lần nữa. Giao thất bại tạo hàng chờ, admin nhận việc, giao lại/success đóng việc.
- COD giao thành công không ghi thu tự động. Thao tác thu riêng yêu cầu đúng tổng/chứng từ/checkbox; ghi receipt, đối soát, accepted session, invoice và hoàn thành cùng transaction, replay cùng chứng từ không cộng thu. PayOS đã thu hoàn thành khi giao thành công. Khách xem vận đơn/timeline riêng.
- PDFBox3.0.8 + Noto Sans Regular/Bold OFL đóng gói; owner/admin tải PDF private/no-store. Snapshot hóa đơn V2 dùng chung PayOS/COD, kiểm tổng và đủ tiền, không thay giá/địa chỉ đã chốt. PDF có dấu Việt, nhiều trang lặp đầu bảng, số trang và nhãn mô phỏng; đã render/xem và sửa đường kẻ sát chữ.
- 33 test pass (28 cũ +5 giao nhận/PDF), gồm oneparcel, bàn giao chống lặp/không bỏ bước, thất bại/giao lại/task, COD riêng/replay/ownership/snapshot, nhiều trang và lỗi invoice rollback toàn transaction. Chưa bổ sung concurrency riêng cho bàn giao.
- qa-fulfillment.cjs pass Edge1440/390/360: đặt COD→xác nhận→vận đơn→bàn giao→thất bại/nhận việc/giao lại→đã giao chưa thu→thuCOD→tảiPDF và từ chối khách khác. Đơn demo đã hoàn thành LM-510A18F4C5CD47DCA545883FCED4A5F5; dùng1máy demo. Lần QA lỗi template trước bàn giao đã hủy trả giữ; đã sửa option HTML sai rồi pass lại.
- JAR chạy PID4864, exec session20463, .local/app-fulfillment-final.log, HTTP8082 tại thời điểm QA; kiểm lại trước thao tác process. Source sau mốc này đang bổ sung returns/refunds, chưa nằm trong JAR đang chạy.

## Trả hàng/hoàn tiền — 08/11/2026

- V8 thêm ghi chú xử lý phiếu/khoản hoàn, nguồn đối soát QUAN_TRI và ghi chú riêng, unique tham chiếu hoàn thành công; đã áp dụng app/test. Không sửa schema thiết kế/migration đã áp dụng.
- Khách tạo/rút phiếu sau giao, mỗi đơn một phiếu mở; kiểm đúng dòng/owner và số mua còn lại. Admin tạo phiếu cả kiện giao thất bại; đơn chỉ hủy sau kho nhận kiểm. Duyệt/từ chối có ghi chú; phiếu được duyệt tạo vận đơn hoàn/manifest nội bộ và việc chờ kiểm.
- Kiểm một lần: tách số thực nhận, duyệt hoàn và nhập lại. Chỉ nhập lại số đủ điều kiện, ledger NHAP_HANG_TRA chống lặp; tiền hoàn tính chênh lệch làm tròn lũy kế trên giá ròng snapshot, phí giao riêng không vượt số đã trả. Khóa đơn và khoản thu để cấp hạn mức hoàn; COD chưa thu trả 0 tiền. Đơn sau giao giữ HOAN_THANH.
- Admin danh sách/chi tiết hoàn, ghi lỗi/thử lại cùng dòng, xác nhận đúng tiền/người nhận/tham chiếu/checkbox/chứng từ. Tệp PDF/PNG/JPEG <=5MB kiểm magic, tên UUID ngoài static, endpoint owner/admin/no-store; lỗi transaction dọn tệp chưa được tham chiếu. Xác nhận là ghi sổ thủ công, không gọi chuyển tiền. Dữ liệu mô phỏng gắn nhãn riêng.
- Hoàn thành khoản TRA_HANG/HUY_DON có hóa đơn phát hành chứng từ hoàn nội bộ riêng, không thay hóa đơn gốc. Đóng phiếu khi đủ khoản hoàn; thanh toán có DA_HOAN/HOAN_MOT_PHAN, không coi hoàn dư đã nhận đủ là mất tiền hàng.
- Duyệt thu dư dưới khóa chỉ khi đúng cờ thu dư, còn hạn/giữ tồn và principal còn đủ sau hạn mức hoàn; phát hành invoice và tiếp tục giữ phần dư trong hàng chờ. Xung đột tham chiếu vẫn chặn, chưa có thao tác sửa receipt/đẩy qua xung đột bằng tay.
- 39 tests pass (33 trước+6 trả/hoàn): trả nhiều đợt/đồng dư chính xác, nhập lại một phần, kiểm chống lặp, COD thất bại không hoàn tiền, owner/chứng từ, thu dư, từ chối/rút phiếu, rollback stock/refund/invoice và dọn file khi rollback. Bằng chứng ở .local/test-returns.log; không giao dịch ngoài.
- qa-returns.cjs pass trên Edge: trả đúng máy demo của qa-fulfillment, hoàn cả phí giao, nhập lại1máy, upload chứng từ mô phỏng, download owner và từ chối khách khác. Dùng invoice demo làm fixture tệp, không phải chứng từ ngân hàng thật. Không tiêu hao thêm tồn demo. Ảnh return-request/return-inspection/refund-form/returned-order trong .local/qa. Đã xem ảnh mobile và đang sửa bảng kết quả bị ép cột sang cuộn ngang.
- App hiện JAR PID21368, session86727, log .local/app-returns.log tại thời điểm QA. Lần khởi động trước bị bộ duyệt báo hết quota, không thực thi; người dùng yêu cầu continue, thử lại qua duyệt bình thường đã được phép và chạy thành công.
- Vẫn chưa có key PayOS/Maps, chưa nghiệm thu giao dịch/API thật. Còn các phần catalogue/CRUD/media, voucher/campaign, tài khoản nâng cao, CSKH/chat/analytics, OAuth/3D/voice/so sánh và kiểm thử cuối theo danh sách đầu tài liệu; không đánh dấu hoàn thành toàn bộ.

## Cước/Maps/PayOS phục hồi/danh mục — 08/11/2026

- Cước giao đi bổ sung cước gốc khi chưa biết, phụ phí/giảm cước có UUID chống lặp, không âm và không đổi phí khách đã chốt. Tác vụ5phút phát hiện quá hẹn, kiểm lại dưới khóa đơn/vận đơn, tạo một việc mở, giao thành công/retry đóng việc. Chưa có form cước chiều hoàn.
- Maps contract server cục bộ: VN ở đúng country type dù đổi thứ tự, bỏ PlaceID client, rounding7chữ số/hạn30ngày; từ chối quốc gia khác/status lỗi/JSON sai/HTTP503/thiếu xác nhận. BanDoService thêm constructor package-private cho contract, production vẫn gọi host Google cố định; chưa có khóa/API thật.
- V9 thêm mốc cooldown/query và lease tạo lại: mỗi phiên tối đa một lần query/cancel API trong5giây; retry create chỉ sau SDK NotFound HTTP404, qua lease30giây và kiểm lại không link/receipt/accepted và còn hạn/giữ tồn. Giữ nguyên orderCode/session, không gọi SDK trong transaction. Chỉ hỗ trợ bằng chứng HTTP404 typed; lỗi nghiệp vụ khác vẫn chờ đối soát/hủy. TestSDKHTTP404 và recovery cùngcode pass.
- APIquery kiểm tất cả receipt cùngcode/link, ref không lặp và tổng khớp amountPaid trước ghi sổ; phản hồi mâu thuẫn giữ cờ đối soát, không xác nhận khoản mới. Không hiển thị link trả tiền thật trên đơn đã đóng/đã nhận đủ hoặc trạng thái không còn chờ thanh toán.
- V10 thêm16ảnh local600px từ metadata/thư viện đúng trang nguồn, đã xem contact sheet và đổi các ảnh đại diện có bảng thông số sang ảnh máy. tai-lieu/21_NguonAnhSanPham.json ghi URL nguồn/ảnh/hash/kích thước. Màu ảnh tham khảo model, không cam kết trùng màu SKU. V11 Việt hóa đúng chuỗi dữ liệu mẫu ban đầu, không đổi snapshot đơn/invoice hoặc ghi đè dữ liệu mẫu đã sửa khác.
- Filter GET theo hãng/category/giá/RAM/storage/CPU/GPU/còn hàng/sort,12model/trang; count/giá/SKU gợi ý đều lấy cùng tập SKU khớp. Card dẫn đúng SKU giá đang hiển thị. Query wildcard được escape, sort whitelist/tie-break ổn định, min>max/type lỗi có thông báo tiếng Việt và giữ input; lỗi mobile giữ form mở. Gallery lazy ảnh/nguồn, placeholder vẫn cho model chưa có ảnh mới.
- 50test pass toàn bộ. Browser catalogue pass1440/390/360, xem ảnh desktop và bảng return mobile đã sửa cuộn ngang; qa-checkout --payos-mock pass lại trên JAR PID4772. QA return đã replay cùng phiếu thành công, không thêm tiền/tồn. Không chạy lại QAfulfillment chỉ để tạo thêm đơn đã giao.
- Bản đang chạy JAR PID4772/session56682/.local/app-catalogue-final.log; V1–V11 áp dụng cả app/test. Chưa goal complete vì phần CRUD/CSKH/dashboard/mở rộng phía trên còn thiếu, PayOS/Maps live chưa có khóa.

## Quản trị catalogue và media — 08/11/2026

- Thêm quản trị /quan-tri/model, /danh-muc/{phan-loai|thuong-hieu}, model/SKU create/edit/hide/stop. Không xóa lịch sử. Nội dung chung, nguồn HTTPS, bảo hành, thông số SKU, mã model hãng, giá/vốn/tồn và lý do cập nhật; SKU chưa thuộc đúng model bị từ chối. Model đổi danh mục khi có thuộc tính phụ bị chặn. LaDuLieuMau=false cho dữ liệu admin tạo; không tự khẳng định ngày đã đọc nguồn.
- Luồng SKU ghi initialprice history, tồn từ0 qua KhoService và ledger, cập nhật cùng transaction; chặn dưới lượng giữ. Test duplicate SKU rollback cả transaction tạo. Admin CRUD service kiểm quyền; mô tả công khai th:text, không chạy HTML. Nội dung chi tiết/bảo hành/nguồn model nay hiện trên trang sản phẩm.
- V12 thêm DangHienThi cho TepSanPham, áp dụng app/test. Quản trị upload/edit/source/order/scope/hide/reopen ảnh. KhoAnhSanPham tách private refund store; PNG/JPEG magic +ImageIO decode, <=5MB, chiều<=6000, điểmảnh<=16M, UUIDpath, rollback dọn file chưa tham chiếu. Ảnh private qua /tep-san-pham/{uuid}, chỉ public khi model/parent/SKU/ảnh phù hợp đang hiện, admin xem bản nháp. Ảnh riêng SKU ưu tiên trước ảnh chung; card dùng ảnh chung để không lấy ảnh màu SKU khác.
- 57tests fullpass; 7admin tests focusedpass sau bổ sung ưu tiên ảnh riêngSKU. Bao gồm CRUD/role, snapshot đơn, ledger/price history, duplicate rollback, nháp/public/ẩn ảnh, sai SKU/nonimage/size/dimension/traversal. Không có dữ liệu thử ghi vào appDB qua Java tests.
- qa-product-admin.cjs pass browser: create/edit/hide category+brand, model nháp+SKU, ảnh upload/edit/private→public/hide, literal script không chạy, chỉnhgiá/stock→0, ngừngSKU/ẩnmodel, mobile. App có fixture model17/sku25, qa-model-b853aaf71f15, đang ẩn/ngừng/tồn0; category/brand QA cũng ẩn. Không tạo thêm fixture nếu chỉ cần đọc/kiểm UI.
- Menu admin mobile dùng native details thu gọn, link active, bảng cuộn; đã xem ảnh admin-product-images-mobile và admin-models-desktop. Có error400/500; uploadoversize trước mapping dùng advice toàn cục riêng, đã sửa tình huống413trắng. qa-upload-errors pass sau sửa, tệp giả bị lỗi form và 6MB trả413cóthôngbáo.
- JAR sau media QA PID18220/session2756/.local/app-product-admin-final.log là mốc lịch sử; đang khởi động bản sửa Maps trong .local/app-maps-browser.log, cần xác minh PID/HTTP mới. Chưa đánh dấu toàn bộ goal hoàn thành.
- Ràsoát Maps: meta same-origin chặn Referrer của SDK khi key giới hạn website; đã đổi strict-origin-when-cross-origin cả meta và script. Async catch giữ button reference, authfail reject/re-enable thay vì treo promise; drag marker hỗ trợ lat/lng số hoặc hàm; geolocation guard map chưa có. qa-maps-contract.cjs đã pass trên JAR mới; requestGoogle được chặn, không gọi dịch vụ thật. Không có APIkey/giao dịch Google thật.

## Điểm tiếp tục sau mốc quản trị catalogue

- Bản cuối đang chạy PID17788/session48601/.local/app-maps-browser.log; HTTP8082 đã phục vụ QA Maps. Tất cả thay đổi Java/template/JS hiện nằm trong JAR này. Không dừng PostgreSQL khi cập nhật app; kiểm commandline PID trước khi dừng.
- Maps browser contract pass: fake key chỉ ở DOM, intercept Google request, kiểm Referer chỉ Origin và không lộ query token giả; countryVN/PlaceID/ward, click/drag literal và LatLng, reset pinconfirm khi sửa địa chỉ, US bị bỏ pin, GPS denial, SDKnetwork/authfailure re-enable button. Đây là test adapter/UX, không thay thế nghiệm thu Google thật.
- Đã xem ảnh admin mobile upload, bảng model desktop, catalogue desktop và bảng trả mobile. Ảnh seeded/CRUD đúng model hoặc fixture QA rõ tên, không tạo claim sản phẩm giả. QAuploaderrors dùng model17 hiện ẩn, không ghi ảnh lỗi. Không cần chạy lại CRUD nếu chỉ đọc UI; có thể dùng fixtureJSON để kiểm tra.
- Nghiên cứu bước kế tiếp: GioHangService.gop hiện bỏ qua SKU ngừng bán và các dòng vượt50, cắt số lượng99; BaoMatConfig success handler luôn xóa SESSION_KEY. Cần sửa để không mất lựa chọn khi login: SKU đã ẩn còn tồn tại nên có thể giữ DBcart và hiện không thể mua; phần chưa gộp do trần cần giữ riêng, thông báo và cho giảm/xóa/gộp lại, có kiểm owner nếu giữ trong session. Chưa viết mã sửa giỏ ở mốc này. Sau đó campaign/voucher admin và tài khoản nâng cao/CSKH/dashboard theo phạm vi còn lại.
- Goal vẫn active, chưa complete. Người dùng đã cho tiếp tục không chờ PayOS/Maps keys và cần hướng dẫn cấu hình sau; README/config example đã có. Không yêu cầu gửi khóa trong chat hoặc lặp lại câu hỏi đã trả lời.

## Cước hoàn và công việc giao nhận — 08/11/2026

- Dùng chung kiểm tra/sổ cước cho chiều giao và hoàn; khóa theo voucher→đơn→vận đơn, UUID chống lặp, một cước gốc, giảm không âm tổng. Chưa biết cước khác cước0; không thay phí khách trả, số tiền duyệt hoàn hoặc hóa đơn.
- Việc có danh sách mở/đã đóng, phân trang30; chi tiết/ghi chú lịch sử50/trang, nhận/đổi hạn/đóng. Chỉ người phụ trách hoặc người nhận việc trống được sửa; đã đóng không sửa tiếp. Ghi chú đầu vào giữ nguyên, ghi chú xử lý có actor/time trong audit.
- Đóng thủ công chỉ đóng việc theo dõi, không tự giao/nhập kho/thu/hoàn. Job không tạo lại cùng việc quá hẹn đã đóng khi vận đơn chưa cập nhật; cập nhật vận đơn mới hoặc lần giao thất bại mới có thể tạo việc mới.
- 16 test giao/hoàn tập trung pass, rồi 79 test toàn bộ pass. Browser `qa-shipping-tasks.cjs` pass1440/360: dùng lại phiếu/kiện hoàn đã xong, ghi cước gốc0/phụ phí1000 chống lặp; tạo một việc QA đối soát trên kiện cũ, ghi chú/đổi hạn/đóng/history. Không tạo đơn hay thay kho/receipt/refund. Fixture `.local/qa/shipping-task-fixture.json` để chạy lại không ghi phụ phí lặp.
- Đã xem ảnh mobile công việc/cước hoàn. Chiến dịch QA trống từ đợt lỗi form trước được tạm dừng; xác nhận modelQA công khai0, giữ tồnQA0.

## Đánh giá và rà soát nhãn — 08/11/2026

- Một khách/model, chỉ có bằng chứng dòng đơn thuộc chính khách và model, đơn HOAN_THANH và có phiên thu chấp nhận. Giao COD chưa ghi thu chưa được đánh giá. Cập nhật giữ bản chứng minh mua; model bị ẩn vẫn sửa được trong khu tài khoản đã mua nhưng trang public không truy cập được.
- Nội dung10–2000,1–5sao; văn bản escaped; public chỉ tên cuối của hồ sơ/nhãn đã mua, không email hay mã đơn. Tóm tắt/phân bố chỉ tính HIEN_THI. Admin phản hồi/ẩn/mở với lý do audit; owner sửa BI_AN không tự mở lại.
- 4 integration test đánh giá +1 lint template mới, full84pass. qa-reviews dùng lại khách/đơn hoàn thành đã trả, không tạo giao dịch mới; đã xem ảnh mobile public/admin; nhận xét QA được ẩn khi kết thúc.
- Khi xem ảnh phát hiện nhãn phụ trách/mã receipt có token literal; đã sửa và chạy lại QA giao nhận/đối soát. Rà tiếp mọi template phát hiện nhãn phần trăm/chế độ/ẩn đang có biến nằm ngoài biểu thức; chuyển sang biểu thức toàn phần, thêm lint tự động. qa-promotions kiểm12,34%/mã10% có trần; qa-checkout --payos-conflict kiểm PAYOS · Mô phỏng và mã receipt; đều pass.
- Quy tắc chiến dịch đã mất điều kiện do giá gốc thay đổi vẫn có thể tắt; quy tắc đang bật tiếp tục bị chặn mức giảm>=giá gốc. Test tập trung được gộp vào full84pass.
- Tiếp theo: yêu thích/theo dõi/thông báo (chưa code ở mốc này), thuộc tính/catalogue, CSKH/dashboard và mở rộng theo phạm vi.

## Theo dõi và thông báo — 08/11/2026

- Yêu thích dùng TheoDoiSanPham, không tạo bảng riêng: tối đa100/user, sửa tùy chọn hoặc bỏ; giá thấp nhất SKU công khai, phát hiện hết→có theo số khả dụng. Job5phút duyệt keyset tới mốc ID cố định, từng dòng một transaction; khách cũng kiểm tra ngay được. Ghi thông báo và cập nhật mốc cùng transaction, khóa theo dõi/chống lặp theo phiên; không giữ hàng/giá.
- Trung tâm thông báo theo đúng chủ, lọc chưa đọc/phân trang30, mở bằng POST/CSRF, đánh dấu tất cả. Đích chỉ whitelist đường dẫn nội bộ; dữ liệu cũ sai đích quay về trung tâm. Lưu ưu đãi công khai tạo một thông báo/user/mã, không giữ lượt voucher.
- Lịch sử đơn đi qua DonHangService.ghiLichSu, ghi lịch sử + thông báo cùng transaction; tạo/hủy, giao, thu, các bước trả/kiểm/hoàn đều có cập nhật. Replay nghiệp vụ không thêm lịch sử nên không gửi lặp. Thông báo của phiên mô phỏng có nhãn Mô phỏng. Phản hồi đánh giá đổi nội dung báo riêng cho chủ, không lặp cùng phản hồi; nội dung escaped.
- Khóa mutex NguoiDung đổi sang FOR NO KEY UPDATE: vẫn tuần tự các thao tác tài khoản nhưng tương thích FK key-share của audit/thông báo. Test hai transaction tái hiện user→follow và follow→notification FK đạt, cùng các test tranh tồn/voucher, auth và rollback toàn hệ thống. Tham khảo PostgreSQL17 explicit locking: https://www.postgresql.org/docs/17/explicit-locking.html . Không sửa khóa chính/email tài khoản trong các đường này.
- 24 test tập trung rồi full90pass; sau bổ sung nhãn mô phỏng và trả/hoàn, full90pass lần cuối `.local/test-notifications-final.log`. `qa-follow-notifications.cjs` pass: fixtureSKU25 từ hết→có, giảm10.000.003→9.000.003, đúng2thông báo, lặp không thêm, link đúngSKU, cấm đọc chéo, đã đọc/tất cả, lưuLUMA100hai lần chỉ một, bỏ chọn và bỏ theo dõi giữ lịch sử. Fixture về ẩn/ngừng/tồn0, không đặt đơn.
- Browser `qa-reviews` kiểm thêm thông báo phản hồi; `qa-checkout --payos-mock` kiểm thông báo đơn và nhãn mô phỏng; cả hai pass trên PID20668. Đã xem ảnh thông báo mobile.
- Chưa làm attrs/dòng sản phẩm/admin pagination còn giới hạn, chat/tri thức đã duyệt, banner/sự kiện/cấu hình/dashboard/analytics, OAuth/3D/voice/so sánh/đã xem/mua lại và nghiệm thu cuối. Không đặt complete.

## Dòng sản phẩm, thuộc tính và CSKH — 08/11/2026

- Dòng cùng hãng, định nghĩa thuộc tính theo danh mục và gán ba kiểu giá trị vào SKU; bảo vệ kiểu/danh mục/định nghĩa đang dùng. Lọc mở rộng và các bộ lọc cũ đều phải khớp cùng SKU, card/link/giá đúng cấu hình. Snapshot đơn giữ tên/kiểu/đơn vị/giá trị. Hướng dẫn `tai-lieu/23_CauHinhSanPhamVaBoLoc.md`.
- V17 chỉ thêm ràng buộc/chỉ mục chat và chủ đề/tài liệu mẫu NHAP; nội dung mẫu cần quản trị duyệt. Chat riêng, nguồn phiên bản duyệt, bàn giao/claim/release/close, kiểm duyệt, notification và rate limit/idempotency/rollback/concurrency đều có. Không cần AI key; bot cục bộ so khớp cụm hỏi và trả nguyên văn tài liệu đã duyệt. Hướng dẫn `tai-lieu/24_HoTroKhachHang.md`.
- Browser `qa-product-configuration.cjs` và `qa-support.cjs` pass, đã xem render mobile/desktop. Fixture QA được trả về ẩn/ngừng/tồn0; tài liệu QA ngừng dùng, chủ đề tắt và cuộc hỗ trợ đóng. Chưa coi phần OAuth/3D/voice/dashboard/banner/so sánh/mua lại là hoàn tất.
- Bản đang chạy PID25392 chứa thông số và chat. Source đang tiếp tục phân trang và sửa nav mobile, chưa đóng gói các thay đổi sau mốc này.

## Phân trang và cấu hình vận hành — 08/11/2026

- Danh sách model/SKU, đơn admin/khách, trả/hoàn, chiến dịch và mã giảm/public offer phân trang30; giữ điều kiện qua link, SKU editor đọc trực tiếp ID. Trang không hợp lệ trả400 có hướng dẫn. `PhanTrangIntegrationTest` kiểm hơn30 mục và ownership; browser `qa-pagination.cjs` pass bảy list admin, đơn/ưu đãi và query dấu cộng/ampersand. Nav mobile đã sửa để nhãn không xuống từng từ.
- `/quan-tri/cau-hinh` chỉ cho ba khóa công khai kiểu rõ: bật bot, phí VND nguyên0–10triệu, thông báo trang chủ≤300. Không nhận khóa/tên tùy ý. Lưu có audit; phí trên đơn cũ và replay giữ nguyên, đơn mới dùng phí mới.
- Bot tôn trọng `BAT_CHATBOT` (mặc định false của V7). Cuộc mới vào hàng chờ nếu tắt; cuộc cũ ở bot chuyển hàng chờ khi gửi tiếp. Test chat bật công tắc trong setup và khôi phục sau test, browser hỗ trợ cũng tạm bật rồi phục hồi.
- `qa-settings.cjs` và `qa-support.cjs` trên JAR settings pass; cấu hình sau QA được khôi phục, hội thoại QA đóng, nguồn ngừng và chủ đề tắt. 112 full test pass. Source đã có module banner/sự kiện và V18, focus8 test pass; đang chạy `.local/test-content-full.log`, pipeline session74963 sẽ thay JAR sau khi pass.

## Mốc đã xác minh 08/11/2026 15:36

- 122 full tests pass: `.local/test-3d-full.log`. App/test Flyway V19, migration tiếp theo V20+.
- JAR mới PID18236, exec session46121, log `.local/app-3d.log`, localhost8082. Các PID ở trên là lịch sử; xác minh commandline trước khi dừng.
- Browser `qa-choices.cjs` pass: so sánh4SKU, recent12model theo phiên, mua lại từ đơn QA có sẵn, dọn giỏ và không tạo đơn mới. `qa-support.cjs` bản polling pass, giữ bản nháp khi nhận tin tự động.
- `qa-3d-voice.cjs` pass: hai GLB thực tải/xoay/reset trên Edge WebGL phần mềm; lỗi GLB vẫn giữ ảnh/mua; SpeechRecognition giả lập kiểm nhập từ khóa/quyền/dừng/nhập tay. Chưa thử micro hoặc dịch vụ nhận dạng thật. Hai GLB tự tạo qua Khronos Validator0errors0warnings.
- Còn dashboard/analytics và OAuth Google/Facebook, nghiệm thu toàn phạm vi. Chưa đạt goal; không đánh dấu complete. PayOS/Maps thật vẫn chờ khóa người dùng đã cho cấu hình sau.
- Đã nghiên cứu tài liệu Spring Boot4 OAuth: starter `spring-boot-starter-security-oauth2-client`; chưa sửa pom hoặc triển khai OAuth. Không tự gộp tài khoản theo email.

## Dashboard bước đầu — 08/11/2026 16:34

- Thêm BaoCaoDAO/Service/Controller và template báo cáo. `/quan-tri` chuyển sang tổng quan, kho còn `/quan-tri/san-pham`. Lọc kỳ giờ VN và gộp ngày/tháng/năm; doanh số đơn hoàn thành, snapshot giá vốn, cash/hoàn tách chế độ, top30SKU, khách quay lại từ trước kỳ, phân bố sao, kho và trạng thái đơn. GET chỉ đọc với REPEATABLE_READ, kiểm role ở service.
- 3 focused tests pass `.local/test-reports-focus.log` (2integration+template), browser dashboard HTTP200/mobile không tràn, ảnh `.local/qa/reports-mobile.png`. Chưa có fixture tài chính tính tay đầy đủ, biểu đồ, tỷ lệ sao/quay lại, phân tích hành vi/heartbeat/ít tương tác hoặc xuất báo cáo; cần hoàn thiện tiếp.
- JAR hiện PID22272, session11078, `.local/app-reports.log`, localhost8082. Flyway vẫnV19; migration tiếpV20+. Bản trước full122pass; chưa chạy full sau dashboard.
- OAuth Google/Facebook chưa triển khai, pom chưa thêm dependency OAuth. Goal chưa đạt; tiếp tục analytics, OAuth, nghiệm thu cuối. Không hỏi lại khóa PayOS/Maps người dùng đã cho cấu hình sau.
