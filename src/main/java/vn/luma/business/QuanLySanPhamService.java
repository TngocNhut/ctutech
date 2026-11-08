package vn.luma.business;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import vn.luma.data.*;
import vn.luma.data.QuanLySanPhamDAO.Loai;
import vn.luma.web.*;
import static vn.luma.data.ThanhToanDAO.id;

@Service
public class QuanLySanPhamService {
    private final QuanLySanPhamDAO dao;private final TaiKhoanService users;private final KhoService stock;private final KhoDAO stockDao;
    public QuanLySanPhamService(QuanLySanPhamDAO dao,TaiKhoanService users,KhoService stock,KhoDAO stockDao){this.dao=dao;this.users=users;this.stock=stock;this.stockDao=stockDao;}
    public Loai loai(String code){return switch(code){case "phan-loai"->Loai.PHAN_LOAI;case "thuong-hieu"->Loai.THUONG_HIEU;default->throw new KhongTimThayException("Không có loại danh mục này.");};}
    public List<Map<String,Object>> danhMuc(String email,String type){users.vanHanh(email);return dao.danhMuc(loai(type));}
    public Map<String,Object> muc(String email,String type,long id){users.vanHanh(email);return dao.muc(loai(type),id);}
    @Transactional public long luuMuc(String email,String type,Long id,MucDanhMucYeuCau form){long actor=users.vanHanh(email).maNguoiDung();var category=loai(type);try{long saved=dao.luuMuc(category,id,form);dao.log(actor,saved,id==null?"TAO_DANH_MUC":"SUA_DANH_MUC",category.name());return saved;}catch(DuplicateKeyException ex){throw new IllegalArgumentException("Tên hoặc đường dẫn đã được dùng trong danh mục.");}}
    @Transactional public void anMuc(String email,String type,long id){long actor=users.vanHanh(email).maNguoiDung();var category=loai(type);dao.anMuc(category,id);dao.log(actor,id,"AN_DANH_MUC",category.name());}
    public List<Map<String,Object>> danhSach(String email,String q){return danhSach(email,q,1);}
    public List<Map<String,Object>> danhSach(String email,String q,int page){users.vanHanh(email);if(page<1||page>10000)throw new IllegalArgumentException("Trang không hợp lệ.");if(q==null)q="";if(q.length()>100)throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");return dao.danhSach(q.strip(),page);}
    public Map<String,Object> model(String email,long product){users.vanHanh(email);var row=new HashMap<String,Object>(dao.model(product,false));row.put("LaPhuKien",dao.muc(Loai.PHAN_LOAI,id(row,"MaPhanLoai")).get("DuongDan").equals("phu-kien"));return row;}
    public List<Map<String,Object>> bienThe(String email,long product){model(email,product);return dao.bienThe(product);}
    public Map<String,Object> sku(String email,long sku){users.vanHanh(email);return dao.bienTheTheoId(sku);}
    private String chuanTimKiem(String text){return java.text.Normalizer.normalize(text,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT);}
    public static void kiemTraNguon(String source){
        if(source==null||source.isBlank())return;
        try{var uri=java.net.URI.create(source.strip());if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)throw new IllegalArgumentException();}catch(IllegalArgumentException ex){throw new IllegalArgumentException("Nguồn tham khảo cần là liên kết HTTPS đầy đủ.");}
    }
    @Transactional public long luuModel(String email,Long product,NoiDungSanPhamYeuCau form){
        long actor=users.vanHanh(email).maNguoiDung();kiemTraNguon(form.nguon());dao.muc(Loai.PHAN_LOAI,form.maPhanLoai());var brand=dao.muc(Loai.THUONG_HIEU,form.maThuongHieu());
        if(product!=null){var old=dao.model(product,true);if(id(old,"MaPhanLoai")!=form.maPhanLoai()&&dao.coThuocTinh(product))throw new IllegalArgumentException("Model đã có thuộc tính theo danh mục. Cần giữ danh mục hoặc tạo model mới.");if(id(old,"MaThuongHieu")!=form.maThuongHieu()&&old.get("MaDongSanPham")!=null)throw new IllegalArgumentException("Hãy bỏ liên kết dòng sản phẩm trước khi đổi hãng của model.");}
        String search=chuanTimKiem(form.tenSanPham()+" "+brand.get("Ten"));if(search.length()>255)search=search.substring(0,255);
        try{long saved=dao.luuModel(product,form,search);dao.log(actor,saved,product==null?"TAO_MODEL":"SUA_MODEL","SAN_PHAM");return saved;}catch(DuplicateKeyException ex){throw new IllegalArgumentException("Đường dẫn sản phẩm đã tồn tại.");}
    }
    @Transactional public void anModel(String email,long product){long actor=users.vanHanh(email).maNguoiDung();dao.anModel(product);dao.log(actor,product,"AN_MODEL","SAN_PHAM");}
    @Transactional public long luuSku(String email,long product,Long sku,NoiDungBienTheYeuCau form){
        long actor=users.vanHanh(email).maNguoiDung();var parent=dao.model(product,false);boolean accessory=dao.muc(Loai.PHAN_LOAI,id(parent,"MaPhanLoai")).get("DuongDan").equals("phu-kien");
        if(form.ram()<0||form.luuTru()<0||form.manHinh()==null||form.manHinh().signum()<0||(!accessory&&(form.luuTru()==0||form.manHinh().signum()==0))||(accessory&&(form.ram()!=0||form.luuTru()!=0||form.manHinh().signum()!=0)))throw new IllegalArgumentException("Phụ kiện không dùng RAM/bộ nhớ/màn hình; điện thoại và laptop cần bộ nhớ và màn hình dương.");
        if(sku!=null){var old=dao.bienTheTheoId(sku);if(id(old,"MaSanPham")!=product)throw new KhongTimThayException("SKU không thuộc model này.");stockDao.khoa(sku);}
        var prices=form.giaTon();if(prices==null||prices.giaBan()==null||prices.giaNiemYet()==null||prices.giaVon()==null||prices.giaBan().signum()<=0||prices.giaVon().signum()<0||prices.giaNiemYet().compareTo(prices.giaBan())<0)throw new IllegalArgumentException("Cần giá bán dương, giá vốn không âm và niêm yết từ giá bán trở lên.");
        try{long saved=dao.luuBienThe(product,sku,form);if(sku==null)dao.giaDau(saved,actor,form);stock.capNhat(email,saved,prices);dao.log(actor,saved,sku==null?"TAO_SKU":"SUA_SKU","BIEN_THE");return saved;}catch(DuplicateKeyException ex){throw new IllegalArgumentException("Mã SKU đã tồn tại.");}
    }
    @Transactional public void ngungSku(String email,long sku){long actor=users.vanHanh(email).maNguoiDung();dao.ngungBienThe(sku);dao.log(actor,sku,"NGUNG_SKU","BIEN_THE");}
}
