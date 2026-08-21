-- B2-02: Variantes de produto (size/color/sku)

CREATE TABLE product_variants (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  color VARCHAR(64) NOT NULL,
  size VARCHAR(32) NOT NULL,
  sku VARCHAR(100) NOT NULL UNIQUE,
  price DECIMAL(12, 2) NOT NULL,
  stock_qty INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_product_variants_product
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE INDEX idx_product_variants_product_id ON product_variants(product_id);
