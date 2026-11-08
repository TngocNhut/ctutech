package vn.luma.business;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Twilio Verify sends and checks the six-digit code; credentials are supplied outside source. */
@Component
public class TwilioVerifyClient {
    private final String service, user, secret, base;
    private final HttpClient client;
    public TwilioVerifyClient(@Value("${app.verify.service-sid:}") String service,
            @Value("${app.verify.api-key:}") String user,@Value("${app.verify.api-secret:}") String secret,
            @Value("${app.verify.base-url:https://verify.twilio.com/v2}") String base) {
        this.service=service;this.user=user;this.secret=secret;this.base=base;
        this.client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    }
    public boolean available(){return service.matches("VA[0-9a-fA-F]{32}")&&!user.isBlank()&&!secret.isBlank();}
    private String enc(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private tools.jackson.databind.JsonNode post(String route,String body){
        if(!available())throw new IllegalStateException("Dịch vụ xác minh chưa được cấu hình.");
        try {
            var request=HttpRequest.newBuilder(URI.create(base+"/Services/"+service+"/"+route)).timeout(Duration.ofSeconds(12))
                .header("Authorization","Basic "+Base64.getEncoder().encodeToString((user+":"+secret).getBytes(StandardCharsets.UTF_8)))
                .header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if(response.statusCode()/100!=2)throw new IllegalArgumentException("Không thể xác minh lúc này. Hãy kiểm tra thông tin và thử lại sau.");
            return JsonMapper.builder().build().readTree(response.body());
        } catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException("Xác minh bị gián đoạn.");}
        catch(java.io.IOException ex){throw new IllegalStateException("Không kết nối được dịch vụ xác minh.");}
        catch(tools.jackson.core.JacksonException ex){throw new IllegalStateException("Phản hồi từ dịch vụ xác minh không hợp lệ. Hãy thử lại sau.");}
    }
    public void send(String destination,String channel){
        if(!java.util.Set.of("email","sms").contains(channel))throw new IllegalArgumentException("Kênh xác minh không hợp lệ.");
        var result=post("Verifications","To="+enc(destination)+"&Channel="+enc(channel)+(channel.equals("sms")?"&Locale=vi":""));
        if(!"pending".equals(result.path("status").asString()))throw new IllegalStateException("Dịch vụ chưa xác nhận gửi mã. Hãy thử lại sau.");
    }
    public boolean check(String destination,String code){
        if(code==null||!code.matches("[0-9]{6}"))return false;
        return "approved".equals(post("VerificationCheck","To="+enc(destination)+"&Code="+enc(code)).path("status").asString());
    }
}
