package vn.luma.web;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record KiemHangYeuCau(@AssertTrue(message="Cần xác nhận đã nhận và kiểm hàng") boolean daKiemTra,
    @NotEmpty @Size(max=50) List<@Valid Dong> dong){
    public record Dong(@Min(1) long maDong,@Min(0) @Max(99) int daNhan,@Min(0) @Max(99) int duyetHoan,
        @Min(0) @Max(99) int nhapLai,@NotBlank @Size(max=500) String ghiChu){}
}
