-- Refine brand-specific series without changing SKU identity, price, stock or snapshots.
DO $series$ DECLARE generation integer; line_id bigint; brand_id bigint; BEGIN
SELECT "MaThuongHieu" INTO brand_id FROM "ThuongHieu" WHERE "DuongDan"='apple';
FOR generation IN 13..17 LOOP
 INSERT INTO "DongSanPham" ("MaThuongHieu","TenDongSanPham","DuongDan") VALUES (brand_id,'iPhone '||generation||' Series','iphone-'||generation||'-series') ON CONFLICT ("MaThuongHieu","DuongDan") DO NOTHING;
 SELECT "MaDongSanPham" INTO line_id FROM "DongSanPham" WHERE "MaThuongHieu"=brand_id AND "DuongDan"='iphone-'||generation||'-series';
 UPDATE "SanPham" s SET "MaDongSanPham"=line_id,"NgayCapNhat"=now() WHERE s."MaThuongHieu"=brand_id AND s."LaDuLieuMau" AND s."TenSanPham" LIKE 'iPhone '||generation||'%' AND s."MaDongSanPham" IN (SELECT "MaDongSanPham" FROM "DongSanPham" WHERE "MaThuongHieu"=brand_id AND "DuongDan"='iphone');
END LOOP;
SELECT "MaThuongHieu" INTO brand_id FROM "ThuongHieu" WHERE "DuongDan"='vivo';
INSERT INTO "DongSanPham" ("MaThuongHieu","TenDongSanPham","DuongDan") VALUES (brand_id,'Y Series','y-series'),(brand_id,'V Series','v-series') ON CONFLICT ("MaThuongHieu","DuongDan") DO NOTHING;
UPDATE "SanPham" s SET "MaDongSanPham"=d."MaDongSanPham","NgayCapNhat"=now() FROM "DongSanPham" d WHERE s."MaThuongHieu"=brand_id AND d."MaThuongHieu"=brand_id AND s."LaDuLieuMau" AND ((s."TenSanPham" ~* '^vivo Y[0-9]' AND d."DuongDan"='y-series') OR (s."TenSanPham" ~* '^vivo V[0-9]' AND d."DuongDan"='v-series'));
UPDATE "SanPham" SET "MoTaNgan"=replace("MoTaNgan",'RAM 0 GB','RAM chưa được nguồn công bố') WHERE "DuongDan"='iphone-17-256gb';
END $series$;
COMMENT ON COLUMN "BienTheSanPham"."DungLuongRam" IS 'Dung lượng RAM GB; 0 khi không áp dụng với phụ kiện hoặc nguồn chưa công bố, không tự suy đoán.';
COMMENT ON COLUMN "BienTheSanPham"."DungLuongLuuTru" IS 'Dung lượng lưu trữ GB; 0 khi không áp dụng với phụ kiện.';
COMMENT ON COLUMN "BienTheSanPham"."KichThuocManHinh" IS 'Kích thước inch; 0 khi không áp dụng với phụ kiện.';
