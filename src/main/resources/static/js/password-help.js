const password = document.getElementById('matKhau');
const hint = document.getElementById('password-help');
password?.addEventListener('input', () => {
 const count = password.value.length, bytes = new TextEncoder().encode(password.value).length;
 if (count < 12) hint.textContent = `Thêm ít nhất ${12-count} ký tự để đạt 12 ký tự.`;
 else if (count > 64 || bytes > 72) hint.textContent = 'Mật khẩu cần tối đa 64 ký tự và 72 byte UTF-8.';
 else hint.textContent = 'Độ dài hợp lệ. Nên dùng mật khẩu dài, duy nhất và khó đoán.';
});
