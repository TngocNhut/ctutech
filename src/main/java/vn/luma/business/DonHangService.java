package vn.luma.business;

import java.math.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.*;
import vn.luma.model.DiaChiChot;
import vn.luma.web.DatHangYeuCau;

@Service
public class DonHangService {
    private final DiaChiDAO addresses;private final ThongBaoService notifications;private final KhuyenMaiDAO promotions;private final DonHangDAO dao;private final GioHangDAO carts;private final TaiKhoanDAO users;private final TaiKhoanService accounts;private final ThanhToanDAO payments;private final vn.luma.config.ThanhToanCauHinh paymentConfig;
    public DonHangService(DonHangDAO dao,GioHangDAO carts,TaiKhoanDAO users,TaiKhoanService accounts,ThanhToanDAO payments,vn.luma.config.ThanhToanCauHinh paymentConfig,KhuyenMaiDAO promotions,ThongBaoService notifications,DiaChiDAO addresses){this.addresses=addresses;this.notifications=notifications;this.promotions=promotions;this.dao=dao;this.carts=carts;this.users=users;this.accounts=accounts;this.payments=payments;this.paymentConfig=paymentConfig;}
    public static String noiDung(Map<Long,Integer> lines){return lines.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getKey()+":"+e.getValue()).collect(java.util.stream.Collectors.joining(","));}
    public static Map<Long,Integer> docNoiDung(String value){
        if(value==null||value.length()>2000||value.isBlank())throw new IllegalArgumentException("Giỏ hàng đang trống.");
        Map<Long,Integer> result=new TreeMap<>();
        for(String item:value.split(",")){
            if(!item.matches("[1-9][0-9]{0,17}:[1-9][0-9]?"))throw new IllegalArgumentException("Nội dung giỏ không hợp lệ.");
            var parts=item.split(":");long id=Long.parseLong(parts[0]);
            if(result.put(id,Integer.parseInt(parts[1]))!=null)throw new IllegalArgumentException("SKU bị lặp trong yêu cầu.");
        }
        if(result.size()>50)throw new IllegalArgumentException("Mỗi đơn tối đa 50 cấu hình.");return result;
    }
    private String dauVanTay(DatHangYeuCau f){
        var d=f.diaChi();StringBuilder result=new StringBuilder();
        for(String s:List.of(noiDung(docNoiDung(f.noiDungGio())),f.maGiamGia()==null?"":f.maGiamGia().strip().toUpperCase(Locale.ROOT),d.tenNguoiNhan().strip(),d.soDienThoai().strip(),d.tinhThanh().strip(),d.phuongXa().strip(),d.diaChiCuThe().strip(),d.ghiChu()==null?"":d.ghiChu().strip(),d.viDo()==null?"":d.viDo().stripTrailingZeros().toPlainString(),d.kinhDo()==null?"":d.kinhDo().stripTrailingZeros().toPlainString(),f.phuongThuc()))result.append(s.length()).append(':').append(s);
        return TaiKhoanService.bam(result.toString());
    }
    public BigDecimal phiGiao(){return dao.phiGiao();}
    public String noiDungGio(String email){return noiDung(carts.doc(accounts.hienTai(email).maNguoiDung()));}
    @Transactional public String datCOD(String email,DatHangYeuCau request,DiaChiChot address){return datHang(email,request,address);}
    @Transactional public String datHang(String email,DatHangYeuCau request,DiaChiChot address){
        String channel="COD".equals(request.phuongThuc())?null:paymentConfig.kenh(request.phuongThuc());
        UUID.fromString(request.khoaChongLap());
        var user=accounts.hienTai(email);users.khoaNguoiDung(user.maNguoiDung());
        user=accounts.hienTai(email);
        if(!user.trangThai().equals("HOAT_DONG"))throw new IllegalArgumentException("Tài khoản không khả dụng.");
        String hash=dauVanTay(request);var previous=dao.theoKhoa(user.maNguoiDung(),request.khoaChongLap());
        if(previous.isPresent()){
            if(!hash.equals(previous.get().get("DauVanTayDatHang")))throw new IllegalArgumentException("Yêu cầu đã được dùng với nội dung khác. Hãy tải lại trang đặt hàng.");
            return (String)previous.get().get("MaDonCongKhai");
        }
        var wanted=docNoiDung(request.noiDungGio());carts.khoaGio(user.maNguoiDung());
        if(!wanted.equals(carts.doc(user.maNguoiDung())))throw new IllegalArgumentException("Giỏ hàng đã thay đổi. Hãy tải lại trang đặt hàng.");
        var tinh=tinhTien(wanted,user.maNguoiDung(),request.maGiamGia());
        var prices=tinh.prices();var amounts=tinh.amounts();var voucher=tinh.voucher();
        var subtotal=tinh.subtotal();var discount=tinh.discount();
        var allocations=phanBo(amounts,discount,subtotal);
        String code="LM-"+UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT);
        long order=dao.tao(user.maNguoiDung(),user.thuDienTu(),code,request.khoaChongLap(),hash,address,subtotal,discount,dao.phiGiao(),voucher,channel==null?"COD":"PAYOS");
        for(var entry:wanted.entrySet()){dao.themDong(order,entry.getKey(),entry.getValue(),prices.get(entry.getKey()),allocations.get(entry.getKey()));promotions.chot(order,tinh.uuDai().get(entry.getKey()));}
        if(channel!=null)payments.taoPhien(order,channel,"PAYOS_MO_PHONG".equals(request.phuongThuc())?"MO_PHONG":"TICH_HOP_THAT");
        if(addresses.danhSach(user.maNguoiDung()).isEmpty())addresses.tao(user.maNguoiDung(),address,true);
        ghiLichSu(order,user.maNguoiDung(),null,"CHO_XAC_NHAN",channel==null?"Khách đặt đơn COD":"Khách đặt đơn chờ thanh toán PayOS");carts.xoa(user.maNguoiDung());return code;
    }
    private record TinhTien(Map<Long,BigDecimal> prices,Map<Long,BigDecimal> amounts,Map<String,Object> voucher,BigDecimal subtotal,BigDecimal discount,Map<Long,KhuyenMaiDAO.Gia> uuDai) { }
    public record BaoGia(BigDecimal tienHang,BigDecimal giamGia,BigDecimal phiGiao,BigDecimal tongThanhToan) { }
    @Transactional public BaoGia baoGia(String email,DatHangYeuCau request){
        var user=accounts.hienTai(email);carts.khoaGio(user.maNguoiDung());
        var wanted=docNoiDung(request.noiDungGio());
        if(!wanted.equals(carts.doc(user.maNguoiDung())))throw new IllegalArgumentException("Giỏ hàng đã thay đổi. Hãy tải lại trang đặt hàng.");
        var tinh=tinhTien(wanted,user.maNguoiDung(),request.maGiamGia());var fee=dao.phiGiao();
        return new BaoGia(tinh.subtotal(),tinh.discount(),fee,tinh.subtotal().subtract(tinh.discount()).add(fee));
    }
    private TinhTien tinhTien(Map<Long,Integer> wanted,long userId,String couponInput){
        String coupon=couponInput==null?"":couponInput.strip().toUpperCase(Locale.ROOT);
        Map<String,Object> voucher=coupon.isBlank()?null:dao.khoaVoucher(coupon);
        Map<Long,BigDecimal> amounts=new TreeMap<>(),prices=new TreeMap<>();
        for(var entry:wanted.entrySet()){
            var sku=dao.khoaSku(entry.getKey());
            int available=((Number)sku.get("SoLuongTon")).intValue()-((Number)sku.get("SoLuongGiu")).intValue();
            if(!Boolean.TRUE.equals(sku.get("cho_mua"))||available<entry.getValue())throw new IllegalArgumentException("Một cấu hình vừa hết hàng hoặc không đủ số lượng.");
            }
        // All SKU/base-price locks are held before reading one common campaign-price snapshot.
        var offers=promotions.gia(wanted.keySet());
        for(var entry:wanted.entrySet()){
            var offer=offers.get(entry.getKey());
            if(offer==null)throw new IllegalArgumentException("Một cấu hình không còn khả dụng.");
            if(voucher!=null&&offer.chuongTrinh()!=null&&!offer.kemMa())throw new IllegalArgumentException("Chiến dịch của một cấu hình không cho dùng kèm mã giảm giá. Hãy xóa mã hoặc đổi cấu hình.");
            BigDecimal price=offer.gia();prices.put(entry.getKey(),price);amounts.put(entry.getKey(),price.multiply(BigDecimal.valueOf(entry.getValue())));
        }
        BigDecimal subtotal=amounts.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add);
        if(subtotal.compareTo(new BigDecimal("999999999000000"))>0)throw new IllegalArgumentException("Giá trị đơn vượt giới hạn hỗ trợ.");
        BigDecimal discount=BigDecimal.ZERO;
        if(voucher!=null){
            long id=((Number)voucher.get("MaGiamGia")).longValue();
            if(!Boolean.TRUE.equals(voucher.get("DangHoatDong"))||!dao.voucherConHan(id)||subtotal.compareTo((BigDecimal)voucher.get("DonToiThieu"))<0)
                throw new IllegalArgumentException("Mã giảm giá chưa đủ điều kiện hoặc đã hết hạn.");
            if(dao.luotVoucher(id,null)>=((Number)voucher.get("TongLuotToiDa")).longValue()||dao.luotVoucher(id,userId)>=((Number)voucher.get("LuotMoiKhach")).longValue())throw new IllegalArgumentException("Mã giảm giá đã hết lượt sử dụng.");
            discount=(BigDecimal)voucher.get("GiaTriGiam");
            if("PHAN_TRAM".equals(voucher.get("KieuGiam")))discount=subtotal.multiply(discount).divide(BigDecimal.valueOf(100),0,RoundingMode.DOWN).min((BigDecimal)voucher.get("GiamToiDa"));
            discount=discount.min(subtotal).setScale(0,RoundingMode.DOWN);
        }
        return new TinhTien(prices,amounts,voucher,subtotal,discount,offers);
    }
    private Map<Long,BigDecimal> phanBo(Map<Long,BigDecimal> amounts,BigDecimal discount,BigDecimal total){
        Map<Long,BigDecimal> result=new TreeMap<>(),remainders=new TreeMap<>();BigDecimal assigned=BigDecimal.ZERO;
        for(var e:amounts.entrySet()){
            BigDecimal value=discount.multiply(e.getValue());var floor=value.divide(total,0,RoundingMode.DOWN);
            result.put(e.getKey(),floor);remainders.put(e.getKey(),value.remainder(total));assigned=assigned.add(floor);
        }
        int left=discount.subtract(assigned).intValueExact();
        var order=remainders.entrySet().stream().sorted(Map.Entry.<Long,BigDecimal>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())).toList();
        for(int i=0;i<left;i++)result.merge(order.get(i).getKey(),BigDecimal.ONE,BigDecimal::add);
        return result;
    }
    @Transactional public void ghiLichSu(long order,long actor,String old,String state,String note){
        long event=dao.lichSu(order,actor,old,state,note);var recipient=dao.nguoiNhanThongBao(order);String code=(String)recipient.get("MaDonCongKhai");
        String message=note==null||note.isBlank()?"Mở đơn hàng để xem trạng thái hiện tại.":note;
        if(message.length()>800)message=message.substring(0,800);
        notifications.tao(((Number)recipient.get("MaNguoiDung")).longValue(),"DON_HANG",(Boolean.TRUE.equals(recipient.get("mo_phong"))?"Mô phỏng · ":"")+(old==null?"Đã tiếp nhận đơn hàng":"Đơn hàng có cập nhật mới"),code+": "+message,"/don-hang/"+code,"order-history:"+event);
    }
    public List<Map<String,Object>> danhSach(String email){return danhSach(email,1);}
    public List<Map<String,Object>> danhSach(String email,int page){
        if(page<1||page>10000)throw new IllegalArgumentException("Trang không hợp lệ.");long user=accounts.hienTai(email).maNguoiDung();var orders=dao.danhSach(user,page);
        var ids=orders.stream().map(o->((Number)o.get("MaDonHang")).longValue()).toList();var items=dao.dongDanhSach(user,ids).stream().collect(java.util.stream.Collectors.groupingBy(d->((Number)d.get("MaDonHang")).longValue()));
        return orders.stream().map(order->{var row=new java.util.HashMap<>(order);row.put("sanPham",items.getOrDefault(((Number)order.get("MaDonHang")).longValue(),java.util.List.of()));return (Map<String,Object>)row;}).toList();
    }
    public Map<String,Object> chiTiet(String email,String code){
        var user=accounts.hienTai(email);var order=dao.theoMa(code,false);
        if(((Number)order.get("MaNguoiDung")).longValue()!=user.maNguoiDung()&&!user.vaiTro().equals("QuanTri")&&!(user.vaiTro().equals("NhanVien")&&user.trangThai().equals("HOAT_DONG")))throw new KhongTimThayException("Không tìm thấy đơn hàng.");return order;
    }
    public List<Map<String,Object>> dong(String email,String code){return dao.dong(((Number)chiTiet(email,code).get("MaDonHang")).longValue());}
    public List<Map<String,Object>> lichSu(String email,String code){return dao.lichSu(((Number)chiTiet(email,code).get("MaDonHang")).longValue());}
    @Transactional public void huy(String email,String code){
        var user=accounts.hienTai(email);users.khoaNguoiDung(user.maNguoiDung());
        var initial=chiTiet(email,code);if("PAYOS".equals(initial.get("PhuongThucThanhToan")))throw new IllegalArgumentException("Đơn PayOS cần xử lý qua dịch vụ thanh toán.");if(initial.get("MaGiamGia")!=null)dao.khoaVoucherId(((Number)initial.get("MaGiamGia")).longValue());
        var order=dao.theoMa(code,true);
        String state=(String)order.get("TrangThaiDon");
        if(Set.of("DA_HUY","HET_HAN").contains(state))return;
        if(!Set.of("CHO_XAC_NHAN","DA_XAC_NHAN").contains(state)||!"DANG_GIU".equals(order.get("TrangThaiKho"))||!"CHUA_THU".equals(order.get("TrangThaiThanhToan")))throw new IllegalArgumentException("Đơn này không thể tự hủy. Hãy liên hệ cửa hàng để được hỗ trợ.");
        long id=((Number)order.get("MaDonHang")).longValue();
        for(var line:dao.dong(id)){long sku=((Number)line.get("MaBienThe")).longValue();dao.khoaSku(sku);dao.nhaGiu(id,sku,((Number)line.get("SoLuong")).intValue());}
        dao.huy(id,"DA_HUY","Hủy trước khi bàn giao");ghiLichSu(id,user.maNguoiDung(),state,"DA_HUY","Hủy trước khi bàn giao");
    }
    @Transactional public void hetHanCOD(String code){
        var initial=dao.theoMa(code,false);long owner=((Number)initial.get("MaNguoiDung")).longValue();users.khoaNguoiDung(owner);
        if(initial.get("MaGiamGia")!=null)dao.khoaVoucherId(((Number)initial.get("MaGiamGia")).longValue());
        var order=dao.theoMa(code,true);long id=((Number)order.get("MaDonHang")).longValue();
        if(!"COD".equals(order.get("PhuongThucThanhToan"))||!"CHO_XAC_NHAN".equals(order.get("TrangThaiDon"))||!"CHUA_THU".equals(order.get("TrangThaiThanhToan"))||!"DANG_GIU".equals(order.get("TrangThaiKho"))||!dao.quaHan(id))return;
        for(var line:dao.dong(id)){long sku=((Number)line.get("MaBienThe")).longValue();dao.khoaSku(sku);dao.nhaGiu(id,sku,((Number)line.get("SoLuong")).intValue());}
        dao.huy(id,"HET_HAN","Quá hạn xác nhận COD");ghiLichSu(id,owner,"CHO_XAC_NHAN","HET_HAN","Quá hạn xác nhận COD");
    }
}
