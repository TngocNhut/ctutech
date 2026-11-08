package vn.luma;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.*;
import vn.luma.business.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
@Import(OAuthProtocolContractTest.Clients.class)
class OAuthProtocolContractTest {
    static final String PASSWORD="OAuthContract!2026";
    static final HttpServer provider;
    static final KeyPair keys;
    static final String issuer;
    static final Map<String,String> nonces=new ConcurrentHashMap<>();
    static final AtomicInteger tokenCalls=new AtomicInteger();
    static {
        try{
            var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);keys=generator.generateKeyPair();provider=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);issuer="http://127.0.0.1:"+provider.getAddress().getPort();
            provider.createContext("/token",exchange->{try{tokenCalls.incrementAndGet();String body=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);String code=query(body).get("code");String response="{\"access_token\":\"local-contract-token\",\"token_type\":\"Bearer\",\"expires_in\":300}";if(code!=null&&code.startsWith("g-"))response="{\"access_token\":\"local-contract-token\",\"token_type\":\"Bearer\",\"expires_in\":300,\"id_token\":\""+jwt(nonces.get(code),code.contains("bad"))+"\"}";reply(exchange,response);}catch(Exception e){exchange.sendResponseHeaders(500,-1);exchange.close();}});
            provider.createContext("/facebook-me",e->reply(e,"{\"id\":\"contract-facebook-sub\",\"name\":\"Provider user\",\"email\":\"same-email@example.test\",\"role\":\"QuanTri\"}"));
            provider.createContext("/google-me",e->reply(e,"{\"sub\":\"contract-google-sub\",\"email\":\"same-email@example.test\",\"email_verified\":true,\"name\":\"Google contract\"}"));
            provider.createContext("/jwks",e->{var key=(RSAPublicKey)keys.getPublic();reply(e,"{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"local-key\",\"use\":\"sig\",\"alg\":\"RS256\",\"n\":\""+b64(unsigned(key.getModulus().toByteArray()))+"\",\"e\":\""+b64(unsigned(key.getPublicExponent().toByteArray()))+"\"}]}");});provider.start();
        }catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    static byte[] unsigned(byte[] b){return b.length>1&&b[0]==0?Arrays.copyOfRange(b,1,b.length):b;}
    static String b64(byte[] b){return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    static void reply(com.sun.net.httpserver.HttpExchange e,String text)throws java.io.IOException{byte[] b=text.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(200,b.length);try(var out=e.getResponseBody()){out.write(b);}}
    static String jwt(String nonce,boolean bad)throws Exception{long now=java.time.Instant.now().getEpochSecond();String payload="{\"iss\":\""+issuer+"\",\"aud\":\"local-client\",\"sub\":\"contract-google-sub\",\"iat\":"+now+",\"exp\":"+(now+300)+",\"nonce\":\""+nonce+"\",\"email\":\"same-email@example.test\",\"email_verified\":true}";String content=b64("{\"alg\":\"RS256\",\"kid\":\"local-key\"}".getBytes(StandardCharsets.UTF_8))+"."+b64(payload.getBytes(StandardCharsets.UTF_8));var signature=Signature.getInstance("SHA256withRSA");signature.initSign(keys.getPrivate());signature.update(content.getBytes(StandardCharsets.US_ASCII));byte[] signed=signature.sign();if(bad)signed[0]^=1;return content+"."+b64(signed);}
    @TestConfiguration static class Clients {
        @Bean ClientRegistrationRepository contractClients(){return new InMemoryClientRegistrationRepository(
            ClientRegistration.withRegistrationId("facebook").clientId("local-client").clientSecret("local-secret").clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri("{baseUrl}/login/oauth2/code/{registrationId}").scope("public_profile","email").authorizationUri(issuer+"/authorize").tokenUri(issuer+"/token").userInfoUri(issuer+"/facebook-me").userNameAttributeName("id").clientName("Facebook contract").build(),
            ClientRegistration.withRegistrationId("google").clientId("local-client").clientSecret("local-secret").clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri("{baseUrl}/login/oauth2/code/{registrationId}").scope("openid","profile","email").authorizationUri(issuer+"/authorize").tokenUri(issuer+"/token").jwkSetUri(issuer+"/jwks").issuerUri(issuer).userInfoUri(issuer+"/google-me").userNameAttributeName("sub").clientName("Google contract").build());}
    }
    @Autowired TaiKhoanService users;
    @Autowired LienKetOAuthService links;
    @LocalServerPort int port;
    @AfterAll static void close(){provider.stop(0);}
    static Map<String,String> query(String query){var result=new HashMap<String,String>();if(query!=null)for(String part:query.split("&")){var pair=part.split("=",2);result.put(URLDecoder.decode(pair[0],StandardCharsets.UTF_8),pair.length==2?URLDecoder.decode(pair[1],StandardCharsets.UTF_8):"");}return result;}
    HttpClient browser(){return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();}
    HttpResponse<String> get(HttpClient c,String path)throws Exception{return c.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    HttpResponse<String> post(HttpClient c,String path,Map<String,String> fields)throws Exception{String data=fields.entrySet().stream().map(e->URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));return c.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(data)).build(),HttpResponse.BodyHandlers.ofString());}
    String csrf(String html){var m=Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);assertThat(m.find()).isTrue();return m.group(1);}
    HttpResponse<String> oauth(HttpClient c,String type,boolean badSignature)throws Exception{var start=get(c,"/oauth2/authorization/"+type);assertThat(start.statusCode()).isEqualTo(302);var args=query(URI.create(start.headers().firstValue("Location").orElseThrow()).getRawQuery());String code=type.equals("google")?"g-"+(badSignature?"bad-":"")+UUID.randomUUID():"fb-code";if(type.equals("google"))nonces.put(code,args.get("nonce"));return get(c,"/login/oauth2/code/"+type+"?code="+code+"&state="+URLEncoder.encode(args.get("state"),StandardCharsets.UTF_8));}
    @Test void facebookFirstLinkNeedsPasswordThenLoginUsesLocalRoleAndSession()throws Exception{
        String email="fb-contract-"+UUID.randomUUID()+"@example.test";users.seedDemo(email,"Khách Facebook hợp đồng","KhachHang",PASSWORD);var c=browser();try{
            assertThat(oauth(c,"facebook",false).headers().firstValue("Location").orElseThrow()).contains("oauthPending");assertThat(get(c,"/tai-khoan").statusCode()).isEqualTo(302);assertThat(links.cuaToi(email)).isEmpty();
            var logged=post(c,"/login",Map.of("_csrf",csrf(get(c,"/login").body()),"username",email,"password",PASSWORD));assertThat(logged.headers().firstValue("Location").orElseThrow()).endsWith("/tai-khoan/lien-ket");String form=get(c,"/tai-khoan/lien-ket").body();assertThat(form).contains("same-email@example.test");assertThat(post(c,"/tai-khoan/lien-ket/xac-nhan",Map.of("matKhau",PASSWORD)).statusCode()).isEqualTo(403);
            post(c,"/tai-khoan/lien-ket/xac-nhan",Map.of("_csrf",csrf(form),"matKhau","wrong"));assertThat(links.cuaToi(email)).isEmpty();post(c,"/tai-khoan/lien-ket/xac-nhan",Map.of("_csrf",csrf(get(c,"/tai-khoan/lien-ket").body()),"matKhau",PASSWORD,"chuThe","attacker-sub"));assertThat(links.cuaToi(email)).hasSize(1);
            post(c,"/logout",Map.of("_csrf",csrf(get(c,"/tai-khoan").body())));assertThat(oauth(c,"facebook",false).headers().firstValue("Location").orElseThrow()).endsWith("/tai-khoan");assertThat(get(c,"/tai-khoan").body()).contains(email);assertThat(get(c,"/quan-tri").statusCode()).isEqualTo(403);
        }finally{links.bo(email,PASSWORD,"FACEBOOK");}
    }
    @Test void googleAuthorizationIsDisabledEvenWhenConfigured()throws Exception{var c=browser();assertThat(get(c,"/oauth2/authorization/google").statusCode()).isEqualTo(404);assertThat(get(c,"/login").body()).doesNotContain("Đăng nhập với Google");}
    @Test void forgedStateIsRejectedBeforeTokenExchange()throws Exception{var c=browser();get(c,"/oauth2/authorization/facebook");int calls=tokenCalls.get();var response=get(c,"/login/oauth2/code/facebook?code=fake&state=forged");assertThat(response.headers().firstValue("Location").orElseThrow()).contains("oauthError");assertThat(tokenCalls.get()).isEqualTo(calls);assertThat(get(c,"/tai-khoan").statusCode()).isEqualTo(302);}
}
