package vn.luma.data;

import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ThongBaoDAO {
    private final JdbcClient jdbc;
    public ThongBaoDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public boolean tao(long user,String type,String title,String body,String target,String key){return jdbc.sql("""
        INSERT INTO "ThongBao" ("MaNguoiDung","LoaiThongBao","TieuDe","NoiDung","DuongDanDich","KhoaChongLap","NgayCapNhat")
        VALUES (:u,:type,:title,:body,:target,:key,clock_timestamp()) ON CONFLICT ("MaNguoiDung","KhoaChongLap") DO NOTHING
        """).param("u",user).param("type",type).param("title",title).param("body",body).param("target",target).param("key",key).update()==1;}
    public List<Map<String,Object>> danhSach(long user,boolean unread,int page){return jdbc.sql("""
        SELECT *,to_char("NgayTao" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI') AS thoi_diem FROM "ThongBao"
        WHERE "MaNguoiDung"=:u AND (NOT :unread OR "NgayDoc" IS NULL) ORDER BY "MaThongBao" DESC LIMIT 30 OFFSET :offset
        """).param("u",user).param("unread",unread).param("offset",(page-1)*30).query().listOfRows();}
    public long chuaDoc(long user){return jdbc.sql("SELECT count(*) FROM \"ThongBao\" WHERE \"MaNguoiDung\"=:u AND \"NgayDoc\" IS NULL").param("u",user).query(Long.class).single();}
    public String doc(long user,long id){return jdbc.sql("""
        UPDATE "ThongBao" SET "NgayDoc"=coalesce("NgayDoc",clock_timestamp()),"NgayCapNhat"=clock_timestamp()
        WHERE "MaNguoiDung"=:u AND "MaThongBao"=:id RETURNING coalesce("DuongDanDich",'/tai-khoan/thong-bao')
        """).param("u",user).param("id",id).query(String.class).optional().orElseThrow(()->new vn.luma.business.KhongTimThayException("Không tìm thấy thông báo."));}
    public void docTatCa(long user){jdbc.sql("UPDATE \"ThongBao\" SET \"NgayDoc\"=clock_timestamp(),\"NgayCapNhat\"=clock_timestamp() WHERE \"MaNguoiDung\"=:u AND \"NgayDoc\" IS NULL").param("u",user).update();}
}
