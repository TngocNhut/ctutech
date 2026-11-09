package vn.luma.business;

import java.awt.Color;
import java.io.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Component;

@Component
public class HoaDonPdf {
    static final Color INK=new Color(21,40,61),TEAL=new Color(8,127,118),MUTED=new Color(75,91,108),PALE=new Color(231,243,240);
    public byte[] tao(Map<String,Object> invoice,List<Map<String,Object>> lines){
        BigDecimal gross=BigDecimal.ZERO,discount=BigDecimal.ZERO;
        for(var row:lines){gross=gross.add(((BigDecimal)row.get("DonGia")).multiply(BigDecimal.valueOf(((Number)row.get("SoLuong")).longValue())));discount=discount.add((BigDecimal)row.get("GiamGia"));}
        BigDecimal fee=invoice.get("PhiGiao")==null?BigDecimal.ZERO:(BigDecimal)invoice.get("PhiGiao");
        if(gross.subtract(discount).add(fee).compareTo((BigDecimal)invoice.get("TongTien"))!=0)throw new IllegalStateException("Dòng hóa đơn không khớp tổng đã chốt.");
        try(var doc=new PDDocument();var output=new ByteArrayOutputStream();var layout=new Layout(doc)){
            doc.getDocumentInformation().setTitle("Hóa đơn mua hàng - "+invoice.get("SoHoaDon"));doc.getDocumentInformation().setAuthor("CTUtech");
            layout.text("CTUtech",18,true,TEAL);layout.gap(14);layout.text("HÓA ĐƠN MUA HÀNG",23,true,INK);
            layout.paragraph("Số: "+invoice.get("SoHoaDon"),10,false);layout.paragraph("Đơn hàng: "+invoice.get("MaDonCongKhai"),10,false);layout.paragraph("Phát hành: "+invoice.get("NgayIn")+" (giờ Việt Nam)",10,false);
            if("MO_PHONG".equals(invoice.get("CheDoXuLy"))){layout.gap(10);layout.paragraph("DỮ LIỆU MÔ PHỎNG - KHÔNG XÁC NHẬN CHUYỂN TIỀN THẬT",10,true);}
            layout.gap(18);layout.text("Thông tin người nhận",12,true,INK);
            for(String key:List.of("NguoiNhan","DienThoai","Email","DiaChi"))layout.paragraph(Objects.toString(invoice.get(key),""),11,false);
            layout.gap(18);layout.tableHeader();int index=1;
            for(var row:lines)layout.row(index++,row);
            layout.gap(18);layout.total("Tiền hàng",gross,false);layout.total("Giảm giá",discount.negate(),false);layout.total("Phí giao hàng",fee,false);layout.total("Tổng thanh toán",(BigDecimal)invoice.get("TongTien"),true);
            layout.gap(22);layout.paragraph("Đơn vị tiền tệ: VND. Thông tin hàng, giá và địa chỉ được chốt tại thời điểm phát hành.",10,false);layout.paragraph("Chứng từ mua hàng nội bộ - không phải hóa đơn thuế điện tử. Khoản hoàn, nếu có, được ghi bằng chứng từ riêng.",10,false);
            layout.close();doc.save(output);return output.toByteArray();
        }catch(IOException ex){throw new IllegalStateException("Không tạo được hóa đơn PDF.",ex);}
    }
    static String money(BigDecimal amount){var formatter=NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));return formatter.format(amount)+" ₫";}
    static class Layout implements AutoCloseable {
        final PDDocument doc;final PDType0Font regular,bold;PDPageContentStream stream;float y;static final float LEFT=48,RIGHT=547,BOTTOM=64;
        Layout(PDDocument doc)throws IOException{this.doc=doc;try(var r=font("NotoSans-Regular.ttf");var b=font("NotoSans-Bold.ttf")){regular=PDType0Font.load(doc,r,true);bold=PDType0Font.load(doc,b,true);}newPage();}
        InputStream font(String file){return Objects.requireNonNull(HoaDonPdf.class.getResourceAsStream("/fonts/"+file),"Missing bundled invoice font");}
        void newPage()throws IOException{
            if(stream!=null)stream.close();var page=new PDPage(PDRectangle.A4);doc.addPage(page);stream=new PDPageContentStream(doc,page);y=790;
            draw("CTUtech | Chứng từ mua hàng nội bộ",LEFT,30,8,false,MUTED);right("Trang "+doc.getNumberOfPages(),RIGHT,30,8,false,MUTED);
        }
        void ensure(float height)throws IOException{if(y-height<BOTTOM)newPage();}
        void gap(float height)throws IOException{ensure(height);y-=height;}
        String safe(String value,PDFont font)throws IOException{
            StringBuilder result=new StringBuilder();
            for(int cp:value.replaceAll("[\\p{Cntrl}]"," ").codePoints().toArray()){
                String ch=new String(Character.toChars(cp));try{font.encode(ch);result.append(ch);}catch(IllegalArgumentException ex){result.append('?');}
            }return result.toString();
        }
        void draw(String value,float x,float yy,float size,boolean strong,Color color)throws IOException{var f=strong?bold:regular;stream.beginText();stream.setFont(f,size);stream.setNonStrokingColor(color);stream.newLineAtOffset(x,yy);stream.showText(safe(value,f));stream.endText();}
        void right(String value,float end,float yy,float size,boolean strong,Color color)throws IOException{var f=strong?bold:regular;String clean=safe(value,f);draw(clean,end-f.getStringWidth(clean)/1000*size,yy,size,strong,color);}
        List<String> wrap(String value,float width,float size,boolean strong)throws IOException{
            var f=strong?bold:regular;String clean=safe(value,f);List<String> result=new ArrayList<>();StringBuilder current=new StringBuilder();
            for(int cp:clean.codePoints().toArray()){
                String ch=new String(Character.toChars(cp));if(!current.isEmpty()&&f.getStringWidth(current+ch)/1000*size>width){int space=current.lastIndexOf(" ");if(space>0){result.add(current.substring(0,space));String rest=current.substring(space+1);current=new StringBuilder(rest);}else{result.add(current.toString());current.setLength(0);}}
                current.append(ch);
            }
            if(!current.isEmpty())result.add(current.toString().strip());if(result.isEmpty())result.add("");return result;
        }
        void text(String value,float size,boolean strong,Color color)throws IOException{ensure(size+9);draw(value,LEFT,y,size,strong,color);y-=size+9;}
        void paragraph(String value,float size,boolean strong)throws IOException{for(String line:wrap(value,RIGHT-LEFT,size,strong)){ensure(size+5);draw(line,LEFT,y,size,strong,INK);y-=size+5;}}
        void tableHeader()throws IOException{
            ensure(40);stream.setNonStrokingColor(PALE);stream.addRect(LEFT,y-22,RIGHT-LEFT,28);stream.fill();
            draw("STT",LEFT+5,y-12,9,true,INK);draw("Sản phẩm / SKU",82,y-12,9,true,INK);draw("SL",308,y-12,9,true,INK);right("Đơn giá",440,y-12,9,true,INK);right("Thành tiền",RIGHT-5,y-12,9,true,INK);y-=40;
        }
        void row(int index,Map<String,Object> row)throws IOException{
            var names=wrap(Objects.toString(row.get("TenMatHang"),""),215,10,true);var skus=wrap(Objects.toString(row.get("MaHangLucDat"),""),215,8.5f,false);
            BigDecimal discount=(BigDecimal)row.get("GiamGia");float height=names.size()*14+skus.size()*12+(discount.signum()>0?14:0)+18;
            if(y-height<BOTTOM){newPage();tableHeader();}
            draw(Integer.toString(index),LEFT+5,y,9,false,INK);float cursor=y;
            for(String name:names){draw(name,82,cursor,10,true,INK);cursor-=14;}
            for(String sku:skus){draw(sku,82,cursor,8.5f,false,MUTED);cursor-=12;}
            if(discount.signum()>0)draw("Giảm: "+money(discount),82,cursor,8.5f,false,MUTED);
            long qty=((Number)row.get("SoLuong")).longValue();draw(Long.toString(qty),308,y,10,false,INK);
            var price=(BigDecimal)row.get("DonGia");String unit=money(price),total=money(price.multiply(BigDecimal.valueOf(qty)).subtract(discount));
            float unitSize=Math.min(9.5f,90/(regular.getStringWidth(unit)/1000));float totalSize=Math.min(9.5f,99/(regular.getStringWidth(total)/1000));
            right(unit,440,y,unitSize,false,INK);right(total,RIGHT-5,y,totalSize,false,INK);y-=height;
            stream.setStrokingColor(new Color(220,226,232));stream.setLineWidth(.5f);stream.moveTo(LEFT,y+14);stream.lineTo(RIGHT,y+14);stream.stroke();
        }
        void total(String label,BigDecimal amount,boolean strong)throws IOException{ensure(30);draw(label,300,y,strong?12:10,strong,INK);right(money(amount),RIGHT,y,strong?13:10,strong,strong?TEAL:INK);y-=strong?28:22;}
        @Override public void close()throws IOException{if(stream!=null){stream.close();stream=null;}}
    }
}
