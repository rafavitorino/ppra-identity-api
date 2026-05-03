-- Atualiza o ENUM da coluna status da tb_verificacao para os valores corretos
ALTER TABLE tb_verificacao
MODIFY COLUMN status ENUM(
    'AGUARDANDO_RESPONSAVEL',
    'AUTORIZADO_RESPONSAVEL',
    'EM_ANALISE',
    'APROVADO',
    'REPROVADO'
) NOT NULL DEFAULT 'AGUARDANDO_RESPONSAVEL';
