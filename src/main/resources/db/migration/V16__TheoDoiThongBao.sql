ALTER TABLE "TheoDoiSanPham" ADD COLUMN "CoHangThamChieu" boolean NOT NULL DEFAULT false;
ALTER TABLE "TheoDoiSanPham" ADD COLUMN "PhienThongBao" bigint NOT NULL DEFAULT 0 CHECK ("PhienThongBao">=0);
COMMENT ON COLUMN "TheoDoiSanPham"."CoHangThamChieu" IS 'Có ít nhất một SKU công khai còn số lượng khả dụng ở lần lưu/kiểm tra gần nhất; phát hiện chuyển hết sang có hàng.';
COMMENT ON COLUMN "TheoDoiSanPham"."PhienThongBao" IS 'Phiên thay đổi quan sát, dùng tạo khóa thông báo chống lặp theo dòng theo dõi.';
