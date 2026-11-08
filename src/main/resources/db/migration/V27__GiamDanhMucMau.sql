-- Remove unused demo products while preserving customer and stock history.
CREATE TEMP TABLE catalogue_remove ON COMMIT DROP AS
WITH eligible AS (
 SELECT s."MaSanPham",p."DuongDan" AS category,
        row_number() OVER (PARTITION BY p."DuongDan",s."MaThuongHieu" ORDER BY s."MaSanPham" DESC) AS brand_rank
 FROM "SanPham" s JOIN "PhanLoai" p USING ("MaPhanLoai")
 WHERE p."DuongDan" IN ('dien-thoai','laptop','phu-kien')
   AND s."LaDuLieuMau" AND s."DangHienThi"
   AND EXISTS (SELECT 1 FROM "BienTheSanPham" b WHERE b."MaSanPham"=s."MaSanPham" AND b."MaHang" LIKE 'LUMA-NEW24-%')
   AND NOT EXISTS (SELECT 1 FROM "BienTheSanPham" b WHERE b."MaSanPham"=s."MaSanPham" AND b."MaHang" NOT LIKE 'LUMA-NEW24-%')
   AND NOT EXISTS (SELECT 1 FROM "BienTheSanPham" b JOIN "ChiTietDonHang" x USING ("MaBienThe") WHERE b."MaSanPham"=s."MaSanPham")
   AND NOT EXISTS (SELECT 1 FROM "BienTheSanPham" b JOIN "ChiTietGioHang" x USING ("MaBienThe") WHERE b."MaSanPham"=s."MaSanPham")
   AND NOT EXISTS (SELECT 1 FROM "BienTheSanPham" b JOIN "BienDongKho" x USING ("MaBienThe") WHERE b."MaSanPham"=s."MaSanPham")
   AND NOT EXISTS (SELECT 1 FROM "BienTheSanPham" b JOIN "LichSuGiaBienThe" x USING ("MaBienThe") WHERE b."MaSanPham"=s."MaSanPham")
   AND NOT EXISTS (SELECT 1 FROM "BienTheSanPham" b JOIN "SanPhamKhuyenMai" x USING ("MaBienThe") WHERE b."MaSanPham"=s."MaSanPham")
   AND NOT EXISTS (SELECT 1 FROM "DanhGia" x WHERE x."MaSanPham"=s."MaSanPham")
   AND NOT EXISTS (SELECT 1 FROM "TheoDoiSanPham" x WHERE x."MaSanPham"=s."MaSanPham")
   AND NOT EXISTS (SELECT 1 FROM "TuongTacSanPham" x WHERE x."MaSanPham"=s."MaSanPham")
), ranked AS (SELECT *,row_number() OVER (PARTITION BY category ORDER BY brand_rank,"MaSanPham" DESC) AS n FROM eligible)
SELECT "MaSanPham",category FROM ranked WHERE n<=40;
DO $$ BEGIN
 IF (SELECT count(*) FROM catalogue_remove) <> 120 OR EXISTS (SELECT 1 FROM catalogue_remove GROUP BY category HAVING count(*)<>40) THEN
   RAISE EXCEPTION 'Không đủ sản phẩm demo không có giao dịch để xóa: cần 40 mẫu mỗi danh mục';
 END IF;
END $$;
DELETE FROM "TepSanPham" WHERE "MaSanPham" IN (SELECT "MaSanPham" FROM catalogue_remove);
DELETE FROM "GiaTriThuocTinhBienThe" WHERE "MaSanPham" IN (SELECT "MaSanPham" FROM catalogue_remove);
DELETE FROM "TonKho" WHERE "MaBienThe" IN (SELECT b."MaBienThe" FROM "BienTheSanPham" b JOIN catalogue_remove r USING ("MaSanPham"));
DELETE FROM "BienTheSanPham" WHERE "MaSanPham" IN (SELECT "MaSanPham" FROM catalogue_remove);
DELETE FROM "SanPham" WHERE "MaSanPham" IN (SELECT "MaSanPham" FROM catalogue_remove);
