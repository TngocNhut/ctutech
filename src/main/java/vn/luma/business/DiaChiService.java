package vn.luma.business;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.*;
import vn.luma.model.DiaChiChot;

@Service
public class DiaChiService {
    private final DiaChiDAO dao;private final TaiKhoanDAO users;private final TaiKhoanService accounts;
    public DiaChiService(DiaChiDAO dao,TaiKhoanDAO users,TaiKhoanService accounts){this.dao=dao;this.users=users;this.accounts=accounts;}
    @Transactional public List<Map<String,Object>> danhSach(String email){dao.donCache();return dao.danhSach(accounts.hienTai(email).maNguoiDung());}
    @Transactional public void tao(String email,DiaChiChot d,boolean macDinh){long id=accounts.hienTai(email).maNguoiDung();users.khoaNguoiDung(id);if(dao.danhSach(id).size()>=20)throw new IllegalArgumentException("Sổ địa chỉ tối đa 20 địa chỉ.");dao.tao(id,d,macDinh);}
    public Map<String,Object> chiTiet(String email,long id){return dao.tim(accounts.hienTai(email).maNguoiDung(),id);}
    public boolean diemConHan(Map<String,Object> row){return row.get("ViDo")!=null&&row.get("HanLuuToaDo") instanceof java.sql.Timestamp t&&t.toInstant().isAfter(java.time.Instant.now());}
    @Transactional public void sua(String email,long id,DiaChiChot d,boolean macDinh,boolean giuDiemCu){
        long user=accounts.hienTai(email).maNguoiDung();users.khoaNguoiDung(user);var old=dao.tim(user,id);
        if(giuDiemCu&&d.viDo()==null){
            if(!diemConHan(old))throw new IllegalArgumentException("Điểm bản đồ cũ đã hết hạn. Bỏ chọn giữ điểm cũ hoặc chọn lại điểm trên bản đồ.");
            if(!d.tinhThanh().equals(old.get("TinhThanh"))||!d.phuongXa().equals(old.get("PhuongXa"))||!d.diaChiCuThe().equals(old.get("DiaChiCuThe")))throw new IllegalArgumentException("Địa chỉ đã thay đổi. Bỏ chọn giữ điểm cũ để dùng địa chỉ nhập tay, hoặc chọn điểm mới.");
            d=new DiaChiChot(d.tenNguoiNhan(),d.soDienThoai(),d.tinhThanh(),d.phuongXa(),d.diaChiCuThe(),d.ghiChu(),(String)old.get("MaDiaDiemGoogle"),
                (java.math.BigDecimal)old.get("ViDo"),(java.math.BigDecimal)old.get("KinhDo"),((java.sql.Timestamp)old.get("NgayLayToaDo")).toInstant().atOffset(java.time.ZoneOffset.UTC),((java.sql.Timestamp)old.get("HanLuuToaDo")).toInstant().atOffset(java.time.ZoneOffset.UTC));
        }
        dao.sua(user,id,d,macDinh);
    }
    @Transactional public void xoa(String email,long id){long user=accounts.hienTai(email).maNguoiDung();users.khoaNguoiDung(user);dao.xoa(user,id);}
    @Transactional public void donCache(){dao.donCache();}
}
