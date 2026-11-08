package vn.luma.business;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import vn.luma.data.*;
import vn.luma.web.CauHinhHeThongYeuCau;

@Service
public class CauHinhHeThongService {
    private final CauHinhHeThongDAO dao;private final TaiKhoanService users;private final QuanLySanPhamDAO audit;private final ObjectMapper json;
    public CauHinhHeThongService(CauHinhHeThongDAO dao,TaiKhoanService users,QuanLySanPhamDAO audit,ObjectMapper json){this.dao=dao;this.users=users;this.audit=audit;this.json=json;}
    public CauHinhHeThongYeuCau hienTai(){var values=dao.doc();return new CauHinhHeThongYeuCau(Boolean.parseBoolean(values.getOrDefault("BAT_CHATBOT","false")),new BigDecimal(values.getOrDefault("PHI_GIAO_HANG_MAC_DINH","30000")),values.getOrDefault("THONG_BAO_TRANG_CHU",""));}
    public CauHinhHeThongYeuCau quanTri(String email){users.quanTri(email);return hienTai();}
    @Transactional public void luu(String email,CauHinhHeThongYeuCau f){long actor=users.quanTri(email).maNguoiDung();if(f.phiGiaoHang()==null||f.phiGiaoHang().signum()<0||f.phiGiaoHang().compareTo(new BigDecimal("10000000"))>0||f.phiGiaoHang().stripTrailingZeros().scale()>0||f.thongBao()==null||f.thongBao().length()>300)throw new IllegalArgumentException("Phí giao hàng là số VND nguyên từ 0 đến 10 triệu; thông báo tối đa 300 ký tự.");dao.luu("BAT_CHATBOT",Boolean.toString(f.batChatbot()),actor);dao.luu("PHI_GIAO_HANG_MAC_DINH",f.phiGiaoHang().toPlainString(),actor);dao.luu("THONG_BAO_TRANG_CHU",json.writeValueAsString(f.thongBao().strip()),actor);audit.log(actor,0,"LUU_CAU_HINH_HE_THONG","CAU_HINH_HE_THONG");}
}
