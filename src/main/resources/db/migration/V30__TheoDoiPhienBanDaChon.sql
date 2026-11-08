ALTER TABLE "TheoDoiSanPham" ADD COLUMN "MaBienTheDaChon" bigint REFERENCES "BienTheSanPham" ("MaBienThe") ON DELETE SET NULL;
