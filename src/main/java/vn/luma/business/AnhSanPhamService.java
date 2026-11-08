package vn.luma.business;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.QuanLySanPhamDAO;
import vn.luma.web.AnhSanPhamYeuCau;
import static vn.luma.data.ThanhToanDAO.id;

@Service
public class AnhSanPhamService {
    private final QuanLySanPhamDAO dao;private final TaiKhoanService users;private final KhoAnhSanPham files;
    public AnhSanPhamService(QuanLySanPhamDAO dao,TaiKhoanService users,KhoAnhSanPham files){this.dao=dao;this.users=users;this.files=files;}
    public List<Map<String,Object>> danhSach(String email,long product){users.vanHanh(email);dao.model(product,false);return dao.anh(product);}
    public Map<String,Object> chiTiet(String email,long product,long image){return danhSach(email,product).stream().filter(a->id(a,"MaTep")==image).findFirst().orElseThrow(()->new KhongTimThayException("Ảnh không thuộc model này."));}
    private void kiemTra(long product,Long image,AnhSanPhamYeuCau f){
        QuanLySanPhamService.kiemTraNguon(f.nguon());if(f.maSku()!=null&&id(dao.bienTheTheoId(f.maSku()),"MaSanPham")!=product)throw new IllegalArgumentException("Ảnh SKU phải thuộc đúng model.");
        if(f.hienThi()&&dao.anh(product).stream().filter(a->(Boolean)a.get("DangHienThi")&&(image==null||id(a,"MaTep")!=image)).count()>=24)throw new IllegalArgumentException("Một model tối đa 24 ảnh đang hiển thị.");
    }
    @Transactional public long tao(String email,long product,AnhSanPhamYeuCau form,byte[] bytes){long actor=users.vanHanh(email).maNguoiDung();dao.model(product,true);kiemTra(product,null,form);String file=files.luu(bytes);long id=dao.taoAnh(product,form,"/tep-san-pham/"+file);dao.log(actor,id,"TAO_ANH_SAN_PHAM","TEP_SAN_PHAM");return id;}
    @Transactional public void sua(String email,long product,long image,AnhSanPhamYeuCau form){long actor=users.vanHanh(email).maNguoiDung();dao.model(product,true);chiTiet(email,product,image);kiemTra(product,image,form);dao.suaAnh(image,form);dao.log(actor,image,"SUA_ANH_SAN_PHAM","TEP_SAN_PHAM");}
    @Transactional public void an(String email,long product,long image){long actor=users.vanHanh(email).maNguoiDung();dao.model(product,true);chiTiet(email,product,image);dao.anAnh(image);dao.log(actor,image,"AN_ANH_SAN_PHAM","TEP_SAN_PHAM");}
    public byte[] doc(String email,String file){
        if(!file.matches("[0-9a-f-]{36}\\.(png|jpg)"))throw new KhongTimThayException("Không tìm thấy ảnh.");var row=dao.tepTheoDuongDan("/tep-san-pham/"+file).orElseThrow(()->new KhongTimThayException("Không tìm thấy ảnh."));
        boolean admin=false;if(email!=null){var account=users.hienTai(email);admin="QuanTri".equals(account.vaiTro())&&"HOAT_DONG".equals(account.trangThai());}
        if(!(Boolean)row.get("ChoXem")&&!admin)throw new KhongTimThayException("Ảnh không khả dụng.");return files.doc(file);
    }
}
