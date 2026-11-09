-- Move accessory groups to real categories while retaining SKU attributes.
INSERT INTO "PhanLoai" ("TenPhanLoai","DuongDan","ThuTu","DangHienThi","NgayCapNhat") VALUES
 ('Tai nghe','tai-nghe',3,true,now()),('Chuột và bàn phím','chuot-ban-phim',4,true,now())
ON CONFLICT ("DuongDan") DO NOTHING;

CREATE TEMP TABLE "ChuyenDanhMuc" ON COMMIT DROP AS
SELECT s."MaSanPham", CASE
 WHEN lower(s."TenTimKiem") LIKE '%tai nghe%' OR EXISTS (SELECT 1 FROM "GiaTriThuocTinhBienThe" v JOIN "ThuocTinhDanhMuc" a USING ("MaThuocTinh") WHERE v."MaSanPham"=s."MaSanPham" AND a."KyHieu"='LOAI_PHU_KIEN' AND v."GiaTriChu" ILIKE 'Tai nghe%') THEN 'tai-nghe'
 WHEN lower(s."TenTimKiem") LIKE '%chuot%' OR lower(s."TenTimKiem") LIKE '%ban phim%' OR EXISTS (SELECT 1 FROM "GiaTriThuocTinhBienThe" v JOIN "ThuocTinhDanhMuc" a USING ("MaThuocTinh") WHERE v."MaSanPham"=s."MaSanPham" AND a."KyHieu"='LOAI_PHU_KIEN' AND v."GiaTriChu" IN ('Chuột','Bàn phím')) THEN 'chuot-ban-phim'
 END AS slug
FROM "SanPham" s JOIN "PhanLoai" p USING ("MaPhanLoai") WHERE p."DuongDan"='phu-kien';
DELETE FROM "ChuyenDanhMuc" WHERE slug IS NULL;

INSERT INTO "ThuocTinhDanhMuc" ("MaPhanLoai","KyHieu","TenThuocTinh","KieuDuLieu","DonVi","ChoPhepLoc","ThuTu")
SELECT dest."MaPhanLoai",a."KyHieu",a."TenThuocTinh",a."KieuDuLieu",a."DonVi",a."ChoPhepLoc",a."ThuTu"
FROM "ThuocTinhDanhMuc" a JOIN "PhanLoai" src ON src."MaPhanLoai"=a."MaPhanLoai" AND src."DuongDan"='phu-kien'
CROSS JOIN "PhanLoai" dest WHERE dest."DuongDan" IN ('tai-nghe','chuot-ban-phim')
ON CONFLICT ("MaPhanLoai","KyHieu") DO NOTHING;

CREATE TEMP TABLE "GiaTriChuyenDanhMuc" ON COMMIT DROP AS
SELECT v.*,a."KyHieu",c.slug FROM "GiaTriThuocTinhBienThe" v JOIN "ThuocTinhDanhMuc" a USING ("MaThuocTinh") JOIN "ChuyenDanhMuc" c USING ("MaSanPham");
DELETE FROM "GiaTriThuocTinhBienThe" v USING "ChuyenDanhMuc" c WHERE v."MaSanPham"=c."MaSanPham";
UPDATE "SanPham" s SET "MaPhanLoai"=p."MaPhanLoai" FROM "ChuyenDanhMuc" c JOIN "PhanLoai" p ON p."DuongDan"=c.slug WHERE s."MaSanPham"=c."MaSanPham";
INSERT INTO "GiaTriThuocTinhBienThe" ("MaBienThe","MaSanPham","MaPhanLoai","MaThuocTinh","KieuDuLieu","GiaTriChu","GiaTriSo","GiaTriCoKhong")
SELECT v."MaBienThe",v."MaSanPham",p."MaPhanLoai",a."MaThuocTinh",v."KieuDuLieu",v."GiaTriChu",v."GiaTriSo",v."GiaTriCoKhong"
FROM "GiaTriChuyenDanhMuc" v JOIN "PhanLoai" p ON p."DuongDan"=v.slug JOIN "ThuocTinhDanhMuc" a ON a."MaPhanLoai"=p."MaPhanLoai" AND a."KyHieu"=v."KyHieu" AND a."KieuDuLieu"=v."KieuDuLieu";

-- Remove the identified QA category only when no product or attribute refers to it.
DELETE FROM "PhanLoai" p WHERE p."DuongDan"='qa-phan-loai-b853aaf71f15' AND NOT p."DangHienThi"
AND NOT EXISTS (SELECT 1 FROM "SanPham" s WHERE s."MaPhanLoai"=p."MaPhanLoai")
AND NOT EXISTS (SELECT 1 FROM "ThuocTinhDanhMuc" a WHERE a."MaPhanLoai"=p."MaPhanLoai");
