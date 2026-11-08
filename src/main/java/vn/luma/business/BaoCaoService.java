package vn.luma.business;
import java.time.*;
import java.util.*;
import java.math.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import vn.luma.data.BaoCaoDAO;

@Service
public class BaoCaoService {
    private final BaoCaoDAO dao;private final TaiKhoanService users;
    public BaoCaoService(BaoCaoDAO dao,TaiKhoanService users){this.dao=dao;this.users=users;}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> xem(String email,LocalDate from,LocalDate to,String group){return xem(email,from,to,group,1);}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> xem(String email,LocalDate from,LocalDate to,String group,int page){
        users.quanTri(email);var today=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));if(from==null||to==null||from.getYear()<1900||to.isAfter(today)||to.isBefore(from)||java.time.temporal.ChronoUnit.DAYS.between(from,to)>3660||group==null||!Set.of("day","month","year").contains(group)||page<1||page>10000)throw new IllegalArgumentException("Kỳ báo cáo không hợp lệ.");
        var result=new HashMap<String,Object>();var sales=dao.doanhSo(from,to,group);result.put("doanhSo",sales);result.put("tien",dao.tien(from,to));result.put("banChay",dao.banChay(from,to));var customers=dao.quayLai(from,to);result.put("quayLai",customers);var ratings=dao.danhGia(from,to);result.put("danhGia",ratings);result.put("hangCho",dao.hangCho());result.put("kho",dao.kho());result.put("danhMuc",dao.danhMuc(from,to));result.put("traHang",dao.traHang(from,to));result.put("hanhVi",dao.hanhVi(from,to));result.put("nhomTrang",dao.nhomTrang(from,to));
        result.put("tyLeQuayLai",ratio(((Number)customers.get("quay_lai")).longValue()*100,((Number)customers.get("tong_khach")).longValue()));long votes=0,stars=0,positive=0;for(var r:ratings){long count=((Number)r.get("so_luong")).longValue();if(((Number)r.get("SoSao")).intValue()>=4)positive+=count;votes+=count;stars+=count*((Number)r.get("SoSao")).longValue();}result.put("saoTrungBinh",ratio(stars,votes));result.put("soDanhGia",votes);result.put("tyLeHaiLong",ratio(positive*100,votes));
        BigDecimal revenue=BigDecimal.ZERO,cost=BigDecimal.ZERO;for(var r:sales){revenue=revenue.add((BigDecimal)r.get("doanh_thu"));cost=cost.add((BigDecimal)r.get("gia_von"));}result.put("tongDoanhSo",revenue);result.put("laiGop",revenue.subtract(cost));
        var month=to.equals(YearMonth.from(to).atEndOfMonth())?to.withDayOfMonth(1):to.withDayOfMonth(1).minusMonths(1);result.put("thangTuongTac",month);result.put("trangTuongTac",page);result.put("hetHanHanhVi",month.isBefore(today.minusDays(90)));result.put("tuongTac",month.isBefore(today.minusDays(90))?List.of():dao.tuongTac(month,page));
        var chart=new ArrayList<Map<String,Object>>();var last=sales.subList(Math.max(0,sales.size()-60),sales.size());BigDecimal maximum=last.stream().map(r->(BigDecimal)r.get("doanh_thu")).max(BigDecimal::compareTo).orElse(BigDecimal.ONE).max(BigDecimal.ONE);double step=last.isEmpty()?1:940.0/last.size();for(int i=0;i<last.size();i++){var row=last.get(i);double height=((BigDecimal)row.get("doanh_thu")).divide(maximum,8,RoundingMode.HALF_UP).doubleValue()*200;chart.add(Map.of("ky",row.get("ky").toString(),"tien",row.get("doanh_thu"),"x",30+i*step,"y",230-height,"w",Math.max(.1,step*.8),"h",height));}result.put("bieuDo",chart);result.put("nhanNhom",Map.of("TRANG_CHU","Trang chủ","DANH_SACH","Danh sách sản phẩm","CHI_TIET","Chi tiết sản phẩm","UU_DAI","Ưu đãi","SU_KIEN","Sự kiện","GIO_HANG","Giỏ hàng","DAT_HANG","Đặt hàng","DON_HANG","Đơn hàng","TAI_KHOAN","Tài khoản"));result.put("nhanSuKien",Map.of("XEM_TRANG","Xem trang","TIM_KIEM","Tìm kiếm","LOC_DANH_MUC","Lọc sản phẩm","BAM_BANNER","Mở banner"));return result;
    }
    private BigDecimal ratio(long numerator,long denominator){return denominator==0?null:BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator),2,RoundingMode.HALF_UP);}
}
