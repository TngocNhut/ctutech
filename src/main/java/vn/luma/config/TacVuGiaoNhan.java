package vn.luma.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.luma.business.GiaoNhanService;

@Component
@ConditionalOnProperty(name="app.scheduling.enabled",havingValue="true",matchIfMissing=true)
public class TacVuGiaoNhan {
    private final GiaoNhanService service;
    private final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(getClass());
    public TacVuGiaoNhan(GiaoNhanService service){this.service=service;}
    @Scheduled(fixedDelay=300000,initialDelay=30000) public void quaHan(){for(String code:service.quaHan())try{service.taoViecQuaHan(code);}catch(RuntimeException ex){log.warn("Chưa tạo được việc giao quá hạn cho {}: {}",code,ex.getClass().getSimpleName());}}
}
