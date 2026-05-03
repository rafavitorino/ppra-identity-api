-- Criação do banco de dados (caso não exista)
CREATE DATABASE IF NOT EXISTS identity_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE identity_db;

-- Tabela de processo de verificação (fluxo externo/legado)
CREATE TABLE IF NOT EXISTS processoverificacao (
    id               CHAR(36)      NOT NULL,
    status           ENUM(
                         'AGUARDANDO_RESPONSAVEL',
                         'DOCUMENTOS_ENVIADOS',
                         'AGUARDANDO_SELFIE',
                         'APROVADO',
                         'REJEITADO'
                     ) NOT NULL DEFAULT 'AGUARDANDO_RESPONSAVEL',
    emailResponsavel VARCHAR(255)  NOT NULL,
    metadadosCliente LONGTEXT,
    dataCriacao      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_processoverificacao PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- Tabela principal de verificações de identidade
CREATE TABLE IF NOT EXISTS tb_verificacao (
    id                BINARY(16)   NOT NULL,
    status            ENUM(
                          'AGUARDANDO_RESPONSAVEL',
                          'AUTORIZADO_RESPONSAVEL',
                          'EM_ANALISE',
                          'APROVADO',
                          'REPROVADO'
                      ) NOT NULL DEFAULT 'AGUARDANDO_RESPONSAVEL',
    email_responsavel VARCHAR(255) NOT NULL,
    id_usuario        VARCHAR(255) NOT NULL,
    cpf_responsavel   VARCHAR(14),
    motivo_rejeicao   VARCHAR(255),
    data_criacao      DATETIME(6)  NOT NULL,

    CONSTRAINT pk_tb_verificacao PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
