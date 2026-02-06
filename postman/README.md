# Postman – Mystic Tarot API

## Import

1. Mở Postman → **Import** → chọn:
   - `MysticTarot_API.postman_collection.json`
   - `MysticTarot.postman_environment.json`
2. Chọn environment **Mystic Tarot - Local** ở góc phải.

## Biến môi trường

| Variable       | Mô tả |
|----------------|--------|
| `baseUrl`      | Base API (mặc định: `http://localhost:8080/api/v1`) |
| `token`        | JWT – gửi trong header `Authorization: Bearer <token>`. Có thể set tự động sau **Login** (script trong request Login). |
| `readingId`    | UUID của một reading – dùng cho **Get Reading Detail** và **Delete Reading**. |
| `stripeSignature` | Chữ ký Stripe webhook (chỉ khi test webhook Stripe). |

## Luồng gợi ý

1. **Register** hoặc **Login** → token được lưu vào env (nếu dùng request Login có test script).
2. Các request trong **User**, **Tarot**, **History**, **Payment** dùng Bearer token từ collection/auth.
3. **Create Order** (Payment) trả về `paymentUrl` – mở trong browser để test thanh toán (khi đã cấu hình Momo/ZaloPay/Stripe).

## Webhooks

Các request trong **Payment Webhooks** dành cho provider gọi về; chỉ dùng khi mô phỏng callback (body và signature phải đúng).
