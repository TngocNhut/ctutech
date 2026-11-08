package vn.luma.web;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record DiaChiYeuCau(
    @NotBlank @Size(max=120) String tenNguoiNhan,
    @NotBlank @Pattern(regexp="[0-9+() .-]{8,20}",message="Nhập số điện thoại từ 8–20 ký tự") String soDienThoai,
    @NotBlank @Size(max=100) String tinhThanh,
    @NotBlank @Size(max=100) String phuongXa,
    @NotBlank @Size(max=255) String diaChiCuThe,
    @Size(max=500) String ghiChu,
    @Size(max=255) String placeId,
    @DecimalMin("-90") @DecimalMax("90") BigDecimal viDo,
    @DecimalMin("-180") @DecimalMax("180") BigDecimal kinhDo,
    boolean xacNhanPin) {
    public static DiaChiYeuCau trong(String ten,String phone){return new DiaChiYeuCau(ten,phone,"","","","","",null,null,false);}
}
