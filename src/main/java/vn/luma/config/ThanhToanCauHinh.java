package vn.luma.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Mutable runtime settings; PayOS secrets are replaced only by the admin settings service. */
@Component
public final class ThanhToanCauHinh {
    public record GiaTri(String mode,String clientId,String apiKey,String checksumKey,boolean simulationEnabled,String publicUrl) { }
    private volatile GiaTri values;

    public ThanhToanCauHinh(@Value("${app.payos.mode:DISABLED}") String mode,
            @Value("${app.payos.client-id:}") String clientId,
            @Value("${app.payos.api-key:}") String apiKey,
            @Value("${app.payos.checksum-key:}") String checksumKey,
            @Value("${app.payos.simulation-enabled:true}") boolean simulationEnabled,
            @Value("${app.public-url:http://localhost:8082}") String publicUrl) {
        values=new GiaTri(mode,clientId,apiKey,checksumKey,simulationEnabled,publicUrl);
    }
    public GiaTri giaTri(){return values;}
    public String mode(){return values.mode();}
    public String clientId(){return values.clientId();}
    public String apiKey(){return values.apiKey();}
    public String checksumKey(){return values.checksumKey();}
    public boolean simulationEnabled(){return values.simulationEnabled();}
    public String publicUrl(){return values.publicUrl();}
    public boolean live(){var v=values;return "LIVE".equals(v.mode())&&!v.clientId().isBlank()&&!v.apiKey().isBlank()&&!v.checksumKey().isBlank();}
    public String kenh(String method){
        if("PAYOS_MO_PHONG".equals(method)&&simulationEnabled())return "PAYOS_LUMA_MOCK";
        if("PAYOS".equals(method)&&live())return "PAYOS_LUMA";
        throw new IllegalArgumentException("Phương thức thanh toán chưa sẵn sàng. Hãy chọn COD hoặc chế độ mô phỏng.");
    }
    public void capNhat(String mode,String clientId,String apiKey,String checksumKey,String publicUrl){
        var before=values;
        values=new GiaTri(mode,clientId,apiKey,checksumKey,before.simulationEnabled(),publicUrl);
    }
    @Override public String toString(){return "ThanhToanCauHinh[credentials redacted]";}
}
