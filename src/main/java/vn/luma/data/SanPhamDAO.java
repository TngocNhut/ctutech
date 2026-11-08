package vn.luma.data;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.model.SanPham;
import vn.luma.model.BienThe;
import java.util.Optional;

@Repository
public class SanPhamDAO {
    private static final String CATEGORY="""
        CASE WHEN p."DuongDan"='phu-kien' AND (lower(s."TenTimKiem") LIKE '%tai nghe%' OR EXISTS (SELECT 1 FROM "GiaTriThuocTinhBienThe" av JOIN "ThuocTinhDanhMuc" at ON at."MaThuocTinh"=av."MaThuocTinh" WHERE av."MaSanPham"=s."MaSanPham" AND at."KyHieu"='LOAI_PHU_KIEN' AND av."GiaTriChu" ILIKE 'Tai nghe%')) THEN 'tai-nghe'
             WHEN p."DuongDan"='phu-kien' AND (lower(s."TenTimKiem") LIKE '%chuot%' OR lower(s."TenTimKiem") LIKE '%ban phim%' OR EXISTS (SELECT 1 FROM "GiaTriThuocTinhBienThe" av JOIN "ThuocTinhDanhMuc" at ON at."MaThuocTinh"=av."MaThuocTinh" WHERE av."MaSanPham"=s."MaSanPham" AND at."KyHieu"='LOAI_PHU_KIEN' AND av."GiaTriChu" IN ('Chuột','Bàn phím'))) THEN 'chuot-ban-phim'
             ELSE p."DuongDan" END
        """;
    private static final String LOC="""
        FROM "SanPham" s JOIN "ThuongHieu" h USING ("MaThuongHieu") JOIN "PhanLoai" p USING ("MaPhanLoai")
        JOIN "BienTheSanPham" b USING ("MaSanPham") JOIN "GiaKhuyenMaiBienThe" g ON g."MaBienThe"=b."MaBienThe" JOIN "KhoHang" k ON k."MaKhoHang"=1 AND k."DangHoatDong"
        LEFT JOIN "TonKho" t ON t."MaBienThe"=b."MaBienThe" AND t."MaKhoHang"=k."MaKhoHang"
        WHERE s."DangHienThi" AND h."DangHienThi" AND p."DangHienThi" AND b."DangKinhDoanh"
          AND (:q='' OR s."TenSanPham" ILIKE :pattern OR s."TenTimKiem" ILIKE :normalized)
          AND (:loai='' OR (%s)=:loai) AND (:hang='' OR h."DuongDan"=:hang)
          AND (CAST(:giaTu AS numeric) IS NULL OR g."GiaHieuLuc">=:giaTu) AND (CAST(:giaDen AS numeric) IS NULL OR g."GiaHieuLuc"<=:giaDen)
          AND (CAST(:ram AS integer) IS NULL OR b."DungLuongRam">=:ram) AND (CAST(:luuTru AS integer) IS NULL OR b."DungLuongLuuTru">=:luuTru)
          AND (:cpu='' OR b."BoXuLy" ILIKE :cpuPattern) AND (:gpu='' OR b."BoXuLyDoHoa" ILIKE :gpuPattern)
          AND (NOT :conHang OR coalesce(t."SoLuongTon"-t."SoLuongGiu",0)>0)
        """.formatted(CATEGORY);
    private final JdbcClient jdbc;
    public SanPhamDAO(JdbcClient jdbc) { this.jdbc = jdbc; }
    public record DieuKienThuocTinh(long ma,String kieu,Object giaTri) { }
    public record LocMoRong(Long dong,List<DieuKienThuocTinh> thuocTinh) {public static LocMoRong trong(){return new LocMoRong(null,List.of());}}
    private String dieuKien(LocMoRong more){
        var sql=new StringBuilder(LOC);if(more.dong()!=null)sql.append(" AND s.\"MaDongSanPham\"=:dong ");int i=0;
        for(var attr:more.thuocTinh()){
            String compare=switch(attr.kieu()){case "CHU"->"lower(v.\"GiaTriChu\")=lower(:value"+i+")";case "SO"->"v.\"GiaTriSo\"=:value"+i;case "CO_KHONG"->"v.\"GiaTriCoKhong\"=:value"+i;default->throw new IllegalArgumentException("Kiểu lọc không hợp lệ.");};
            sql.append(" AND EXISTS(SELECT 1 FROM \"GiaTriThuocTinhBienThe\" v JOIN \"ThuocTinhDanhMuc\" a USING (\"MaThuocTinh\") WHERE v.\"MaBienThe\"=b.\"MaBienThe\" AND a.\"ChoPhepLoc\" AND a.\"MaThuocTinh\"=:attr").append(i).append(" AND ").append(compare).append(") ");i++;
        }return sql.toString();
    }
    private JdbcClient.StatementSpec loc(String sql,vn.luma.web.BoLocSanPham f,LocMoRong more){var query=loc(sql,f);if(more.dong()!=null)query.param("dong",more.dong());int i=0;for(var attr:more.thuocTinh()){query.param("attr"+i,attr.ma()).param("value"+i,attr.giaTri(),switch(attr.kieu()){case "CHU"->java.sql.Types.VARCHAR;case "SO"->java.sql.Types.NUMERIC;default->java.sql.Types.BOOLEAN;});i++;}return query;}
    private String pattern(String value){return "%"+value.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";}
    private JdbcClient.StatementSpec loc(String sql,vn.luma.web.BoLocSanPham f){String normalized=java.text.Normalizer.normalize(f.q(),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D');return jdbc.sql(sql).param("q",f.q()).param("pattern",pattern(f.q())).param("normalized",pattern(normalized)).param("loai",f.loai()).param("hang",f.hang()).param("giaTu",f.giaTu(),java.sql.Types.NUMERIC).param("giaDen",f.giaDen(),java.sql.Types.NUMERIC).param("ram",f.ram(),java.sql.Types.INTEGER).param("luuTru",f.luuTru(),java.sql.Types.INTEGER).param("cpu",f.cpu()).param("cpuPattern",pattern(f.cpu())).param("gpu",f.gpu()).param("gpuPattern",pattern(f.gpu())).param("conHang",f.conHang());}
    public long dem(vn.luma.web.BoLocSanPham f){return dem(f,LocMoRong.trong());}
    public long dem(vn.luma.web.BoLocSanPham f,LocMoRong more){return loc("SELECT count(DISTINCT s.\"MaSanPham\") "+dieuKien(more),f,more).query(Long.class).single();}
    public List<SanPham> loc(vn.luma.web.BoLocSanPham f,int page,int size){
        return loc(f,page,size,LocMoRong.trong());
    }
    public List<SanPham> loc(vn.luma.web.BoLocSanPham f,int page,int size,LocMoRong more){
        String sort=switch(f.sapXep()){case "gia-tang"->"gia ASC";case "gia-giam"->"gia DESC";case "moi-nhat"->"s.\"MaSanPham\" DESC";case "ten"->"s.\"TenSanPham\" ASC";default->"s.\"MaSanPham\" ASC";};
        String sql="""
            SELECT s."MaSanPham",s."TenSanPham",s."DuongDan",s."MoTaNgan",h."TenThuongHieu",p."TenPhanLoai",
              min(g."GiaHieuLuc") AS gia,coalesce(sum(t."SoLuongTon"-t."SoLuongGiu"),0) AS ton,
              (SELECT a."DuongDanTep" FROM "TepSanPham" a WHERE a."MaSanPham"=s."MaSanPham" AND a."LoaiTep"='ANH' AND a."DangHienThi" AND (a."MaBienThe" IS NULL OR a."MaBienThe"=(array_agg(b."MaBienThe" ORDER BY g."GiaHieuLuc",b."MaBienThe"))[1]) ORDER BY a."MaBienThe" NULLS LAST,a."ThuTu",a."MaTep" LIMIT 1) AS anh, (array_agg(b."MaBienThe" ORDER BY g."GiaHieuLuc",b."MaBienThe"))[1] AS sku_goi_y, (array_agg(b."TenBienThe" ORDER BY g."GiaHieuLuc",b."MaBienThe"))[1] AS cau_hinh_goi_y
            """+dieuKien(more)+" GROUP BY s.\"MaSanPham\",h.\"TenThuongHieu\",p.\"TenPhanLoai\" ORDER BY "+sort+",s.\"MaSanPham\" ASC LIMIT :size OFFSET :offset";
        return loc(sql,f,more).param("size",size).param("offset",(page-1)*size).query((r,i)->new SanPham(r.getLong("MaSanPham"),r.getString("TenSanPham"),r.getString("DuongDan"),r.getString("MoTaNgan"),r.getString("TenThuongHieu"),r.getString("TenPhanLoai"),r.getBigDecimal("gia"),r.getLong("ton"),r.getString("anh"),r.getLong("sku_goi_y"),r.getString("cau_hinh_goi_y"))).list();
    }
    public java.util.Map<Long,List<String>> giaTriLoc(){var result=new java.util.LinkedHashMap<Long,List<String>>();for(var r:jdbc.sql("""
        SELECT DISTINCT v."MaThuocTinh",v."GiaTriChu" FROM "GiaTriThuocTinhBienThe" v JOIN "ThuocTinhDanhMuc" a USING("MaThuocTinh") JOIN "SanPham" s ON s."MaSanPham"=v."MaSanPham" JOIN "BienTheSanPham" b ON b."MaBienThe"=v."MaBienThe" WHERE a."ChoPhepLoc" AND v."KieuDuLieu"='CHU' AND s."DangHienThi" AND b."DangKinhDoanh" ORDER BY v."MaThuocTinh",v."GiaTriChu"
        """).query().listOfRows()){result.computeIfAbsent(((Number)r.get("MaThuocTinh")).longValue(),id->new java.util.ArrayList<>()).add((String)r.get("GiaTriChu"));}return result;}
    public List<java.util.Map<String,Object>> thuocTinhLoc(){return jdbc.sql("SELECT a.*,p.\"TenPhanLoai\" FROM \"ThuocTinhDanhMuc\" a JOIN \"PhanLoai\" p USING (\"MaPhanLoai\") WHERE a.\"ChoPhepLoc\" AND p.\"DangHienThi\" ORDER BY p.\"ThuTu\",a.\"ThuTu\",a.\"MaThuocTinh\"").query().listOfRows();}
    public List<java.util.Map<String,Object>> dongSanPham(){return jdbc.sql("""
        SELECT d.*,h."TenThuongHieu" FROM "DongSanPham" d JOIN "ThuongHieu" h USING ("MaThuongHieu") WHERE h."DangHienThi"
          AND EXISTS(SELECT 1 FROM "SanPham" s JOIN "PhanLoai" p USING ("MaPhanLoai") JOIN "BienTheSanPham" b USING ("MaSanPham")
            WHERE s."MaDongSanPham"=d."MaDongSanPham" AND s."DangHienThi" AND p."DangHienThi" AND b."DangKinhDoanh")
        ORDER BY h."TenThuongHieu",d."TenDongSanPham",d."MaDongSanPham"
        """).query().listOfRows();}
    public List<java.util.Map<String,Object>> thuongHieu(String category){return jdbc.sql("""
        SELECT h.* FROM "ThuongHieu" h WHERE h."DangHienThi" AND EXISTS(SELECT 1 FROM "SanPham" s JOIN "PhanLoai" p USING("MaPhanLoai") JOIN "BienTheSanPham" b USING("MaSanPham") WHERE s."MaThuongHieu"=h."MaThuongHieu" AND s."DangHienThi" AND p."DangHienThi" AND (%s)=:category AND b."DangKinhDoanh") ORDER BY h."TenThuongHieu"
        """.formatted(CATEGORY)).param("category",category).query().listOfRows();}
    public List<java.util.Map<String,Object>> dongSanPham(String category){return jdbc.sql("""
        SELECT d.*,h."TenThuongHieu",h."DuongDan" AS "DuongDanThuongHieu" FROM "DongSanPham" d JOIN "ThuongHieu" h USING("MaThuongHieu") WHERE h."DangHienThi" AND EXISTS(SELECT 1 FROM "SanPham" s JOIN "PhanLoai" p USING("MaPhanLoai") JOIN "BienTheSanPham" b USING("MaSanPham") WHERE s."MaDongSanPham"=d."MaDongSanPham" AND s."DangHienThi" AND p."DangHienThi" AND (%s)=:category AND b."DangKinhDoanh") ORDER BY h."TenThuongHieu",d."TenDongSanPham"
        """.formatted(CATEGORY)).param("category",category).query().listOfRows();}
    public List<java.util.Map<String,Object>> danhMuc(){return jdbc.sql("SELECT * FROM \"PhanLoai\" WHERE \"DangHienThi\" ORDER BY \"ThuTu\",\"MaPhanLoai\"").query().listOfRows();}
    public List<java.util.Map<String,Object>> thuongHieu(){return jdbc.sql("SELECT * FROM \"ThuongHieu\" WHERE \"DangHienThi\" ORDER BY \"TenThuongHieu\"").query().listOfRows();}

    public List<SanPham> danhSach(String tuKhoa, String loai) {
        return danhSach(tuKhoa,loai,"");
    }
    public Optional<SanPham> tim(String slug) {return danhSach("","",slug).stream().findFirst();}
    private List<SanPham> danhSach(String tuKhoa,String loai,String slug) {
        return jdbc.sql("""
            SELECT s."MaSanPham", s."TenSanPham", s."DuongDan", s."MoTaNgan",
                   h."TenThuongHieu", p."TenPhanLoai", min(g."GiaHieuLuc") AS gia,
                   coalesce(sum(t."SoLuongTon" - t."SoLuongGiu"), 0) AS ton,
                   (SELECT a."DuongDanTep" FROM "TepSanPham" a WHERE a."MaSanPham"=s."MaSanPham" AND a."LoaiTep"='ANH' AND a."DangHienThi" AND (a."MaBienThe" IS NULL OR a."MaBienThe"=(array_agg(b."MaBienThe" ORDER BY g."GiaHieuLuc",b."MaBienThe"))[1]) ORDER BY a."MaBienThe" NULLS LAST,a."ThuTu",a."MaTep" LIMIT 1) AS anh, (array_agg(b."MaBienThe" ORDER BY g."GiaHieuLuc",b."MaBienThe"))[1] AS sku_goi_y, (array_agg(b."TenBienThe" ORDER BY g."GiaHieuLuc",b."MaBienThe"))[1] AS cau_hinh_goi_y
            FROM "SanPham" s JOIN "ThuongHieu" h USING ("MaThuongHieu")
            JOIN "PhanLoai" p USING ("MaPhanLoai")
            JOIN "BienTheSanPham" b USING ("MaSanPham") JOIN "GiaKhuyenMaiBienThe" g ON g."MaBienThe"=b."MaBienThe"
            LEFT JOIN "TonKho" t ON t."MaBienThe"=b."MaBienThe" AND t."MaKhoHang"=1
            WHERE s."DangHienThi" AND h."DangHienThi" AND p."DangHienThi" AND b."DangKinhDoanh"
              AND (:q = '' OR s."TenSanPham" ILIKE :pattern OR s."TenTimKiem" ILIKE :pattern)
              AND (:loai = '' OR p."DuongDan" = :loai)
              AND (:slug = '' OR s."DuongDan" = :slug)
            GROUP BY s."MaSanPham", h."TenThuongHieu", p."TenPhanLoai"
            ORDER BY s."MaSanPham"
            LIMIT 100
            """)
            .param("q", tuKhoa).param("pattern", "%" + tuKhoa + "%").param("loai", loai).param("slug",slug)
            .query((rs, n) -> new SanPham(rs.getLong("MaSanPham"), rs.getString("TenSanPham"),
                rs.getString("DuongDan"), rs.getString("MoTaNgan"), rs.getString("TenThuongHieu"),
                rs.getString("TenPhanLoai"), rs.getBigDecimal("gia"), rs.getLong("ton"),rs.getString("anh"),rs.getLong("sku_goi_y"),rs.getString("cau_hinh_goi_y")))
            .list();
    }
    public List<java.util.Map<String,Object>> anh(long product,long sku){return jdbc.sql("SELECT * FROM \"TepSanPham\" WHERE \"MaSanPham\"=:p AND (\"MaBienThe\" IS NULL OR \"MaBienThe\"=:sku) AND \"LoaiTep\"='ANH' AND \"DangHienThi\" ORDER BY CASE WHEN \"MaBienThe\"=:sku THEN 0 ELSE 1 END,\"ThuTu\",\"MaTep\" LIMIT 24").param("p",product).param("sku",sku).query().listOfRows();}
    public java.util.Map<String,Object> noiDung(String slug){return jdbc.sql("SELECT s.\"MoTaChiTiet\",s.\"SoThangBaoHanh\",s.\"DuongDanNguon\",d.\"TenDongSanPham\" FROM \"SanPham\" s LEFT JOIN \"DongSanPham\" d USING (\"MaDongSanPham\") WHERE s.\"DuongDan\"=:slug").param("slug",slug).query().singleRow();}
    public Optional<java.util.Map<String,Object>> moHinh(long product,long sku){return jdbc.sql("SELECT * FROM \"TepSanPham\" WHERE \"MaSanPham\"=:product AND (\"MaBienThe\" IS NULL OR \"MaBienThe\"=:sku) AND \"LoaiTep\"='MO_HINH_3D' AND \"DangHienThi\" ORDER BY CASE WHEN \"MaBienThe\"=:sku THEN 0 ELSE 1 END,\"ThuTu\",\"MaTep\" LIMIT 1").param("product",product).param("sku",sku).query().listOfRows().stream().findFirst();}
    public List<BienThe> bienThe(String slug) {return bienThe(slug,false);}
    public List<BienThe> bienTheGiaGoc() {return bienThe("",true);}
    public List<BienThe> bienTheGiaGoc(int page,String q){return bienThe("",true,null,q,page);}
    public Optional<BienThe> bienTheGiaGoc(long id){return bienThe("",true,id,"",null).stream().findFirst();}
    public Optional<BienThe> bienTheTheoId(long id){return bienThe("",false,id,"",null).stream().findFirst();}
    private List<BienThe> bienThe(String slug,boolean raw){return bienThe(slug,raw,null,"",null);}
    private List<BienThe> bienThe(String slug,boolean raw,Long id,String q,Integer page) {
        return jdbc.sql("""
            SELECT b.*,g."GiaGoc",g."TenChuongTrinh",CASE WHEN :raw THEN b."GiaBan" ELSE g."GiaHieuLuc" END AS gia_hien_thi,s."TenSanPham",s."DuongDan",coalesce(t."SoLuongTon",0) AS ton,coalesce(t."SoLuongGiu",0) AS giu,
              (s."DangHienThi" AND h."DangHienThi" AND p."DangHienThi" AND b."DangKinhDoanh") AS cho_mua
            FROM "BienTheSanPham" b JOIN "GiaKhuyenMaiBienThe" g USING ("MaBienThe") JOIN "SanPham" s USING ("MaSanPham")
            JOIN "ThuongHieu" h USING ("MaThuongHieu") JOIN "PhanLoai" p USING ("MaPhanLoai")
            LEFT JOIN "TonKho" t ON t."MaBienThe"=b."MaBienThe" AND t."MaKhoHang"=1
            WHERE (CAST(:skuId AS bigint) IS NULL OR b."MaBienThe"=:skuId) AND (:adminQ='' OR s."TenSanPham" ILIKE :adminPattern OR b."MaHang" ILIKE :adminPattern OR b."TenBienThe" ILIKE :adminPattern) AND (:slug='' OR (s."DuongDan"=:slug AND s."DangHienThi" AND h."DangHienThi" AND p."DangHienThi" AND b."DangKinhDoanh"))
            ORDER BY b."MaBienThe"
            """+(page==null?"":" LIMIT 30 OFFSET :offset")).param("skuId",id,java.sql.Types.BIGINT).param("adminQ",q).param("adminPattern",pattern(q)).param("offset",page==null?0:(page-1)*30).param("slug",slug).param("raw",raw).query((r,i)->new BienThe(r.getLong("MaBienThe"),r.getString("TenSanPham"),r.getString("DuongDan"),
                r.getString("MaHang"),r.getString("TenBienThe"),r.getString("MauSac"),r.getString("BoXuLy"),r.getString("BoXuLyDoHoa"),
                r.getInt("DungLuongRam"),r.getInt("DungLuongLuuTru"),r.getBigDecimal("KichThuocManHinh"),r.getString("HeDieuHanh"),
                r.getBigDecimal("gia_hien_thi"),r.getBigDecimal("GiaNiemYet"),r.getBigDecimal("GiaVon"),r.getInt("ton"),r.getInt("giu"),r.getBoolean("cho_mua"),r.getBigDecimal("GiaGoc"),raw?null:r.getString("TenChuongTrinh"))).list();
    }
}
