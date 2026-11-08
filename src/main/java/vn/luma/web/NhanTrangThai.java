package vn.luma.web;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class NhanTrangThai {
    @ModelAttribute("nhanTrangThai") public Map<String,String> nhan(){return Map.ofEntries(
        Map.entry("CHO_XAC_NHAN","Chờ cửa hàng xác nhận"),Map.entry("DA_XAC_NHAN","Đã xác nhận"),
        Map.entry("DOI_SOAT_VAN_DON","Đối soát vận đơn"),Map.entry("PHU_PHI","Phụ phí"),Map.entry("GIAM_CUOC","Giảm cước"),Map.entry("QUA_HAN_GIAO","Đã quá hẹn giao"),Map.entry("CHO_DUYET","Chờ duyệt trả"),Map.entry("CHO_NHAN_HANG","Chờ hàng về kho"),Map.entry("CHO_HOAN_TIEN","Chờ hoàn tiền"),Map.entry("TU_CHOI","Đã từ chối"),Map.entry("DANG_HOAN","Đang hoàn về"),Map.entry("DA_VE_KHO","Đã về kho"),Map.entry("CHO_XU_LY","Chờ xử lý"),Map.entry("THANH_CONG","Đã hoàn thành theo chứng từ"),Map.entry("THAT_BAI","Chưa hoàn được"),Map.entry("HOAN_MOT_PHAN","Đã hoàn một phần"),Map.entry("CHO_DUYET_TRA","Duyệt yêu cầu trả"),Map.entry("CHO_KIEM_HANG","Kiểm hàng hoàn về"),
        Map.entry("CHO_BAN_GIAO","Chờ bàn giao"),Map.entry("DA_GIAO","Đã giao hàng"),Map.entry("CUOC_GOC","Cước vận chuyển"),Map.entry("MOI","Mới tiếp nhận"),Map.entry("DANG_XU_LY","Đang xử lý"),Map.entry("DA_XONG","Đã xử lý"),
        Map.entry("DANG_VAN_CHUYEN","Đang vận chuyển"),Map.entry("CHO_GIAO_HANG","Đang chờ giao"),
        Map.entry("GIAO_THAT_BAI","Giao chưa thành công"),Map.entry("CHO_HANG_HOAN_VE","Chờ hàng hoàn về"),
        Map.entry("HOAN_THANH","Hoàn thành"),Map.entry("DA_HUY","Đã hủy"),Map.entry("HET_HAN","Hết hạn"),
        Map.entry("CHUA_THU","Chờ thu tiền"),Map.entry("THU_THIEU","Đã thu một phần"),
        Map.entry("CAN_DOI_SOAT","Đang đối soát"),Map.entry("DA_THU","Đã thu tiền"),Map.entry("CHO_HOAN","Chờ hoàn tiền"),Map.entry("DA_HOAN","Đã hoàn tiền"),
        Map.entry("DANG_GIU","Đang giữ hàng"),Map.entry("DA_XUAT","Đã xuất kho"),Map.entry("DA_TRA_GIU","Đã trả giữ hàng"),
        Map.entry("MOI_TAO","Chờ tạo link thanh toán"),Map.entry("CHO_THANH_TOAN","Chờ thanh toán"),Map.entry("DU_TIEN","Đã xác minh đủ tiền"),Map.entry("THU_DU","Có tiền dư, chờ đối soát"),Map.entry("LOI_TAO_LINK","Chưa tạo được link"));}
}
