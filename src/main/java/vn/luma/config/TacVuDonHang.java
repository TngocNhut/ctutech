package vn.luma.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.*;
import vn.luma.business.DonHangService;
import vn.luma.data.DonHangDAO;

@Configuration @EnableScheduling
@ConditionalOnProperty(name="app.scheduling.enabled",havingValue="true",matchIfMissing=true)
public class TacVuDonHang {
    private final DonHangDAO dao;private final DonHangService service;
    public TacVuDonHang(DonHangDAO dao,DonHangService service){this.dao=dao;this.service=service;}
    @Scheduled(initialDelay=60000,fixedDelay=60000) public void hetHan(){
        for(String code:dao.donQuaHan())try{service.hetHanCOD(code);}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Chưa xử lý được một đơn quá hạn; sẽ thử lại.");}
    }
}
