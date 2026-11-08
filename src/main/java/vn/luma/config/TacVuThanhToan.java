package vn.luma.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import vn.luma.business.*;
import vn.luma.data.ThanhToanDAO;

@Configuration
@ConditionalOnProperty(name="app.scheduling.enabled",havingValue="true",matchIfMissing=true)
public class TacVuThanhToan {
    private final ThanhToanDAO dao;private final ThanhToanGhiSoService ledger;private final ThanhToanService service;private final DiaChiService addresses;
    public TacVuThanhToan(ThanhToanDAO dao,ThanhToanGhiSoService ledger,ThanhToanService service,DiaChiService addresses){this.dao=dao;this.ledger=ledger;this.service=service;this.addresses=addresses;}
    @Scheduled(initialDelay=30000,fixedDelay=60000) public void doiSoat(){
        for(long id:dao.choXuLy())try{ledger.xuLy(id);}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Sự kiện thanh toán còn chờ xử lý.");}
        for(long id:dao.canDoiSoat())try{service.doiSoatId(id,false);}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Một phiên thanh toán cần thử đối soát lại.");}
        addresses.donCache();
    }
}
