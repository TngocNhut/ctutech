package vn.luma.data;

import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.model.DiaChiChot;

@Repository
public class DiaChiDAO {
    private final JdbcClient jdbc;
    public DiaChiDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public List<Map<String,Object>> danhSach(long user){return jdbc.sql("SELECT * FROM \"DiaChi\" WHERE \"MaNguoiDung\"=:u ORDER BY \"LaMacDinh\" DESC,\"MaDiaChi\" DESC").param("u",user).query().listOfRows();}
    public Map<String,Object> tim(long user,long id){return jdbc.sql("SELECT * FROM \"DiaChi\" WHERE \"MaNguoiDung\"=:u AND \"MaDiaChi\"=:id").param("u",user).param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new vn.luma.business.KhongTimThayException("Không tìm thấy địa chỉ."));}
    public void sua(long user,long id,DiaChiChot d,boolean macDinh){
        if(macDinh)jdbc.sql("UPDATE \"DiaChi\" SET \"LaMacDinh\"=false WHERE \"MaNguoiDung\"=:u").param("u",user).update();
        if(jdbc.sql("""
            UPDATE "DiaChi" SET "TenNguoiNhan"=:ten,"SoDienThoaiNhan"=:phone,"TinhThanh"=:tinh,"PhuongXa"=:xa,"DiaChiCuThe"=:street,
              "GhiChuGiaoHang"=:note,"LaMacDinh"=:def,"NgayCapNhat"=now(),"MaDiaDiemGoogle"=:place,"ViDo"=:lat,"KinhDo"=:lng,
              "NguonToaDo"=:source,"NgayLayToaDo"=:ngay,"HanLuuToaDo"=:han,"NgayXacNhanViTri"=:ngay
            WHERE "MaNguoiDung"=:u AND "MaDiaChi"=:id
            """).param("u",user).param("id",id).param("ten",d.tenNguoiNhan()).param("phone",d.soDienThoai()).param("tinh",d.tinhThanh()).param("xa",d.phuongXa()).param("street",d.diaChiCuThe()).param("note",d.ghiChu()).param("def",macDinh)
            .param("place",d.placeId()).param("lat",d.viDo()).param("lng",d.kinhDo()).param("source",d.viDo()==null?null:"GOOGLE_GEOCODING")
            .param("ngay",d.ngayLay(),java.sql.Types.TIMESTAMP_WITH_TIMEZONE).param("han",d.hanLuu(),java.sql.Types.TIMESTAMP_WITH_TIMEZONE).update()!=1)throw new vn.luma.business.KhongTimThayException("Không tìm thấy địa chỉ.");
    }
    public void tao(long user,DiaChiChot d,boolean macDinh){
        if(macDinh)jdbc.sql("UPDATE \"DiaChi\" SET \"LaMacDinh\"=false WHERE \"MaNguoiDung\"=:u").param("u",user).update();
        jdbc.sql("""
            INSERT INTO "DiaChi" ("MaNguoiDung","TenNguoiNhan","SoDienThoaiNhan","TinhThanh","PhuongXa","DiaChiCuThe","MaQuocGia","GhiChuGiaoHang","LaMacDinh","NgayCapNhat",
            "MaDiaDiemGoogle","ViDo","KinhDo","NguonToaDo","NgayLayToaDo","HanLuuToaDo","NgayXacNhanViTri")
            VALUES (:u,:ten,:phone,:tinh,:xa,:street,'VN',:note,:def,now(),:place,:lat,:lng,:source,:ngay,:han,:ngay)
            """).param("u",user).param("ten",d.tenNguoiNhan()).param("phone",d.soDienThoai()).param("tinh",d.tinhThanh()).param("xa",d.phuongXa()).param("street",d.diaChiCuThe()).param("note",d.ghiChu()).param("def",macDinh)
            .param("place",d.placeId()).param("lat",d.viDo()).param("lng",d.kinhDo()).param("source",d.viDo()==null?null:"GOOGLE_GEOCODING")
            .param("ngay",d.ngayLay(),java.sql.Types.TIMESTAMP_WITH_TIMEZONE).param("han",d.hanLuu(),java.sql.Types.TIMESTAMP_WITH_TIMEZONE).update();
    }
    public void xoa(long user,long id){if(jdbc.sql("DELETE FROM \"DiaChi\" WHERE \"MaNguoiDung\"=:u AND \"MaDiaChi\"=:id").param("u",user).param("id",id).update()!=1)throw new vn.luma.business.KhongTimThayException("Không tìm thấy địa chỉ.");}
    public void donCache(){
        jdbc.sql("UPDATE \"DiaChi\" SET \"ViDo\"=NULL,\"KinhDo\"=NULL,\"NguonToaDo\"=NULL,\"NgayLayToaDo\"=NULL,\"HanLuuToaDo\"=NULL,\"NgayXacNhanViTri\"=NULL WHERE \"HanLuuToaDo\"<now()").update();
        jdbc.sql("UPDATE \"DonHang\" SET \"ViDoNhan\"=NULL,\"KinhDoNhan\"=NULL,\"NguonToaDoNhan\"=NULL,\"NgayLayToaDoNhan\"=NULL,\"HanLuuToaDoNhan\"=NULL,\"NgayXacNhanViTriNhan\"=NULL WHERE \"HanLuuToaDoNhan\"<now()").update();
    }
}
