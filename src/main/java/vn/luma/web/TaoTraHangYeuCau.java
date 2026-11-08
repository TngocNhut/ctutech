package vn.luma.web;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record TaoTraHangYeuCau(@NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}") String khoaChongLap,
    @NotBlank @Size(max=500) String lyDo,@NotEmpty @Size(max=50) List<@Valid Dong> dong){
    public record Dong(@Min(1) long maDong,@Min(0) @Max(99) int soLuong){}
}
