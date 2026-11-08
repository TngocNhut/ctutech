package vn.luma.business;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

@Component
public class KhoChungTu {
    private final Path root;
    public KhoChungTu(@Value("${app.refunds.proof-directory:.local/refund-proofs}") String root){this.root=Path.of(root).toAbsolutePath().normalize();}
    public String luu(byte[] bytes){
        if(bytes==null||bytes.length==0||bytes.length>5*1024*1024)throw new IllegalArgumentException("Chứng từ cần là PDF, PNG hoặc JPEG, tối đa 5 MB.");
        String type=loai(bytes);if(type==null)throw new IllegalArgumentException("Nội dung chứng từ phải là PDF, PNG hoặc JPEG.");
        String file=UUID.randomUUID()+"."+type;Path path=root.resolve(file);
        try{Files.createDirectories(root);Files.write(path,bytes,StandardOpenOption.CREATE_NEW);}catch(IOException ex){throw new IllegalStateException("Không lưu được chứng từ.",ex);}
        if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(path);}catch(IOException ignored){/* An unreferenced private file can be removed during maintenance. */}}
        });
        return file;
    }
    public byte[] doc(String file){
        if(file==null||!file.matches("[0-9a-f-]{36}\\.(pdf|png|jpg)"))throw new KhongTimThayException("Không có chứng từ.");
        try{return Files.readAllBytes(root.resolve(file));}catch(IOException ex){throw new KhongTimThayException("Không tìm thấy tệp chứng từ.");}
    }
    private String loai(byte[] b){
        if(b.length>=5&&b[0]=='%'&&b[1]=='P'&&b[2]=='D'&&b[3]=='F'&&b[4]=='-')return "pdf";
        if(b.length>=8&&Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))return "png";
        if(b.length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255)return "jpg";
        return null;
    }
}
