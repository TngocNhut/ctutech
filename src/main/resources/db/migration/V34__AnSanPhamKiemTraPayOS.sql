-- Hide the temporary checkout item while retaining order history.
UPDATE "SanPham" SET "DangHienThi"=false,"NgayCapNhat"=now() WHERE "DuongDan"='san-pham-kiem-tra-thanh-toan';
