INSERT INTO "VaiTro" ("TenVaiTro", "MoTa", "NgayCapNhat")
VALUES ('KhachHang', 'Khách mua hàng', now()), ('QuanTri', 'Quản trị cửa hàng', now())
ON CONFLICT ("TenVaiTro") DO NOTHING;
