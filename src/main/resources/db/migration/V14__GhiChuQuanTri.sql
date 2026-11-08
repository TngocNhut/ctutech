ALTER TABLE "NhatKyQuanTri" ADD COLUMN "GhiChu" varchar(255);
COMMENT ON COLUMN "NhatKyQuanTri"."GhiChu" IS 'Lý do nghiệp vụ do quản trị nhập, chỉ hiển thị trong nhật ký quản trị. Không nhập mật khẩu, khóa hoặc mã xác thực.';
