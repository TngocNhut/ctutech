package vn.luma.data;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.simple.JdbcClient;

@Repository
public class LienKetOAuthDAO {
    private final JdbcClient jdbc;
    public LienKetOAuthDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    // ponytail: global mutex fits the local demo; split by identity/user if link throughput requires it.
    public void khoa(){jdbc.sql("SELECT pg_advisory_xact_lock(74190312)").query().listOfRows();}
    public Optional<String> email(String provider,String subject){return jdbc.sql("SELECT coalesce(n.\"ThuDienTu\",n.\"SoDienThoai\") FROM \"TaiKhoanLienKet\" l JOIN \"NguoiDung\" n USING (\"MaNguoiDung\") WHERE l.\"NhaCungCap\"=:p AND l.\"MaTaiKhoanNgoai\"=:s").param("p",provider).param("s",subject).query(String.class).optional();}
    public List<Map<String,Object>> cuaToi(long user){return jdbc.sql("SELECT \"NhaCungCap\",to_char(\"NgayTao\" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI') AS ngay FROM \"TaiKhoanLienKet\" WHERE \"MaNguoiDung\"=:u ORDER BY \"NhaCungCap\"").param("u",user).query().listOfRows();}
    public boolean matKhau(long user,String password,org.springframework.security.crypto.password.PasswordEncoder encoder){return jdbc.sql("SELECT \"MatKhauBam\" FROM \"NguoiDung\" WHERE \"MaNguoiDung\"=:u").param("u",user).query(String.class).optional().filter(hash->encoder.matches(password,hash)).isPresent();}
    public void gan(long user,String provider,String subject){jdbc.sql("INSERT INTO \"TaiKhoanLienKet\" (\"MaNguoiDung\",\"NhaCungCap\",\"MaTaiKhoanNgoai\",\"NgayCapNhat\") VALUES (:u,:p,:s,clock_timestamp())").param("u",user).param("p",provider).param("s",subject).update();}
    public void bo(long user,String provider){jdbc.sql("DELETE FROM \"TaiKhoanLienKet\" WHERE \"MaNguoiDung\"=:u AND \"NhaCungCap\"=:p").param("u",user).param("p",provider).update();}
}
