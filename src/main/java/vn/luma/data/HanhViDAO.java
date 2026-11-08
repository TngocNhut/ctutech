package vn.luma.data;
import java.util.*;
import java.sql.Types;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.simple.JdbcClient;

@Repository
public class HanhViDAO {
    private final JdbcClient jdbc;
    public HanhViDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public long tao(String key,Long user){return jdbc.sql("INSERT INTO \"PhienTruyCap\" (\"MaKhachAnDanh\",\"MaNguoiDung\",\"LanHoatDongCuoi\",\"SoGiayHoatDong\",\"NgayCapNhat\") VALUES (:key,:u,clock_timestamp(),0,clock_timestamp()) RETURNING \"MaPhienTruyCap\"").param("key",key).param("u",user,Types.BIGINT).query(Long.class).single();}
    public void khoa(long id,String key){if(jdbc.sql("SELECT \"MaPhienTruyCap\" FROM \"PhienTruyCap\" WHERE \"MaPhienTruyCap\"=:id AND \"MaKhachAnDanh\"=:key FOR UPDATE").param("id",id).param("key",key).query(Long.class).optional().isEmpty())throw new IllegalArgumentException("Phiên thống kê đã hết hạn.");}
    public int dem(long id){return jdbc.sql("SELECT (SELECT count(*) FROM \"SuKienTruyCap\" WHERE \"MaPhienTruyCap\"=:id)+(SELECT count(*) FROM \"TuongTacSanPham\" WHERE \"MaPhienTruyCap\"=:id)").param("id",id).query(Integer.class).single();}
    public void nhip(long id,boolean active){jdbc.sql("UPDATE \"PhienTruyCap\" SET \"SoGiayHoatDong\"=least(28800,\"SoGiayHoatDong\"+CASE WHEN :active AND clock_timestamp()-\"LanHoatDongCuoi\" BETWEEN interval '0 seconds' AND interval '30 seconds' THEN least(15,floor(extract(epoch FROM clock_timestamp()-\"LanHoatDongCuoi\")))::int ELSE 0 END),\"LanHoatDongCuoi\"=clock_timestamp(),\"NgayCapNhat\"=clock_timestamp() WHERE \"MaPhienTruyCap\"=:id").param("id",id).param("active",active).update();}
    public boolean suKien(long id,String type,String group,String key,Long banner){return jdbc.sql("INSERT INTO \"SuKienTruyCap\" (\"MaPhienTruyCap\",\"LoaiSuKien\",\"NhomTrang\",\"MaBanner\",\"KhoaChongLap\") VALUES (:id,:type,:group,:banner,:key) ON CONFLICT (\"MaPhienTruyCap\",\"KhoaChongLap\") DO NOTHING").param("id",id).param("type",type).param("group",group).param("key",key).param("banner",banner,Types.BIGINT).update()==1;}
    public Optional<Long> sanPham(long sku){return jdbc.sql("SELECT b.\"MaSanPham\" FROM \"BienTheSanPham\" b JOIN \"SanPham\" s USING (\"MaSanPham\") JOIN \"PhanLoai\" p USING (\"MaPhanLoai\") JOIN \"ThuongHieu\" h USING (\"MaThuongHieu\") WHERE b.\"MaBienThe\"=:id AND b.\"DangKinhDoanh\" AND s.\"DangHienThi\" AND p.\"DangHienThi\" AND h.\"DangHienThi\"").param("id",sku).query(Long.class).optional();}
    public void tuongTac(long id,long product,Long sku,String type,String key){jdbc.sql("INSERT INTO \"TuongTacSanPham\" (\"MaPhienTruyCap\",\"MaSanPham\",\"MaBienThe\",\"LoaiTuongTac\",\"KhoaChongLap\") VALUES (:id,:product,:sku,:type,:key) ON CONFLICT (\"KhoaChongLap\") DO NOTHING").param("id",id).param("product",product).param("sku",sku,Types.BIGINT).param("type",type).param("key",id+":"+key).update();}
    public void donDep(){jdbc.sql("DELETE FROM \"TuongTacSanPham\" t USING \"PhienTruyCap\" p WHERE t.\"MaPhienTruyCap\"=p.\"MaPhienTruyCap\" AND p.\"NgayTao\"<clock_timestamp()-interval '90 days'").update();jdbc.sql("DELETE FROM \"PhienTruyCap\" WHERE \"NgayTao\"<clock_timestamp()-interval '90 days'").update();}
}
