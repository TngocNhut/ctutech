CREATE TABLE "CauHinhPayOSMaHoa" (
    "MaCauHinh" smallint PRIMARY KEY DEFAULT 1 CHECK ("MaCauHinh" = 1),
    "Nonce" bytea NOT NULL CHECK (octet_length("Nonce") = 12),
    "BanMa" bytea NOT NULL,
    "MaNguoiCapNhat" bigint REFERENCES "NguoiDung" ON DELETE SET NULL,
    "NgayCapNhat" timestamptz NOT NULL DEFAULT now()
);

COMMENT ON TABLE "CauHinhPayOSMaHoa" IS 'Cấu hình kết nối PayOS được mã hóa bằng AES-256-GCM; khóa giải mã chỉ lưu trong biến môi trường của ứng dụng.';
COMMENT ON COLUMN "CauHinhPayOSMaHoa"."Nonce" IS 'Nonce ngẫu nhiên 96 bit, duy nhất cho lần mã hóa hiện tại.';
COMMENT ON COLUMN "CauHinhPayOSMaHoa"."BanMa" IS 'Payload cấu hình đã mã hóa và xác thực toàn vẹn; không lưu bí mật dạng rõ.';
