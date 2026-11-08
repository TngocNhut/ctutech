package vn.luma.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// Only SDK-verified webhook/API data or the explicit mock channel may construct ledger input.
public record BaoThu(long orderCode,String linkId,String reference,BigDecimal amount,String currency,OffsetDateTime time) {
    public BaoThu {
        if(orderCode<=0||linkId==null||!linkId.matches("[A-Za-z0-9_-]{1,120}")||reference==null||reference.isBlank()||reference.length()>150||amount==null||amount.signum()<=0||amount.stripTrailingZeros().scale()>0||amount.compareTo(new BigDecimal("999999999999999"))>0||!"VND".equals(currency)||time==null)
            throw new IllegalArgumentException("Thông tin khoản thu không hợp lệ.");
        amount=amount.setScale(0);
    }
}
