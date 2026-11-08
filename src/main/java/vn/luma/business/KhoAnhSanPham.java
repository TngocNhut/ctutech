package vn.luma.business;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

@Component
public class KhoAnhSanPham {
    private final Path root;
    public KhoAnhSanPham(@Value("${app.media.product-directory:.local/product-media}") String root){this.root=Path.of(root).toAbsolutePath().normalize();}
    public String luu(byte[] bytes){
        if(bytes==null||bytes.length==0||bytes.length>5*1024*1024)throw new IllegalArgumentException("Ảnh cần là PNG hoặc JPEG, tối đa 5 MB.");
        String type=bytes.length>=8&&bytes[0]==(byte)137&&bytes[1]=='P'&&bytes[2]=='N'&&bytes[3]=='G'?"png":bytes.length>=3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255?"jpg":null;
        if(type==null)throw new IllegalArgumentException("Nội dung tệp phải là ảnh PNG hoặc JPEG.");
        try(var input=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IllegalArgumentException("Không đọc được ảnh.");var reader=readers.next();
            try{reader.setInput(input,true,true);int width=reader.getWidth(0),height=reader.getHeight(0);if(width<1||height<1||width>6000||height>6000||(long)width*height>16000000)throw new IllegalArgumentException("Ảnh tối đa 6000 px mỗi chiều và 16 triệu điểm ảnh.");reader.read(0);}finally{reader.dispose();}
        }catch(IOException ex){throw new IllegalArgumentException("Ảnh không đầy đủ hoặc bị lỗi.");}
        String file=UUID.randomUUID()+"."+type;Path target=root.resolve(file);
        try{Files.createDirectories(root);Files.write(target,bytes,StandardOpenOption.CREATE_NEW);}catch(IOException ex){throw new IllegalStateException("Không lưu được ảnh sản phẩm.",ex);}
        if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(target);}catch(IOException ignored){/* Unreferenced private file may be removed during maintenance. */}}});return file;
    }
    public byte[] doc(String file){if(file==null||!file.matches("[0-9a-f-]{36}\\.(png|jpg)"))throw new KhongTimThayException("Không có ảnh này.");try{return Files.readAllBytes(root.resolve(file));}catch(IOException ex){throw new KhongTimThayException("Không tìm thấy tệp ảnh.");}}
}
