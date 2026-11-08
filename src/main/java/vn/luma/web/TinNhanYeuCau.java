package vn.luma.web;
import jakarta.validation.constraints.*;
public record TinNhanYeuCau(@NotBlank @Pattern(regexp="[a-fA-F0-9-]{36}") String khoa,@NotBlank @Size(max=2000) String noiDung) { }
