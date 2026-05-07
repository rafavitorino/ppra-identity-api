-- Converte a coluna status de ENUM para VARCHAR,
-- eliminando conflitos de compatibilidade com MariaDB.
ALTER TABLE tb_verificacao
  DROP COLUMN status,
  ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'AGUARDANDO_RESPONSAVEL';
