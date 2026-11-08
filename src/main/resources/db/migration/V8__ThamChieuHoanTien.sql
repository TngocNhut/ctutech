-- Một mã chứng từ đối soát chỉ được dùng cho một khoản hoàn thành công.
ALTER TABLE "HoanTien" ADD COLUMN "GhiChuXuLy" varchar(500);
ALTER TABLE "PhieuTraHang" ADD COLUMN "GhiChuXuLy" varchar(500);
ALTER TABLE "LanDoiSoatThanhToan" ADD COLUMN "GhiChuQuanTri" varchar(500);
ALTER TABLE "LanDoiSoatThanhToan" DROP CONSTRAINT "LanDoiSoatThanhToan_NguonKiemTra_check";
ALTER TABLE "LanDoiSoatThanhToan" ADD CONSTRAINT "KiemTra_LanDoiSoatThanhToan_Nguon"
    CHECK ("NguonKiemTra" IN ('PAYOS_API','COD_NOI_BO','MO_PHONG','QUAN_TRI'));
CREATE UNIQUE INDEX "DuyNhat_HoanTien_ThamChieuThanhCong" ON "HoanTien" ("MaThamChieuHoan")
    WHERE "TrangThai"='THANH_CONG' AND "MaThamChieuHoan" IS NOT NULL;
