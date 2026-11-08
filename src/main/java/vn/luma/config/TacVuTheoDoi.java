package vn.luma.config;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import vn.luma.business.TheoDoiService;

@Component
@ConditionalOnProperty(name="app.scheduling.enabled",havingValue="true",matchIfMissing=true)
public class TacVuTheoDoi {
    private final TheoDoiService service;private final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(getClass());
    public TacVuTheoDoi(TheoDoiService service){this.service=service;}
    @Scheduled(fixedDelay=300000,initialDelay=45000) public void kiemTra(){long after=0,until=service.mocCuoi();while(true){var ids=service.tiepTheo(after,until);if(ids.isEmpty())return;for(long id:ids){try{service.kiemTraMot(id);}catch(RuntimeException ex){log.warn("Chưa kiểm tra theo dõi {}: {}",id,ex.getClass().getSimpleName());}after=id;}}}
}
