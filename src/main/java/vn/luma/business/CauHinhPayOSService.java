package vn.luma.business;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.luma.config.ThanhToanCauHinh;
import vn.luma.data.QuanLySanPhamDAO;

@Service
public class CauHinhPayOSService {
    private static final byte[] AAD="CTUtech PayOS credentials v1".getBytes(StandardCharsets.UTF_8);
    private static final SecureRandom RANDOM=new SecureRandom();
    private final TaiKhoanService users;private final ThanhToanCauHinh current;private final QuanLySanPhamDAO audit;private final JdbcClient jdbc;private final Path file;private final String encryptionKey;private volatile boolean pending;
    @Value("${app.payos.file-config-enabled:true}") boolean fileConfigEnabled;
    public CauHinhPayOSService(TaiKhoanService users,ThanhToanCauHinh current,QuanLySanPhamDAO audit,JdbcClient jdbc,
            @Value("${app.payos.config-file:config/payos-local.properties}") String path,
            @Value("${app.payos.config-encryption-key:}") String encryptionKey){this.users=users;this.current=current;this.audit=audit;this.jdbc=jdbc;this.file=Path.of(path).toAbsolutePath().normalize();this.encryptionKey=encryptionKey;}
    CauHinhPayOSService(TaiKhoanService users,ThanhToanCauHinh current,QuanLySanPhamDAO audit,JdbcClient jdbc,String path){this(users,current,audit,jdbc,path,"");}
    public record TrangThai(boolean dangBat,boolean batDuKien,boolean coClient,boolean coApi,boolean coChecksum,String publicUrl,boolean coBanChoKhoiDongLai,boolean choPhepLuuTep,boolean choPhepCauHinh,boolean luuTrongDatabase){}
    private record Stored(byte[] nonce,byte[] ciphertext){}

    @PostConstruct
    void napCauHinhDaMaHoa(){
        if(fileConfigEnabled)return;
        Stored stored=docBanMa();
        if(stored==null)return;
        Properties p=giaiMa(stored);
        capNhatRuntime(p);
    }

    public synchronized TrangThai trangThai(String email){
        users.quanTri(email);Properties staged=new Properties();boolean hasPending=pending;
        if(fileConfigEnabled&&pending)try(var in=Files.newInputStream(file)){staged.load(in);}catch(IOException ex){throw new IllegalStateException("Không đọc được cấu hình đang chờ áp dụng.");}
        if(!fileConfigEnabled){Properties stored=docCauHinhDatabase();if(stored!=null)staged=stored;}
        String mode=staged.getProperty("app.payos.mode",current.mode());
        boolean active="LIVE".equals(mode)&&hasKeys(staged,current);
        return new TrangThai(current.live(),active,has(staged,"app.payos.client-id",current.clientId()),has(staged,"app.payos.api-key",current.apiKey()),has(staged,"app.payos.checksum-key",current.checksumKey()),staged.getProperty("app.public-url",current.publicUrl()),hasPending,fileConfigEnabled,fileConfigEnabled||encryptionKeyBytesOrNull()!=null,!fileConfigEnabled);
    }

