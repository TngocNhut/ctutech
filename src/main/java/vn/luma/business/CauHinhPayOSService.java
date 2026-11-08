package vn.luma.business;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import vn.luma.config.ThanhToanCauHinh;
import vn.luma.data.QuanLySanPhamDAO;

@Service
public class CauHinhPayOSService {
    private final TaiKhoanService users;private final ThanhToanCauHinh current;private final QuanLySanPhamDAO audit;private final JdbcClient jdbc;private final Path file;private volatile boolean pending;
    @Value("${app.payos.file-config-enabled:true}") boolean fileConfigEnabled;
    public CauHinhPayOSService(TaiKhoanService users,ThanhToanCauHinh current,QuanLySanPhamDAO audit,JdbcClient jdbc,@Value("${app.payos.config-file:config/payos-local.properties}") String path){this.users=users;this.current=current;this.audit=audit;this.jdbc=jdbc;this.file=Path.of(path).toAbsolutePath().normalize();}
    public record TrangThai(boolean dangBat,boolean batDuKien,boolean coClient,boolean coApi,boolean coChecksum,String publicUrl,boolean coBanChoKhoiDongLai,boolean choPhepLuuTep){}
    public synchronized TrangThai trangThai(String email){users.quanTri(email);Properties staged=new Properties();if(pending)try(var in=Files.newInputStream(file)){staged.load(in);}catch(IOException ex){throw new IllegalStateException("Không đọc được cấu hình đang chờ áp dụng.");}return new TrangThai(current.live(),pending?"LIVE".equals(staged.getProperty("app.payos.mode")):current.live(),!staged.getProperty("app.payos.client-id",current.clientId()).isBlank(),!staged.getProperty("app.payos.api-key",current.apiKey()).isBlank(),!staged.getProperty("app.payos.checksum-key",current.checksumKey()).isBlank(),staged.getProperty("app.public-url",current.publicUrl()),pending,fileConfigEnabled);}
    private static String pick(String value,String existing){return value==null||value.isBlank()?existing:value.strip();}
    private static void key(String value){if(value.length()>512||value.chars().anyMatch(Character::isWhitespace)||value.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Thông tin kết nối không hợp lệ. Kiểm tra khoảng trắng và độ dài.");}
    private static String publicUrl(String value,boolean enabled){
        String v=Objects.toString(value,"").strip();while(v.endsWith("/"))v=v.substring(0,v.length()-1);
        if(v.length()>500||v.chars().anyMatch(Character::isWhitespace))throw new IllegalArgumentException("Địa chỉ website không hợp lệ.");
        try{URI uri=URI.create(v);String host=uri.getHost();if(host==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||(!"https".equalsIgnoreCase(uri.getScheme())&&!("http".equalsIgnoreCase(uri.getScheme())&&!enabled))|| (uri.getPath()!=null&&!uri.getPath().isEmpty()))throw new IllegalArgumentException();if(enabled){String h=host.toLowerCase(Locale.ROOT);if(h.equals("localhost")||h.endsWith(".localhost")||h.endsWith(".local")||(!h.contains(".")&&!h.contains(":")))throw new IllegalArgumentException();if(h.matches("[0-9.]+")||h.contains(":")){var address=java.net.InetAddress.getByName(h.replace("[","").replace("]",""));if(address.isAnyLocalAddress()||address.isLoopbackAddress()||address.isLinkLocalAddress()||address.isSiteLocalAddress()||address.isMulticastAddress())throw new IllegalArgumentException();}}return v;}
        catch(Exception ex){throw new IllegalArgumentException("Khi bật PayOS, nhập URL gốc HTTPS công khai của website, không có đường dẫn hoặc tham số.");}
    }
    public synchronized void luu(String email,boolean enable,String url,String client,String api,String checksum){
        long actor=users.quanTri(email).maNguoiDung();if(!fileConfigEnabled)throw new IllegalArgumentException("Trên Render, cấu hình PayOS bằng biến môi trường trong Render Dashboard; tệp trên máy chủ không được lưu lâu dài.");Properties staged=new Properties();if(pending)try(var in=Files.newInputStream(file)){staged.load(in);}catch(IOException ex){throw new IllegalStateException("Không đọc được cấu hình đang chờ áp dụng.");}
        String c=pick(client,staged.getProperty("app.payos.client-id",current.clientId())),a=pick(api,staged.getProperty("app.payos.api-key",current.apiKey())),k=pick(checksum,staged.getProperty("app.payos.checksum-key",current.checksumKey()));key(c);key(a);key(k);String base=publicUrl(url,enable);
        if(enable&&(c.isBlank()||a.isBlank()||k.isBlank()))throw new IllegalArgumentException("Cần Client ID, API key và checksum key để bật PayOS.");
        boolean active=jdbc.sql("SELECT EXISTS(SELECT 1 FROM \"PhienThanhToan\" WHERE \"CheDoXuLy\"='TICH_HOP_THAT' AND \"TrangThaiXuLy\" IN ('MOI_TAO','CHO_THANH_TOAN','THU_THIEU','THU_DU','CAN_DOI_SOAT'))").query(Boolean.class).single();
        if(active)throw new IllegalArgumentException("Còn phiên PayOS cần xử lý hoặc đối soát. Hãy hoàn tất trước khi đổi cấu hình.");
        Properties values=new Properties();values.setProperty("app.payos.mode",enable?"LIVE":"DISABLED");values.setProperty("app.public-url",base);values.setProperty("app.payos.client-id",c);values.setProperty("app.payos.api-key",a);values.setProperty("app.payos.checksum-key",k);
        Path parent=file.getParent(),temp=null;
        try{Files.createDirectories(parent);if(Files.isSymbolicLink(parent)||Files.isSymbolicLink(file))throw new IOException("Invalid config path");temp=Files.createTempFile(parent,"payos-local.properties.",".tmp");restrict(temp);try(var out=Files.newOutputStream(temp,StandardOpenOption.TRUNCATE_EXISTING)){values.store(out,"PayOS local configuration; do not share");}Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);pending=true;audit.log(actor,0,"LUU_CAU_HINH_PAYOS","CAU_HINH_HE_THONG");}
        catch(IOException ex){throw new IllegalStateException("Không thể lưu cấu hình PayOS an toàn trên máy chủ.",ex);}
        finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(IOException ignored){}}
    }
    private static void restrict(Path path)throws IOException{
        var posix=Files.getFileAttributeView(path,PosixFileAttributeView.class);if(posix!=null){posix.setPermissions(PosixFilePermissions.fromString("rw-------"));return;}
        var acl=Files.getFileAttributeView(path,AclFileAttributeView.class);if(acl==null)throw new IOException("Unsupported secure file permissions");
        var owner=Files.getOwner(path);var rule=AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(owner).setPermissions(EnumSet.allOf(AclEntryPermission.class)).build();acl.setAcl(List.of(rule));
    }
}
