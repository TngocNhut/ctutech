package vn.luma.model;

public record TaiKhoan(long maNguoiDung, String hoTen, String thuDienTu,
                       String soDienThoai, boolean daXacMinh, String vaiTro, String trangThai) { }