    private static boolean hasKeys(Properties p,ThanhToanCauHinh c){return has(p,"app.payos.client-id",c.clientId())&&has(p,"app.payos.api-key",c.apiKey())&&has(p,"app.payos.checksum-key",c.checksumKey());}
    private static boolean has(Properties p,String key,String fallback){return !p.getProperty(key,fallback).isBlank();}
    private static String pick(String value,String existing){return value==null||value.isBlank()?existing:value.strip();}
    private static void key(String value){if(value.length()>512||value.chars().anyMatch(Character::isWhitespace)||value.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Thông tin kết nối không hợp lệ. Kiểm tra khoảng trắng và độ dài.");}
    private static String publicUrl(String value,boolean enabled){
        String v=Objects.toString(value,"").strip();while(v.endsWith("/"))v=v.substring(0,v.length()-1);
        if(v.length()>500||v.chars().anyMatch(Character::isWhitespace))throw new IllegalArgumentException("Địa chỉ website không hợp lệ.");
        try{URI uri=URI.create(v);String host=uri.getHost();if(host==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||(!"https".equalsIgnoreCase(uri.getScheme())&&!("http".equalsIgnoreCase(uri.getScheme())&&!enabled))|| (uri.getPath()!=null&&!uri.getPath().isEmpty()))throw new IllegalArgumentException();if(enabled){String h=host.toLowerCase(Locale.ROOT);if(h.equals("localhost")||h.endsWith(".localhost")||h.endsWith(".local")||(!h.contains(".")&&!h.contains(":")))throw new IllegalArgumentException();if(h.matches("[0-9.]+")||h.contains(":")){var address=java.net.InetAddress.getByName(h.replace("[","").replace("]",""));if(address.isAnyLocalAddress()||address.isLoopbackAddress()||address.isLinkLocalAddress()||address.isSiteLocalAddress()||address.isMulticastAddress())throw new IllegalArgumentException();}}return v;}
        catch(Exception ex){throw new IllegalArgumentException("Khi bật PayOS, nhập URL gốc HTTPS công khai của website, không có đường dẫn hoặc tham số.");}
    }

    @Transactional
    public synchronized boolean luu(String email,boolean enable,String url,String clientId,String apiKey,String checksumKey){
        long actor=users.quanTri(email).maNguoiDung();if(!fileConfigEnabled&&encryptionKeyBytesOrNull()==null)throw new IllegalArgumentException("Trên Render, hãy đặt PAYOS_CONFIG_ENCRYPTION_KEY (Base64 32 byte) trong Environment trước khi cấu hình PayOS tại trang quản trị.");Properties old=docCauHinhHienTai();
        String c=pick(clientId,old.getProperty("app.payos.client-id",current.clientId())),a=pick(apiKey,old.getProperty("app.payos.api-key",current.apiKey())),k=pick(checksumKey,old.getProperty("app.payos.checksum-key",current.checksumKey()));key(c);key(a);key(k);String base=publicUrl(url,enable);
        if(enable&&(c.isBlank()||a.isBlank()||k.isBlank()))throw new IllegalArgumentException("Cần Client ID, API key và checksum key để bật PayOS.");
        boolean active=jdbc.sql("SELECT EXISTS(SELECT 1 FROM \"PhienThanhToan\" WHERE \"CheDoXuLy\"='TICH_HOP_THAT' AND \"TrangThaiXuLy\" IN ('MOI_TAO','CHO_THANH_TOAN','THU_THIEU','THU_DU','CAN_DOI_SOAT'))").query(Boolean.class).single();
        if(active)throw new IllegalArgumentException("Còn phiên PayOS cần xử lý hoặc đối soát. Hãy hoàn tất trước khi đổi cấu hình.");
        Properties values=new Properties();values.setProperty("app.payos.mode",enable?"LIVE":"DISABLED");values.setProperty("app.public-url",base);values.setProperty("app.payos.client-id",c);values.setProperty("app.payos.api-key",a);values.setProperty("app.payos.checksum-key",k);
        if(fileConfigEnabled){luuTep(values,actor);pending=true;return true;}
        luuDatabase(values,actor);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCommit(){synchronized(CauHinhPayOSService.this){capNhatRuntime(values);pending=false;}}
        });
        return false;
    }

    private Properties docCauHinhHienTai(){
        if(fileConfigEnabled){Properties p=new Properties();if(pending)try(var in=Files.newInputStream(file)){p.load(in);}catch(IOException ex){throw new IllegalStateException("Không đọc được cấu hình đang chờ áp dụng.");}return p;}
        Properties p=docCauHinhDatabase();return p==null?new Properties():p;
    }
    private Properties docCauHinhDatabase(){Stored stored=docBanMa();return stored==null?null:giaiMa(stored);}
    private Stored docBanMa(){return jdbc.sql("SELECT \"Nonce\",\"BanMa\" FROM \"CauHinhPayOSMaHoa\" WHERE \"MaCauHinh\"=1").query((rs,row)->new Stored(rs.getBytes("Nonce"),rs.getBytes("BanMa"))).optional().orElse(null);}
    private void capNhatRuntime(Properties p){current.capNhat(p.getProperty("app.payos.mode","DISABLED"),p.getProperty("app.payos.client-id",""),p.getProperty("app.payos.api-key",""),p.getProperty("app.payos.checksum-key",""),p.getProperty("app.public-url",current.publicUrl()));}

