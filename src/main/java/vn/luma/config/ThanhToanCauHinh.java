package vn.luma.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record ThanhToanCauHinh(@Value("${app.payos.mode:DISABLED}") String mode,
    @Value("${app.payos.client-id:}") String clientId,@Value("${app.payos.api-key:}") String apiKey,
    @Value("${app.payos.checksum-key:}") String checksumKey,@Value("${app.payos.simulation-enabled:true}") boolean simulationEnabled,
    @Value("${app.public-url:http://localhost:8082}") String publicUrl) {
    public boolean live(){return "LIVE".equals(mode)&&!clientId.isBlank()&&!apiKey.isBlank()&&!checksumKey.isBlank();}
    public String kenh(String method){
        if("PAYOS_MO_PHONG".equals(method)&&simulationEnabled)return "PAYOS_LUMA_MOCK";
        if("PAYOS".equals(method)&&live())return "PAYOS_LUMA";
        throw new IllegalArgumentException("Phương thức thanh toán chưa sẵn sàng. Hãy chọn COD hoặc chế độ mô phỏng.");
    }
    @Override public String toString(){return "ThanhToanCauHinh[credentials redacted]";}
}
