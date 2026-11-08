package vn.luma.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.luma.business.TaiKhoanService.ThuThuNghiem;

@Component
public class HopThuThuNghiem {
    private final Path folder;
    public HopThuThuNghiem(@Value("${app.mail.local-directory:.local/mail}") String directory) {folder=Path.of(directory);}
    @TransactionalEventListener
    public void ghi(ThuThuNghiem mail) {
        String route=mail.purpose().equals("XAC_MINH_THU")?"/xac-minh-email":"/dat-lai-mat-khau";
        String text="Hộp thư thử nghiệm local (không gửi email thật)\nĐến: "+mail.email()+"\nMục đích: "+mail.purpose()+"\nThời điểm: "+Instant.now()+"\nLiên kết (hạn30phút): http://localhost:8082"+route+"?token="+mail.token()+"\n";
        try {Files.createDirectories(folder);Files.writeString(folder.resolve(UUID.randomUUID()+".txt"),text,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);}
        catch(IOException ex){throw new IllegalStateException("Không ghi được hộp thư thử nghiệm",ex);}
    }
}
