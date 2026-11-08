package vn.luma.business;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.*;
import vn.luma.model.BaoThu;
import static vn.luma.data.ThanhToanDAO.id;

@Service
public class ThanhToanGhiSoService {
    private final ThanhToanDAO dao;private final DonHangDAO orders;private final DonHangService orderService;private final HoaDonService invoices;
    public ThanhToanGhiSoService(ThanhToanDAO dao,DonHangDAO orders,DonHangService orderService,HoaDonService invoices){this.dao=dao;this.orders=orders;this.orderService=orderService;this.invoices=invoices;}
    public record ChuanBi(Map<String,Object> phien,Map<String,Object> don,boolean duocTao) { }
    private Map<String,Object> khoaDon(long order){
        var initial=orders.theoMa(dao.maDon(order),false);
        if(initial.get("MaGiamGia")!=null)orders.khoaVoucherId(id(initial,"MaGiamGia"));
        return orders.theoMa((String)initial.get("MaDonCongKhai"),true);
    }
    @Transactional public ChuanBi chuanBi(String email,String code){
        var checked=orderService.chiTiet(email,code);var order=khoaDon(id(checked,"MaDonHang"));
        var session=dao.phienDon(id(order,"MaDonHang"),true).orElseThrow(()->new IllegalArgumentException("Đơn chưa có phiên PayOS."));
        boolean canCreate="CHO_XAC_NHAN".equals(order.get("TrangThaiDon"))&&!orders.quaHan(id(order,"MaDonHang"))&&order.get("MaPhienThanhToanChapNhan")==null;
        return new ChuanBi(session,order,canCreate&&dao.nhanTao(id(session,"MaPhienThanhToan")));
    }
    @Transactional public ChuanBi chuanBiTaoLai(String email,String code){
        var checked=orderService.chiTiet(email,code);var order=khoaDon(id(checked,"MaDonHang"));var session=dao.phienDon(id(order,"MaDonHang"),true).orElseThrow();long sid=id(session,"MaPhienThanhToan");
        boolean eligible="CHO_XAC_NHAN".equals(order.get("TrangThaiDon"))&&"DANG_GIU".equals(order.get("TrangThaiKho"))&&!orders.quaHan(id(order,"MaDonHang"))&&order.get("MaPhienThanhToanChapNhan")==null&&session.get("MaLienKetPayOS")==null&&dao.tongThu(sid).signum()==0&&Set.of("DANG_TAO_LINK","TAO_LINK_CHUA_RO","TRA_CUU_CHUA_RO").contains(Objects.toString(session.get("MaLoi"),""));
        return new ChuanBi(session,order,eligible&&dao.nhanTaoLai(sid));
    }
    @Transactional public void doiSoatKhongKhop(long sid,BigDecimal provider){var initial=dao.phien(sid);var order=khoaDon(id(initial,"MaDonHang"));dao.khoaPhien(sid);dao.trangThaiPhien(sid,"CAN_DOI_SOAT","API_KHONG_KHOP_SO");dao.doiSoat(sid,"PAYOS_API",provider,dao.tongThu(sid),"CHUA_RO","API_KHONG_KHOP_SO");if(order.get("MaPhienThanhToanChapNhan")==null)dao.trangThaiDon(id(order,"MaDonHang"),"CAN_DOI_SOAT");}
    @Transactional public void ghiLink(long session,String link,String url,String providerState){
        var first=dao.phien(session);var order=khoaDon(id(first,"MaDonHang"));var current=dao.khoaPhien(session);
        if(current.get("MaLienKetPayOS")!=null&&!link.equals(current.get("MaLienKetPayOS"))){dao.trangThaiPhien(session,"CAN_DOI_SOAT","LINK_KHONG_KHOP");return;}
        dao.ghiLink(session,link,url,providerState);
        if(Set.of("DANG_TAO_LINK","TAO_LINK_CHUA_RO","TRA_CUU_CHUA_RO").contains(Objects.toString(current.get("MaLoi"),""))&&order.get("MaPhienThanhToanChapNhan")==null&&!Set.of("DA_HUY","HET_HAN").contains((String)order.get("TrangThaiDon"))){
            var total=dao.tongThu(session);
            if(total.compareTo((BigDecimal)current.get("SoTienYeuCau"))<0){dao.trangThaiPhien(session,total.signum()==0?"CHO_THANH_TOAN":"THU_THIEU",null);dao.trangThaiDon(id(order,"MaDonHang"),total.signum()==0?"CHUA_THU":"THU_THIEU");}
        }
    }
    @Transactional public void chuaRo(long session,String reason){
        var first=dao.phien(session);var order=khoaDon(id(first,"MaDonHang"));var current=dao.khoaPhien(session);
        if(Set.of("DU_TIEN","DA_HUY","HET_HAN").contains((String)current.get("TrangThaiXuLy")))return;
        if(Set.of("THAM_CHIEU_XUNG_DOT","LINK_KHONG_KHOP","API_KHONG_KHOP_SO").contains(Objects.toString(current.get("MaLoi"),"")))return;
        dao.trangThaiPhien(session,"CAN_DOI_SOAT",reason);
        if(order.get("MaPhienThanhToanChapNhan")==null)dao.trangThaiDon(id(order,"MaDonHang"),"CAN_DOI_SOAT");
    }
    @Transactional public long nhanDaXacMinh(String channel,BaoThu receipt){
        if(!Set.of("PAYOS_LUMA","PAYOS_LUMA_MOCK").contains(channel))throw new IllegalArgumentException("Kênh không hợp lệ.");
        return dao.luuSuKien(channel,receipt);
    }
    @Transactional public void xuLy(long eventId){
        var event=dao.suKien(eventId,false);
        if("DA_XU_LY".equals(event.get("TrangThaiXuLy")))return;
        var found=dao.phienMa((String)event.get("MaKenhThanhToan"),Long.parseLong((String)event.get("code")));
        if(found.isEmpty())return; // Retain the verified inbox record until its local session can be reconciled.
        var order=khoaDon(id(found.get(),"MaDonHang"));var session=dao.khoaPhien(id(found.get(),"MaPhienThanhToan"));
        event=dao.suKien(eventId,true);if("DA_XU_LY".equals(event.get("TrangThaiXuLy")))return;
        long sid=id(session,"MaPhienThanhToan"),oid=id(order,"MaDonHang");
        var receipt=new BaoThu(Long.parseLong((String)event.get("code")),(String)event.get("link"),(String)event.get("ref"),new BigDecimal((String)event.get("amount")),"VND",OffsetDateTime.parse((String)event.get("time")));
        if(session.get("MaLienKetPayOS")!=null&&!receipt.linkId().equals(session.get("MaLienKetPayOS"))){loi(eventId,sid,oid,order,"LINK_KHONG_KHOP");return;}
        if(session.get("MaLienKetPayOS")==null)dao.ghiLink(sid,receipt.linkId(),null,"PENDING");
        var saved=dao.ghiThu(session,receipt);
        if(id(saved,"MaPhienThanhToan")!=sid||((BigDecimal)saved.get("SoTien")).compareTo(receipt.amount())!=0){loi(eventId,sid,oid,order,"THAM_CHIEU_XUNG_DOT");return;}
        BigDecimal total=dao.tongThu(sid),due=(BigDecimal)session.get("SoTienYeuCau");
        boolean accepted=order.get("MaPhienThanhToanChapNhan")!=null;
        boolean closed=Set.of("DA_HUY","HET_HAN").contains((String)order.get("TrangThaiDon"));
        if(closed||(!accepted&&orders.quaHan(oid))){
            if(!closed)dong(order,"HET_HAN","Thanh toán đến sau hạn xử lý");
            taoHoan(sid,total,"THU_TRE");dao.trangThaiPhien(sid,"DA_HUY".equals(order.get("TrangThaiDon"))?"DA_HUY":"HET_HAN","CHO_HUY_LINK");
            dao.trangThaiDon(oid,dao.tongDaHoan(sid).compareTo(total)>=0?"DA_HOAN":"CHO_HOAN");
        }else if(accepted){
            if(id(order,"MaPhienThanhToanChapNhan")!=sid){taoHoan(sid,total,"THU_DU");dao.trangThaiPhien(sid,"THU_DU","PHIEN_KHAC_DA_CHAP_NHAN");}
            else if(total.compareTo(due)>0){taoHoan(sid,total.subtract(due),"THU_DU");dao.trangThaiPhien(sid,"THU_DU","THU_THEM_SAU_DU_TIEN");}
        }else if(Set.of("THAM_CHIEU_XUNG_DOT","LINK_KHONG_KHOP","API_KHONG_KHOP_SO").contains(Objects.toString(session.get("MaLoi"),""))){
            dao.trangThaiDon(oid,"CAN_DOI_SOAT");
        }else if(total.compareTo(due)==0){
            dao.chapNhan(oid,sid);invoices.phatHanh(oid);
            orderService.ghiLichSu(oid,id(order,"MaNguoiDung"),(String)order.get("TrangThaiDon"),(String)order.get("TrangThaiDon"),"Đã xác minh đủ tiền PayOS; hàng tiếp tục được giữ đến khi bàn giao");
        }else if(total.compareTo(due)>0){
            taoHoan(sid,total.subtract(due),"THU_DU");dao.trangThaiPhien(sid,"THU_DU","CAN_DOI_SOAT_THU_DU");dao.trangThaiDon(oid,"CAN_DOI_SOAT");
        }else{dao.trangThaiPhien(sid,"THU_THIEU",null);dao.trangThaiDon(oid,"THU_THIEU");}
        dao.xongSuKien(eventId,sid,id(saved,"MaGiaoDich"),"DA_XU_LY");
    }
    private void loi(long event,long sid,long oid,Map<String,Object> order,String reason){
        dao.trangThaiPhien(sid,"CAN_DOI_SOAT",reason);
        if(order.get("MaPhienThanhToanChapNhan")==null)dao.trangThaiDon(oid,"CAN_DOI_SOAT");
        dao.doiSoat(sid,"PAYOS_API",null,dao.tongThu(sid),"CHUA_RO",reason);dao.xongSuKien(event,sid,null,"LOI");
    }
    private void taoHoan(long session,BigDecimal target,String reason){
        var receipts=dao.khoanThu(session);
        BigDecimal claimed=receipts.stream().map(r->dao.daGiuHoan(id(r,"MaGiaoDich"))).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal remaining=target.subtract(claimed);
        for(var receipt:receipts){
            if(remaining.signum()<=0)break;
            long id=id(receipt,"MaGiaoDich");var before=dao.daGiuHoan(id);var amount=((BigDecimal)receipt.get("SoTien")).subtract(before).min(remaining);
            if(amount.signum()>0){dao.yeuCauHoan(id,amount,before,reason);remaining=remaining.subtract(amount);}
        }
    }
    private void dong(Map<String,Object> order,String state,String reason){
        long id=id(order,"MaDonHang");
        if("DANG_GIU".equals(order.get("TrangThaiKho")))for(var line:orders.dong(id)){long sku=id(line,"MaBienThe");orders.khoaSku(sku);orders.nhaGiu(id,sku,((Number)line.get("SoLuong")).intValue());}
        orders.huy(id,state,reason);orderService.ghiLichSu(id,id(order,"MaNguoiDung"),(String)order.get("TrangThaiDon"),state,reason);
    }
    @Transactional public void ketThuc(String email,String code,boolean expired){
        var checked=email==null?orders.theoMa(code,false):orderService.chiTiet(email,code);var order=khoaDon(id(checked,"MaDonHang"));
        var session=dao.phienDon(id(order,"MaDonHang"),true).orElseThrow(()->new IllegalArgumentException("Không có phiên PayOS."));long sid=id(session,"MaPhienThanhToan");
        if(order.get("MaPhienThanhToanChapNhan")!=null&&expired)return;
        if(Set.of("DA_HUY","HET_HAN").contains((String)order.get("TrangThaiDon")))return;
        if(expired&&!orders.quaHan(id(order,"MaDonHang")))return;
        if(!Set.of("CHO_XAC_NHAN","DA_XAC_NHAN").contains((String)order.get("TrangThaiDon"))||!"DANG_GIU".equals(order.get("TrangThaiKho")))throw new IllegalArgumentException("Đơn đã bàn giao, không thể tự hủy. Hãy liên hệ cửa hàng.");
        dong(order,expired?"HET_HAN":"DA_HUY",expired?"Hết hạn chờ thanh toán PayOS":"Khách hủy trước khi thanh toán hoàn tất");
        BigDecimal total=dao.tongThu(sid);if(total.signum()>0){taoHoan(sid,total,expired?"THU_TRE":"HUY_DON");dao.trangThaiDon(id(order,"MaDonHang"),"CHO_HOAN");}
        dao.trangThaiPhien(sid,expired?"HET_HAN":"DA_HUY","CHO_HUY_LINK");
    }
    @Transactional public void luuDoiSoat(long sid,PayOSClient.DoiSoat remote){
        var original=dao.phien(sid);var order=khoaDon(id(original,"MaDonHang"));var session=dao.khoaPhien(sid);var total=dao.tongThu(sid);
        boolean same=remote.orderCode()==id(session,"MaDonPayOS")&&remote.id().equals(session.get("MaLienKetPayOS"))&&remote.requested().compareTo((BigDecimal)session.get("SoTienYeuCau"))==0&&remote.amountPaid().compareTo(total)==0;
        dao.doiSoat(sid,"MO_PHONG".equals(session.get("CheDoXuLy"))?"MO_PHONG":"PAYOS_API",remote.amountPaid(),total,same?"KHOP":"LECH",same?null:"API_KHONG_KHOP_SO");
        if(!same){dao.trangThaiPhien(sid,"CAN_DOI_SOAT","API_KHONG_KHOP_SO");if(order.get("MaPhienThanhToanChapNhan")==null)dao.trangThaiDon(id(order,"MaDonHang"),"CAN_DOI_SOAT");}
    }
}
