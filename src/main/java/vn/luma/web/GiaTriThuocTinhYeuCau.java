package vn.luma.web;

import jakarta.validation.constraints.*;

public record GiaTriThuocTinhYeuCau(@NotBlank @Size(max=250) String giaTri) { }
