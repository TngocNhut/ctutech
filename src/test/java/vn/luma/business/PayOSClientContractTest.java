package vn.luma.business;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import vn.luma.config.ThanhToanCauHinh;
import static org.assertj.core.api.Assertions.*;

class PayOSClientContractTest {
    static final String KEY="test-checksum";
    static Map<String,Object> data(){var d=new TreeMap<String,Object>();d.put("orderCode",123L);d.put("amount",3000L);d.put("description","LUMA123");d.put("accountNumber","000000");d.put("reference","BANK-123");d.put("transactionDateTime","2026-11-08 08:30:00");d.put("currency","VND");d.put("paymentLinkId","link123");d.put("code","00");d.put("desc","success");for(String key:List.of("counterAccountBankId","counterAccountBankName","counterAccountName","counterAccountNumber","virtualAccountName","virtualAccountNumber"))d.put(key,"");return d;}
    static String hmac(Map<String,Object> data)throws Exception{String canonical=new TreeMap<>(data).entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(java.util.stream.Collectors.joining("&"));var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));}
    static String body(Map<String,Object> data,String signature)throws Exception{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("code","00","desc","success","success",true,"data",data,"signature",signature));}
    PayOSClient adapter(){return new PayOSClient(new ThanhToanCauHinh("LIVE","test-id","test-api",KEY,true,"http://localhost:8082"));}
    @Test void officialSdkVerifiesIndependentHmacAndConvertsVietnamTime()throws Exception{
        var data=data();var receipt=adapter().xacMinh(body(data,hmac(data)));assertThat(receipt.amount()).isEqualByComparingTo(new BigDecimal("3000"));assertThat(receipt.time().toInstant()).isEqualTo(Instant.parse("2026-11-08T01:30:00Z"));
    }
    @Test void changedAmountSignatureOrCurrencyIsRejected()throws Exception{
        var data=data();String signature=hmac(data);data.put("amount",4000L);assertThatThrownBy(()->adapter().xacMinh(body(data,signature))).isInstanceOf(IllegalArgumentException.class);
        data.put("currency","USD");String usdSignature=hmac(data);assertThatThrownBy(()->adapter().xacMinh(body(data,usdSignature))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void signedNonPaymentIsAcknowledgedWithoutReceipt()throws Exception{
        var data=data();data.put("code","01");
        assertThat(adapter().xacMinh(body(data,hmac(data)))).isNull();
        assertThatThrownBy(()->adapter().xacMinh(body(data,"invalid"))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void sdkCreatesQueriesAndCancelsAgainstLocalContractServer()throws Exception{
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v2/payment-requests",exchange->{
            try{
                Map<String,Object> response=new TreeMap<>();String requestText=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
                if("POST".equals(exchange.getRequestMethod())&&!exchange.getRequestURI().getPath().endsWith("/cancel")){
                    var request=mapper.readTree(requestText);assertThat(request.path("orderCode").asLong()).isEqualTo(123);assertThat(request.path("amount").asLong()).isEqualTo(3000);
                    var signedRequest=new TreeMap<String,Object>();for(String key:List.of("amount","cancelUrl","description","orderCode","returnUrl"))signedRequest.put(key,request.get(key).asText());assertThat(request.path("signature").asText()).isEqualTo(hmac(signedRequest));
                    response.put("bin","970422");response.put("accountNumber","000000");response.put("accountName","Test");response.put("amount",3000L);response.put("description","LUMA123");response.put("orderCode",123L);response.put("currency","VND");response.put("paymentLinkId","contract-link");response.put("status","PENDING");response.put("expiredAt",1794100000L);response.put("checkoutUrl","https://pay.payos.vn/web/contract-link");response.put("qrCode","TEST_QR");
                }else{
                    response.put("id","contract-link");response.put("orderCode",123L);response.put("amount",3000L);response.put("amountPaid",0L);response.put("amountRemaining",3000L);response.put("status",exchange.getRequestURI().getPath().endsWith("/cancel")?"CANCELLED":"PENDING");response.put("createdAt","2026-11-08T08:00:00+07:00");response.put("transactions",List.of());response.put("cancellationReason","");response.put("canceledAt","");
                }
                String signature=new vn.payos.crypto.CryptoProviderImpl().createSignatureFromObj(response,KEY);byte[] bytes=mapper.writeValueAsBytes(Map.of("code","00","desc","success","data",response,"signature",signature));exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);
            }catch(Throwable ex){failure.set(ex);exchange.sendResponseHeaders(500,0);}finally{exchange.close();}
        });server.start();
        var http=new okhttp3.OkHttpClient.Builder().build();
        try{
            var sdk=new vn.payos.PayOS(vn.payos.core.ClientOptions.builder().clientId("test-id").apiKey("test-api").checksumKey(KEY).baseURL("http://127.0.0.1:"+server.getAddress().getPort()).maxRetries(0).timeoutMs(3000).httpClient(http).build());
            var adapter=new PayOSClient(new ThanhToanCauHinh("LIVE","test-id","test-api",KEY,true,"http://localhost:8082"),sdk);
            assertThat(adapter.tao(123,new BigDecimal("3000"),Instant.parse("2026-11-08T09:00:00Z"),"LM-TEST").id()).isEqualTo("contract-link");
            assertThat(adapter.tra(123).status()).isEqualTo("PENDING");assertThat(adapter.huy(123).status()).isEqualTo("CANCELLED");assertThat(failure.get()).isNull();
        }finally{server.stop(0);http.dispatcher().executorService().shutdown();http.connectionPool().evictAll();}
    }
    @Test void sdkExposesHttp404AsTypedNotFound()throws Exception{
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v2/payment-requests",exchange->{byte[] body="{\"code\":\"NOT_FOUND\",\"desc\":\"Payment link not found\"}".getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(404,body.length);exchange.getResponseBody().write(body);exchange.close();});server.start();var http=new okhttp3.OkHttpClient.Builder().build();
        try{
            var sdk=new vn.payos.PayOS(vn.payos.core.ClientOptions.builder().clientId("test-id").apiKey("test-api").checksumKey(KEY).baseURL("http://127.0.0.1:"+server.getAddress().getPort()).maxRetries(0).timeoutMs(3000).httpClient(http).build());
            var adapter=new PayOSClient(new ThanhToanCauHinh("LIVE","test-id","test-api",KEY,true,"http://localhost:8082"),sdk);
            assertThatThrownBy(()->adapter.tra(123)).isInstanceOfSatisfying(vn.payos.exception.NotFoundException.class,ex->assertThat(ex.getStatusCode()).contains(404));
        }finally{server.stop(0);http.dispatcher().executorService().shutdown();http.connectionPool().evictAll();}
    }
}
