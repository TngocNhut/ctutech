-- Sản phẩm thử thanh toán PayOS, có thể ẩn trong quản trị sau khi dùng.
DO $$
DECLARE product_id bigint; sku_id bigint; category_id bigint; brand_id bigint; warehouse_id bigint;
BEGIN
 SELECT "MaPhanLoai" INTO STRICT category_id FROM "PhanLoai" WHERE "DuongDan"='phu-kien';
 SELECT "MaThuongHieu" INTO brand_id FROM "ThuongHieu" WHERE "DangHienThi" ORDER BY "MaThuongHieu" LIMIT 1;
 SELECT "MaKhoHang" INTO warehouse_id FROM "KhoHang" WHERE "KyHieu"='KHO_MAC_DINH' AND "DangHoatDong";
 IF brand_id IS NULL OR warehouse_id IS NULL THEN RAISE EXCEPTION 'Thiếu thương hiệu hoặc kho mặc định hoạt động'; END IF;
 INSERT INTO "SanPham" ("MaPhanLoai","MaThuongHieu","TenSanPham","DuongDan","TenTimKiem","MoTaNgan","MoTaChiTiet","SoThangBaoHanh","ThongSoChung","LaDuLieuMau","DangHienThi","NgayCapNhat")
 VALUES (category_id,brand_id,'Sản phẩm kiểm tra thanh toán','san-pham-kiem-tra-thanh-toan','san pham kiem tra thanh toan','Sản phẩm dùng để kiểm tra thanh toán PayOS.','Sản phẩm dùng để kiểm tra thanh toán; không phải hàng hóa giao thực tế.',0,'{}'::jsonb,true,true,now())
 ON CONFLICT ("DuongDan") DO NOTHING;
 SELECT "MaSanPham" INTO STRICT product_id FROM "SanPham" WHERE "DuongDan"='san-pham-kiem-tra-thanh-toan';
 INSERT INTO "BienTheSanPham" ("MaSanPham","MaHang","TenBienThe","MauSac","BoXuLy","DungLuongRam","DungLuongLuuTru","KichThuocManHinh","HeDieuHanh","ThongSoBoSung","GiaNiemYet","GiaBan","GiaVon","DangKinhDoanh","NgayCapNhat")
 VALUES (product_id,'CTU-PAYOS-TEST-20000','Kiểm tra thanh toán','Không áp dụng','Không áp dụng',0,0,0,'Không áp dụng','{}'::jsonb,20000,20000,0,true,now()) ON CONFLICT ("MaHang") DO NOTHING;
 SELECT "MaBienThe" INTO STRICT sku_id FROM "BienTheSanPham" WHERE "MaHang"='CTU-PAYOS-TEST-20000' AND "MaSanPham"=product_id;
 INSERT INTO "TonKho" ("MaKhoHang","MaBienThe","SoLuongTon","SoLuongGiu","NgayCapNhat") VALUES (warehouse_id,sku_id,10,0,now()) ON CONFLICT ("MaKhoHang","MaBienThe") DO NOTHING;
END $$;
