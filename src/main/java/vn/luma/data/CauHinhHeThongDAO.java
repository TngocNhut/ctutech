package vn.luma.data;

import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CauHinhHeThongDAO {
    private final JdbcClient jdbc;
    public CauHinhHeThongDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public Map<String,String> doc(){var result=new HashMap<String,String>();jdbc.sql("SELECT \"KyHieu\",\"GiaTri\" #>> '{}' AS value FROM \"CauHinhHeThong\" WHERE \"KyHieu\" IN ('BAT_CHATBOT','PHI_GIAO_HANG_MAC_DINH','THONG_BAO_TRANG_CHU')").query((r,n)->Map.entry(r.getString("KyHieu"),r.getString("value"))).list().forEach(e->result.put(e.getKey(),e.getValue()));return result;}
    public void luu(String key,String json,long actor){jdbc.sql("INSERT INTO \"CauHinhHeThong\" (\"KyHieu\",\"GiaTri\",\"MaNguoiCapNhat\") VALUES (:key,CAST(:json AS jsonb),:u) ON CONFLICT (\"KyHieu\") DO UPDATE SET \"GiaTri\"=excluded.\"GiaTri\",\"MaNguoiCapNhat\"=excluded.\"MaNguoiCapNhat\",\"NgayCapNhat\"=clock_timestamp()").param("key",key).param("json",json).param("u",actor).update();}
}
