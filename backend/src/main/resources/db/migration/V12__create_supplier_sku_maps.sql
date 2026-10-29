CREATE TABLE supplier_sku_maps (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  supplier_cnpj VARCHAR(14) NOT NULL,
  supplier_code VARCHAR(60) NOT NULL,
  sku VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_supplier_sku_maps UNIQUE (supplier_cnpj, supplier_code)
);
