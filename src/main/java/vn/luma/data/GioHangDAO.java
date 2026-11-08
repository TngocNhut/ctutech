package vn.luma.data;

import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class GioHangDAO {
    private final JdbcClient jdbc;
    public GioHangDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public long khoaGio(long user){
        jdbc.sql("SELECT \"MaNguoiDung\" FROM \"NguoiDung\" WHERE \"MaNguoiDung\"=:u FOR UPDATE").param("u",user).query(Long.class).single();
        return jdbc.sql("""
            INSERT INTO "GioHang" ("MaNguoiDung","NgayCapNhat") VALUES (:u,now())
            ON CONFLICT ("MaNguoiDung") DO UPDATE SET "NgayCapNhat"=now() RETURNING "MaGioHang"
            """).param("u",user).query(Long.class).single();
    }
    public Map<Long,Integer> doc(long user){
        Map<Long,Integer> result=new TreeMap<>();
        jdbc.sql("SELECT c.\"MaBienThe\",c.\"SoLuong\" FROM \"ChiTietGioHang\" c JOIN \"GioHang\" g USING (\"MaGioHang\") WHERE g.\"MaNguoiDung\"=:u ORDER BY c.\"MaBienThe\"")
            .param("u",user).query((r,i)->Map.entry(r.getLong(1),r.getInt(2))).list().forEach(e->result.put(e.getKey(),e.getValue()));
        return result;
    }
    public void ghi(long cart,long sku,int quantity){
        if(quantity==0){jdbc.sql("DELETE FROM \"ChiTietGioHang\" WHERE \"MaGioHang\"=:g AND \"MaBienThe\"=:s").param("g",cart).param("s",sku).update();return;}
        jdbc.sql("""
            INSERT INTO "ChiTietGioHang" ("MaGioHang","MaBienThe","SoLuong","NgayCapNhat") VALUES (:g,:s,:q,now())
            ON CONFLICT ("MaGioHang","MaBienThe") DO UPDATE SET "SoLuong"=:q,"NgayCapNhat"=now()
            """).param("g",cart).param("s",sku).param("q",quantity).update();
    }
    public void xoa(long user){jdbc.sql("DELETE FROM \"ChiTietGioHang\" c USING \"GioHang\" g WHERE c.\"MaGioHang\"=g.\"MaGioHang\" AND g.\"MaNguoiDung\"=:u").param("u",user).update();}
}
