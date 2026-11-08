package vn.luma;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import vn.luma.business.BaoCaoService;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
class BaoCaoIntegrationTest {
    @Autowired BaoCaoService reports;
    @Autowired JdbcClient jdbc;
    @Test @SuppressWarnings("unchecked") void emptyPeriodHasZeroCashAndNoInventedSalesAndAllGroupingQueriesRun(){
        for(String group:List.of("day","month","year")){var result=reports.xem("admin@luma.local",LocalDate.of(1900,1,1),LocalDate.of(1900,1,31),group);assertThat((List<?>)result.get("doanhSo")).isEmpty();assertThat((List<?>)result.get("banChay")).isEmpty();assertThat((List<Map<String,Object>>)result.get("tien")).hasSize(2).allSatisfy(row->{assertThat((BigDecimal)row.get("da_thu")).isZero();assertThat((BigDecimal)row.get("da_hoan")).isZero();assertThat((BigDecimal)row.get("thu_rong")).isZero();});}
    }
    @Test void reportRequiresAdminAndRejectsInvalidPeriods(){var from=LocalDate.of(2026,1,1);assertThatThrownBy(()->reports.xem("khach@luma.local",from,from,"day")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);assertThatThrownBy(()->reports.xem("admin@luma.local",from,from.minusDays(1),"day")).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->reports.xem("admin@luma.local",from,from,"DROP TABLE")).isInstanceOf(IllegalArgumentException.class);}
}
