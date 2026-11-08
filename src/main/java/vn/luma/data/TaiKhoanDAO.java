package vn.luma.data;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.model.TaiKhoan;

@Repository
public class TaiKhoanDAO {
    private final JdbcClient jdbc;
    public TaiKhoanDAO(JdbcClient jdbc) { this.jdbc=jdbc; }
    public Optional<TaiKhoan> tim(String email) {
        return jdbc.sql("""
            SELECT n.*, v."TenVaiTro" FROM "NguoiDung" n
            JOIN "VaiTro" v USING ("MaVaiTro") WHERE (n."ThuDienTu"=:email OR (n."SoDienThoai"=:email AND n."NgayXacMinhDienThoai" IS NOT NULL))
            """).param("email",email).query((r,i)->new TaiKhoan(r.getLong("MaNguoiDung"),
                r.getString("HoTen"),r.getString("ThuDienTu"),r.getString("SoDienThoai"),
                (r.getObject("NgayXacMinhThuDienTu")!=null || r.getObject("NgayXacMinhDienThoai")!=null),r.getString("TenVaiTro"),r.getString("TrangThai"))).optional();
    }
    public long tao(String ten,String email,String bam,String role,boolean verified) {
        return jdbc.sql("""
            INSERT INTO "NguoiDung" ("MaVaiTro","HoTen","ThuDienTu","MatKhauBam","TrangThai","NgayCapNhat","NgayXacMinhThuDienTu")
            SELECT "MaVaiTro",:ten,:email,:bam,'HOAT_DONG',now(),:verified FROM "VaiTro" WHERE "TenVaiTro"=:role
            RETURNING "MaNguoiDung"
            """).param("ten",ten).param("email",email).param("bam",bam).param("role",role)
            .param("verified",verified?OffsetDateTime.now():null,java.sql.Types.TIMESTAMP_WITH_TIMEZONE).query(Long.class).single();
    }
    public long taoKhachChuaXacMinh(String ten,String email,java.time.LocalDate sinh,String hash) {
        return jdbc.sql("""
            INSERT INTO "NguoiDung" ("MaVaiTro","HoTen","ThuDienTu","NgaySinh","MatKhauBam","TrangThai","NgayCapNhat")
            SELECT "MaVaiTro",:ten,:email,:sinh,:hash,'HOAT_DONG',now() FROM "VaiTro" WHERE "TenVaiTro"='KhachHang' RETURNING "MaNguoiDung"
            """).param("ten",ten).param("email",email).param("sinh",sinh).param("hash",hash).query(Long.class).single();
    }
    public long taoDaXacMinh(String ten,String email,String phone,java.time.LocalDate sinh,String hash) {
        return jdbc.sql("""
            INSERT INTO "NguoiDung" ("MaVaiTro","HoTen","ThuDienTu","SoDienThoai","NgaySinh","MatKhauBam","TrangThai","NgayCapNhat","NgayXacMinhThuDienTu","NgayXacMinhDienThoai")
            SELECT "MaVaiTro",:ten,:email,:phone,:sinh,:hash,'HOAT_DONG',now(),CASE WHEN :email IS NOT NULL THEN now() END,CASE WHEN :phone IS NOT NULL THEN now() END
            FROM "VaiTro" WHERE "TenVaiTro"='KhachHang' RETURNING "MaNguoiDung"
            """).param("ten",ten).param("email",email,java.sql.Types.VARCHAR).param("phone",phone,java.sql.Types.VARCHAR).param("sinh",sinh).param("hash",hash).query(Long.class).single();
    }
    public void suaHoSo(long id,String ten,String phone) {
        jdbc.sql("UPDATE \"NguoiDung\" SET \"HoTen\"=:ten,\"SoDienThoai\"=:phone,\"NgayCapNhat\"=now() WHERE \"MaNguoiDung\"=:id")
            .param("ten",ten).param("phone",phone).param("id",id).update();
    }
    public void khoaNguoiDung(long id) {
        // Serialize account operations while allowing FK key-share checks from audit/notification inserts.
        jdbc.sql("SELECT \"MaNguoiDung\" FROM \"NguoiDung\" WHERE \"MaNguoiDung\"=:id FOR NO KEY UPDATE").param("id",id).query(Long.class).single();
    }
    public void capMa(long id,String purpose,String hash) {
        jdbc.sql("""
            UPDATE "MaXacThuc" SET "NgayThuHoi"=now() WHERE "MaNguoiDung"=:id AND "MucDich"=:purpose
            AND "NgaySuDung" IS NULL AND "NgayThuHoi" IS NULL
            """).param("id",id).param("purpose",purpose).update();
        jdbc.sql("""
            INSERT INTO "MaXacThuc" ("MaNguoiDung","MucDich","GiaTriBam","NgayHetHan")
            VALUES (:id,:purpose,:hash,now()+interval '30 minutes')
            """).param("id",id).param("purpose",purpose).param("hash",hash).update();
    }
    public int soMaGanDay(long id) {
        return jdbc.sql("SELECT count(*) FROM \"MaXacThuc\" WHERE \"MaNguoiDung\"=:id AND \"NgayTao\">now()-interval '15 minutes'")
            .param("id",id).query(Integer.class).single();
    }
    public Optional<Long> chuMa(String hash,String purpose) {
        return jdbc.sql("SELECT \"MaNguoiDung\" FROM \"MaXacThuc\" WHERE \"GiaTriBam\"=:hash AND \"MucDich\"=:purpose")
            .param("hash",hash).param("purpose",purpose).query(Long.class).optional();
    }
    public boolean dungMa(String hash,String purpose) {
        return jdbc.sql("""
            UPDATE "MaXacThuc" SET "NgaySuDung"=now() WHERE "GiaTriBam"=:hash AND "MucDich"=:purpose
            AND "NgaySuDung" IS NULL AND "NgayThuHoi" IS NULL AND "NgayHetHan">now()
            """).param("hash",hash).param("purpose",purpose).update()==1;
    }
    public void xacMinh(long id) {
        jdbc.sql("UPDATE \"NguoiDung\" SET \"NgayXacMinhThuDienTu\"=now(),\"NgayCapNhat\"=now() WHERE \"MaNguoiDung\"=:id").param("id",id).update();
    }
    public void doiMatKhau(long id,String hash) {
        jdbc.sql("UPDATE \"NguoiDung\" SET \"MatKhauBam\"=:hash,\"NgayCapNhat\"=now() WHERE \"MaNguoiDung\"=:id").param("hash",hash).param("id",id).update();
        jdbc.sql("UPDATE \"PhienDangNhap\" SET \"NgayThuHoi\"=now() WHERE \"MaNguoiDung\"=:id AND \"NgayThuHoi\" IS NULL").param("id",id).update();
        jdbc.sql("UPDATE \"MaXacThuc\" SET \"NgayThuHoi\"=now() WHERE \"MaNguoiDung\"=:id AND \"MucDich\"='DAT_LAI_MAT_KHAU' AND \"NgaySuDung\" IS NULL AND \"NgayThuHoi\" IS NULL").param("id",id).update();
    }
    public long ghiPhien(long id,String hash,boolean remember) {
        return jdbc.sql("INSERT INTO \"PhienDangNhap\" (\"MaNguoiDung\",\"MaPhienBam\",\"NgayHetHan\") VALUES (:id,:hash,now()+CASE WHEN :remember THEN interval '30 days' ELSE interval '8 hours' END) RETURNING \"MaPhienDangNhap\"")
            .param("id",id).param("hash",hash).param("remember",remember).query(Long.class).single();
    }
    public boolean phienHopLe(String email,String hash) {
        return jdbc.sql("""
            SELECT EXISTS(SELECT 1 FROM "PhienDangNhap" p JOIN "NguoiDung" n USING ("MaNguoiDung")
            WHERE (n."ThuDienTu"=:email OR (n."SoDienThoai"=:email AND n."NgayXacMinhDienThoai" IS NOT NULL)) AND n."TrangThai"='HOAT_DONG' AND p."MaPhienBam"=:hash
            AND p."NgayThuHoi" IS NULL AND p."NgayHetHan">now())
            """).param("email",email).param("hash",hash).query(Boolean.class).single();
    }
    public void thuHoiPhien(String hash) {
        jdbc.sql("UPDATE \"PhienDangNhap\" SET \"NgayThuHoi\"=now() WHERE \"MaPhienBam\"=:hash AND \"NgayThuHoi\" IS NULL").param("hash",hash).update();
    }
}
