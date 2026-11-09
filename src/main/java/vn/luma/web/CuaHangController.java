package vn.luma.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import vn.luma.business.SanPhamService;

@Controller
public class CuaHangController {
    private final vn.luma.data.SanPhamTrangChuDAO trangChuDAO;private final LuaChonTrinhDuyet choices;private final vn.luma.business.NoiDungService editorial;private final vn.luma.business.CauHinhHeThongService settings;private final vn.luma.business.CauHinhSanPhamService configuration;private final SanPhamService sanPham;private final vn.luma.business.TheoDoiService theoDoi;private final vn.luma.business.DanhGiaService danhGia;
    public CuaHangController(SanPhamService sanPham,vn.luma.business.TheoDoiService theoDoi,vn.luma.business.DanhGiaService danhGia,vn.luma.business.CauHinhSanPhamService configuration,vn.luma.business.CauHinhHeThongService settings,vn.luma.business.NoiDungService editorial,LuaChonTrinhDuyet choices,vn.luma.data.SanPhamTrangChuDAO trangChuDAO) { this.trangChuDAO=trangChuDAO;this.choices=choices; this.editorial=editorial; this.settings=settings; this.configuration=configuration; this.sanPham = sanPham;this.theoDoi=theoDoi;this.danhGia=danhGia; }
    @GetMapping({"/", "/san-pham"})
    public String trangChu(@jakarta.validation.Valid @org.springframework.web.bind.annotation.ModelAttribute("loc") BoLocSanPham form,org.springframework.validation.BindingResult errors,@RequestParam org.springframework.util.MultiValueMap<String,String> query,Model model,jakarta.servlet.http.HttpServletRequest request){
        boolean home="/".equals(request.getRequestURI());model.addAttribute("laTrangChu",home);if(home){model.addAttribute("sanPhamMoi",trangChuDAO.lay(false));model.addAttribute("sanPhamBanChay",trangChuDAO.lay(true));}
        model.addAttribute("bannerCongKhai",editorial.bannerCongKhai("TRANG_CHU"));model.addAttribute("thongBaoTrangChu",settings.hienTai().thongBao());
        var result=new SanPhamService.Trang(java.util.List.of(),0,1,12);
        var more=BoLocBoSung.trong();
        try{more=BoLocBoSung.doc(query);}catch(IllegalArgumentException ex){errors.addError(new org.springframework.validation.ObjectError("loc",ex.getMessage()));}
        if(!errors.hasErrors())try{result=sanPham.loc(form,more);}catch(IllegalArgumentException ex){errors.addError(new org.springframework.validation.ObjectError("loc",ex.getMessage()));}
        String loaiGoc=form.loai();model.addAttribute("trongDanhMuc",sanPham.danhMuc().stream().anyMatch(c->loaiGoc.equals(c.get("DuongDan"))));model.addAttribute("boSung",more);model.addAttribute("giaTriLoc",sanPham.giaTriLoc());model.addAttribute("thuocTinhLoc",sanPham.thuocTinhLoc().stream().filter(a->sanPham.danhMuc().stream().anyMatch(c->loaiGoc.equals(c.get("DuongDan"))&&c.get("MaPhanLoai").equals(a.get("MaPhanLoai")))).toList());model.addAttribute("dongSanPham",sanPham.dongSanPham(form.loai()));
        model.addAttribute("loc",form);model.addAttribute("sanPham",result.noiDung());model.addAttribute("ketQua",result);model.addAttribute("q",form.q());model.addAttribute("loai",form.loai());model.addAttribute("danhMuc",sanPham.danhMuc());model.addAttribute("thuongHieu",sanPham.thuongHieu(form.loai()));
        var labels=java.util.Map.ofEntries(java.util.Map.entry("q","từ khóa"),java.util.Map.entry("loai","danh mục"),java.util.Map.entry("hang","thương hiệu"),java.util.Map.entry("giaTu","giá thấp nhất"),java.util.Map.entry("giaDen","giá cao nhất"),java.util.Map.entry("ram","RAM"),java.util.Map.entry("luuTru","bộ nhớ"),java.util.Map.entry("cpu","CPU"),java.util.Map.entry("gpu","GPU"),java.util.Map.entry("sapXep","cách sắp xếp"),java.util.Map.entry("trang","số trang"));
        var messages=new java.util.ArrayList<String>();errors.getGlobalErrors().forEach(e->messages.add(e.getDefaultMessage()));errors.getFieldErrors().forEach(e->messages.add("Kiểm tra lại "+labels.getOrDefault(e.getField(),"bộ lọc")+": giá trị không hợp lệ hoặc vượt giới hạn."));model.addAttribute("loiLoc",messages);
        model.addAttribute(org.springframework.validation.BindingResult.MODEL_KEY_PREFIX+"loc",errors);model.addAttribute("linkTruoc",link(form,more,result.trang()-1));model.addAttribute("linkSau",link(form,more,result.trang()+1));return "cua-hang";
    }
    static String link(BoLocSanPham f,BoLocBoSung more,int page){
        var params=new java.util.LinkedHashMap<String,Object>();params.put("q",f.q());params.put("loai",f.loai());params.put("hang",f.hang());params.put("giaTu",f.giaTu());params.put("giaDen",f.giaDen());params.put("ram",f.ram());params.put("luuTru",f.luuTru());params.put("cpu",f.cpu());params.put("gpu",f.gpu());params.put("conHang",f.conHang());params.put("sapXep",f.sapXep());params.put("trang",page);params.put("dong",more.dong());more.thuocTinh().forEach((id,value)->params.put("tt."+id,value));
        return "/san-pham?"+params.entrySet().stream().filter(e->e.getValue()!=null).map(e->encode(e.getKey())+"="+encode(e.getValue().toString())).collect(java.util.stream.Collectors.joining("&"));
    }
    private static String encode(String value){return java.net.URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8);}
    @GetMapping("/san-pham/{slug}")
    public String chiTiet(@PathVariable String slug,@RequestParam(required=false) Long sku,java.security.Principal user,jakarta.servlet.http.HttpSession session,Model model){
        var variants=sanPham.bienThe(slug);
        var selected=variants.stream().filter(v->sku==null||v.maBienThe()==sku).findFirst()
            .orElseThrow(()->new vn.luma.business.KhongTimThayException("Cấu hình không khả dụng."));
        model.addAttribute("sp",sanPham.chiTiet(slug));model.addAttribute("bienThe",variants);model.addAttribute("sku",selected);model.addAttribute("anh",sanPham.anh(slug,selected.maBienThe()));model.addAttribute("noiDung",sanPham.noiDung(slug));
        model.addAttribute("moHinh",sanPham.moHinh(slug,selected.maBienThe()).orElse(null));model.addAttribute("thongSoMoRong",configuration.congKhai(slug,selected.maBienThe()));model.addAttribute("daDangNhap",user!=null);model.addAttribute("theoDoi",user==null?null:theoDoi.cuaToi(user.getName(),sanPham.chiTiet(slug).maSanPham()).orElse(null));model.addAttribute("tongHopDanhGia",danhGia.congKhai(slug,1).tongHop());
        choices.daXem(session,selected.maBienThe());return "san-pham/chi-tiet";
    }
}
