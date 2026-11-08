package vn.luma.business;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.*;
import vn.luma.model.BienThe;

@Service
public class GioHangService {
    public static final String SESSION_KEY="gioKhach";
    public static final String OWNER_KEY="chuGioChoGop",NOTICE_KEY="thongBaoGopGio";
    private final GioHangDAO dao;private final SanPhamDAO products;private final TaiKhoanService users;
    public GioHangService(GioHangDAO dao,SanPhamDAO products,TaiKhoanService users){this.dao=dao;this.products=products;this.users=users;}
    public record Dong(BienThe sku,int soLuong){
        public BigDecimal thanhTien(){return sku.giaBan().multiply(BigDecimal.valueOf(soLuong));}
        public boolean coTheMua(){return sku.dangKinhDoanh() && sku.tonKhaDung()>=soLuong;}
    }
    public record Gio(List<Dong> dong,BigDecimal tongTien){public boolean coTheDat(){return !dong.isEmpty()&&dong.stream().allMatch(Dong::coTheMua);}}
    public Gio xem(String email,Map<Long,Integer> guest){
        var quantities=email==null?new TreeMap<>(guest):dao.doc(users.hienTai(email).maNguoiDung());
        var lines=products.bienThe("").stream().filter(s->quantities.containsKey(s.maBienThe())).map(s->new Dong(s,quantities.get(s.maBienThe()))).toList();
        return new Gio(lines,lines.stream().map(Dong::thanhTien).reduce(BigDecimal.ZERO,BigDecimal::add));
    }
    public Map<Long,Integer> suaKhach(Map<Long,Integer> old,long sku,int quantity,boolean add){
        if(quantity<0||quantity>99||add&&quantity==0) throw new IllegalArgumentException("Số lượng cần từ 1 đến 99; dùng Xóa để bỏ sản phẩm.");
        var result=new TreeMap<>(old);
        int next=add?Math.addExact(result.getOrDefault(sku,0),quantity):quantity;
        if(next>99) throw new IllegalArgumentException("Mỗi SKU tối đa 99 máy trong giỏ.");
        if(next==0){result.remove(sku);return result;}
        var selected=products.bienThe("").stream().filter(s->s.maBienThe()==sku&&s.dangKinhDoanh()).findFirst().orElseThrow(()->new IllegalArgumentException("Cấu hình không còn bán."));
        if(selected.tonKhaDung()<next) throw new IllegalArgumentException("Số lượng vượt tồn khả dụng. Giỏ hàng chưa giữ hàng.");
        result.put(sku,next);
        if(result.size()>50) throw new IllegalArgumentException("Giỏ tối đa 50 cấu hình khác nhau.");
        return result;
    }
    @Transactional public void sua(String email,long sku,int quantity,boolean add){
        long user=users.hienTai(email).maNguoiDung(),cart=dao.khoaGio(user);
        var changed=suaKhach(dao.doc(user),sku,quantity,add);
        dao.ghi(cart,sku,changed.getOrDefault(sku,0));
    }
    public record KetQuaGop(Map<Long,Integer> conLai,int daGop,int canKiemTra){public String thongBao(){return "Đã gộp "+daGop+" máy từ lựa chọn trước đăng nhập."+(canKiemTra>0?" Có cấu hình ngừng bán hoặc thiếu tồn; hãy kiểm tra giỏ.":"")+(conLai.isEmpty()?"":" Còn lựa chọn chưa gộp do giới hạn giỏ; bạn có thể điều chỉnh rồi gộp lại.");}}
    public record DongChoGop(long sku,String ten,int soLuong,String lyDo){}
    public List<DongChoGop> chuaGop(String email,Map<Long,Integer> pending){
        var old=dao.doc(users.hienTai(email).maNguoiDung());var known=products.bienThe("").stream().collect(java.util.stream.Collectors.toMap(BienThe::maBienThe,s->s));
        return pending.entrySet().stream().map(e->{var sku=known.get(e.getKey());String reason=sku==null?"Sản phẩm không còn trong danh mục.":old.size()>=50&&!old.containsKey(e.getKey())?"Giỏ đã có 50 cấu hình.":old.getOrDefault(e.getKey(),0)>=99?"Cấu hình trong giỏ đã đạt 99 máy.":"Chưa gộp vào giỏ. Có thể thử lại sau khi điều chỉnh.";return new DongChoGop(e.getKey(),sku==null?"Lựa chọn trước đây":sku.tenSanPham()+" · "+sku.tenBienThe(),e.getValue(),reason);}).toList();
    }
    @Transactional public KetQuaGop gop(String email,Map<Long,Integer> guest){
        if(guest.isEmpty())return new KetQuaGop(Map.of(),0,0);
        long user=users.hienTai(email).maNguoiDung(),cart=dao.khoaGio(user);
        var old=dao.doc(user);
        var known=products.bienThe("").stream().collect(java.util.stream.Collectors.toMap(BienThe::maBienThe,s->s));Map<Long,Integer> remaining=new TreeMap<>();int merged=0,review=0;
        for(var line:new TreeMap<>(guest).entrySet()){
            if(line.getValue()==null||line.getValue()<1||line.getValue()>99)throw new IllegalArgumentException("Lựa chọn trong giỏ không hợp lệ.");
            var sku=known.get(line.getKey());if(sku==null||old.size()>=50&&!old.containsKey(line.getKey())){remaining.put(line.getKey(),line.getValue());continue;}
            int used=Math.min(99-old.getOrDefault(line.getKey(),0),line.getValue());if(used>0){int q=old.getOrDefault(line.getKey(),0)+used;old.put(line.getKey(),q);dao.ghi(cart,line.getKey(),q);merged+=used;if(!sku.dangKinhDoanh()||sku.tonKhaDung()<q)review++;}
            if(used<line.getValue())remaining.put(line.getKey(),line.getValue()-used);
        }
        return new KetQuaGop(Map.copyOf(remaining),merged,review);
    }
}
