# Selenium Learning

Repository này chứa các thử nghiệm tự động Selenium cho ứng dụng WMS/OMS, sử dụng Node.js và `selenium-webdriver`.

## Tổng quan

- `tests/`: các file chạy test chính
- `pages/`: các lớp page object để thao tác UI
- `utils/`: module hỗ trợ cho luồng đăng nhập và gọi API
- `data/`: dữ liệu test như thông tin tài khoản

## Yêu cầu

- Node.js (phiên bản hiện tại hoặc tương thích)
- Chrome đã cài đặt
- ChromeDriver hoặc Selenium WebDriver tương thích với phiên bản Chrome
  - Với Selenium 4, ChromeDriver thường được quản lý tự động
  - Nếu cần, đảm bảo ChromeDriver có trên `PATH`
- Mạng truy cập được tới môi trường staging của ứng dụng

## Cài đặt

1. Sao chép file cấu hình mẫu:

- Trên macOS/Linux:

```bash
cp .env.example .env
```

- Trên Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

2. Cập nhật giá trị trong `.env` với tài khoản và môi trường phù hợp.
3. Cài đặt phụ thuộc:

```bash
npm install
```

## Biến môi trường

File `.env` cần chứa các biến:

```env
OMS_EMAIL=your_oms_email@example.com
OMS_PASSWORD=YourOmsPassword123
OPS_EMAIL=your_ops_email@example.com
OPS_PASSWORD=YourOpsPassword123
WMS_EMAIL=your_wms_email@example.com
WMS_PASSWORD=YourWmsPassword123
```

> Lưu ý: không commit file `.env` lên Git vì chứa thông tin nhạy cảm.

## Chạy test

### Sử dụng npm script

```bash
npm run login
npm run pick-pack
npm run product
npm run inbound
npm run create-order
```

### Chạy toàn bộ suite Mocha

```bash
npm test
```

### Chạy riêng login test bằng Mocha

```bash
npm run test:login
```

### Chạy trực tiếp bằng Node

```bash
node tests/product_test.js
node tests/inbound_product_test.js
node tests/create_order_test.js
node tests/pick_pack_test.js
```

## Nội dung test

- `tests/login_test.js`: đăng nhập WMS, gồm cả trường hợp success, negative và edge case
- `tests/pick_pack_test.js`: luồng Pick & Pack
- `tests/product_test.js`: tạo và duyệt sản phẩm
- `tests/inbound_product_test.js`: luồng nhập kho
- `tests/create_order_test.js`: luồng tạo đơn hàng

## Cấu trúc project

- `tests/BaseTest.js`: lớp cơ sở khởi tạo Selenium WebDriver và xử lý setup/teardown
- `pages/`: các page object dùng chung cho test
- `utils/`: helper cho đăng nhập và thao tác API
- `data/account.js`: đọc thông tin tài khoản từ `.env`

## Artifact và log

- `screenshots/`: thư mục lưu ảnh màn hình nếu test thất bại
- `test-results/` (nếu bạn thêm mô-đun báo cáo) có thể dùng để lưu kết quả chạy test

## Báo cáo test Mocha

- Mặc định `npm test` chạy Mocha với báo cáo console.
- Nếu bạn muốn xuất báo cáo HTML/JSON, có thể cài thêm reporter như `mochawesome`.
- Ví dụ sau khi cài reporter:

```bash
npx mocha --reporter mochawesome
```

## Troubleshooting

- Nếu trình duyệt không mở được:
  - kiểm tra ChromeDriver và Chrome tương thích
  - kiểm tra biến môi trường `PATH`
- Nếu login thất bại do selector thay đổi:
  - kiểm tra lại `pages/LoginPage.js`
  - cập nhật locator phù hợp
- Nếu báo lỗi thiếu biến môi trường:
  - kiểm tra file `.env`
  - đảm bảo đã chạy `cp .env.example .env`

## Ghi chú

- Project dùng CommonJS (`require` / `module.exports`).
- Nếu thêm test mới, giữ chúng trong `tests/` và tái sử dụng `BaseTest.js`.
- Nếu muốn thêm lệnh nhanh, sửa `package.json` với `scripts` phù hợp.
