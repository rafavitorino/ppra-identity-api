# Identity Web

Frontend do fluxo de verificação de identidade KYC. Página estática (HTML + Tailwind CSS + JavaScript puro) que guia o responsável legal pelo envio dos documentos RG — primeiro o seu próprio, depois o do menor.

## Como usar

Abra o `index.html` com o parâmetro `?id=` contendo o UUID da verificação criada pelo backend:

```
index.html?id=550e8400-e29b-41d4-a716-446655440000
```

Esse link é gerado pelo backend e enviado por e-mail ao responsável.

## Estrutura

```
identity-web/
├── index.html       # Página única com os 4 steps (boas-vindas + 3 etapas)
├── css/
│   └── style.css    # Estilos complementares ao Tailwind
└── js/
    └── app.js       # Lógica de câmera, upload, chamadas à API e navegação entre steps
```

## Configuração da API

Edite a constante `API_BASE` no topo de `js/app.js`:

```js
const API_BASE = 'http://localhost:8080';
```

## Executando localmente

```bash
cd identity-web
npx serve .
```

O frontend estará disponível em `http://localhost:3000`.

## Acesso via câmera em dispositivos móveis (HTTPS obrigatório)

A API de câmera do navegador (`getUserMedia`) só funciona em contextos seguros — **HTTPS** ou `localhost`. Ao acessar o frontend pelo IP da rede local via HTTP (ex: `http://192.168.x.x:3000`), o Chrome bloqueia o acesso à câmera mesmo com permissão concedida.

Para testar com câmera no celular, sirva o frontend com HTTPS usando `mkcert`:

### 1. Instalar o mkcert

```bash
# Windows (requer Chocolatey)
choco install mkcert
```

### 2. Criar a autoridade certificadora local

```bash
mkcert -install
```

### 3. Gerar o certificado para o IP da sua máquina

Substitua pelo IP exibido no seu terminal (ex: `ipconfig` no Windows):

```bash
mkcert 192.168.18.58
```

Isso gera dois arquivos na pasta atual:
- `192.168.18.58.pem` — certificado
- `192.168.18.58-key.pem` — chave privada

### 4. Servir com HTTPS

```bash
cd identity-web
npx serve . --ssl-cert 192.168.18.58.pem --ssl-key 192.168.18.58-key.pem
```

Acesse no celular:

```
https://192.168.18.58:3000/index.html?id={uuid}
```

> O celular precisa estar na mesma rede Wi-Fi que o computador. Na primeira vez, o browser pode exibir um aviso de certificado — clique em "Avançar" para prosseguir.

## Fluxo das telas

| Step | Descrição | Endpoint chamado |
|---|---|---|
| 0 | Boas-vindas, instruções e escolha do modo (câmera ou arquivo) | — |
| 1 | Envio do RG do responsável (frente + verso) | `POST /api/verificacoes/{id}/documento-responsavel` |
| 2 | Dados extraídos do responsável + envio do RG do menor | `POST /api/verificacoes/{id}/documento-menor` |
| 3 | Confirmação com dados extraídos do menor | — |

## Modos de envio

**Tirar foto** — abre a câmera do dispositivo com guia de enquadramento. Permite alternar entre câmera frontal e traseira, visualizar preview antes de confirmar e repetir a foto se necessário.

**Enviar arquivo** — seleciona uma imagem salva no dispositivo via clique ou drag & drop. Aceita JPG, PNG e WEBP até 10 MB.
