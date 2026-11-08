package vn.luma.business;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.*;
import vn.luma.web.*;
import static vn.luma.data.ThanhToanDAO.id;

@Service
public class TraHangService {
    private final TraHangDAO dao;private final DonHangDAO orders;private final DonHangService orderService;private final TaiKhoanService users;private final GiaoNhanDAO shipping;private final ThanhToanDAO payments;private final HoanTienDAO refunds;
    public TraHangService(TraHangDAO dao,DonHangDAO orders,DonHangService orderService,TaiKhoanService users,GiaoNhanDAO shipping,ThanhToanDAO payments,HoanTienDAO refunds){this.dao=dao;this.orders=orders;this.orderService=orderService;this.users=users;this.shipping=shipping;this.payments=payments;this.refunds=refunds;}
    public List<Map<String,Object>> danhSach(String admin){return danhSach(admin,1);}
    public List<Map<String,Object>> danhSach(String admin,int page){users.quanTri(admin);if(page<1||page>10000)throw new IllegalArgumentException("Trang không hợp lệ.");return dao.danhSach(page);}
    public List<Map<String,Object>> theoDon(String email,String code){return dao.theoDon(id(orderService.chiTiet(email,code),"MaDonHang"));}
    public Map<String,Object> chiTiet(String email,long ticket){var row=dao.doc(ticket,false);var order=orderService.chiTiet(email,payments.maDon(id(row,"MaDonHang")));var result=new HashMap<>(row);result.put("MaDonCongKhai",order.get("MaDonCongKhai"));return result;}
    public List<Map<String,Object>> dong(String email,long ticket){chiTiet(email,ticket);return dao.dong(ticket);}
    public Optional<Map<String,Object>> vanDon(String email,long ticket){chiTiet(email,ticket);return dao.vanDon(ticket);}
    public List<Map<String,Object>> lichSuVanDon(String email,long ticket){return vanDon(email,ticket).map(v->shipping.lichSu(id(v,"MaVanDon"))).orElse(List.of());}
    private Map<String,Object> khoaDon(String code){var row=orders.theoMa(code,false);if(row.get("MaGiamGia")!=null)orders.khoaVoucherId(id(row,"MaGiamGia"));return orders.theoMa(code,true);}
    public List<Map<String,Object>> coTheTra(String email,String code){
        var order=orderService.chiTiet(email,code);return orders.dong(id(order,"MaDonHang")).stream().map(line->{var row=new HashMap<>(line);row.put("ConCoTheTra",((Number)line.get("SoLuong")).intValue()-dao.daNhan(id(line,"MaChiTietDonHang"),0));return (Map<String,Object>)row;}).toList();
    }
    private Map<Long,Integer> yeuCau(TaoTraHangYeuCau form){
        UUID.fromString(form.khoaChongLap());if(form.lyDo()==null||form.lyDo().isBlank()||form.lyDo().length()>500||form.dong()==null||form.dong().isEmpty()||form.dong().size()>50)throw new IllegalArgumentException("Cần chọn hàng và nêu lý do trả.");
        Map<Long,Integer> result=new TreeMap<>();Set<Long> seen=new HashSet<>();
        for(var line:form.dong()){if(!seen.add(line.maDong())||line.soLuong()<0||line.soLuong()>99)throw new IllegalArgumentException("Dòng trả bị trùng hoặc số lượng không hợp lệ.");if(line.soLuong()>0)result.put(line.maDong(),line.soLuong());}
        if(result.isEmpty())throw new IllegalArgumentException("Chọn ít nhất một máy cần trả.");return result;
    }
    @Transactional public long tao(String email,String code,TaoTraHangYeuCau form,boolean failedDelivery){
        var actor=users.hienTai(email);if(failedDelivery)users.quanTri(email);orderService.chiTiet(email,code);var order=khoaDon(code);long oid=id(order,"MaDonHang");Map<Long,Integer> requested=yeuCau(form);String type=failedDelivery?"GIAO_THAT_BAI":"SAU_GIAO";
        var previous=dao.theoKhoa(oid,form.khoaChongLap());if(previous.isPresent()){
            long ticket=id(previous.get(),"MaPhieuTraHang");Map<Long,Integer> old=new TreeMap<>();for(var row:dao.dong(ticket))old.put(id(row,"MaChiTietDonHang"),((Number)row.get("SoLuongYeuCau")).intValue());
            if(old.equals(requested)&&form.lyDo().strip().equals(previous.get().get("LyDo"))&&type.equals(previous.get().get("LoaiTraHang")))return ticket;
            throw new IllegalArgumentException("Yêu cầu trả đã được gửi với nội dung khác.");
        }
        if(dao.dangMo(oid))throw new IllegalArgumentException("Đơn đang có phiếu trả cần xử lý.");
        var outbound=shipping.vanDon(oid,true).orElseThrow(()->new IllegalArgumentException("Đơn chưa có vận đơn."));
        if(!"DA_XUAT".equals(order.get("TrangThaiKho")))throw new IllegalArgumentException("Đơn chưa bàn giao hàng.");
        if(failedDelivery){if(!"GIAO_THAT_BAI".equals(order.get("TrangThaiDon"))||!"GIAO_THAT_BAI".equals(outbound.get("TrangThai")))throw new IllegalArgumentException("Chỉ nhận kiện hoàn từ lần giao thất bại.");}
        else if(!"HOAN_THANH".equals(order.get("TrangThaiDon"))||!"DA_GIAO".equals(outbound.get("TrangThai")))throw new IllegalArgumentException("Chỉ yêu cầu trả sau khi đơn đã giao và hoàn tất kiểm tra thanh toán.");
        var original=orders.dong(oid);Map<Long,Integer> remaining=new TreeMap<>();
        for(var line:original)remaining.put(id(line,"MaChiTietDonHang"),((Number)line.get("SoLuong")).intValue()-dao.daNhan(id(line,"MaChiTietDonHang"),0));
        for(var line:requested.entrySet())if(!remaining.containsKey(line.getKey())||line.getValue()>remaining.get(line.getKey()))throw new IllegalArgumentException("Số trả không được vượt số mua còn lại và phải thuộc đúng đơn.");
        if(failedDelivery){remaining.values().removeIf(q->q==0);if(!remaining.equals(requested))throw new IllegalArgumentException("Kiện giao thất bại cần nhận lại toàn bộ hàng còn trong kiện.");}
        long ticket=dao.tao(oid,actor.maNguoiDung(),type,form.khoaChongLap(),form.lyDo().strip());for(var line:requested.entrySet())dao.themDong(oid,ticket,line.getKey(),line.getValue());
        dao.taoViec(ticket,"CHO_DUYET_TRA",form.lyDo().strip());
        if(failedDelivery){shipping.capNhatDon(oid,"CHO_HANG_HOAN_VE",false);shipping.capNhatVanDon(id(outbound,"MaVanDon"),"DANG_HOAN");shipping.suKien(id(outbound,"MaVanDon"),"return-request:"+ticket,"DANG_HOAN","Kiện giao thất bại đang được xử lý hoàn về",actor.maNguoiDung());shipping.dongViec(id(outbound,"MaVanDon"));}
        orderService.ghiLichSu(oid,actor.maNguoiDung(),(String)order.get("TrangThaiDon"),failedDelivery?"CHO_HANG_HOAN_VE":(String)order.get("TrangThaiDon"),"Đã tạo phiếu trả #"+ticket);return ticket;
    }
    @Transactional public void duyet(String admin,long ticket,boolean accept,BigDecimal fee,String note){
        long actor=users.quanTri(admin).maNguoiDung();var initial=dao.doc(ticket,false);var order=khoaDon(payments.maDon(id(initial,"MaDonHang")));var row=dao.doc(ticket,true);long oid=id(row,"MaDonHang");
        if(note==null||note.isBlank()||note.length()>500||fee==null||fee.signum()<0||fee.scale()>0)throw new IllegalArgumentException("Cần ghi kết quả duyệt và phí giao được hoàn theo VND nguyên.");
        if(!"CHO_DUYET".equals(row.get("TrangThai"))){if((accept?"CHO_NHAN_HANG":"TU_CHOI").equals(row.get("TrangThai"))&&fee.compareTo((BigDecimal)row.get("PhiVanChuyenDuocHoan"))==0&&note.strip().equals(row.get("GhiChuXuLy")))return;throw new IllegalArgumentException("Phiếu không còn chờ duyệt.");}
        if(!accept&&fee.signum()!=0)throw new IllegalArgumentException("Phiếu bị từ chối không có phí hoàn.");
        if(fee.add(dao.phiDaDuyet(oid,ticket)).compareTo((BigDecimal)order.get("PhiVanChuyen"))>0)throw new IllegalArgumentException("Tổng phí giao được hoàn không thể vượt phí khách đã trả.");
        if(order.get("MaPhienThanhToanChapNhan")==null&&fee.signum()>0)throw new IllegalArgumentException("Đơn chưa thu tiền không có phí giao để hoàn.");
        dao.duyet(ticket,actor,accept,fee,note.strip());dao.dongViec(ticket);
        if(accept){long waybill=dao.taoVanDonHoan(oid,ticket);shipping.suKien(waybill,"return-create:"+ticket,"CHO_BAN_GIAO","Đã duyệt nhận hàng trả về kho",actor);dao.taoViec(ticket,"CHO_KIEM_HANG","Theo dõi kiện hoàn và kiểm hàng thực nhận");}
        else if("GIAO_THAT_BAI".equals(row.get("LoaiTraHang"))){shipping.capNhatDon(oid,"GIAO_THAT_BAI",false);var outbound=shipping.vanDon(oid,true).orElseThrow();shipping.capNhatVanDon(id(outbound,"MaVanDon"),"GIAO_THAT_BAI");shipping.suKien(id(outbound,"MaVanDon"),"return-reject:"+ticket,"GIAO_THAT_BAI","Đã dừng yêu cầu nhận kiện hoàn; cần liên hệ giao nhận",actor);shipping.taoViec(id(outbound,"MaVanDon"),"GIAO_THAT_BAI",note.strip());}
        orderService.ghiLichSu(oid,actor,(String)order.get("TrangThaiDon"),!accept&&"GIAO_THAT_BAI".equals(row.get("LoaiTraHang"))?"GIAO_THAT_BAI":(String)order.get("TrangThaiDon"),(accept?"Đã duyệt nhận hàng trả để kiểm, phiếu #":"Đã từ chối yêu cầu trả, phiếu #")+ticket);
        dao.log(actor,ticket,accept?"DUYET_TRA_HANG":"TU_CHOI_TRA_HANG");
    }
    @Transactional public void huy(String email,long ticket){
        chiTiet(email,ticket);var initial=dao.doc(ticket,false);var order=khoaDon(payments.maDon(id(initial,"MaDonHang")));var row=dao.doc(ticket,true);
        if("DA_HUY".equals(row.get("TrangThai")))return;
        if(!"CHO_DUYET".equals(row.get("TrangThai"))||!"SAU_GIAO".equals(row.get("LoaiTraHang")))throw new IllegalArgumentException("Chỉ rút yêu cầu trả sau giao khi cửa hàng chưa duyệt.");dao.huy(ticket);dao.dongViec(ticket);
        orderService.ghiLichSu(id(row,"MaDonHang"),users.hienTai(email).maNguoiDung(),(String)order.get("TrangThaiDon"),(String)order.get("TrangThaiDon"),"Đã rút yêu cầu trả hàng, phiếu #"+ticket);
    }
    @Transactional public void dangHoan(String admin,long ticket){
        long actor=users.quanTri(admin).maNguoiDung();var initial=dao.doc(ticket,false);var order=khoaDon(payments.maDon(id(initial,"MaDonHang")));var row=dao.doc(ticket,true);
        if(!"CHO_NHAN_HANG".equals(row.get("TrangThai")))throw new IllegalArgumentException("Phiếu không chờ hàng về.");var waybill=dao.vanDon(ticket).orElseThrow();
        if("DANG_VAN_CHUYEN".equals(waybill.get("TrangThai")))return;
        shipping.capNhatVanDon(id(waybill,"MaVanDon"),"DANG_VAN_CHUYEN");shipping.suKien(id(waybill,"MaVanDon"),"return-moving:"+ticket,"DANG_VAN_CHUYEN","Kiện trả đang trên đường về kho",actor);
        orderService.ghiLichSu(id(row,"MaDonHang"),actor,(String)order.get("TrangThaiDon"),(String)order.get("TrangThaiDon"),"Kiện trả của phiếu #"+ticket+" đang trên đường về kho.");
    }
    record KetQua(Map<String,Object> dong,KiemHangYeuCau.Dong form,BigDecimal tien){}
    @Transactional public void kiemHang(String admin,long ticket,KiemHangYeuCau form){
        long actor=users.quanTri(admin).maNguoiDung();var initial=dao.doc(ticket,false);var order=khoaDon(payments.maDon(id(initial,"MaDonHang")));long oid=id(order,"MaDonHang");var row=dao.doc(ticket,true);var lines=dao.dong(ticket);
        if(!form.daKiemTra()||form.dong()==null||form.dong().size()!=lines.size())throw new IllegalArgumentException("Cần xác nhận đã kiểm toàn bộ dòng của phiếu.");
        Map<Long,KiemHangYeuCau.Dong> supplied=new HashMap<>();for(var line:form.dong()){if(supplied.put(line.maDong(),line)!=null)throw new IllegalArgumentException("Dòng kiểm bị trùng.");}
        if(!"CHO_NHAN_HANG".equals(row.get("TrangThai"))){
            if(row.get("NgayNhanHang")!=null&&lines.stream().allMatch(line->{var f=supplied.get(id(line,"MaChiTietTraHang"));return f!=null&&f.daNhan()==((Number)line.get("SoLuongDaNhan")).intValue()&&f.duyetHoan()==((Number)line.get("SoLuongDuyetHoan")).intValue()&&f.nhapLai()==((Number)line.get("SoLuongNhapLai")).intValue()&&f.ghiChu().strip().equals(line.get("GhiChuKiemHang"));}))return;
            throw new IllegalArgumentException("Phiếu đã kiểm hàng hoặc không còn chờ nhận. Không được ghi lại kết quả khác.");
        }
        boolean paid=order.get("MaPhienThanhToanChapNhan")!=null;List<KetQua> results=new ArrayList<>();BigDecimal total=(BigDecimal)row.get("PhiVanChuyenDuocHoan");
        for(var line:lines){
            var f=supplied.get(id(line,"MaChiTietTraHang"));if(f==null||f.daNhan()<0||f.duyetHoan()<0||f.nhapLai()<0||f.daNhan()>((Number)line.get("SoLuongYeuCau")).intValue()||f.duyetHoan()>f.daNhan()||f.nhapLai()>f.daNhan()||f.ghiChu()==null||f.ghiChu().isBlank()||f.ghiChu().length()>500)throw new IllegalArgumentException("Số nhận/duyệt hoàn/nhập lại không hợp lệ hoặc thiếu kết quả kiểm.");
            int bought=((Number)line.get("SoLuong")).intValue();long original=id(line,"MaChiTietDonHang");if(dao.daNhan(original,ticket)+f.daNhan()>bought)throw new IllegalArgumentException("Số thực nhận qua các đợt vượt số đã mua.");
            int before=dao.daDuyet(original,ticket);BigDecimal net=((BigDecimal)line.get("DonGia")).multiply(BigDecimal.valueOf(bought)).subtract((BigDecimal)line.get("GiamGiaPhanBo"));
            BigDecimal amount=paid?net.multiply(BigDecimal.valueOf(before+f.duyetHoan())).divideToIntegralValue(BigDecimal.valueOf(bought)).subtract(net.multiply(BigDecimal.valueOf(before)).divideToIntegralValue(BigDecimal.valueOf(bought))):BigDecimal.ZERO;
            results.add(new KetQua(line,f,amount));total=total.add(amount);
        }
        List<Map<String,Object>> receipts=List.of();if(paid){long sid=id(order,"MaPhienThanhToanChapNhan");payments.khoaPhien(sid);receipts=payments.khoanThu(sid);BigDecimal free=receipts.stream().map(r->((BigDecimal)r.get("SoTien")).subtract(payments.daGiuHoan(id(r,"MaGiaoDich")))).reduce(BigDecimal.ZERO,BigDecimal::add);if(free.compareTo(total)<0)throw new IllegalArgumentException("Hạn mức hoàn còn lại không đủ. Cần đối soát trước khi kiểm nhận.");}
        for(var result:results){long line=id(result.dong(),"MaChiTietTraHang");dao.kiemDong(line,result.form(),result.tien());if(result.form().nhapLai()>0){long sku=id(result.dong(),"MaBienThe");orders.khoaSku(sku);dao.nhapLai(oid,line,sku,result.form().nhapLai(),actor);}}
        BigDecimal remaining=total;for(var receipt:receipts){if(remaining.signum()<=0)break;long rid=id(receipt,"MaGiaoDich");BigDecimal amount=((BigDecimal)receipt.get("SoTien")).subtract(payments.daGiuHoan(rid)).min(remaining);if(amount.signum()>0){dao.taoHoan(rid,ticket,amount);remaining=remaining.subtract(amount);}}
        dao.nhanHang(ticket,total.signum()>0);dao.dongViec(ticket);if(total.signum()>0)dao.taoViec(ticket,"CHO_HOAN_TIEN","Hoàn tiền theo kết quả kiểm hàng đã chốt");
        var waybill=dao.vanDon(ticket).orElseThrow();shipping.capNhatVanDon(id(waybill,"MaVanDon"),"DA_VE_KHO");shipping.suKien(id(waybill,"MaVanDon"),"return-received:"+ticket,"DA_VE_KHO","Kho đã nhận và kiểm hàng",actor);
        if("GIAO_THAT_BAI".equals(row.get("LoaiTraHang"))){dao.huyDonSauNhan(oid);var outbound=shipping.vanDon(oid,true).orElseThrow();shipping.capNhatVanDon(id(outbound,"MaVanDon"),"DA_VE_KHO");shipping.suKien(id(outbound,"MaVanDon"),"return-received:"+ticket,"DA_VE_KHO","Kiện giao thất bại đã nhận về và kiểm hàng",actor);if(total.signum()>0)refunds.trangThaiDon(oid,"CHO_HOAN");}
        orderService.ghiLichSu(oid,actor,(String)order.get("TrangThaiDon"),"GIAO_THAT_BAI".equals(row.get("LoaiTraHang"))?"DA_HUY":(String)order.get("TrangThaiDon"),"Đã kiểm hàng phiếu trả #"+ticket+"; phần nhập lại và hoàn tiền được ghi riêng");dao.log(actor,ticket,"KIEM_HANG_TRA");
    }
}
