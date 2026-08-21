-- B4-01: Idempotência de pagamento mock por usuário + chave

CREATE TABLE payment_idempotency_keys (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  order_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pay_idem_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_pay_idem_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT uk_pay_idem_user_key UNIQUE (user_id, idempotency_key)
);
