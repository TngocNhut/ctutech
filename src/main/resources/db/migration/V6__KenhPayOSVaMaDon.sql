CREATE SEQUENCE "MaDonPayOSSeq" START WITH 10000001 MAXVALUE 9007199254740991 NO CYCLE;
INSERT INTO "KenhThanhToan" ("MaKenhThanhToan","NhaCungCap","CheDoXuLy","TenHienThi","DangHoatDong")
VALUES ('PAYOS_LUMA','PAYOS','TICH_HOP_THAT','PayOS',true),
       ('PAYOS_LUMA_MOCK','PAYOS','MO_PHONG','PayOS mô phỏng (không chuyển tiền)',true);
