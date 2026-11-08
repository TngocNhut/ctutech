package vn.luma.business;

import java.math.BigDecimal;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import vn.luma.config.ThanhToanCauHinh;
import vn.luma.model.BaoThu;
import vn.payos.PayOS;
import vn.payos.core.ClientOptions;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;

@Component
public class PayOSClient {
    private final PayOS sdk;
    private final ThanhToanCauHinh config;
    public record LienKet(long orderCode,String id,BigDecimal amount,String currency,String url,String status) { }
    public record DoiSoat(long orderCode,String id,BigDecimal requested,BigDecimal amountPaid,String status,List<BaoThu> receipts) { }
    @org.springframework.beans.factory.annotation.Autowired
    public PayOSClient(ThanhToanCauHinh config){
        this.config=config;
        this.sdk=config.live()?new PayOS(ClientOptions.builder().clientId(config.clientId()).apiKey(config.apiKey()).checksumKey(config.checksumKey()).timeoutMs(10000).maxRetries(0).logLevel(ClientOptions.LogLevel.NONE).build()):null;
    }
    PayOSClient(ThanhToanCauHinh config,PayOS sdk){this.config=config;this.sdk=sdk;}
    private void sanSang(){if(sdk==null)throw new IllegalStateException("Chưa bật tích hợp PayOS thật.");}
    public LienKet tao(long orderCode,BigDecimal amount,Instant expiry,String code){
        sanSang();
        var request=CreatePaymentLinkRequest.builder().orderCode(orderCode).amount(amount.longValueExact()).description("CTU"+Math.floorMod(orderCode,100000))
            .returnUrl(config.publicUrl()+"/thanh-toan/ket-qua/"+code).cancelUrl(config.publicUrl()+"/thanh-toan/ket-qua/"+code+"?cancel=true").expiredAt(expiry.getEpochSecond()).build();
        var response=sdk.paymentRequests().create(request);
        if(response.getOrderCode()!=orderCode||response.getAmount()!=amount.longValueExact()||!"VND".equals(response.getCurrency()))throw new IllegalStateException("Dữ liệu tạo link không khớp.");
        var uri=URI.create(response.getCheckoutUrl());
        if(!"https".equals(uri.getScheme())||!"pay.payos.vn".equals(uri.getHost())||uri.getUserInfo()!=null)throw new IllegalStateException("Đường dẫn thanh toán không hợp lệ.");
        return new LienKet(orderCode,response.getPaymentLinkId(),amount,"VND",response.getCheckoutUrl(),response.getStatus().toString());
    }
    public BaoThu xacMinh(String body){
        sanSang();
        try{
            var envelope=new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
            var data=sdk.webhooks().verify(body);
            if(!envelope.path("success").asBoolean()||!"00".equals(envelope.path("code").asText())||!"00".equals(data.getCode()))throw new IllegalArgumentException("Sự kiện chưa xác nhận chuyển khoản thành công.");
            return new BaoThu(data.getOrderCode(),data.getPaymentLinkId(),data.getReference(),BigDecimal.valueOf(data.getAmount()),data.getCurrency(),thoiDiem(data.getTransactionDateTime()));
        }catch(Exception ex){throw new IllegalArgumentException("Webhook PayOS không hợp lệ.");}
    }
    public DoiSoat tra(long code){sanSang();return chuyen(sdk.paymentRequests().get(code));}
    public DoiSoat huy(long code){sanSang();return chuyen(sdk.paymentRequests().cancel(code,"Khách hủy hoặc đơn hết hạn"));}
    private DoiSoat chuyen(vn.payos.model.v2.paymentRequests.PaymentLink link){
        List<BaoThu> receipts=link.getTransactions().stream().map(t->new BaoThu(link.getOrderCode(),link.getId(),t.getReference(),BigDecimal.valueOf(t.getAmount()),"VND",thoiDiem(t.getTransactionDateTime()))).toList();
        return new DoiSoat(link.getOrderCode(),link.getId(),BigDecimal.valueOf(link.getAmount()),BigDecimal.valueOf(link.getAmountPaid()),link.getStatus().toString(),receipts);
    }
    public static OffsetDateTime thoiDiem(String value){
        try{return OffsetDateTime.parse(value);}catch(java.time.format.DateTimeParseException ex){return LocalDateTime.parse(value.replace(' ','T')).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();}
    }
}
