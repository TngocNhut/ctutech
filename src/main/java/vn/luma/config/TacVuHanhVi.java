package vn.luma.config;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import vn.luma.business.HanhViService;
@Component
@ConditionalOnProperty(name="app.scheduling.enabled",havingValue="true",matchIfMissing=true)
public class TacVuHanhVi {
    private final HanhViService service;
    public TacVuHanhVi(HanhViService service){this.service=service;}
    @Scheduled(fixedDelay=3600000,initialDelay=60000) public void donDep(){service.donDep();}
}
