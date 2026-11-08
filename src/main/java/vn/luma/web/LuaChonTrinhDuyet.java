package vn.luma.web;

import java.util.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import vn.luma.business.*;
import vn.luma.model.BienThe;

@Component
public class LuaChonTrinhDuyet {
    private final SanPhamService products;
    public LuaChonTrinhDuyet(SanPhamService products){this.products=products;}
    public record LuaChon(BienThe sku,String anh) { }
    private static final String RECENT="skuDaXem";
    @SuppressWarnings("unchecked") private List<Long> ids(HttpSession session,String key){var value=session.getAttribute(key);return value instanceof List<?>?new ArrayList<>((List<Long>)value):new ArrayList<>();}
    public List<LuaChon> xem(HttpSession session){synchronized(session){var result=new ArrayList<LuaChon>();var valid=new ArrayList<Long>();for(long id:ids(session,RECENT))try{var sku=products.cauHinh(id);String image=products.chiTiet(sku.duongDan()).anhDaiDien();result.add(new LuaChon(sku,image));valid.add(id);}catch(KhongTimThayException ignored){}session.setAttribute(RECENT,List.copyOf(valid));return List.copyOf(result);}}
    public void daXem(HttpSession session,long sku){var selected=products.cauHinh(sku);synchronized(session){var list=ids(session,RECENT);list.removeIf(id->{try{return products.cauHinh(id).duongDan().equals(selected.duongDan());}catch(KhongTimThayException ignored){return true;}});list.addFirst(sku);if(list.size()>12)list=list.subList(0,12);session.setAttribute(RECENT,List.copyOf(list));}}
    public void xoaDaXem(HttpSession session){synchronized(session){session.removeAttribute(RECENT);}}
}
