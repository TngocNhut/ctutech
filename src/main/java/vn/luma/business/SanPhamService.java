package vn.luma.business;

import java.util.List;
import org.springframework.stereotype.Service;
import vn.luma.data.SanPhamDAO;
import vn.luma.model.SanPham;
import vn.luma.model.BienThe;

@Service
public class SanPhamService {
    private final SanPhamDAO dao;
    public SanPhamService(SanPhamDAO dao) { this.dao = dao; }
    public record Trang(List<SanPham> noiDung,long tong,int trang,int kichThuoc){public int soTrang(){return (int)Math.max(1,(tong+kichThuoc-1)/kichThuoc);}}
    public Trang loc(vn.luma.web.BoLocSanPham form){
        return loc(form,vn.luma.web.BoLocBoSung.trong());
    }
    public Trang loc(vn.luma.web.BoLocSanPham form,vn.luma.web.BoLocBoSung more){
        if(form.giaTu()!=null&&form.giaDen()!=null&&form.giaTu().compareTo(form.giaDen())>0)throw new IllegalArgumentException("Giá thấp nhất không được lớn hơn giá cao nhất.");
        if(!java.util.Set.of("mac-dinh","gia-tang","gia-giam","moi-nhat","ten").contains(form.sapXep()))throw new IllegalArgumentException("Cách sắp xếp không hợp lệ.");
        var allowed=new java.util.HashMap<Long,java.util.Map<String,Object>>();dao.thuocTinhLoc().forEach(a->allowed.put(((Number)a.get("MaThuocTinh")).longValue(),a));var filters=new java.util.ArrayList<SanPhamDAO.DieuKienThuocTinh>();
        for(var e:more.thuocTinh().entrySet()){var attribute=allowed.get(e.getKey());if(attribute==null)throw new IllegalArgumentException("Có thuộc tính không còn được phép lọc. Hãy xóa bộ lọc và chọn lại.");String type=(String)attribute.get("KieuDuLieu");filters.add(new SanPhamDAO.DieuKienThuocTinh(e.getKey(),type,CauHinhSanPhamService.docGiaTri(type,e.getValue())));}
        var extended=new SanPhamDAO.LocMoRong(more.dong(),filters);long total=dao.dem(form,extended);int page=Math.min(Math.max(1,form.trang()),(int)Math.max(1,(total+11)/12));return new Trang(dao.loc(form,page,12,extended),total,page,12);
    }
    public java.util.Map<Long,List<String>> giaTriLoc(){return dao.giaTriLoc();}
    public List<java.util.Map<String,Object>> thuocTinhLoc(){return dao.thuocTinhLoc();}
    public List<java.util.Map<String,Object>> dongSanPham(){return dao.dongSanPham();}
    public List<java.util.Map<String,Object>> thuongHieu(String category){return dao.thuongHieu(category);}
    public List<java.util.Map<String,Object>> dongSanPham(String category){return dao.dongSanPham(category);}
    public List<java.util.Map<String,Object>> danhMuc(){return dao.danhMuc();}
    public List<java.util.Map<String,Object>> thuongHieu(){return dao.thuongHieu();}
    public List<SanPham> danhSach(String tuKhoa, String loai) {
        return dao.danhSach(tuKhoa == null ? "" : tuKhoa.strip(), loai == null ? "" : loai.strip());
    }
    public SanPham chiTiet(String slug){return dao.tim(slug).orElseThrow(()->new KhongTimThayException("Sản phẩm không khả dụng."));}
    public List<BienThe> bienThe(String slug){chiTiet(slug);return dao.bienThe(slug);}
    public BienThe cauHinh(long id){return dao.bienTheTheoId(id).filter(BienThe::dangKinhDoanh).orElseThrow(()->new KhongTimThayException("Cấu hình không còn công khai."));}
    public java.util.Optional<java.util.Map<String,Object>> moHinh(String slug,long sku){var product=chiTiet(slug);if(dao.bienThe(slug).stream().noneMatch(v->v.maBienThe()==sku))throw new KhongTimThayException("Cấu hình không thuộc sản phẩm.");return dao.moHinh(product.maSanPham(),sku);}
    public List<java.util.Map<String,Object>> anh(String slug,long sku){var product=chiTiet(slug);if(dao.bienThe(slug).stream().noneMatch(v->v.maBienThe()==sku))throw new KhongTimThayException("Cấu hình không khả dụng.");return dao.anh(product.maSanPham(),sku);}
    public java.util.Map<String,Object> noiDung(String slug){chiTiet(slug);return dao.noiDung(slug);}
}
