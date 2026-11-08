package vn.luma.data;

import java.util.*;
import java.sql.Types;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.business.KhongTimThayException;
import vn.luma.web.*;

@Repository
public class HoTroDAO {
    private final JdbcClient jdbc;
    public HoTroDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public Map<String,Object> cuoc(long id,boolean lock){return jdbc.sql("SELECT c.*,n.\"HoTen\" AS ten_khach,a.\"HoTen\" AS ten_phu_trach FROM \"CuocTroChuyen\" c JOIN \"NguoiDung\" n ON n.\"MaNguoiDung\"=c.\"MaKhachHang\" LEFT JOIN \"NguoiDung\" a ON a.\"MaNguoiDung\"=c.\"MaQuanTriPhuTrach\" WHERE c.\"MaCuocTroChuyen\"=:id"+(lock?" FOR UPDATE OF c":"")).param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new KhongTimThayException("Không tìm thấy cuộc hỗ trợ."));}
    public List<Map<String,Object>> danhSach(Long user,String state,String mode,int page){return jdbc.sql("""
        SELECT c.*,n."HoTen" AS ten_khach,a."HoTen" AS ten_phu_trach,to_char(c."NgayCapNhat" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI') AS luc_cap_nhat
        FROM "CuocTroChuyen" c JOIN "NguoiDung" n ON n."MaNguoiDung"=c."MaKhachHang" LEFT JOIN "NguoiDung" a ON a."MaNguoiDung"=c."MaQuanTriPhuTrach"
        WHERE (CAST(:user AS bigint) IS NULL OR c."MaKhachHang"=:user) AND (:state='' OR c."TrangThai"=:state) AND (:mode='' OR c."CheDoHoTro"=:mode)
        ORDER BY c."NgayCapNhat" DESC,c."MaCuocTroChuyen" DESC LIMIT 30 OFFSET :offset
        """).param("user",user,Types.BIGINT).param("state",state).param("mode",mode).param("offset",(page-1)*30).query().listOfRows();}
    public Optional<Long> dangMo(long user){return jdbc.sql("SELECT \"MaCuocTroChuyen\" FROM \"CuocTroChuyen\" WHERE \"MaKhachHang\"=:u AND \"TrangThai\"='DANG_MO'").param("u",user).query(Long.class).optional();}
    public int soLanMo(long user){return jdbc.sql("SELECT count(*) FROM \"CuocTroChuyen\" WHERE \"MaKhachHang\"=:u AND \"NgayTao\">now()-interval '1 day'").param("u",user).query(Integer.class).single();}
    public long mo(long user,String mode){return jdbc.sql("INSERT INTO \"CuocTroChuyen\" (\"MaKhachHang\",\"TrangThai\",\"CheDoHoTro\",\"NgayCapNhat\") VALUES (:u,'DANG_MO',:mode,clock_timestamp()) RETURNING \"MaCuocTroChuyen\"").param("u",user).param("mode",mode).query(Long.class).single();}
    public void cheDo(long conversation,String mode,Long admin){jdbc.sql("UPDATE \"CuocTroChuyen\" SET \"CheDoHoTro\"=:mode,\"MaQuanTriPhuTrach\"=:admin,\"NgayCapNhat\"=clock_timestamp() WHERE \"MaCuocTroChuyen\"=:id").param("id",conversation).param("mode",mode).param("admin",admin,Types.BIGINT).update();}
    public void dong(long id){jdbc.sql("UPDATE \"CuocTroChuyen\" SET \"TrangThai\"='DA_DONG',\"NgayDong\"=clock_timestamp(),\"NgayCapNhat\"=clock_timestamp() WHERE \"MaCuocTroChuyen\"=:id").param("id",id).update();}
    public List<Map<String,Object>> tin(long conversation,Long before){return tin(conversation,before,null);}
    public List<Map<String,Object>> tinSau(long conversation,long after){return tin(conversation,null,after);}
    private List<Map<String,Object>> tin(long conversation,Long before,Long after){var rows=jdbc.sql("""
        SELECT t.*,to_char(t."NgayTao" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI') AS luc_gui,
          l."MaLanXuLy",l."KetQua" AS ket_qua_bot,l."MaLyDo",d."TieuDe" AS ten_nguon,d."SoPhienBan",d."MaTaiLieuHoTro"
        FROM "TinNhan" t LEFT JOIN "LanXuLyChatbot" l ON l."MaTinNhanRa"=t."MaTinNhan" LEFT JOIN "NguonTraLoiChatbot" s ON s."MaLanXuLy"=l."MaLanXuLy" LEFT JOIN "TaiLieuHoTro" d USING ("MaTaiLieuHoTro")
        WHERE t."MaCuocTroChuyen"=:id AND (CAST(:before AS bigint) IS NULL OR t."MaTinNhan"<:before) AND (CAST(:after AS bigint) IS NULL OR t."MaTinNhan">:after)
        """+" ORDER BY t.\"MaTinNhan\" "+(after==null?"DESC":"ASC")+" LIMIT 50").param("id",conversation).param("before",before,Types.BIGINT).param("after",after,Types.BIGINT).query().listOfRows();var ordered=new ArrayList<>(rows);if(after==null)Collections.reverse(ordered);return ordered;}
    public Optional<Map<String,Object>> daGui(long user,String key){return jdbc.sql("SELECT * FROM \"TinNhan\" WHERE \"MaNguoiGui\"=:u AND \"KhoaChongLap\"=:key").param("u",user).param("key",key).query().listOfRows().stream().findFirst();}
    public int tinGanDay(long user){return jdbc.sql("SELECT count(*) FROM \"TinNhan\" WHERE \"MaNguoiGui\"=:u AND \"NgayTao\">now()-interval '1 minute'").param("u",user).query(Integer.class).single();}
    public long gui(long conversation,Long user,String type,String key,String text){long id=jdbc.sql("INSERT INTO \"TinNhan\" (\"MaCuocTroChuyen\",\"MaNguoiGui\",\"LoaiNguoiGui\",\"KhoaChongLap\",\"NoiDung\") VALUES (:id,:u,:type,:key,:text) RETURNING \"MaTinNhan\"").param("id",conversation).param("u",user,Types.BIGINT).param("type",type).param("key",key).param("text",text).query(Long.class).single();jdbc.sql("UPDATE \"CuocTroChuyen\" SET \"NgayCapNhat\"=clock_timestamp() WHERE \"MaCuocTroChuyen\"=:id").param("id",conversation).update();return id;}
    public List<Map<String,Object>> timSanPham(String question){return jdbc.sql("""
        SELECT s."TenSanPham",s."DuongDan",s."MoTaNgan",min(g."GiaHieuLuc") AS gia
        FROM "SanPham" s JOIN "ThuongHieu" h USING ("MaThuongHieu") JOIN "PhanLoai" p USING ("MaPhanLoai")
        JOIN "BienTheSanPham" b USING ("MaSanPham") JOIN "GiaKhuyenMaiBienThe" g ON g."MaBienThe"=b."MaBienThe"
        WHERE s."DangHienThi" AND h."DangHienThi" AND p."DangHienThi" AND b."DangKinhDoanh"
          AND length(s."TenSanPham")>=5 AND strpos(lower(:question),lower(s."TenSanPham"))>0
        GROUP BY s."MaSanPham" ORDER BY length(s."TenSanPham") DESC LIMIT 2
        """).param("question",question).query().listOfRows();}
    public List<Map<String,Object>> chuDe(){return jdbc.sql("SELECT c.*,c.\"ViDuCauHoi\"::text AS vi_du_json,(SELECT string_agg(x.value,E'\\n' ORDER BY x.n) FROM jsonb_array_elements_text(c.\"ViDuCauHoi\") WITH ORDINALITY x(value,n)) AS vi_du_form FROM \"ChuDeHoTro\" c ORDER BY c.\"MaChuDeHoTro\"").query().listOfRows();}
    public Map<String,Object> chuDe(long id,boolean lock){return jdbc.sql("SELECT * FROM \"ChuDeHoTro\" WHERE \"MaChuDeHoTro\"=:id"+(lock?" FOR UPDATE":"")).param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new KhongTimThayException("Không có chủ đề này."));}
    public void khoaChuDe(){jdbc.sql("SELECT pg_advisory_xact_lock(72491831)").query().listOfRows();}
    public long luuChuDe(Long id,ChuDeHoTroYeuCau f,String examples){return jdbc.sql(id==null?"INSERT INTO \"ChuDeHoTro\" (\"KyHieu\",\"TenChuDe\",\"ViDuCauHoi\",\"ChuyenNhanVien\",\"DangHoatDong\") VALUES (:code,:name,CAST(:examples AS jsonb),:handoff,:active) RETURNING \"MaChuDeHoTro\"":"UPDATE \"ChuDeHoTro\" SET \"TenChuDe\"=:name,\"ViDuCauHoi\"=CAST(:examples AS jsonb),\"ChuyenNhanVien\"=:handoff,\"DangHoatDong\"=:active WHERE \"MaChuDeHoTro\"=:id RETURNING \"MaChuDeHoTro\"").param("id",id).param("code",f.kyHieu()).param("name",f.ten().strip()).param("examples",examples).param("handoff",f.chuyenNhanVien()).param("active",f.hoatDong()).query(Long.class).single();}
    public List<Map<String,Object>> taiLieu(int page){return jdbc.sql("SELECT d.*,c.\"TenChuDe\",to_char(d.\"NgayDuyet\" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI') AS luc_duyet FROM \"TaiLieuHoTro\" d JOIN \"ChuDeHoTro\" c USING (\"MaChuDeHoTro\") ORDER BY d.\"MaTaiLieuHoTro\" DESC LIMIT 30 OFFSET :offset").param("offset",(page-1)*30).query().listOfRows();}
    public Map<String,Object> taiLieu(long id){return jdbc.sql("SELECT d.*,c.\"TenChuDe\" FROM \"TaiLieuHoTro\" d JOIN \"ChuDeHoTro\" c USING (\"MaChuDeHoTro\") WHERE d.\"MaTaiLieuHoTro\"=:id").param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new KhongTimThayException("Không tìm thấy tài liệu hỗ trợ."));}
    public long taoTaiLieu(TaiLieuHoTroYeuCau f){return jdbc.sql("INSERT INTO \"TaiLieuHoTro\" (\"MaChuDeHoTro\",\"SoPhienBan\",\"TieuDe\",\"NoiDungDaSoan\") SELECT :topic,coalesce(max(\"SoPhienBan\"),0)+1,:title,:text FROM \"TaiLieuHoTro\" WHERE \"MaChuDeHoTro\"=:topic RETURNING \"MaTaiLieuHoTro\"").param("topic",f.chuDe()).param("title",f.tieuDe().strip()).param("text",f.noiDung().strip()).query(Long.class).single();}
    public void duyet(long id,long topic,long actor){jdbc.sql("UPDATE \"TaiLieuHoTro\" SET \"TrangThai\"='NGUNG_DUNG' WHERE \"MaChuDeHoTro\"=:topic AND \"TrangThai\"='DA_DUYET'").param("topic",topic).update();jdbc.sql("UPDATE \"TaiLieuHoTro\" SET \"TrangThai\"='DA_DUYET',\"MaNguoiDuyet\"=:u,\"NgayDuyet\"=clock_timestamp() WHERE \"MaTaiLieuHoTro\"=:id AND \"TrangThai\"='NHAP'").param("u",actor).param("id",id).update();}
    public void ngung(long id){jdbc.sql("UPDATE \"TaiLieuHoTro\" SET \"TrangThai\"='NGUNG_DUNG' WHERE \"MaTaiLieuHoTro\"=:id").param("id",id).update();}
    public Optional<Map<String,Object>> daDuyet(long topic){return jdbc.sql("SELECT * FROM \"TaiLieuHoTro\" WHERE \"MaChuDeHoTro\"=:id AND \"TrangThai\"='DA_DUYET' FOR SHARE").param("id",topic).query().listOfRows().stream().findFirst();}
    public long xuLy(long conversation,long input,Long output,Long topic,String result,String reason,Long source){
        jdbc.sql("INSERT INTO \"PhanTichTinNhan\" (\"MaTinNhan\",\"MaChuDeHoTro\",\"TenMoHinh\",\"TrangThai\") VALUES (:input,:topic,'QUY_TAC_CUC_BO_V1',:state)").param("input",input).param("topic",topic,Types.BIGINT).param("state",topic==null?"KHONG_HIEU":"DA_PHAN_TICH").update();
        long turn=jdbc.sql("INSERT INTO \"LanXuLyChatbot\" (\"MaCuocTroChuyen\",\"MaTinNhanVao\",\"MaTinNhanRa\",\"KhoaChongLap\",\"PhienBanChinhSach\",\"TenMoHinh\",\"KetQua\",\"MaLyDo\",\"NgayKetThuc\") VALUES (:id,:input,:output,:key,'NGUON_DA_DUYET_V1','QUY_TAC_CUC_BO_V1',:result,:reason,clock_timestamp()) RETURNING \"MaLanXuLy\"").param("id",conversation).param("input",input).param("output",output,Types.BIGINT).param("key","support:"+input).param("result",result).param("reason",reason).query(Long.class).single();
        if(source!=null)jdbc.sql("INSERT INTO \"NguonTraLoiChatbot\" VALUES (:turn,:source)").param("turn",turn).param("source",source).update();return turn;
    }
    public List<Map<String,Object>> xuLy(long conversation){return jdbc.sql("SELECT l.*,d.\"TieuDe\",d.\"SoPhienBan\",d.\"MaTaiLieuHoTro\" FROM \"LanXuLyChatbot\" l LEFT JOIN \"NguonTraLoiChatbot\" s USING (\"MaLanXuLy\") LEFT JOIN \"TaiLieuHoTro\" d USING (\"MaTaiLieuHoTro\") WHERE l.\"MaCuocTroChuyen\"=:id ORDER BY l.\"MaLanXuLy\" DESC LIMIT 50").param("id",conversation).query().listOfRows();}
    public void kiemDuyet(long conversation,long turn,long actor,String result,String note){if(!jdbc.sql("SELECT EXISTS(SELECT 1 FROM \"LanXuLyChatbot\" WHERE \"MaLanXuLy\"=:turn AND \"MaCuocTroChuyen\"=:id)").param("turn",turn).param("id",conversation).query(Boolean.class).single())throw new KhongTimThayException("Lượt xử lý không thuộc cuộc hỗ trợ.");jdbc.sql("INSERT INTO \"KiemDuyetChatbot\" (\"MaLanXuLy\",\"MaNguoiKiemDuyet\",\"KetQua\",\"NhanXet\") VALUES (:turn,:u,:result,:note) ON CONFLICT (\"MaLanXuLy\",\"MaNguoiKiemDuyet\") DO UPDATE SET \"KetQua\"=excluded.\"KetQua\",\"NhanXet\"=excluded.\"NhanXet\",\"NgayKiemDuyet\"=clock_timestamp()").param("turn",turn).param("u",actor).param("result",result).param("note",note).update();}
    public List<Map<String,Object>> kiemDuyet(long conversation){return jdbc.sql("SELECT k.*,n.\"HoTen\" FROM \"KiemDuyetChatbot\" k JOIN \"LanXuLyChatbot\" l USING (\"MaLanXuLy\") JOIN \"NguoiDung\" n ON n.\"MaNguoiDung\"=k.\"MaNguoiKiemDuyet\" WHERE l.\"MaCuocTroChuyen\"=:id ORDER BY k.\"NgayKiemDuyet\" DESC LIMIT 50").param("id",conversation).query().listOfRows();}
}
