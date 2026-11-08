package vn.luma.data;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.web.*;

@Repository
public class CauHinhSanPhamDAO {
    private final JdbcClient jdbc;
    public CauHinhSanPhamDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public List<Map<String,Object>> dong(long brand,int page){return jdbc.sql("""
        SELECT d.*,h."TenThuongHieu",(SELECT count(*) FROM "SanPham" s WHERE s."MaDongSanPham"=d."MaDongSanPham") AS so_model
        FROM "DongSanPham" d JOIN "ThuongHieu" h USING ("MaThuongHieu") WHERE (:brand=0 OR d."MaThuongHieu"=:brand)
        ORDER BY d."MaDongSanPham" DESC LIMIT 30 OFFSET :offset
        """).param("brand",brand).param("offset",(page-1)*30).query().listOfRows();}
    public Map<String,Object> dong(long id,boolean lock){return jdbc.sql("SELECT * FROM \"DongSanPham\" WHERE \"MaDongSanPham\"=:id"+(lock?" FOR UPDATE":"")).param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new vn.luma.business.KhongTimThayException("Không tìm thấy dòng sản phẩm."));}
    public boolean dongDangDung(long id){return jdbc.sql("SELECT EXISTS(SELECT 1 FROM \"SanPham\" WHERE \"MaDongSanPham\"=:id)").param("id",id).query(Boolean.class).single();}
    public long luuDong(Long id,DongSanPhamYeuCau f){return jdbc.sql(id==null?"INSERT INTO \"DongSanPham\" (\"MaThuongHieu\",\"TenDongSanPham\",\"DuongDan\") VALUES (:brand,:name,:slug) RETURNING \"MaDongSanPham\"":"UPDATE \"DongSanPham\" SET \"MaThuongHieu\"=:brand,\"TenDongSanPham\"=:name,\"DuongDan\"=:slug WHERE \"MaDongSanPham\"=:id RETURNING \"MaDongSanPham\"").param("id",id).param("brand",f.maThuongHieu()).param("name",f.ten().strip()).param("slug",f.duongDan()).query(Long.class).single();}
    public void xoaDong(long id){jdbc.sql("DELETE FROM \"DongSanPham\" WHERE \"MaDongSanPham\"=:id").param("id",id).update();}
    public List<Map<String,Object>> dongCuaHang(long brand){return jdbc.sql("SELECT * FROM \"DongSanPham\" WHERE \"MaThuongHieu\"=:id ORDER BY \"TenDongSanPham\",\"MaDongSanPham\"").param("id",brand).query().listOfRows();}
    public void ganDong(long model,Long line){jdbc.sql("UPDATE \"SanPham\" SET \"MaDongSanPham\"=:line,\"NgayCapNhat\"=clock_timestamp() WHERE \"MaSanPham\"=:id").param("id",model).param("line",line,Types.BIGINT).update();}
    public List<Map<String,Object>> thuocTinh(long category,int page){return jdbc.sql("""
        SELECT t.*,p."TenPhanLoai",(SELECT count(*) FROM "GiaTriThuocTinhBienThe" v WHERE v."MaThuocTinh"=t."MaThuocTinh") AS so_sku
        FROM "ThuocTinhDanhMuc" t JOIN "PhanLoai" p USING ("MaPhanLoai") WHERE (:category=0 OR t."MaPhanLoai"=:category)
        ORDER BY t."MaPhanLoai",t."ThuTu",t."MaThuocTinh" LIMIT 30 OFFSET :offset
        """).param("category",category).param("offset",(page-1)*30).query().listOfRows();}
    public Map<String,Object> thuocTinh(long id,boolean keyShare){return jdbc.sql("SELECT * FROM \"ThuocTinhDanhMuc\" WHERE \"MaThuocTinh\"=:id"+(keyShare?" FOR KEY SHARE":" FOR UPDATE")).param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new vn.luma.business.KhongTimThayException("Không tìm thấy thuộc tính."));}
    public Map<String,Object> docThuocTinh(long id){return jdbc.sql("SELECT * FROM \"ThuocTinhDanhMuc\" WHERE \"MaThuocTinh\"=:id").param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new vn.luma.business.KhongTimThayException("Không tìm thấy thuộc tính."));}
    public boolean thuocTinhDangDung(long id){return jdbc.sql("SELECT EXISTS(SELECT 1 FROM \"GiaTriThuocTinhBienThe\" WHERE \"MaThuocTinh\"=:id)").param("id",id).query(Boolean.class).single();}
    public long luuThuocTinh(Long id,ThuocTinhYeuCau f){return jdbc.sql(id==null?"INSERT INTO \"ThuocTinhDanhMuc\" (\"MaPhanLoai\",\"KyHieu\",\"TenThuocTinh\",\"KieuDuLieu\",\"DonVi\",\"ChoPhepLoc\",\"ThuTu\") VALUES (:category,:code,:name,:type,:unit,:filter,:sort) RETURNING \"MaThuocTinh\"":"UPDATE \"ThuocTinhDanhMuc\" SET \"MaPhanLoai\"=:category,\"KyHieu\"=:code,\"TenThuocTinh\"=:name,\"KieuDuLieu\"=:type,\"DonVi\"=:unit,\"ChoPhepLoc\"=:filter,\"ThuTu\"=:sort WHERE \"MaThuocTinh\"=:id RETURNING \"MaThuocTinh\"").param("id",id).param("category",f.maPhanLoai()).param("code",f.kyHieu()).param("name",f.ten().strip()).param("type",f.kieu()).param("unit",f.donVi()==null?null:f.donVi().strip()).param("filter",f.choLoc()).param("sort",f.thuTu()).query(Long.class).single();}
    public void xoaThuocTinh(long id){jdbc.sql("DELETE FROM \"ThuocTinhDanhMuc\" WHERE \"MaThuocTinh\"=:id").param("id",id).update();}
    public void khoaSku(long id){jdbc.sql("SELECT \"MaBienThe\" FROM \"BienTheSanPham\" WHERE \"MaBienThe\"=:id FOR UPDATE").param("id",id).query(Long.class).single();}
    public List<Map<String,Object>> giaTri(long sku,long category){return jdbc.sql("""
        SELECT t.*,v."GiaTriChu",v."GiaTriSo",v."GiaTriCoKhong",v."MaBienThe" AS da_gan,
          CASE t."KieuDuLieu" WHEN 'CHU' THEN v."GiaTriChu" WHEN 'SO' THEN v."GiaTriSo"::text ELSE v."GiaTriCoKhong"::text END AS gia_tri_form
        FROM "ThuocTinhDanhMuc" t LEFT JOIN "GiaTriThuocTinhBienThe" v ON v."MaThuocTinh"=t."MaThuocTinh" AND v."MaBienThe"=:sku
        WHERE t."MaPhanLoai"=:category ORDER BY t."ThuTu",t."MaThuocTinh"
        """).param("sku",sku).param("category",category).query().listOfRows();}
    public void luuGiaTri(long sku,long model,long category,long attribute,String type,String text,BigDecimal number,Boolean yes){jdbc.sql("""
        INSERT INTO "GiaTriThuocTinhBienThe" ("MaBienThe","MaSanPham","MaPhanLoai","MaThuocTinh","KieuDuLieu","GiaTriChu","GiaTriSo","GiaTriCoKhong") VALUES (:sku,:model,:category,:attribute,:type,:text,:number,:yes)
        ON CONFLICT ("MaBienThe","MaThuocTinh") DO UPDATE SET "GiaTriChu"=excluded."GiaTriChu","GiaTriSo"=excluded."GiaTriSo","GiaTriCoKhong"=excluded."GiaTriCoKhong"
        """).param("sku",sku).param("model",model).param("category",category).param("attribute",attribute).param("type",type).param("text",text,Types.VARCHAR).param("number",number,Types.NUMERIC).param("yes",yes,Types.BOOLEAN).update();}
    public void xoaGiaTri(long sku,long attribute){jdbc.sql("DELETE FROM \"GiaTriThuocTinhBienThe\" WHERE \"MaBienThe\"=:sku AND \"MaThuocTinh\"=:attr").param("sku",sku).param("attr",attribute).update();}
    public List<Map<String,Object>> congKhai(long sku){return jdbc.sql("""
        SELECT t."TenThuocTinh",t."DonVi",t."KieuDuLieu",v."GiaTriChu",v."GiaTriSo",v."GiaTriCoKhong"
        FROM "GiaTriThuocTinhBienThe" v JOIN "ThuocTinhDanhMuc" t USING ("MaThuocTinh") WHERE v."MaBienThe"=:sku ORDER BY t."ThuTu",t."MaThuocTinh"
        """).param("sku",sku).query().listOfRows();}
}
