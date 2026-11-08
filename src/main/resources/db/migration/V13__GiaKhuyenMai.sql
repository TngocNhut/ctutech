-- Một cách tính giá dùng chung cho danh mục, giỏ và chốt đơn.
-- Ưu tiên lớn hơn thắng; cùng ưu tiên chọn giá thấp hơn rồi mã chiến dịch nhỏ hơn.
CREATE VIEW "GiaKhuyenMaiBienThe" AS
WITH moc AS MATERIALIZED (SELECT clock_timestamp() AS luc)
SELECT b."MaBienThe", b."GiaBan" AS "GiaGoc",
       coalesce(k.gia, b."GiaBan") AS "GiaHieuLuc",
       k."MaChuongTrinh", k."TenChuongTrinh", k."ChoPhepKemMaGiamGia"
FROM "BienTheSanPham" b CROSS JOIN moc
LEFT JOIN LATERAL (
    SELECT c."MaChuongTrinh", c."TenChuongTrinh", c."ChoPhepKemMaGiamGia", v.gia
    FROM "SanPhamKhuyenMai" a JOIN "ChuongTrinhKhuyenMai" c USING ("MaChuongTrinh")
    CROSS JOIN LATERAL (SELECT CASE a."KieuGiam"
        WHEN 'PHAN_TRAM' THEN b."GiaBan" - floor(b."GiaBan" * a."GiaTriGiam" / 100)
        ELSE b."GiaBan" - a."GiaTriGiam" END AS gia) v
    WHERE a."MaBienThe"=b."MaBienThe" AND a."DangHoatDong" AND c."TrangThai"='CONG_BO'
      AND c."NgayBatDau"<=moc.luc AND c."NgayKetThuc">moc.luc
      AND v.gia>0 AND v.gia<b."GiaBan"
    ORDER BY c."DoUuTien" DESC,v.gia,c."MaChuongTrinh"
    LIMIT 1
) k ON true;
COMMENT ON VIEW "GiaKhuyenMaiBienThe" IS 'Giá VND hiện hành: tối đa một chiến dịch/SKU, giảm phần trăm làm tròn xuống số tiền giảm; không trả giá bằng 0. Chốt đơn lưu riêng ApDungKhuyenMai.';
