package vn.luma.business;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static vn.luma.data.ThanhToanDAO.id;

@Service
public class MuaLaiService {
    private final DonHangService orders;private final TaiKhoanService users;private final GioHangService carts;
    public MuaLaiService(DonHangService orders,TaiKhoanService users,GioHangService carts){this.orders=orders;this.users=users;this.carts=carts;}
    @Transactional public GioHangService.KetQuaGop them(String email,String code){var order=orders.chiTiet(email,code);if(id(order,"MaNguoiDung")!=users.hienTai(email).maNguoiDung())throw new KhongTimThayException("Không tìm thấy đơn của bạn.");if(!Set.of("HOAN_THANH","DA_HUY","HET_HAN").contains(order.get("TrangThaiDon")))throw new IllegalArgumentException("Chỉ thêm lại từ đơn đã hoàn thành, đã hủy hoặc hết hạn.");var items=new TreeMap<Long,Integer>();for(var line:orders.dong(email,code))items.put(id(line,"MaBienThe"),((Number)line.get("SoLuong")).intValue());return carts.gop(email,items);}
}
