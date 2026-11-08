package vn.luma.business;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import vn.luma.web.DiaChiYeuCau;
import static org.assertj.core.api.Assertions.*;

class BanDoContractTest {
    DiaChiYeuCau pin(boolean confirmed){return new DiaChiYeuCau("Nguyễn Văn An","0901234567","Thành phố Hồ Chí Minh","Phường Bến Nghé","123 Nguyễn Huệ","Tầng 2","untrusted-client-place",new BigDecimal("10.772123456"),new BigDecimal("106.704987654"),confirmed);}
    @Test void verifiesVietnamFromTypedCountryAndUsesServerPlaceId()throws Exception{
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);AtomicReference<String> query=new AtomicReference<>();
        server.createContext("/maps/api/geocode/json",exchange->{query.set(exchange.getRequestURI().getRawQuery());byte[] body="""
            {"status":"OK","results":[{"place_id":"verified-place","address_components":[
              {"short_name":"Q1","types":["administrative_area_level_2"]},
              {"short_name":"VN","types":["country","political"]}]}]}
            """.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();});server.start();
        try{
            var service=new BanDoService("test-map-key",RestClient.builder().baseUrl("http://127.0.0.1:"+server.getAddress().getPort()).build());var before=OffsetDateTime.now();var result=service.chuanHoa(pin(true));
            assertThat(result.placeId()).isEqualTo("verified-place");assertThat(result.viDo()).isEqualByComparingTo("10.7721235");assertThat(result.kinhDo()).isEqualByComparingTo("106.7049877");
            assertThat(result.ngayLay()).isAfterOrEqualTo(before);assertThat(result.hanLuu()).isEqualTo(result.ngayLay().plusDays(30));
            assertThat(query.get()).contains("latlng=10.7721235,106.7049877","language=vi");
            assertThatThrownBy(()->service.chuanHoa(pin(false))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("xác nhận");
        }finally{server.stop(0);}
    }
    @Test void rejectsOtherCountriesMalformedPayloadAndApiFailure()throws Exception{
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);AtomicReference<String> response=new AtomicReference<>();AtomicInteger status=new AtomicInteger(200);
        server.createContext("/maps/api/geocode/json",exchange->{byte[] bytes=response.get().getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(status.get(),bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});server.start();
        try{
            var service=new BanDoService("test-map-key",RestClient.builder().baseUrl("http://127.0.0.1:"+server.getAddress().getPort()).build());
            response.set("{\"status\":\"OK\",\"results\":[{\"place_id\":\"outside\",\"address_components\":[{\"short_name\":\"US\",\"types\":[\"country\"]}]}]}");
            assertThatThrownBy(()->service.chuanHoa(pin(true))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Việt Nam");
            for(String body:new String[]{"{\"status\":\"ZERO_RESULTS\",\"results\":[]}","{\"status\":\"REQUEST_DENIED\"}","{\"status\":\"OK\",\"results\":{\"not\":\"array\"}}","not JSON"}){response.set(body);assertThatThrownBy(()->service.chuanHoa(pin(true))).isInstanceOf(IllegalArgumentException.class);}
            status.set(503);response.set("unavailable");assertThatThrownBy(()->service.chuanHoa(pin(true))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nhập tay");
        }finally{server.stop(0);}
    }
}
