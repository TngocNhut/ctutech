package vn.luma.business;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import vn.luma.data.*;
import vn.luma.web.*;
import static vn.luma.data.ThanhToanDAO.id;

@Service
public class GiaoNhanService {
    private final GiaoNhanDAO dao;private final DonHangDAO orders;private final DonHangService orderService;private final TaiKhoanService users;private final HoaDonService invoices;private final ThanhToanDAO payments;private final QuanLySanPhamDAO audit;
    public GiaoNhanService(GiaoNhanDAO dao,DonHangDAO orders,DonHangService orderService,TaiKhoanService users,HoaDonService invoices,ThanhToanDAO payments,QuanLySanPhamDAO audit){this.dao=dao;this.orders=orders;this.orderService=orderService;this.users=users;this.invoices=invoices;this.payments=payments;this.audit=audit;}
    private Map<String,Object> khoa(String code){var row=orders.theoMa(code,false);if(row.get("MaGiamGia")!=null)orders.khoaVoucherId(id(row,"MaGiamGia"));return orders.theoMa(code,true);}
    public List<Map<String,Object>> danhSach(String admin,String state){return danhSach(admin,state,1);}
    public List<Map<String,Object>> danhSach(String admin,String state,int page){users.vanHanh(admin);if(page<1||page>10000||state!=null&&!Set.of("","CHO_XAC_NHAN","DA_XAC_NHAN","DANG_VAN_CHUYEN","CHO_GIAO_HANG","GIAO_THAT_BAI","CHO_HANG_HOAN_VE","HOAN_THANH","DA_HUY","HET_HAN").contains(state))throw new IllegalArgumentException("Bộ lọc đơn hàng không hợp lệ.");return dao.donHang(state==null?"":state,page);}
    public List<Map<String,Object>> donVi(String admin){users.vanHanh(admin);return dao.donVi();}
    public Optional<Map<String,Object>> vanDon(String email,String code){return dao.vanDon(id(orderService.chiTiet(email,code),"MaDonHang"),false);}
    public List<Map<String,Object>> lichSu(String email,String code){return vanDon(email,code).map(v->dao.lichSu(id(v,"MaVanDon"))).orElse(List.of());}
    public List<Map<String,Object>> cuoc(String admin,String code){users.vanHanh(admin);return vanDon(admin,code).map(v->dao.cuoc(id(v,"MaVanDon"))).orElse(List.of());}
    public BigDecimal tongCuoc(String admin,String code){return cuoc(admin,code).stream().map(f->"GIAM_CUOC".equals(f.get("LoaiPhi"))?((BigDecimal)f.get("SoTien")).negate():(BigDecimal)f.get("SoTien")).reduce(BigDecimal.ZERO,BigDecimal::add);}
    public List<Map<String,Object>> viec(String admin){users.vanHanh(admin);return dao.viec();}
    public List<Map<String,Object>> viec(String admin,boolean closed,int page){users.vanHanh(admin);kiemTraTrang(page);return dao.viec(closed,page);}
    private void kiemTraTrang(int page){if(page<1||page>10000)throw new IllegalArgumentException("Trang từ 1 đến 10000.");}
    public Map<String,Object> viec(String admin,long task){users.vanHanh(admin);return dao.viec(task,false);}
    public List<Map<String,Object>> ghiChuViec(String admin,long task,int page){viec(admin,task);kiemTraTrang(page);return dao.ghiChuViec(task,page);}
    public boolean duocSuaViec(String admin,Map<String,Object> task){long actor=users.vanHanh(admin).maNguoiDung();return Set.of("MOI","DANG_XU_LY").contains((String)task.get("TrangThai"))&&(task.get("MaQuanTriPhuTrach")==null||id(task,"MaQuanTriPhuTrach")==actor);}
    @Transactional public void nhanViec(String admin,long task){long actor=users.vanHanh(admin).maNguoiDung();var old=dao.viec(task,true);dao.nhanViec(task,actor);if(!"DANG_XU_LY".equals(old.get("TrangThai"))||old.get("MaQuanTriPhuTrach")==null)audit.log(actor,task,"NHAN_VIEC_GIAO_NHAN","VIEC_GIAO_NHAN");}
    @Transactional public void suaViec(String admin,long task,ViecGiaoNhanYeuCau f){
        long actor=users.vanHanh(admin).maNguoiDung();var old=dao.viec(task,true);
        if(!duocSuaViec(admin,old))throw new IllegalArgumentException("Việc đã đóng hoặc do người khác phụ trách.");
        if(f.ghiChu()==null||f.ghiChu().isBlank()||f.ghiChu().length()>255||f.hanXuLy()!=null&&(f.hanXuLy().getYear()<2000||f.hanXuLy().getYear()>2100))throw new IllegalArgumentException("Ghi chú cần 1–255 ký tự; hạn xử lý thuộc năm 2000–2100 hoặc để trống.");
        var deadline=f.hanXuLy()==null?null:f.hanXuLy().atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
        dao.suaViec(task,actor,deadline,f.dongViec());audit.log(actor,task,f.dongViec()?"DONG_VIEC_GIAO_NHAN":"CAP_NHAT_VIEC_GIAO_NHAN","VIEC_GIAO_NHAN",f.ghiChu().strip());
    }
    public List<String> quaHan(){return dao.quaHan();}
    @Transactional public void taoViecQuaHan(String code){var order=khoa(code);var shipment=dao.vanDon(id(order,"MaDonHang"),true);if(shipment.isPresent()&&Set.of("DANG_VAN_CHUYEN","CHO_GIAO_HANG").contains((String)shipment.get().get("TrangThai"))&&dao.daQuaHan(id(shipment.get(),"MaVanDon")))dao.taoViec(id(shipment.get(),"MaVanDon"),"QUA_HAN_GIAO","Đã quá ngày giờ dự kiến giao; cần liên hệ đội giao nhận");}
    @Transactional public void themCuoc(String admin,String code,PhiVanChuyenYeuCau form){
        long actor=users.vanHanh(admin).maNguoiDung();UUID.fromString(form.khoaChongLap());var order=khoa(code);var shipment=dao.vanDon(id(order,"MaDonHang"),true).orElseThrow(()->new IllegalArgumentException("Cần tạo vận đơn trước khi ghi cước."));long vid=id(shipment,"MaVanDon");
        ghiCuoc(actor,order,vid,form);
    }
    public List<Map<String,Object>> cuocVanDon(String admin,long waybill){users.vanHanh(admin);dao.vanDonTheoId(waybill,false);return dao.cuoc(waybill);}
    @Transactional public void themCuocVanDon(String admin,long waybill,PhiVanChuyenYeuCau form){
        long actor=users.vanHanh(admin).maNguoiDung();UUID.fromString(form.khoaChongLap());var initial=dao.vanDonTheoId(waybill,false);String code=payments.maDon(id(initial,"MaDonHang"));var order=khoa(code);var shipment=dao.vanDonTheoId(waybill,true);
        if(id(shipment,"MaDonHang")!=id(order,"MaDonHang"))throw new IllegalArgumentException("Vận đơn không khớp đơn hàng.");ghiCuoc(actor,order,waybill,form);
    }
    private void ghiCuoc(long actor,Map<String,Object> order,long vid,PhiVanChuyenYeuCau form){
        if(form.soTien()==null||form.soTien().signum()<0||form.soTien().scale()>0||!Set.of("CUOC_GOC","PHU_PHI","GIAM_CUOC").contains(form.loaiPhi())||form.ghiChu()==null||form.ghiChu().isBlank()||form.ghiChu().length()>500)throw new IllegalArgumentException("Cước cần là VND nguyên không âm và có ghi chú.");
        var fees=dao.cuoc(vid);for(var fee:fees)if(form.khoaChongLap().equals(fee.get("KhoaChongLap"))){if(form.loaiPhi().equals(fee.get("LoaiPhi"))&&form.soTien().compareTo((BigDecimal)fee.get("SoTien"))==0&&form.ghiChu().strip().equals(fee.get("GhiChu")))return;throw new IllegalArgumentException("Yêu cầu ghi cước đã dùng với nội dung khác.");}
        if("CUOC_GOC".equals(form.loaiPhi())&&dao.cuocGoc(vid)!=null)throw new IllegalArgumentException("Đã có cước gốc. Ghi phụ phí hoặc giảm cước để điều chỉnh.");
        BigDecimal total=fees.stream().map(f->"GIAM_CUOC".equals(f.get("LoaiPhi"))?((BigDecimal)f.get("SoTien")).negate():(BigDecimal)f.get("SoTien")).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(total.add("GIAM_CUOC".equals(form.loaiPhi())?form.soTien().negate():form.soTien()).signum()<0)throw new IllegalArgumentException("Giảm cước không thể vượt tổng cước đã ghi.");
        dao.themCuoc(vid,form);dao.nhatKy(actor,id(order,"MaDonHang"),"GHI_CUOC_VAN_DON");
    }
    private void kiemTraThu(Map<String,Object> order){if("PAYOS".equals(order.get("PhuongThucThanhToan"))&&(!"DA_THU".equals(order.get("TrangThaiThanhToan"))||order.get("MaPhienThanhToanChapNhan")==null))throw new IllegalArgumentException("Cần xác minh đủ tiền PayOS trước khi xác nhận hoặc bàn giao.");}
    @Transactional public void xacNhan(String admin,String code,boolean addressChecked){
        long actor=users.vanHanh(admin).maNguoiDung();var order=khoa(code);String old=(String)order.get("TrangThaiDon");if("DA_XAC_NHAN".equals(old))return;
        if(!addressChecked)throw new IllegalArgumentException("Cần kiểm tra địa chỉ giao nhận trước khi xác nhận đơn.");
        if(!"CHO_XAC_NHAN".equals(old)||!"DANG_GIU".equals(order.get("TrangThaiKho")))throw new IllegalArgumentException("Đơn không còn chờ xác nhận.");
        kiemTraThu(order);
        if("COD".equals(order.get("PhuongThucThanhToan"))&&orders.quaHan(id(order,"MaDonHang")))throw new IllegalArgumentException("Đơn COD đã hết hạn xác nhận.");
        dao.capNhatDon(id(order,"MaDonHang"),"DA_XAC_NHAN",false);orderService.ghiLichSu(id(order,"MaDonHang"),actor,old,"DA_XAC_NHAN","Cửa hàng đã kiểm tra địa chỉ và xác nhận đơn");dao.nhatKy(actor,id(order,"MaDonHang"),"XAC_NHAN_DON");
    }
    @Transactional public void taoVanDon(String admin,String code,TaoVanDonYeuCau form){
        long actor=users.vanHanh(admin).maNguoiDung();var order=khoa(code);long oid=id(order,"MaDonHang");var existing=dao.vanDon(oid,true);
        if(existing.isPresent()){
            var v=existing.get();BigDecimal cost=dao.cuocGoc(id(v,"MaVanDon"));boolean sameCost=cost==null?form.cuocGoc()==null:form.cuocGoc()!=null&&cost.compareTo(form.cuocGoc())==0;
            if(id(v,"MaDonViVanChuyen")==form.maDonViVanChuyen()&&form.maTheoDoi().strip().equals(v.get("MaTheoDoi"))&&sameCost)return;
            throw new IllegalArgumentException("Đơn đã có vận đơn giao đi. Không tạo thêm kiện thứ hai.");
        }
        if(!"DA_XAC_NHAN".equals(order.get("TrangThaiDon")))throw new IllegalArgumentException("Cần xác nhận đơn trước khi tạo vận đơn.");
        long shipment;try{shipment=dao.tao(oid,form);}catch(DuplicateKeyException ex){throw new IllegalArgumentException("Mã theo dõi đã tồn tại tại đơn vị vận chuyển.");}
        dao.suKien(shipment,"create:"+shipment,"CHO_BAN_GIAO","Cửa hàng đã chuẩn bị kiện hàng",actor);dao.nhatKy(actor,oid,"TAO_VAN_DON");
    }
    @Transactional public void capNhat(String admin,String code,SuKienGiaoNhanYeuCau form){
        UUID.fromString(form.khoaSuKien());long actor=users.vanHanh(admin).maNguoiDung();var order=khoa(code);long oid=id(order,"MaDonHang");
        var shipment=dao.vanDon(oid,true).orElseThrow(()->new IllegalArgumentException("Cần tạo vận đơn trước."));long vid=id(shipment,"MaVanDon");String current=(String)shipment.get("TrangThai");
        String target=switch(form.hanhDong()){case "BAN_GIAO","GIAO_LAI"->"DANG_VAN_CHUYEN";case "CHO_GIAO_HANG","DA_GIAO","GIAO_THAT_BAI"->form.hanhDong();default->throw new IllegalArgumentException("Thao tác không hợp lệ.");};
        var previous=dao.suKien(vid,form.khoaSuKien());if(previous.isPresent()){
            if(!target.equals(previous.get().get("TrangThaiGhiNhan"))||!form.ghiChu().strip().equals(previous.get().get("GhiChu")))throw new IllegalArgumentException("Khóa sự kiện đã dùng với nội dung khác.");return;
        }
        if(Set.of("DA_HUY","HET_HAN","HOAN_THANH").contains((String)order.get("TrangThaiDon")))throw new IllegalArgumentException("Đơn đã kết thúc, không thể đổi vận đơn.");
        if("BAN_GIAO".equals(form.hanhDong())){
            if("DA_XUAT".equals(order.get("TrangThaiKho"))&&"DANG_VAN_CHUYEN".equals(current))return;
            if(!"DA_XAC_NHAN".equals(order.get("TrangThaiDon"))||!"DANG_GIU".equals(order.get("TrangThaiKho"))||!"CHO_BAN_GIAO".equals(current))throw new IllegalArgumentException("Kiện chưa đủ điều kiện bàn giao.");
            kiemTraThu(order);
            for(var line:orders.dong(oid)){long sku=id(line,"MaBienThe");orders.khoaSku(sku);dao.xuatKho(oid,sku,((Number)line.get("SoLuong")).intValue(),actor);}dao.daXuat(oid);
        }else{
            if(!"DA_XUAT".equals(order.get("TrangThaiKho")))throw new IllegalArgumentException("Cần bàn giao và xuất kho trước khi cập nhật giao hàng.");
            Set<String> allowed="GIAO_LAI".equals(form.hanhDong())?Set.of("GIAO_THAT_BAI"):Set.of("DANG_VAN_CHUYEN","CHO_GIAO_HANG");
            if(current.equals(target))return;
            if(!allowed.contains(current))throw new IllegalArgumentException("Không được chuyển lùi hoặc bỏ qua bước giao nhận.");
        }
        String state=target.equals("DA_GIAO")?("DA_THU".equals(order.get("TrangThaiThanhToan"))?"HOAN_THANH":"CHO_GIAO_HANG"):target;
        dao.capNhatVanDon(vid,target);dao.capNhatDon(oid,state,"HOAN_THANH".equals(state));dao.suKien(vid,form.khoaSuKien(),target,form.ghiChu().strip(),actor);
        orderService.ghiLichSu(oid,actor,(String)order.get("TrangThaiDon"),state,target.equals("DA_GIAO")&&!"HOAN_THANH".equals(state)?"Hàng đã giao; chờ cửa hàng ghi thu COD":form.ghiChu().strip());
        if("GIAO_THAT_BAI".equals(target))dao.taoViec(vid,"GIAO_THAT_BAI",form.ghiChu().strip());else dao.dongViec(vid);
        dao.nhatKy(actor,oid,"GIAO_NHAN_"+form.hanhDong());
    }
    @Transactional public void thuCOD(String admin,String code,ThuCODYeuCau form){
        long actor=users.vanHanh(admin).maNguoiDung();var order=khoa(code);long oid=id(order,"MaDonHang");
        if(!form.daKiemTra()||form.soTien()==null||form.soTien().compareTo((BigDecimal)order.get("TongThanhToan"))!=0||form.maChungTu()==null||form.maChungTu().isBlank())throw new IllegalArgumentException("Cần xác nhận chứng từ và số tiền COD khớp tổng đơn.");
        if(!"COD".equals(order.get("PhuongThucThanhToan")))throw new IllegalArgumentException("Đây không phải đơn COD.");
        var old=dao.codDaThu(oid);if(old.isPresent()){
            if(((BigDecimal)old.get().get("SoTien")).compareTo(form.soTien())==0&&form.maChungTu().strip().equals(old.get().get("ChungTu")))return;
            throw new IllegalArgumentException("Đơn đã có khoản thu COD khác. Cần đối soát, không ghi thêm.");
        }
        var shipment=dao.vanDon(oid,true).orElseThrow(()->new IllegalArgumentException("Không có vận đơn."));
        if(!"DA_GIAO".equals(shipment.get("TrangThai"))||!"DA_XUAT".equals(order.get("TrangThaiKho"))||!"CHO_GIAO_HANG".equals(order.get("TrangThaiDon")))throw new IllegalArgumentException("Chỉ ghi thu COD khi kiện đã giao thành công và đơn chưa kết thúc.");
        long session=dao.thuCOD(oid,form.soTien(),form.maChungTu().strip(),actor);
        payments.doiSoat(session,"MO_PHONG",form.soTien(),form.soTien(),"KHOP","THU_COD_CO_CHUNG_TU");invoices.phatHanh(oid);dao.capNhatDon(oid,"HOAN_THANH",true);
        orderService.ghiLichSu(oid,actor,(String)order.get("TrangThaiDon"),"HOAN_THANH","Đã ghi thu COD mô phỏng theo chứng từ");dao.nhatKy(actor,oid,"GHI_THU_COD");
    }
}
