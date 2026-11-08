package vn.luma.web;

import java.util.*;

public record BoLocBoSung(Long dong,Map<Long,String> thuocTinh) {
    public BoLocBoSung {
        if(dong!=null&&dong<1)throw new IllegalArgumentException("Dòng sản phẩm không hợp lệ.");
        Map<Long,String> cleaned=new TreeMap<>();
        if(thuocTinh!=null)for(var e:thuocTinh.entrySet()){
            if(e.getKey()==null||e.getKey()<1)throw new IllegalArgumentException("Mã thuộc tính không hợp lệ.");
            if(e.getValue()==null||e.getValue().isBlank())continue;
            if(e.getValue().length()>250)throw new IllegalArgumentException("Giá trị lọc thuộc tính tối đa 250 ký tự.");cleaned.put(e.getKey(),e.getValue().strip());
        }
        if(cleaned.size()>10)throw new IllegalArgumentException("Chọn tối đa 10 thông số mở rộng mỗi lần lọc.");thuocTinh=Collections.unmodifiableMap(cleaned);
    }
    public static BoLocBoSung trong(){return new BoLocBoSung(null,Map.of());}
    public boolean coDieuKien(){return dong!=null||!thuocTinh.isEmpty();}
    public static BoLocBoSung doc(Map<String,List<String>> query){
        Long line=null;Map<Long,String> attrs=new TreeMap<>();
        try{
            if(query.containsKey("dong")){String value=mot(query.get("dong"));if(!value.isBlank())line=Long.valueOf(value);}
            for(var e:query.entrySet())if(e.getKey().startsWith("tt.")){
                if(!e.getKey().matches("tt\\.[1-9][0-9]{0,18}"))throw new IllegalArgumentException("Mã thuộc tính lọc không hợp lệ.");attrs.put(Long.valueOf(e.getKey().substring(3)),mot(e.getValue()));
            }
        }catch(NumberFormatException ex){throw new IllegalArgumentException("Mã dòng hoặc thuộc tính không hợp lệ.");}
        return new BoLocBoSung(line,attrs);
    }
    private static String mot(List<String> values){if(values==null||values.size()!=1||values.getFirst()==null)throw new IllegalArgumentException("Mỗi điều kiện lọc chỉ được có một giá trị.");return values.getFirst().strip();}
}
