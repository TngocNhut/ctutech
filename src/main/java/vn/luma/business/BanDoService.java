package vn.luma.business;

import java.time.OffsetDateTime;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import tools.jackson.databind.JsonNode;
import vn.luma.model.DiaChiChot;
import vn.luma.web.DiaChiYeuCau;

@Service
public class BanDoService {
    private final String key;private final RestClient client;
    @org.springframework.beans.factory.annotation.Autowired
    public BanDoService(@Value("${app.maps.server-key:}") String key){this(key,taoClient());}
    BanDoService(String key,RestClient client){this.key=key;this.client=client;}
    private static RestClient taoClient(){
        var http=java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
        var factory=new JdkClientHttpRequestFactory(http);factory.setReadTimeout(java.time.Duration.ofSeconds(8));
        return RestClient.builder().baseUrl("https://maps.googleapis.com").requestFactory(factory).build();
    }
    // Google HTTP work happens before entering the order/address database transaction.
    public DiaChiChot chuanHoa(DiaChiYeuCau form){
        if((form.viDo()==null)!=(form.kinhDo()==null))throw new IllegalArgumentException("Cần chọn đủ vị trí giao nhận hoặc bỏ điểm bản đồ để nhập tay.");
        if(form.viDo()==null)return new DiaChiChot(form.tenNguoiNhan().strip(),form.soDienThoai().strip(),form.tinhThanh().strip(),form.phuongXa().strip(),form.diaChiCuThe().strip(),form.ghiChu(),null,null,null,null,null);
        if(!form.xacNhanPin())throw new IllegalArgumentException("Hãy xác nhận pin giao nhận trên bản đồ.");
        if(key.isBlank())throw new IllegalArgumentException("Chưa thể xác minh điểm bản đồ. Bạn có thể bỏ điểm bản đồ và dùng địa chỉ nhập tay.");
        var lat=form.viDo().setScale(7,RoundingMode.HALF_UP);var lng=form.kinhDo().setScale(7,RoundingMode.HALF_UP);
        JsonNode data;
        try{data=client.get().uri(b->b.path("/maps/api/geocode/json").queryParam("latlng",lat+","+lng).queryParam("language","vi").queryParam("key",key).build()).retrieve().body(JsonNode.class);}
        catch(Exception ex){throw new IllegalArgumentException("Không xác minh được bản đồ lúc này. Hãy thử lại hoặc dùng địa chỉ nhập tay.");}
        if(data==null||!"OK".equals(data.path("status").asText())||!data.path("results").isArray()||data.path("results").isEmpty())throw new IllegalArgumentException("Không tìm thấy địa chỉ cho điểm đã chọn. Hãy chọn lại hoặc nhập tay.");
        JsonNode place=data.path("results").get(0);boolean vietnam=false;
        for(JsonNode component:place.path("address_components"))for(JsonNode type:component.path("types"))
            if("country".equals(type.asText())&&"VN".equals(component.path("short_name").asText()))vietnam=true;
        if(!vietnam)throw new IllegalArgumentException("Website hiện chỉ nhận điểm giao hàng tại Việt Nam.");
        if(place.path("place_id").asText().isBlank()||place.path("place_id").asText().length()>255)throw new IllegalArgumentException("Điểm bản đồ chưa có thông tin đầy đủ. Hãy chọn lại hoặc nhập tay.");
        var now=OffsetDateTime.now();
        return new DiaChiChot(form.tenNguoiNhan().strip(),form.soDienThoai().strip(),form.tinhThanh().strip(),form.phuongXa().strip(),form.diaChiCuThe().strip(),form.ghiChu(),place.path("place_id").asText(),lat,lng,now,now.plusDays(30));
    }
}