    private void luuDatabase(Properties p,long actor){
        byte[] key=encryptionKeyBytesOrNull();if(key==null)throw new IllegalArgumentException("Chưa bật cấu hình PayOS trong trang quản trị. Hãy đặt PAYOS_CONFIG_ENCRYPTION_KEY (khóa Base64 32 byte) trong Environment của Render trước.");
        byte[] nonce=new byte[12];RANDOM.nextBytes(nonce);byte[] ciphertext=maHoa(p,key,nonce);
        jdbc.sql("INSERT INTO \"CauHinhPayOSMaHoa\" (\"MaCauHinh\",\"Nonce\",\"BanMa\",\"MaNguoiCapNhat\",\"NgayCapNhat\") VALUES (1,:nonce,:cipher,:actor,now()) ON CONFLICT (\"MaCauHinh\") DO UPDATE SET \"Nonce\"=EXCLUDED.\"Nonce\",\"BanMa\"=EXCLUDED.\"BanMa\",\"MaNguoiCapNhat\"=EXCLUDED.\"MaNguoiCapNhat\",\"NgayCapNhat\"=now()").param("nonce",nonce).param("cipher",ciphertext).param("actor",actor).update();
        audit.log(actor,0,"LUU_CAU_HINH_PAYOS","CAU_HINH_HE_THONG");
    }
    private void luuTep(Properties values,long actor){
        Path parent=file.getParent(),temp=null;
        try{Files.createDirectories(parent);if(Files.isSymbolicLink(parent)||Files.isSymbolicLink(file))throw new IOException("Invalid config path");temp=Files.createTempFile(parent,"payos-local.properties.",".tmp");restrict(temp);try(var out=Files.newOutputStream(temp,StandardOpenOption.TRUNCATE_EXISTING)){values.store(out,"PayOS local configuration; do not share");}Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);audit.log(actor,0,"LUU_CAU_HINH_PAYOS","CAU_HINH_HE_THONG");}
        catch(IOException ex){throw new IllegalStateException("Không thể lưu cấu hình PayOS an toàn trên máy chủ.",ex);}
        finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(IOException ignored){}}
    }
    private byte[] maHoa(Properties p,byte[] key,byte[] nonce){try{ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,null);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,nonce));cipher.updateAAD(AAD);return cipher.doFinal(out.toByteArray());}catch(GeneralSecurityException|IOException ex){throw new IllegalStateException("Không thể mã hóa cấu hình PayOS.");}}
    private Properties giaiMa(Stored stored){byte[] key=encryptionKeyBytesOrNull();if(key==null)throw new IllegalStateException("Thiếu PAYOS_CONFIG_ENCRYPTION_KEY để đọc cấu hình PayOS đã mã hóa.");try{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,stored.nonce()));cipher.updateAAD(AAD);Properties p=new Properties();p.load(new ByteArrayInputStream(cipher.doFinal(stored.ciphertext())));return p;}catch(Exception ex){throw new IllegalStateException("Không giải mã được cấu hình PayOS. Kiểm tra khóa PAYOS_CONFIG_ENCRYPTION_KEY đã dùng khi lưu.");}}
    private byte[] encryptionKeyBytesOrNull(){try{byte[] key=Base64.getDecoder().decode(encryptionKey.strip());return key.length==32?key:null;}catch(RuntimeException ex){return null;}}
    private static void restrict(Path path)throws IOException{
        var posix=Files.getFileAttributeView(path,PosixFileAttributeView.class);if(posix!=null){posix.setPermissions(PosixFilePermissions.fromString("rw-------"));return;}
        var acl=Files.getFileAttributeView(path,AclFileAttributeView.class);if(acl==null)throw new IOException("Unsupported secure file permissions");
        var owner=Files.getOwner(path);var rule=AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(owner).setPermissions(EnumSet.allOf(AclEntryPermission.class)).build();acl.setAcl(List.of(rule));
    }
}
