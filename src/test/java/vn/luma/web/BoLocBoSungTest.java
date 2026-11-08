package vn.luma.web;

import java.util.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BoLocBoSungTest {
    @Test void parserRejectsAmbiguousMalformedAndExcessiveInputs(){
        assertThat(BoLocBoSung.doc(Map.of("tt.1",List.of(" "),"dong",List.of(""))).coDieuKien()).isFalse();
        for(var bad:List.of(Map.of("tt.1",List.of("a","b")),Map.of("tt.0",List.of("x")),Map.of("tt.9999999999999999999",List.of("x")),Map.of("dong",List.of("-1"))))assertThatThrownBy(()->BoLocBoSung.doc(bad)).isInstanceOf(IllegalArgumentException.class);
        var many=new HashMap<Long,String>();for(long i=1;i<=11;i++)many.put(i,"x");assertThatThrownBy(()->new BoLocBoSung(null,many)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void paginationRetainsExtendedFiltersAndLiteralPlusSigns(){
        var form=new BoLocSanPham("C++ & máy Việt","","",null,null,null,null,"A+B","",false,"ten",1);var extra=new BoLocBoSung(5L,Map.of(12L,"Wi-Fi 6+"));String url=CuaHangController.link(form,extra,2);
        var params=new HashMap<String,String>();for(String item:URI.create(url).getRawQuery().split("&")){var pair=item.split("=",2);params.put(URLDecoder.decode(pair[0],StandardCharsets.UTF_8),URLDecoder.decode(pair[1],StandardCharsets.UTF_8));}
        assertThat(params).containsEntry("q","C++ & máy Việt").containsEntry("cpu","A+B").containsEntry("dong","5").containsEntry("tt.12","Wi-Fi 6+").containsEntry("trang","2");
    }
}
