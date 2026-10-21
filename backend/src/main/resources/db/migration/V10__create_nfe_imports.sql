CREATE TABLE nfe_imports (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  access_key VARCHAR(44) NOT NULL,
  direction VARCHAR(10) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_nfe_imports_access_key UNIQUE (access_key)
);
