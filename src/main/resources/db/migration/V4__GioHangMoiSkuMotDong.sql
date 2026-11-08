-- UNIQUE(MaGioHang,MaBienThe) đã có từ V7; chỉ bổ sung trần số lượng UI.
ALTER TABLE "ChiTietGioHang" ADD CONSTRAINT "GioHang_SoLuongToiDa" CHECK ("SoLuong" <= 99);
