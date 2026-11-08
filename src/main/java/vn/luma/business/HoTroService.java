package vn.luma.business;

import java.util.*;
import java.text.Normalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import vn.luma.data.*;
import vn.luma.model.TaiKhoan;
import vn.luma.web.*;
import static vn.luma.data.ThanhToanDAO.id;

@Service
public class HoTroService {
    private final CauHinhHeThongService settings;private final HoTroDAO dao;private final TaiKhoanService users;private final TaiKhoanDAO accounts;private final QuanLySanPhamDAO audit;private final ThongBaoService notices;
    public HoTroService(HoTroDAO dao,TaiKhoanService users,TaiKhoanDAO accounts,QuanLySanPhamDAO audit,ThongBaoService notices,CauHinhHeThongService settings){this.settings=settings;this.dao=dao;this.users=users;this.accounts=accounts;this.audit=audit;this.notices=notices;}
    private TaiKhoan khach(String email){var u=users.hienTai(email);if(!"HOAT_DONG".equals(u.trangThai()))throw new org.springframework.security.access.AccessDeniedException("Tài khoản không khả dụng.");return u;}
    private void trang(int page){if(page<1||page>10000)throw new IllegalArgumentException("Trang từ 1 đến 10000.");}
    private Map<String,Object> cuaToi(String email,long conversation,boolean lock){long user=khach(email).maNguoiDung();var c=dao.cuoc(conversation,lock);if(id(c,"MaKhachHang")!=user)throw new KhongTimThayException("Không tìm thấy cuộc hỗ trợ.");return c;}
    public List<Map<String,Object>> cuaToi(String email,int page){trang(page);return dao.danhSach(khach(email).maNguoiDung(),"","",page);}
    public Map<String,Object> cuaToi(String email,long conversation){return cuaToi(email,conversation,false);}
    public List<Map<String,Object>> quanTri(String email,String state,String mode,int page){users.vanHanh(email);trang(page);if(!Set.of("","DANG_MO","DA_DONG").contains(state)||!Set.of("","CHATBOT","CHO_NHAN_VIEN","NHAN_VIEN").contains(mode))throw new IllegalArgumentException("Bộ lọc hỗ trợ không hợp lệ.");return dao.danhSach(null,state,mode,page);}
    public Map<String,Object> quanTri(String email,long conversation){users.vanHanh(email);return dao.cuoc(conversation,false);}
    public List<Map<String,Object>> tin(String email,long conversation,Long before,boolean admin){if(before!=null&&before<1)throw new IllegalArgumentException("Mốc tin nhắn không hợp lệ.");if(admin)quanTri(email,conversation);else cuaToi(email,conversation);return dao.tin(conversation,before);}
    public record TinMoi(String ma,String loai,String noiDung,String lucGui,String nguon,Integer phienBan) { }
    public record CapNhat(List<TinMoi> tin,String trangThai,String cheDo,String nguoiPhuTrach) { }
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public CapNhat tinMoi(String email,long conversation,long after,boolean admin){if(after<0)throw new IllegalArgumentException("Mốc tin nhắn không hợp lệ.");var c=admin?quanTri(email,conversation):cuaToi(email,conversation);return new CapNhat(dao.tinSau(conversation,after).stream().map(t->new TinMoi(Long.toString(id(t,"MaTinNhan")),(String)t.get("LoaiNguoiGui"),(String)t.get("NoiDung"),(String)t.get("luc_gui"),(String)t.get("ten_nguon"),t.get("SoPhienBan")==null?null:((Number)t.get("SoPhienBan")).intValue())).toList(),(String)c.get("TrangThai"),(String)c.get("CheDoHoTro"),(String)c.get("ten_phu_trach"));}
    @Transactional(readOnly=true) public CapNhat tinGanNhat(String email,long conversation){var c=cuaToi(email,conversation);return new CapNhat(dao.tin(conversation,null).stream().map(t->new TinMoi(Long.toString(id(t,"MaTinNhan")),(String)t.get("LoaiNguoiGui"),(String)t.get("NoiDung"),(String)t.get("luc_gui"),(String)t.get("ten_nguon"),t.get("SoPhienBan")==null?null:((Number)t.get("SoPhienBan")).intValue())).toList(),(String)c.get("TrangThai"),(String)c.get("CheDoHoTro"),(String)c.get("ten_phu_trach"));}
    @Transactional public long mo(String email){var user=khach(email);accounts.khoaNguoiDung(user.maNguoiDung());khach(email);var old=dao.dangMo(user.maNguoiDung());if(old.isPresent())return old.get();if(dao.soLanMo(user.maNguoiDung())>=10)throw new IllegalArgumentException("Bạn đã mở 10 cuộc hỗ trợ trong 24 giờ. Hãy xem cuộc đang có hoặc quay lại sau.");return dao.mo(user.maNguoiDung(),settings.hienTai().batChatbot()?"CHATBOT":"CHO_NHAN_VIEN");}
    @Transactional public long moVaGui(String email,String text){long id=mo(email);gui(email,id,new TinNhanYeuCau(java.util.UUID.randomUUID().toString(),text),false);return id;}
    @Transactional public long gui(String email,long conversation,TinNhanYeuCau f,boolean admin){
        var user=admin?users.vanHanh(email):khach(email);accounts.khoaNguoiDung(user.maNguoiDung());if(admin)users.vanHanh(email);else khach(email);
        String text=f.noiDung()==null?"":f.noiDung().strip();if(text.isEmpty()||text.length()>2000)throw new IllegalArgumentException("Tin nhắn cần 1–2000 ký tự.");try{if(!UUID.fromString(f.khoa()).toString().equalsIgnoreCase(f.khoa()))throw new IllegalArgumentException();}catch(RuntimeException ex){throw new IllegalArgumentException("Khóa gửi không hợp lệ. Tải lại trang rồi gửi lại.");}
        var c=admin?dao.cuoc(conversation,true):cuaToi(email,conversation,true);String type=admin?"NHAN_VIEN":"KHACH_HANG";var old=dao.daGui(user.maNguoiDung(),f.khoa());
        if(old.isPresent()){var t=old.get();if(id(t,"MaCuocTroChuyen")!=conversation||!text.equals(t.get("NoiDung"))||!type.equals(t.get("LoaiNguoiGui")))throw new IllegalArgumentException("Lượt gửi đã dùng với nội dung khác. Tải lại trang rồi gửi lại.");return id(t,"MaTinNhan");}
        if(!"DANG_MO".equals(c.get("TrangThai")))throw new IllegalArgumentException("Cuộc hỗ trợ đã đóng. Hãy mở cuộc mới nếu cần hỗ trợ thêm.");
        if(admin&&(!"NHAN_VIEN".equals(c.get("CheDoHoTro"))||!Objects.equals(c.get("MaQuanTriPhuTrach"),user.maNguoiDung())))throw new IllegalArgumentException("Hãy nhận cuộc hỗ trợ trước khi trả lời. Chỉ người phụ trách được gửi.");
        if(dao.tinGanDay(user.maNguoiDung())>=20)throw new IllegalArgumentException("Bạn đã gửi 20 tin trong một phút. Chờ một chút rồi thử lại cùng lượt gửi.");
        long message=dao.gui(conversation,user.maNguoiDung(),type,f.khoa(),text);
        if(admin){notices.tao(id(c,"MaKhachHang"),"IT_TUONG_TAC","Nhân viên đã phản hồi hỗ trợ","Mở cuộc hỗ trợ để xem phản hồi riêng của bạn.","/tai-khoan/ho-tro/"+conversation,"support-message:"+message);audit.log(user.maNguoiDung(),conversation,"TRA_LOI_HO_TRO","CUOC_TRO_CHUYEN");}
        else if("CHATBOT".equals(c.get("CheDoHoTro")))traLoi(conversation,message,text);
        return message;
    }
    public static String chuan(String text){return Normalizer.normalize(Objects.toString(text,""),Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").strip().replaceAll("\\s+"," ");}
    private void traLoi(long conversation,long input,String question){
        if(!settings.hienTai().batChatbot()){dao.xuLy(conversation,input,null,null,"CHUYEN_NHAN_VIEN","BOT_DA_TAT",null);dao.cheDo(conversation,"CHO_NHAN_VIEN",null);return;}
        var products=dao.timSanPham(question);
        if(!products.isEmpty()){
            var product=products.getFirst();String amount=java.text.NumberFormat.getIntegerInstance(java.util.Locale.of("vi","VN")).format(product.get("gia"));
            String answer=product.get("TenSanPham")+" — giá từ "+amount+" ₫. "+Objects.toString(product.get("MoTaNgan"),"")+" Xem chi tiết: /san-pham/"+product.get("DuongDan");
            long output=dao.gui(conversation,null,"CHATBOT","bot:"+input,answer);dao.xuLy(conversation,input,output,null,"DA_TRA_LOI",null,null);return;
        }
        String normalized=" "+chuan(question)+" ";var matches=new ArrayList<Map<String,Object>>();
        for(var topic:dao.chuDe())if(Boolean.TRUE.equals(topic.get("DangHoatDong"))){String examples=Objects.toString(topic.get("vi_du_form"),"");boolean hit=examples.lines().map(HoTroService::chuan).filter(e->!e.isBlank()).anyMatch(e->normalized.contains(" "+e+" "));if(hit)matches.add(topic);}
        if(matches.size()!=1){dao.xuLy(conversation,input,null,null,"CHUYEN_NHAN_VIEN",matches.isEmpty()?"CHUA_CO_CAN_CU":"NHIEU_CHU_DE",null);dao.cheDo(conversation,"CHO_NHAN_VIEN",null);return;}
        long topic=id(matches.getFirst(),"MaChuDeHoTro");var locked=dao.chuDe(topic,true);
        if(!Boolean.TRUE.equals(locked.get("DangHoatDong"))||Boolean.TRUE.equals(locked.get("ChuyenNhanVien"))){dao.xuLy(conversation,input,null,topic,"CHUYEN_NHAN_VIEN","CAN_NHAN_VIEN",null);dao.cheDo(conversation,"CHO_NHAN_VIEN",null);return;}
        var document=dao.daDuyet(topic);if(document.isEmpty()){dao.xuLy(conversation,input,null,topic,"CHUYEN_NHAN_VIEN","CHUA_CO_NGUON_DUYET",null);dao.cheDo(conversation,"CHO_NHAN_VIEN",null);return;}
        var source=document.get();long output=dao.gui(conversation,null,"CHATBOT","bot:"+input,(String)source.get("NoiDungDaSoan"));dao.xuLy(conversation,input,output,topic,"DA_TRA_LOI",null,id(source,"MaTaiLieuHoTro"));
    }
    @Transactional public void chuyen(String email,long conversation){var c=cuaToi(email,conversation,true);if(!"DANG_MO".equals(c.get("TrangThai")))throw new IllegalArgumentException("Cuộc hỗ trợ đã đóng.");if("CHATBOT".equals(c.get("CheDoHoTro")))dao.cheDo(conversation,"CHO_NHAN_VIEN",null);}
    @Transactional public void nhan(String email,long conversation){long actor=users.vanHanh(email).maNguoiDung();var c=dao.cuoc(conversation,true);if(!"DANG_MO".equals(c.get("TrangThai")))throw new IllegalArgumentException("Cuộc hỗ trợ đã đóng.");if(c.get("MaQuanTriPhuTrach")!=null){if(id(c,"MaQuanTriPhuTrach")==actor)return;throw new IllegalArgumentException("Cuộc hỗ trợ đã có người phụ trách.");}dao.cheDo(conversation,"NHAN_VIEN",actor);audit.log(actor,conversation,"NHAN_HO_TRO","CUOC_TRO_CHUYEN");}
    @Transactional public void traHangCho(String email,long conversation){long actor=users.vanHanh(email).maNguoiDung();var c=dao.cuoc(conversation,true);if(!"DANG_MO".equals(c.get("TrangThai"))||!Objects.equals(c.get("MaQuanTriPhuTrach"),actor))throw new IllegalArgumentException("Chỉ người phụ trách cuộc đang mở được trả về hàng chờ.");dao.cheDo(conversation,"CHO_NHAN_VIEN",null);audit.log(actor,conversation,"TRA_HO_TRO_VE_HANG_CHO","CUOC_TRO_CHUYEN");}
    @Transactional public void dong(String email,long conversation,boolean admin){long actor=(admin?users.vanHanh(email):khach(email)).maNguoiDung();var c=admin?dao.cuoc(conversation,true):cuaToi(email,conversation,true);if("DA_DONG".equals(c.get("TrangThai")))return;if(admin&&c.get("MaQuanTriPhuTrach")!=null&&!Objects.equals(c.get("MaQuanTriPhuTrach"),actor))throw new IllegalArgumentException("Chỉ người phụ trách được đóng cuộc đã nhận.");dao.dong(conversation);if(admin)audit.log(actor,conversation,"DONG_HO_TRO","CUOC_TRO_CHUYEN");}
    public List<Map<String,Object>> chuDe(String email){users.quanTri(email);return dao.chuDe();}
    @Transactional public long luuChuDe(String email,Long topic,ChuDeHoTroYeuCau f){long actor=users.quanTri(email).maNguoiDung();dao.khoaChuDe();var topics=dao.chuDe();if(topic==null&&topics.size()>=100)throw new IllegalArgumentException("Tối đa 100 chủ đề trong bản demo.");if(topic!=null){var old=dao.chuDe(topic,true);if(!old.get("KyHieu").equals(f.kyHieu()))throw new IllegalArgumentException("Ký hiệu chủ đề giữ cố định.");}var examples=Objects.toString(f.viDu(),"").lines().map(HoTroService::chuan).filter(e->!e.isBlank()).distinct().toList();if(examples.size()>10||examples.stream().anyMatch(e->e.length()<5||e.length()>200))throw new IllegalArgumentException("Nhập tối đa 10 cụm hỏi, mỗi dòng 5–200 ký tự; dùng cụm từ đủ rõ.");String json="["+examples.stream().map(e->"\""+e+"\"").collect(java.util.stream.Collectors.joining(","))+"]";try{long saved=dao.luuChuDe(topic,f,json);audit.log(actor,saved,"LUU_CHU_DE_HO_TRO","CHU_DE_HO_TRO");return saved;}catch(DuplicateKeyException ex){throw new IllegalArgumentException("Ký hiệu chủ đề đã được dùng.");}}
    public List<Map<String,Object>> taiLieu(String email,int page){users.quanTri(email);trang(page);return dao.taiLieu(page);}
    public Map<String,Object> taiLieu(String email,long id){users.quanTri(email);return dao.taiLieu(id);}
    @Transactional public long taoTaiLieu(String email,TaiLieuHoTroYeuCau f){long actor=users.quanTri(email).maNguoiDung();dao.chuDe(f.chuDe(),true);long saved=dao.taoTaiLieu(f);audit.log(actor,saved,"SOAN_TAI_LIEU_HO_TRO","TAI_LIEU_HO_TRO");return saved;}
    @Transactional public void duyet(String email,long document){long actor=users.quanTri(email).maNguoiDung();var d=dao.taiLieu(document);long topic=id(d,"MaChuDeHoTro");dao.chuDe(topic,true);d=dao.taiLieu(document);if("DA_DUYET".equals(d.get("TrangThai")))return;if(!"NHAP".equals(d.get("TrangThai")))throw new IllegalArgumentException("Bản đã ngừng dùng không thể duyệt lại. Hãy tạo phiên bản mới.");dao.duyet(document,topic,actor);audit.log(actor,document,"DUYET_TAI_LIEU_HO_TRO","TAI_LIEU_HO_TRO");}
    @Transactional public void ngung(String email,long document){long actor=users.quanTri(email).maNguoiDung();var d=dao.taiLieu(document);dao.chuDe(id(d,"MaChuDeHoTro"),true);dao.ngung(document);audit.log(actor,document,"NGUNG_TAI_LIEU_HO_TRO","TAI_LIEU_HO_TRO");}
    public List<Map<String,Object>> xuLy(String email,long conversation){quanTri(email,conversation);return dao.xuLy(conversation);}
    public List<Map<String,Object>> kiemDuyet(String email,long conversation){quanTri(email,conversation);return dao.kiemDuyet(conversation);}
    @Transactional public void kiemDuyet(String email,long conversation,long turn,String result,String note){long actor=users.quanTri(email).maNguoiDung();dao.cuoc(conversation,false);if(!Set.of("DUNG","SAI","THIEU_NGUON","RUI_RO_BAO_MAT").contains(result)||note==null||note.isBlank()||note.length()>500)throw new IllegalArgumentException("Chọn kết quả và nhập nhận xét 1–500 ký tự.");dao.kiemDuyet(conversation,turn,actor,result,note.strip());audit.log(actor,turn,"KIEM_DUYET_CHATBOT","LAN_XU_LY_CHATBOT");}
}
