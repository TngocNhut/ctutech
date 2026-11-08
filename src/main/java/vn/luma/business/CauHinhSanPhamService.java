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
public class CauHinhSanPhamService {
    private final CauHinhSanPhamDAO dao;private final QuanLySanPhamDAO models;private final TaiKhoanService users;private final SanPhamService shop;
    public CauHinhSanPhamService(CauHinhSanPhamDAO dao,QuanLySanPhamDAO models,TaiKhoanService users,SanPhamService shop){this.dao=dao;this.models=models;this.users=users;this.shop=shop;}
    private void trang(int page){if(page<1||page>10000)throw new IllegalArgumentException("Trang từ 1 đến 10000.");}
    public List<Map<String,Object>> dong(String email,long brand,int page){users.vanHanh(email);trang(page);if(brand<0)throw new IllegalArgumentException("Hãng không hợp lệ.");return dao.dong(brand,page);}
    public Map<String,Object> dong(String email,long id){users.vanHanh(email);return dao.dong(id,false);}
    @Transactional public long luuDong(String email,Long id,DongSanPhamYeuCau f){long actor=users.vanHanh(email).maNguoiDung();models.muc(QuanLySanPhamDAO.Loai.THUONG_HIEU,f.maThuongHieu());if(id!=null){var old=dao.dong(id,true);if(id(old,"MaThuongHieu")!=f.maThuongHieu()&&dao.dongDangDung(id))throw new IllegalArgumentException("Dòng đã có model; không thể đổi hãng. Tạo dòng mới cho hãng khác.");}try{long saved=dao.luuDong(id,f);models.log(actor,saved,"LUU_DONG_SAN_PHAM","DONG_SAN_PHAM");return saved;}catch(DuplicateKeyException ex){throw new IllegalArgumentException("Đường dẫn dòng đã được dùng trong hãng này.");}}
    @Transactional public void xoaDong(String email,long id){long actor=users.vanHanh(email).maNguoiDung();dao.dong(id,true);if(dao.dongDangDung(id))throw new IllegalArgumentException("Dòng đang được model sử dụng; hãy bỏ liên kết trước khi xóa.");dao.xoaDong(id);models.log(actor,id,"XOA_DONG_SAN_PHAM","DONG_SAN_PHAM");}
    public List<Map<String,Object>> dongCuaModel(String email,long model){users.vanHanh(email);return dao.dongCuaHang(id(models.model(model,false),"MaThuongHieu"));}
    @Transactional public void ganDong(String email,long model,Long line){long actor=users.vanHanh(email).maNguoiDung();var product=models.model(model,true);if(line!=null){var selected=dao.dong(line,true);if(id(selected,"MaThuongHieu")!=id(product,"MaThuongHieu"))throw new IllegalArgumentException("Dòng và model phải cùng hãng.");}dao.ganDong(model,line);models.log(actor,model,"GAN_DONG_SAN_PHAM","SAN_PHAM");}
    public List<Map<String,Object>> thuocTinh(String email,long category,int page){users.vanHanh(email);trang(page);if(category<0)throw new IllegalArgumentException("Danh mục không hợp lệ.");return dao.thuocTinh(category,page);}
    public Map<String,Object> thuocTinh(String email,long id){users.vanHanh(email);return dao.docThuocTinh(id);}
    @Transactional public long luuThuocTinh(String email,Long id,ThuocTinhYeuCau f){long actor=users.vanHanh(email).maNguoiDung();models.muc(QuanLySanPhamDAO.Loai.PHAN_LOAI,f.maPhanLoai());if(id!=null){var old=dao.thuocTinh(id,false);if((id(old,"MaPhanLoai")!=f.maPhanLoai()||!old.get("KieuDuLieu").equals(f.kieu()))&&dao.thuocTinhDangDung(id))throw new IllegalArgumentException("Thuộc tính đã gán SKU; không thể đổi danh mục hoặc kiểu dữ liệu.");}try{long saved=dao.luuThuocTinh(id,f);models.log(actor,saved,"LUU_THUOC_TINH","THUOC_TINH");return saved;}catch(DuplicateKeyException ex){throw new IllegalArgumentException("Ký hiệu thuộc tính đã được dùng trong danh mục.");}}
    @Transactional public void xoaThuocTinh(String email,long id){long actor=users.vanHanh(email).maNguoiDung();dao.thuocTinh(id,false);if(dao.thuocTinhDangDung(id))throw new IllegalArgumentException("Thuộc tính đang có giá trị SKU; không thể xóa.");dao.xoaThuocTinh(id);models.log(actor,id,"XOA_THUOC_TINH","THUOC_TINH");}
    private Map<String,Object> sku(String email,long model,long sku,boolean lock){users.vanHanh(email);var product=models.model(model,lock);var variant=models.bienTheTheoId(sku);if(id(variant,"MaSanPham")!=model)throw new KhongTimThayException("SKU không thuộc model này.");if(lock)dao.khoaSku(sku);return product;}
    public List<Map<String,Object>> giaTri(String email,long model,long sku){var product=sku(email,model,sku,false);return dao.giaTri(sku,id(product,"MaPhanLoai"));}
    public Map<String,Object> giaTri(String email,long model,long sku,long attribute){return giaTri(email,model,sku).stream().filter(a->id(a,"MaThuocTinh")==attribute).findFirst().orElseThrow(()->new KhongTimThayException("Thuộc tính không thuộc danh mục của SKU."));}
    public static Object docGiaTri(String type,String input){
        if(input==null||input.isBlank()||input.length()>250)throw new IllegalArgumentException("Giá trị cần 1–250 ký tự.");String value=input.strip();
        if("CHU".equals(type))return value;
        if("CO_KHONG".equals(type)){if(!Set.of("true","false").contains(value))throw new IllegalArgumentException("Hãy chọn Có hoặc Không.");return Boolean.valueOf(value);}
        if(!"SO".equals(type))throw new IllegalArgumentException("Kiểu thuộc tính không hợp lệ.");
        try{var n=new BigDecimal(value).stripTrailingZeros();if(n.scale()>4||(long)n.precision()-n.scale()>12)throw new NumberFormatException();return n;}catch(NumberFormatException ex){throw new IllegalArgumentException("Nhập số tối đa 12 chữ số nguyên và 4 chữ số thập phân, dùng dấu chấm.");}
    }
    @Transactional public void luuGiaTri(String email,long model,long sku,long attribute,GiaTriThuocTinhYeuCau f){long actor=users.vanHanh(email).maNguoiDung();var product=sku(email,model,sku,true);var attr=dao.thuocTinh(attribute,true);if(id(attr,"MaPhanLoai")!=id(product,"MaPhanLoai"))throw new IllegalArgumentException("Thuộc tính không cùng danh mục với SKU.");String type=(String)attr.get("KieuDuLieu");Object value=docGiaTri(type,f.giaTri());dao.luuGiaTri(sku,model,id(product,"MaPhanLoai"),attribute,type,value instanceof String t?t:null,value instanceof BigDecimal n?n:null,value instanceof Boolean yes?yes:null);models.log(actor,sku,"GAN_GIA_TRI_THUOC_TINH","BIEN_THE");}
    @Transactional public void xoaGiaTri(String email,long model,long sku,long attribute){long actor=users.vanHanh(email).maNguoiDung();sku(email,model,sku,true);giaTri(email,model,sku,attribute);dao.xoaGiaTri(sku,attribute);models.log(actor,sku,"XOA_GIA_TRI_THUOC_TINH","BIEN_THE");}
    public List<Map<String,Object>> congKhai(String slug,long sku){if(shop.bienThe(slug).stream().noneMatch(v->v.maBienThe()==sku))throw new KhongTimThayException("Cấu hình không khả dụng.");return dao.congKhai(sku).stream().map(r->{var row=new HashMap<>(r);String value=switch((String)r.get("KieuDuLieu")){case "CHU"->(String)r.get("GiaTriChu");case "SO"->((BigDecimal)r.get("GiaTriSo")).stripTrailingZeros().toPlainString();default->Boolean.TRUE.equals(r.get("GiaTriCoKhong"))?"Có":"Không";};row.put("hienThi",value+(r.get("DonVi")==null||((String)r.get("DonVi")).isBlank()?"":" "+r.get("DonVi")));return (Map<String,Object>)row;}).toList();}
}
