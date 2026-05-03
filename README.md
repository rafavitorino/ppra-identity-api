# Identity API

API REST para verificação de identidade (KYC) com extração automática de dados de documentos RG via OCR. Desenvolvida com Spring Boot, a aplicação processa imagens de documentos, extrai informações relevantes e gerencia o fluxo de verificação de menores de idade com autorização de responsável.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Pré-requisitos](#pré-requisitos)
- [Configuração](#configuração)
- [Banco de Dados](#banco-de-dados)
- [Executando a Aplicação](#executando-a-aplicação)
- [Endpoints](#endpoints)
- [Fluxo de Verificação](#fluxo-de-verificação)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Pipeline de Processamento de Documentos](#pipeline-de-processamento-de-documentos)

---

## Visão Geral

A Identity API implementa um fluxo de KYC (Know Your Customer) voltado para verificação de menores de idade. O processo exige que um responsável legal envie seu próprio documento antes de submeter o documento do menor, garantindo a autorização parental no fluxo.

O sistema utiliza OCR (Tesseract) para extrair automaticamente os seguintes dados de um RG:

- Nome completo
- CPF
- Data de nascimento / Idade calculada
- Naturalidade
- Nome do pai e da mãe
- Órgão expedidor
- Número do Registro Geral (RG)
- Data de expedição

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 25 |
| Spring Boot | 4.0.5 |
| Spring Data JPA | — |
| Spring Validation | — |
| MySQL Connector/J | — |
| Tess4J (Tesseract OCR) | 5.11.0 |
| Lombok | — |
| Maven | — |

---

## Pré-requisitos

Antes de rodar a aplicação, certifique-se de ter instalado:

1. **Java 25+**
2. **Maven 3.9+**
3. **MySQL 8+** rodando localmente na porta `3306`
4. **Tesseract OCR** instalado na máquina com o pacote de idioma português (`por`)

### Instalando o Tesseract (Windows)

Baixe o instalador em [https://github.com/UB-Mannheim/tesseract/wiki](https://github.com/UB-Mannheim/tesseract/wiki) e durante a instalação marque o idioma **Portuguese**.

O caminho padrão dos dados de idioma após a instalação é:
```
C:\Users\<seu-usuario>\AppData\Local\Programs\Tesseract-OCR\tessdata
```

---

## Configuração

Edite o arquivo `src/main/resources/application.yaml` com as suas configurações locais:

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/identity_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: sua_senha_aqui

tesseract:
  datapath: C:/Users/<seu-usuario>/AppData/Local/Programs/Tesseract-OCR/tessdata
```

> O banco `identity_db` é criado automaticamente na primeira execução graças ao parâmetro `createDatabaseIfNotExist=true`.

---

## Banco de Dados

O schema é gerenciado pelos scripts SQL na pasta `sql/`. Execute-os manualmente na ordem caso prefira não usar o `ddl-auto: update` do Hibernate:

| Script | Descrição |
|---|---|
| `V0__create_tables.sql` | Cria o banco `identity_db` e as tabelas `tb_verificacao` e `processoverificacao` |
| `V1__alter_status_enum.sql` | Atualiza o ENUM da coluna `status` da `tb_verificacao` |

### Tabela `tb_verificacao`

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | `BINARY(16)` | UUID gerado automaticamente |
| `id_usuario` | `VARCHAR(255)` | Identificador do usuário no sistema externo |
| `email_responsavel` | `VARCHAR(255)` | E-mail do responsável legal |
| `status` | `ENUM` | Status atual da verificação |
| `data_criacao` | `DATETIME` | Data/hora de criação do registro |
| `cpf_responsavel` | `VARCHAR(14)` | CPF do responsável (opcional) |
| `motivo_rejeicao` | `VARCHAR(255)` | Motivo em caso de reprovação |

---

## Executando a Aplicação

```bash
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`.

---

## Endpoints

### `POST /api/verificacoes`

Cria uma nova verificação. Retorna `201 Created` com o objeto criado e o header `Location` apontando para o recurso.

**Request Body:**
```json
{
  "idUsuario": "usr_123",
  "emailResponsavel": "responsavel@email.com"
}
```

**Response `201 Created`:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "idUsuario": "usr_123",
  "emailResponsavel": "responsavel@email.com",
  "status": "AGUARDANDO_RESPONSAVEL",
  "dataCriacao": "2026-05-03T10:00:00"
}
```

---

### `POST /api/verificacoes/{id}/documento-responsavel`

Envia as imagens (frente e verso) do RG do responsável legal. Processa o OCR e retorna os dados extraídos. Atualiza o status para `AUTORIZADO_RESPONSAVEL`.

**Content-Type:** `multipart/form-data`

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `frente` | `MultipartFile` | Imagem da frente do RG |
| `verso` | `MultipartFile` | Imagem do verso do RG |

**Response `200 OK`:**
```json
{
  "status": "SUCESSO",
  "nome": "JOAO DA SILVA",
  "idade": 45,
  "cpf": "123.456.789-00",
  "dataNascimento": "15/03/1981",
  "naturalidade": "SAO PAULO SP",
  "nomePai": "JOSE DA SILVA",
  "nomeMae": "MARIA DA SILVA",
  "orgaoExpedidor": "SSP SP",
  "registroGeral": "38.123.456-7",
  "dataExpedicao": "10/06/2010",
  "textoExtraido": "..."
}
```

---

### `POST /api/verificacoes/{id}/documento-menor`

Envia as imagens do RG do menor de idade. Valida que a idade extraída é menor que 18 anos. Atualiza o status para `EM_ANALISE`.

**Content-Type:** `multipart/form-data`

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `frente` | `MultipartFile` | Imagem da frente do RG |
| `verso` | `MultipartFile` | Imagem do verso do RG |

**Response `200 OK`:** mesmo formato do endpoint anterior.

**Erros possíveis (`440`):**

| Situação | Mensagem |
|---|---|
| Idade não extraída | `Não foi possível determinar a idade do menor` |
| Idade negativa | `Idade inválida detectada no documento` |
| Idade >= 18 | `Documento não pertence a um menor de idade` |

```json
{
  "timestamp": "2026-05-03T10:00:00",
  "erro": "Documento não pertence a um menor de idade",
  "detalhe": "A idade extraída do documento é de 22 anos. Este endpoint aceita apenas documentos de menores de 18 anos."
}
```

> **Limite de upload:** 10 MB por arquivo e por requisição (configurável em `application.yaml`).

---

## Fluxo de Verificação

```
[Criar Verificação]
        │
        ▼
AGUARDANDO_RESPONSAVEL
        │
        │  POST /documento-responsavel
        ▼
AUTORIZADO_RESPONSAVEL
        │
        │  POST /documento-menor (menor < 18 anos)
        ▼
    EM_ANALISE
        │
        ├──► APROVADO
        │
        └──► REPROVADO
```

---

## Estrutura do Projeto

```
src/main/java/br/com/identityapi/
├── controller/
│   └── VerificationController.java      # Endpoints REST
├── domain/
│   ├── Verification.java                # Entidade JPA
│   └── VerificationStatus.java          # Enum de status
├── dto/
│   ├── VerificationRequest.java         # Request de criação
│   ├── DocumentoRgResponse.java         # Response do RG do responsável
│   └── DocumentoMenorResponse.java      # Response do RG do menor
├── exception/
│   ├── GlobalExceptionHandler.java      # Handler global de erros
│   └── IdadeInvalidaException.java      # Exceção de idade inválida
├── repository/
│   └── VerificationRepository.java      # Repositório JPA
├── service/
│   ├── DocumentoProcessamentoService.java  # Orquestra o processamento
│   ├── ImagePreProcessingService.java      # Pré-processamento de imagem
│   ├── OcrService.java                     # Integração com Tesseract
│   └── RgParserService.java                # Parser dos dados do RG
└── IdentityApiApplication.java          # Entry point
```

---

## Pipeline de Processamento de Documentos

Cada imagem enviada passa pelo seguinte pipeline antes da extração de dados:

```
Imagem recebida (MultipartFile)
        │
        ▼
1. Salva em arquivo temporário
        │
        ▼
2. ImagePreProcessingService
   ├── Converte para escala de cinza
   ├── Aumenta contraste (fator 1.8)
   ├── Corrige rotação (portrait → landscape)
   └── Amplia 2x com interpolação bicúbica
        │
        ▼
3. OcrService (Tesseract)
   ├── Idioma: Português (por)
   ├── Engine: LSTM (modo 1)
   └── Page Seg Mode: 3 (frente) / 4 (verso)
        │
        ▼
4. RgParserService
   └── Extrai campos via regex e busca por labels
        │
        ▼
5. Response com dados estruturados
```
