package vn.luma.config;

import java.time.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GioiHanDangNhapFilterTest {
    static class DongHo extends Clock {
        Instant luc=Instant.parse("2026-11-08T00:00:00Z");
        @Override public Instant instant(){return luc;}
        @Override public ZoneId getZone(){return ZoneOffset.UTC;}
        @Override public Clock withZone(ZoneId zone){return this;}
    }
    @Test void limitsAccountAcrossAddressesAndAddressAcrossAccountsThenExpires(){
        var clock=new DongHo();var filter=new GioiHanDangNhapFilter(null,clock);
        for(int i=0;i<10;i++)assertThat(filter.nhan("address-a","account").choPhep()).isTrue();
        var blocked=filter.nhan("address-b","account");assertThat(blocked.choPhep()).isFalse();assertThat(blocked.ghiLog()).isTrue();assertThat(blocked.thuLaiSau()).isBetween(900,901);
        assertThat(filter.nhan("address-c","account").ghiLog()).isFalse();
        for(int i=0;i<90;i++)assertThat(filter.nhan("address-a","account-"+i).choPhep()).isTrue();
        assertThat(filter.nhan("address-a","new").choPhep()).isFalse();
        clock.luc=clock.luc.plusSeconds(900);assertThat(filter.nhan("address-a","account").choPhep()).isTrue();
    }
    @Test void successfulLoginResetsOnlyAccountAndCapacityIsBounded(){
        var clock=new DongHo();var filter=new GioiHanDangNhapFilter(null,clock);
        for(int i=0;i<10;i++)filter.nhan("a","account");filter.thanhCong("account");assertThat(filter.nhan("a","account").choPhep()).isTrue();
        var capacity=new GioiHanDangNhapFilter(null,clock);
        for(int i=0;i<5000;i++)assertThat(capacity.nhan("address-"+i,"account-"+i).choPhep()).isTrue();
        assertThat(capacity.nhan("new-address","new-account").choPhep()).isFalse();clock.luc=clock.luc.plusSeconds(900);assertThat(capacity.nhan("new-address","new-account").choPhep()).isTrue();
    }
    @Test void parallelAttemptsCannotExceedAccountLimit()throws Exception{
        var filter=new GioiHanDangNhapFilter(null,new DongHo());var pool=Executors.newFixedThreadPool(10);var go=new CountDownLatch(1);
        try{var tasks=IntStream.range(0,30).mapToObj(i->pool.submit(()->{go.await();return filter.nhan("address-"+i,"account").choPhep();})).toList();go.countDown();int accepted=0;for(var t:tasks)if(t.get())accepted++;assertThat(accepted).isEqualTo(10);}finally{pool.shutdownNow();}
    }
}
