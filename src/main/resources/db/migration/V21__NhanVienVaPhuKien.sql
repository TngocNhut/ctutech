INSERT INTO "VaiTro" ("TenVaiTro","MoTa","NgayCapNhat") VALUES ('NhanVien','Nhân viên vận hành sản phẩm, kho và đơn hàng',now()) ON CONFLICT ("TenVaiTro") DO NOTHING;
INSERT INTO "PhanLoai" ("TenPhanLoai","DuongDan","MoTa","ThuTu","DangHienThi","NgayCapNhat") VALUES ('Phụ kiện','phu-kien','Tai nghe, sạc, cáp, đầu chuyển, bàn phím và chuột',3,true,now()) ON CONFLICT ("DuongDan") DO NOTHING;
