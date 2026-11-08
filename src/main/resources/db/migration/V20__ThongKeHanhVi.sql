ALTER TABLE "PhienTruyCap" ADD CONSTRAINT "KiemTra_PhienTruyCap_Giay" CHECK ("SoGiayHoatDong" BETWEEN 0 AND 28800);
ALTER TABLE "TuongTacSanPham" ADD COLUMN "MaBienThe" bigint REFERENCES "BienTheSanPham";
ALTER TABLE "TuongTacSanPham" ADD CONSTRAINT "KhoaNgoai_TuongTac_DungSku" FOREIGN KEY ("MaSanPham","MaBienThe") REFERENCES "BienTheSanPham" ("MaSanPham","MaBienThe");
ALTER TABLE "TuongTacSanPham" ADD CONSTRAINT "KiemTra_TuongTac_Loai" CHECK ("LoaiTuongTac" IN ('XEM','THEM_GIO','THEO_DOI'));
CREATE INDEX "ChiMuc_PhienTruyCap_DonDep" ON "PhienTruyCap" ("NgayTao");
CREATE INDEX "ChiMuc_TuongTacSanPham_Ky" ON "TuongTacSanPham" ("MaSanPham","NgayTao","LoaiTuongTac");
