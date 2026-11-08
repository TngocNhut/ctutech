package vn.luma.business;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.*;
import vn.luma.web.NguoiDungYeuCau;

@Service
public class QuanLyNguoiDungService {
    private final QuanLyNguoiDungDAO dao;private final TaiKhoanDAO accounts;private final TaiKhoanService users;private final QuanLySanPhamDAO audit;
    public QuanLyNguoiDungService(QuanLyNguoiDungDAO dao,TaiKhoanDAO accounts,TaiKhoanService users,QuanLySanPhamDAO audit){this.dao=dao;this.accounts=accounts;this.users=users;this.audit=audit;}
    public List<Map<String,Object>> nhanVien(String email,String q,int page){users.quanTri(email);kiemTra(q,page);return dao.nhanVien(q.strip(),page);}
    public long demNhanVien(String email,String q){users.quanTri(email);kiemTra(q,1);return dao.demNhanVien(q.strip());}
    @Transactional public long taoNhanVien(String email,vn.luma.web.DangKyYeuCau form){var actor=users.quanTri(email);long id=users.taoNhanVienTrucTiep(form);audit.log(actor.maNguoiDung(),id,"TAO_NHAN_VIEN","NGUOI_DUNG");return id;}
    public List<Map<String,Object>> danhSach(String email,String q,int page){users.quanTri(email);kiemTra(q,page);return dao.danhSach(q.strip(),page);}
    public long dem(String email,String q){users.quanTri(email);kiemTra(q,1);return dao.dem(q.strip());}
    private void kiemTra(String q,int page){if(q==null||q.length()>100||page<1||page>10000)throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự; trang từ 1 đến 10000.");}
    public Map<String,Object> chiTiet(String email,long id){users.quanTri(email);return dao.tim(id);}
    @Transactional public void luu(String email,long id,NguoiDungYeuCau f){
        users.quanTri(email);dao.khoaQuanTri();var actor=users.quanTri(email);accounts.khoaNguoiDung(id);var target=dao.tim(id);
        if(!Set.of("QuanTri","KhachHang","NhanVien").contains(f.vaiTro())||!Set.of("HOAT_DONG","BI_KHOA").contains(f.trangThai())||f.lyDo()==null||f.lyDo().isBlank()||f.lyDo().length()>255)throw new IllegalArgumentException("Vai trò, trạng thái hoặc lý do không hợp lệ.");
        boolean changed=!f.vaiTro().equals(target.get("TenVaiTro"))||!f.trangThai().equals(target.get("TrangThai"));
        if(!changed)return;
        if(id==actor.maNguoiDung())throw new IllegalArgumentException("Không thể tự khóa hoặc đổi vai trò của chính bạn. Hãy nhờ một quản trị khác thực hiện.");
        if("QuanTri".equals(f.vaiTro())&&target.get("NgayXacMinhThuDienTu")==null)throw new IllegalArgumentException("Tài khoản phải xác minh email trước khi cấp quyền quản trị.");
        if("QuanTri".equals(target.get("TenVaiTro"))&&"HOAT_DONG".equals(target.get("TrangThai"))&&(!"QuanTri".equals(f.vaiTro())||!"HOAT_DONG".equals(f.trangThai()))&&dao.soQuanTri()<=1)throw new IllegalArgumentException("Cần giữ ít nhất một quản trị đang hoạt động.");
        dao.luu(id,f);dao.thuHoi(id);dao.baoMat(id,null,"THU_HOI_PHIEN","THANH_CONG","DOI_QUYEN_HOAC_TRANG_THAI");audit.log(actor.maNguoiDung(),id,"SUA_NGUOI_DUNG","NGUOI_DUNG",f.lyDo().strip());
    }
    @Transactional public void thuHoi(String email,long id){users.quanTri(email);dao.khoaQuanTri();var actor=users.quanTri(email);accounts.khoaNguoiDung(id);dao.tim(id);dao.thuHoi(id);dao.baoMat(id,null,"THU_HOI_PHIEN","THANH_CONG","QUAN_TRI_THU_HOI");audit.log(actor.maNguoiDung(),id,"THU_HOI_PHIEN","NGUOI_DUNG");}
    public List<Map<String,Object>> nhatKy(String email,boolean security,int page){users.quanTri(email);kiemTra("",page);return dao.nhatKy(security,page);}
}
