package vn.luma.business;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class TwilioVerifyClientTest {
 @Test void handlesBothChannelsAuthenticationWrongCodeAndProviderFailures()throws Exception{
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var body=new AtomicReference<String>();var auth=new AtomicReference<String>();var mode=new AtomicReference<>("normal");
  server.createContext("/v2/Services/VA00000000000000000000000000000000/",exchange->{body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));auth.set(exchange.getRequestHeaders().getFirst("Authorization"));String response=mode.get().equals("invalid")?"not json":mode.get().equals("unexpected")?"{\"status\":\"failed\"}":exchange.getRequestURI().getPath().endsWith("VerificationCheck")?(body.get().contains("Code=123456")?"{\"status\":\"approved\"}":"{\"status\":\"pending\"}"):"{\"status\":\"pending\"}";var bytes=response.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(mode.get().equals("error")?429:200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});server.start();
  try{
   var client=new TwilioVerifyClient("VA00000000000000000000000000000000","test-key","test-secret","http://127.0.0.1:"+server.getAddress().getPort()+"/v2");
   client.send("+84901234567","sms");assertThat(body.get()).contains("To=%2B84901234567","Channel=sms","Locale=vi");assertThat(auth.get()).isEqualTo("Basic "+Base64.getEncoder().encodeToString("test-key:test-secret".getBytes(StandardCharsets.UTF_8)));
   client.send("user+demo@example.test","email");assertThat(body.get()).contains("To=user%2Bdemo%40example.test","Channel=email").doesNotContain("Locale=");
   assertThat(client.check("+84901234567","123456")).isTrue();assertThat(client.check("+84901234567","111111")).isFalse();assertThat(client.check("+84901234567","12345")).isFalse();
   mode.set("error");assertThatThrownBy(()->client.send("a@example.test","email")).isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("test-secret");
   mode.set("invalid");assertThatThrownBy(()->client.send("a@example.test","email")).isInstanceOf(IllegalStateException.class).hasMessageContaining("dịch vụ xác minh không hợp lệ");
   mode.set("unexpected");assertThatThrownBy(()->client.send("a@example.test","email")).isInstanceOf(IllegalStateException.class).hasMessageContaining("chưa xác nhận");
  }finally{server.stop(0);}
 }
}
