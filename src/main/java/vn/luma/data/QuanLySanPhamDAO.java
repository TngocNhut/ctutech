package vn.luma.data;

import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.business.KhongTimThayException;
import vn.luma.web.*;

@Repository
public class QuanLySanPhamDAO {
    private final JdbcClient jdbc;
    public QuanLySanPhamDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public enum Loai {
        PHAN_LOAI("PhanLoai","MaPhanLoai","TenPhanLoai"),THUONG_HIEU("ThuongHieu","MaThuongHieu","TenThuongHieu");
        final String bang,ma,ten;Loai(String bang,String ma,String ten){this.bang=bang;this.ma=ma;this.ten=ten;}
    }
    public List<Map<String,Object>> danhMuc(Loai type){return jdbc.sql("SELECT *,\""+type.ma+"\" AS \"Ma\",\""+type.ten+"\" AS \"Ten\""+(type==Loai.THUONG_HIEU?",'' AS \"MoTa\",0 AS \"ThuTu\"":"")+" FROM \""+type.bang+"\" ORDER BY \""+type.ma+"\"").query().listOfRows();}
    public Map<String,Object> muc(Loai type,long id){return danhMuc(type).stream().filter(r->((Number)r.get("Ma")).longValue()==id).findFirst().orElseThrow(()->new KhongTimThayException("Không tìm thấy mục danh mục."));}
    public long luuMuc(Loai type,Long id,MucDanhMucYeuCau f){
        String sql;
        if(id==null)sql="INSERT INTO \""+type.bang+"\" (\""+type.ten+"\",\"DuongDan\",\"DangHienThi\",\"NgayCapNhat\""+(type==Loai.PHAN_LOAI?",\"MoTa\",\"ThuTu\"":"")+") VALUES (:name,:slug,:show,now()"+(type==Loai.PHAN_LOAI?",:description,:sort":"")+") RETURNING \""+type.ma+"\"";
        else sql="UPDATE \""+type.bang+"\" SET \""+type.ten+"\"=:name,\"DuongDan\"=:slug,\"DangHienThi\"=:show,\"NgayCapNhat\"=now()"+(type==Loai.PHAN_LOAI?",\"MoTa\"=:description,\"ThuTu\"=:sort":"")+" WHERE \""+type.ma+"\"=:id RETURNING \""+type.ma+"\"";
        var statement=jdbc.sql(sql).param("name",f.ten().strip()).param("slug",f.duongDan()).param("show",f.hienThi());
        if(type==Loai.PHAN_LOAI)statement.param("description",f.moTa()).param("sort",f.thuTu());if(id!=null)statement.param("id",id);
        return statement.query(Long.class).optional().orElseThrow(()->new KhongTimThayException("Mục danh mục không tồn tại."));
    }
    public void anMuc(Loai type,long id){if(jdbc.sql("UPDATE \""+type.bang+"\" SET \"DangHienThi\"=false,\"NgayCapNhat\"=now() WHERE \""+type.ma+"\"=:id").param("id",id).update()!=1)throw new KhongTimThayException("Mục danh mục không tồn tại.");}
    public List<Map<String,Object>> danhSach(String q){return danhSach(q,1);}
    public List<Map<String,Object>> danhSach(String q,int page){return jdbc.sql("""
        SELECT s.*,p."TenPhanLoai",h."TenThuongHieu",(SELECT count(*) FROM "BienTheSanPham" b WHERE b."MaSanPham"=s."MaSanPham") AS "SoCauHinh"
        FROM "SanPham" s JOIN "PhanLoai" p USING ("MaPhanLoai") JOIN "ThuongHieu" h USING ("MaThuongHieu")
        WHERE (:q='' OR s."TenSanPham" ILIKE :pattern OR s."TenTimKiem" ILIKE :pattern) ORDER BY s."MaSanPham" DESC LIMIT 30 OFFSET :offset
        """).param("offset",(page-1)*30).param("q",q).param("pattern","%"+q.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%").query().listOfRows();}
    public Map<String,Object> model(long id,boolean lock){return jdbc.sql("SELECT * FROM \"SanPham\" WHERE \"MaSanPham\"=:id"+(lock?" FOR UPDATE":"")).param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new KhongTimThayException("Model không tồn tại."));}
    public boolean coThuocTinh(long product){return jdbc.sql("SELECT EXISTS (SELECT 1 FROM \"GiaTriThuocTinhBienThe\" WHERE \"MaSanPham\"=:id)").param("id",product).query(Boolean.class).single();}
    public long luuModel(Long id,NoiDungSanPhamYeuCau f,String search){
        String sql=id==null?"""
            INSERT INTO "SanPham" ("MaPhanLoai","MaThuongHieu","TenSanPham","DuongDan","TenTimKiem","MoTaNgan","MoTaChiTiet","SoThangBaoHanh","ThongSoChung","DuongDanNguon","NgayThuThap","LaDuLieuMau","DangHienThi","NgayCapNhat")
            VALUES (:category,:brand,:name,:slug,:search,:summary,:description,:warranty,'{}',:source,NULL,false,:show,now()) RETURNING "MaSanPham"
            """:"""
            UPDATE "SanPham" SET "MaDongSanPham"=CASE WHEN "MaThuongHieu"=:brand THEN "MaDongSanPham" ELSE NULL END,
              "MaPhanLoai"=:category,"MaThuongHieu"=:brand,"TenSanPham"=:name,"DuongDan"=:slug,"TenTimKiem"=:search,"MoTaNgan"=:summary,"MoTaChiTiet"=:description,"SoThangBaoHanh"=:warranty,"NgayThuThap"=CASE WHEN "DuongDanNguon" IS NOT DISTINCT FROM :source THEN "NgayThuThap" ELSE NULL END,"DuongDanNguon"=:source,"DangHienThi"=:show,"NgayCapNhat"=now()
            WHERE "MaSanPham"=:id RETURNING "MaSanPham"
            """;
        var statement=jdbc.sql(sql).param("category",f.maPhanLoai()).param("brand",f.maThuongHieu()).param("name",f.tenSanPham().strip()).param("slug",f.duongDan()).param("search",search).param("summary",f.moTaNgan().strip()).param("description",f.moTaChiTiet()).param("warranty",f.baoHanh()).param("source",f.nguon()==null||f.nguon().isBlank()?null:f.nguon().strip()).param("show",f.hienThi());if(id!=null)statement.param("id",id);return statement.query(Long.class).single();
    }
    public void anModel(long id){if(jdbc.sql("UPDATE \"SanPham\" SET \"DangHienThi\"=false,\"NgayCapNhat\"=now() WHERE \"MaSanPham\"=:id").param("id",id).update()!=1)throw new KhongTimThayException("Model không tồn tại.");}
    public List<Map<String,Object>> bienThe(long product){return jdbc.sql("SELECT b.*,coalesce(t.\"SoLuongTon\",0) AS \"TonVatLy\",coalesce(t.\"SoLuongGiu\",0) AS \"DangGiu\" FROM \"BienTheSanPham\" b LEFT JOIN \"TonKho\" t ON t.\"MaBienThe\"=b.\"MaBienThe\" AND t.\"MaKhoHang\"=1 WHERE b.\"MaSanPham\"=:id ORDER BY b.\"MaBienThe\"").param("id",product).query().listOfRows();}
    public Map<String,Object> bienTheTheoId(long sku){return jdbc.sql("SELECT * FROM \"BienTheSanPham\" WHERE \"MaBienThe\"=:id").param("id",sku).query().listOfRows().stream().findFirst().orElseThrow(()->new KhongTimThayException("SKU không tồn tại."));}
    public long luuBienThe(long product,Long id,NoiDungBienTheYeuCau f){
        String fields="\"MaHang\"=:code,\"MaMayHang\"=:part,\"TenBienThe\"=:name,\"MauSac\"=:color,\"BoXuLy\"=:cpu,\"BoXuLyDoHoa\"=:gpu,\"DungLuongRam\"=:ram,\"DungLuongLuuTru\"=:storage,\"KichThuocManHinh\"=:screen,\"HeDieuHanh\"=:os,\"DangKinhDoanh\"=:active,\"NgayCapNhat\"=now()";
        String sql=id==null?"""
            INSERT INTO "BienTheSanPham" ("MaSanPham","MaHang","MaMayHang","TenBienThe","MauSac","BoXuLy","BoXuLyDoHoa","DungLuongRam","DungLuongLuuTru","KichThuocManHinh","HeDieuHanh","ThongSoBoSung","GiaNiemYet","GiaBan","GiaVon","DangKinhDoanh","NgayCapNhat")
            VALUES (:product,:code,:part,:name,:color,:cpu,:gpu,:ram,:storage,:screen,:os,'{}',:list,:price,:cost,:active,now()) RETURNING "MaBienThe"
            """:"UPDATE \"BienTheSanPham\" SET "+fields+" WHERE \"MaBienThe\"=:id RETURNING \"MaBienThe\"";
        var statement=jdbc.sql(sql).param("code",f.maHang().strip()).param("part",f.maMayHang()).param("name",f.tenBienThe().strip()).param("color",f.mauSac().strip()).param("cpu",f.cpu().strip()).param("gpu",f.gpu()).param("ram",f.ram()).param("storage",f.luuTru()).param("screen",f.manHinh()).param("os",f.heDieuHanh().strip()).param("active",f.kinhDoanh());
        if(id==null)statement.param("product",product).param("list",f.giaTon().giaNiemYet()).param("price",f.giaTon().giaBan()).param("cost",f.giaTon().giaVon());else statement.param("id",id);long sku=statement.query(Long.class).single();
        if(id==null)jdbc.sql("INSERT INTO \"TonKho\" (\"MaKhoHang\",\"MaBienThe\",\"SoLuongTon\",\"SoLuongGiu\",\"NgayCapNhat\") VALUES (1,:id,0,0,now())").param("id",sku).update();return sku;
    }
    public void ngungBienThe(long sku){if(jdbc.sql("UPDATE \"BienTheSanPham\" SET \"DangKinhDoanh\"=false,\"NgayCapNhat\"=now() WHERE \"MaBienThe\"=:id").param("id",sku).update()!=1)throw new KhongTimThayException("SKU không tồn tại.");}
    public void giaDau(long sku,long actor,NoiDungBienTheYeuCau f){jdbc.sql("INSERT INTO \"LichSuGiaBienThe\" (\"MaBienThe\",\"MaNguoiCapNhat\",\"GiaBan\",\"GiaNiemYet\",\"LyDo\") VALUES (:s,:u,:price,:list,:reason)").param("s",sku).param("u",actor).param("price",f.giaTon().giaBan()).param("list",f.giaTon().giaNiemYet()).param("reason",f.giaTon().lyDo()).update();}
    public void log(long actor,long id,String action,String type){log(actor,id,action,type,null);}
    public void log(long actor,long id,String action,String type,String reason){jdbc.sql("INSERT INTO \"NhatKyQuanTri\" (\"MaNguoiThucHien\",\"HanhDong\",\"LoaiDoiTuong\",\"MaDoiTuong\",\"GhiChu\") VALUES (:u,:action,:type,:id,:reason)").param("u",actor).param("action",action).param("type",type).param("id",Long.toString(id)).param("reason",reason).update();}
    public List<Map<String,Object>> anh(long product){return jdbc.sql("SELECT * FROM \"TepSanPham\" WHERE \"MaSanPham\"=:p AND \"LoaiTep\"='ANH' ORDER BY \"ThuTu\",\"MaTep\" LIMIT 200").param("p",product).query().listOfRows();}
    public long taoAnh(long product,AnhSanPhamYeuCau f,String path){return jdbc.sql("""
        INSERT INTO "TepSanPham" ("MaSanPham","MaBienThe","LoaiTep","DuongDanTep","MoTaThayThe","ThuTu","DuongDanNguon","ThongTinSuDung","DangHienThi","NgayCapNhat")
        VALUES (:p,:sku,'ANH',:path,:alt,:sort,:source,:usage,:show,now()) RETURNING "MaTep"
        """).param("p",product).param("sku",f.maSku(),java.sql.Types.BIGINT).param("path",path).param("alt",f.moTa().strip()).param("sort",f.thuTu()).param("source",f.nguon()).param("usage",f.thongTinSuDung()).param("show",f.hienThi()).query(Long.class).single();}
    public void suaAnh(long id,AnhSanPhamYeuCau f){jdbc.sql("UPDATE \"TepSanPham\" SET \"MaBienThe\"=:sku,\"MoTaThayThe\"=:alt,\"ThuTu\"=:sort,\"DuongDanNguon\"=:source,\"ThongTinSuDung\"=:usage,\"DangHienThi\"=:show,\"NgayCapNhat\"=now() WHERE \"MaTep\"=:id").param("id",id).param("sku",f.maSku(),java.sql.Types.BIGINT).param("alt",f.moTa().strip()).param("sort",f.thuTu()).param("source",f.nguon()).param("usage",f.thongTinSuDung()).param("show",f.hienThi()).update();}
    public void anAnh(long id){jdbc.sql("UPDATE \"TepSanPham\" SET \"DangHienThi\"=false,\"NgayCapNhat\"=now() WHERE \"MaTep\"=:id").param("id",id).update();}
    public Optional<Map<String,Object>> tepTheoDuongDan(String path){return jdbc.sql("""
        SELECT a.*, (a."DangHienThi" AND s."DangHienThi" AND p."DangHienThi" AND h."DangHienThi"
          AND EXISTS(SELECT 1 FROM "BienTheSanPham" b WHERE b."MaSanPham"=s."MaSanPham" AND b."DangKinhDoanh" AND (a."MaBienThe" IS NULL OR a."MaBienThe"=b."MaBienThe"))) AS "ChoXem"
        FROM "TepSanPham" a JOIN "SanPham" s USING ("MaSanPham") JOIN "PhanLoai" p USING ("MaPhanLoai") JOIN "ThuongHieu" h USING ("MaThuongHieu") WHERE a."DuongDanTep"=:path AND a."LoaiTep"='ANH'
        """).param("path",path).query().listOfRows().stream().findFirst();}
}
