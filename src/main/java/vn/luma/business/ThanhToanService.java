package vn.luma.business;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import vn.luma.config.ThanhToanCauHinh;
import vn.luma.data.ThanhToanDAO;
import vn.luma.model.BaoThu;
import static vn.luma.data.ThanhToanDAO.id;

/** Network orchestration: no transaction spans an SDK request. */
@Service
public class ThanhToanService {
    private final ThanhToanDAO dao;private final ThanhToanGhiSoService ledger;private final PayOSClient client;private final DonHangService orders;private final ThanhToanCauHinh config;
    public ThanhToanService(ThanhToanDAO dao,ThanhToanGhiSoService ledger,PayOSClient client,DonHangService orders,ThanhToanCauHinh config){this.dao=dao;this.ledger=ledger;this.client=client;this.orders=orders;this.config=config;}
    public Map<String,Object> phien(String email,String code){var order=orders.chiTiet(email,code);return dao.phienDon(id(order,"MaDonHang"),false).orElseThrow(()->new KhongTimThayException("Không tìm thấy phiên thanh toán."));}
    public BigDecimal daThu(String email,String code){return dao.tongThu(id(phien(email,code),"MaPhienThanhToan"));}
    public void taoLink(String email,String code){
        var prepared=ledger.chuanBi(email,code);var session=prepared.phien();long sid=id(session,"MaPhienThanhToan");
        if("MO_PHONG".equals(session.get("CheDoXuLy"))){
            if(!config.simulationEnabled())throw new IllegalArgumentException("Chế độ mô phỏng đang tắt.");
            if(session.get("MaLienKetPayOS")==null)ledger.ghiLink(sid,"mock-"+sid,"/thanh-toan/"+code,"PENDING");return;
        }
        if(!config.live())throw new IllegalArgumentException("PayOS thật chưa được cấu hình. Đơn được giữ trong hạn xử lý, bạn có thể hủy trước khi thanh toán.");
        try{
            if(prepared.duocTao()){
                var link=client.tao(id(session,"MaDonPayOS"),(BigDecimal)session.get("SoTienYeuCau"),ThanhToanDAO.instant(session.get("NgayHetHan")),code);
                ledger.ghiLink(sid,link.id(),link.url(),link.status());
            }else try{doiSoatId(sid,false);}catch(vn.payos.exception.NotFoundException ex){
                if(ex.getStatusCode().orElse(0)!=404)throw ex;
                var retry=ledger.chuanBiTaoLai(email,code);
                if(!retry.duocTao())throw new IllegalArgumentException("Chưa đến lượt thử lại hoặc đơn không còn đủ điều kiện.");
                var current=retry.phien();var link=client.tao(id(current,"MaDonPayOS"),(BigDecimal)current.get("SoTienYeuCau"),ThanhToanDAO.instant(current.get("NgayHetHan")),code);
                ledger.ghiLink(sid,link.id(),link.url(),link.status());
            }
        }catch(RuntimeException ex){ledger.chuaRo(sid,"TAO_LINK_CHUA_RO");throw new IllegalArgumentException("Chưa xác nhận được link PayOS. Hãy kiểm tra lại cùng đơn; hệ thống giữ nguyên mã thanh toán.",ex);}
    }
    public void webhook(String body){
        if(!config.live())throw new IllegalStateException("Chưa bật PayOS thật.");
        var receipt=client.xacMinh(body);long event=ledger.nhanDaXacMinh("PAYOS_LUMA",receipt);
        // A verified webhook is acknowledged after durable storage even if downstream processing needs a retry.
        try{ledger.xuLy(event);}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Webhook đã lưu; xử lý sổ thu sẽ được thử lại.");}
    }
    public void moPhong(String email,String code,BigDecimal amount,String nonce){
        UUID.fromString(nonce);var session=phien(email,code);
        if(!config.simulationEnabled()||!"MO_PHONG".equals(session.get("CheDoXuLy")))throw new IllegalArgumentException("Chỉ được mô phỏng trên kênh mô phỏng riêng.");
        if(session.get("MaLienKetPayOS")==null){taoLink(email,code);session=phien(email,code);}
        var receipt=new BaoThu(id(session,"MaDonPayOS"),(String)session.get("MaLienKetPayOS"),"MOCK-"+nonce,amount,"VND",OffsetDateTime.now().withNano(0));
        long event=ledger.nhanDaXacMinh("PAYOS_LUMA_MOCK",receipt);ledger.xuLy(event);
    }
    public void doiSoat(String email,String code){var session=phien(email,code);try{doiSoatId(id(session,"MaPhienThanhToan"),false);}catch(RuntimeException ex){ledger.chuaRo(id(session,"MaPhienThanhToan"),"TRA_CUU_CHUA_RO");throw new IllegalArgumentException("Chưa lấy được kết quả PayOS. Tiền chỉ được xác nhận sau khi kiểm chứng.");}}
    public void huy(String email,String code){
        var session=phien(email,code);ledger.ketThuc(email,code,false);
        if("TICH_HOP_THAT".equals(session.get("CheDoXuLy"))&&config.live())try{doiSoatId(id(session,"MaPhienThanhToan"),true);}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Đơn đã đóng; link PayOS sẽ được hủy/đối soát lại.");}
    }
    public void doiSoatId(long sid,boolean cancel){
        var session=dao.phien(sid);String code=dao.maDon(id(session,"MaDonHang"));
        ledger.ketThuc(null,code,true); // Checks the deadline under the order lock; paid orders remain active.
        session=dao.phien(sid);
        if("MO_PHONG".equals(session.get("CheDoXuLy")))return;
        if(!config.live())return;
        if(!dao.nhanTraCuu(sid))return;
        var remote=(cancel||"CHO_HUY_LINK".equals(session.get("MaLoi")))?client.huy(id(session,"MaDonPayOS")):client.tra(id(session,"MaDonPayOS"));
        BigDecimal sum=remote.receipts().stream().map(BaoThu::amount).reduce(BigDecimal.ZERO,BigDecimal::add);Set<String> references=new HashSet<>();
        boolean same=remote.orderCode()==id(session,"MaDonPayOS")&&remote.requested().compareTo((BigDecimal)session.get("SoTienYeuCau"))==0&&sum.compareTo(remote.amountPaid())==0&&(session.get("MaLienKetPayOS")==null||remote.id().equals(session.get("MaLienKetPayOS")));
        for(var receipt:remote.receipts())same&=receipt.orderCode()==remote.orderCode()&&receipt.linkId().equals(remote.id())&&references.add(receipt.reference());
        if(!same){ledger.doiSoatKhongKhop(sid,remote.amountPaid());throw new IllegalArgumentException("Thông tin đối soát không khớp. Chưa ghi thêm khoản thu.");}
        if(session.get("MaLienKetPayOS")==null)ledger.ghiLink(sid,remote.id(),"https://pay.payos.vn/web/"+remote.id(),remote.status());
        for(var receipt:remote.receipts())ledger.xuLy(ledger.nhanDaXacMinh("PAYOS_LUMA",receipt));
        ledger.luuDoiSoat(sid,remote);
        if("CANCELLED".equals(remote.status()))ledger.ketThuc(null,code,false);
        session=dao.phien(sid);
        if(session.get("MaLienKetPayOS")!=null)ledger.ghiLink(sid,(String)session.get("MaLienKetPayOS"),(String)session.get("DuongDanThanhToan"),remote.status());
    }
}
