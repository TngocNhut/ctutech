ALTER TABLE "TinNhan" ADD CONSTRAINT "KiemTra_TinNhan_DoDai" CHECK (char_length(btrim("NoiDung")) BETWEEN 1 AND 2000);
CREATE INDEX "ChiMuc_TinNhan_GioiHanGui" ON "TinNhan" ("MaNguoiGui", "NgayTao" DESC);
CREATE INDEX "ChiMuc_CuocTroChuyen_HangCho" ON "CuocTroChuyen" ("TrangThai", "CheDoHoTro", "NgayCapNhat" DESC);
INSERT INTO "ChuDeHoTro" ("KyHieu","TenChuDe","ViDuCauHoi","ChuyenNhanVien") VALUES
('HUONG_DAN_DAT_HANG','Hướng dẫn đặt hàng','["cach dat hang","huong dan dat hang","mua hang nhu the nao"]',false),
('HUONG_DAN_DIA_CHI','Chọn địa chỉ giao hàng','["chon dia chi giao hang","su dung ban do","nhap dia chi thu cong"]',false),
('THANH_TOAN_CO_BAN','Phương thức thanh toán','["phuong thuc thanh toan","thanh toan payos","thanh toan cod"]',false),
('TRA_CUU_CA_NHAN','Đơn hàng và yêu cầu cá nhân','["kiem tra don hang","tra cuu don hang","hoan tien cua toi","don hang cua toi"]',true)
ON CONFLICT ("KyHieu") DO NOTHING;
INSERT INTO "TaiLieuHoTro" ("MaChuDeHoTro","SoPhienBan","TieuDe","NoiDungDaSoan")
SELECT "MaChuDeHoTro",1,"TenChuDe",CASE "KyHieu"
 WHEN 'HUONG_DAN_DAT_HANG' THEN 'Chọn model và cấu hình còn hàng, thêm vào giỏ, đăng nhập và xác minh email. Tại bước đặt hàng, kiểm tra người nhận, địa chỉ, số lượng và tổng tiền trước khi xác nhận. Giỏ hàng chưa giữ tồn; kết quả đặt hàng được máy chủ kiểm tra lại.'
 WHEN 'HUONG_DAN_DIA_CHI' THEN 'Bạn có thể nhập địa chỉ bằng tay tại bước đặt hàng hoặc chọn địa chỉ đã lưu. Nếu bản đồ khả dụng, chọn vị trí tại Việt Nam và xác nhận điểm giao sau khi kiểm tra. Khi bản đồ không mở được, tiếp tục bằng địa chỉ nhập tay đầy đủ.'
 ELSE 'Cửa hàng demo hỗ trợ COD và PayOS khi được cấu hình. Giao hàng thành công và thu COD là hai bước riêng. Với PayOS, chỉ trạng thái đã xác minh từ máy chủ xác nhận khoản thu; quay về website chưa chứng minh đã trả tiền. Phiên mô phỏng có nhãn Mô phỏng và không chuyển tiền thật.' END
FROM "ChuDeHoTro" WHERE "KyHieu" IN ('HUONG_DAN_DAT_HANG','HUONG_DAN_DIA_CHI','THANH_TOAN_CO_BAN')
ON CONFLICT ("MaChuDeHoTro","SoPhienBan") DO NOTHING;
-- Các nội dung mẫu ở trạng thái NHAP. Quản trị đọc và duyệt trước khi bot được sử dụng.
