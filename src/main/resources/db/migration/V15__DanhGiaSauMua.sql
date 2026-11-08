ALTER TABLE "DanhGia" ADD CONSTRAINT "KiemTra_DanhGia_TrangThai"
    CHECK ("TrangThai" IN ('HIEN_THI','BI_AN'));
ALTER TABLE "DanhGia" ADD CONSTRAINT "KiemTra_DanhGia_DoDai"
    CHECK (length(btrim("NoiDung")) BETWEEN 10 AND 2000 AND ("PhanHoiQuanTri" IS NULL OR length("PhanHoiQuanTri")<=2000));
