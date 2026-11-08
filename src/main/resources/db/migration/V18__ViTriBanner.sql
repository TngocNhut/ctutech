INSERT INTO "ViTriHienThi" ("KyHieu","TenViTri") VALUES ('TRANG_CHU','Trang chủ và danh mục'),('UU_DAI','Trang ưu đãi') ON CONFLICT ("KyHieu") DO NOTHING;
CREATE INDEX "ChiMuc_SuKien_CongBo" ON "SuKien" ("NgayBatDau" DESC,"MaSuKien" DESC) WHERE "TrangThai"='CONG_BO';
