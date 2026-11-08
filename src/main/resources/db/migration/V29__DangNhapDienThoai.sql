-- Số điện thoại xác minh có thể là danh tính đăng nhập độc lập.
ALTER TABLE "NguoiDung" ALTER COLUMN "ThuDienTu" DROP NOT NULL;
ALTER TABLE "NguoiDung" ADD COLUMN "NgayXacMinhDienThoai" timestamptz;
ALTER TABLE "NguoiDung" ADD COLUMN "NgaySinh" date;
ALTER TABLE "NguoiDung" ADD CONSTRAINT "NguoiDung_CoDanhTinh" CHECK ("ThuDienTu" IS NOT NULL OR "SoDienThoai" IS NOT NULL);
CREATE UNIQUE INDEX "NguoiDung_DienThoaiDaXacMinh" ON "NguoiDung" ("SoDienThoai") WHERE "NgayXacMinhDienThoai" IS NOT NULL;
ALTER TABLE "DonHang" ALTER COLUMN "ThuDienTuNhan" DROP NOT NULL;
