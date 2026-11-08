-- Mô hình hình học minh họa do dự án tự tạo, không phải bản dựng chính xác sản phẩm có thương hiệu.
INSERT INTO "TepSanPham" ("MaSanPham","LoaiTep","DuongDanTep","MoTaThayThe","ThuTu","ThongTinSuDung","DangHienThi","NgayCapNhat")
SELECT "MaSanPham",'MO_HINH_3D','/media/models/demo-phone.glb','Mô hình minh họa hình khối điện thoại',0,'Mô hình do dự án tạo bằng scripts/generate-demo-models.py; không dùng mô hình bên thứ ba. Hình dáng, màu và chi tiết không đại diện chính xác cho model hoặc SKU đang chọn.',true,now()
FROM "SanPham" WHERE "DuongDan"='iphone-13';
INSERT INTO "TepSanPham" ("MaSanPham","LoaiTep","DuongDanTep","MoTaThayThe","ThuTu","ThongTinSuDung","DangHienThi","NgayCapNhat")
SELECT "MaSanPham",'MO_HINH_3D','/media/models/demo-laptop.glb','Mô hình minh họa hình khối laptop',0,'Mô hình do dự án tạo bằng scripts/generate-demo-models.py; không dùng mô hình bên thứ ba. Hình dáng, màu và chi tiết không đại diện chính xác cho model hoặc SKU đang chọn.',true,now()
FROM "SanPham" WHERE "DuongDan"='macbook-air-m2';
