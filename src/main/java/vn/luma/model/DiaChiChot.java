package vn.luma.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DiaChiChot(String tenNguoiNhan,String soDienThoai,String tinhThanh,String phuongXa,String diaChiCuThe,
    String ghiChu,String placeId,BigDecimal viDo,BigDecimal kinhDo,OffsetDateTime ngayLay,OffsetDateTime hanLuu) { }
