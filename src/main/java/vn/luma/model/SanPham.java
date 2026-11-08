package vn.luma.model;

import java.math.BigDecimal;

public record SanPham(long maSanPham, String tenSanPham, String duongDan,
                      String moTaNgan, String thuongHieu, String phanLoai,
                      BigDecimal giaTu, long tonKhaDung,String anhDaiDien,long maSkuGoiY,String cauHinhGoiY) { }
