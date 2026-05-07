/**
 * Identity Verify — Frontend
 *
 * Fluxo:
 *  0. Boas-vindas + escolha de modo (câmera ou arquivo)
 *  1. Step 1: envia frente + verso do RG do responsável
 *             → POST /api/verificacoes/{id}/documento-responsavel
 *  2. Step 2: exibe dados extraídos do responsável, envia RG do menor
 *             → POST /api/verificacoes/{id}/documento-menor
 *  3. Step 3: confirmação com dados do menor
 */

const API_BASE = 'http://localhost:8080';

// ─── Estado global ────────────────────────────────────────────────────────────

const state = {
  verificationId: null,
  modo: null, // 'camera' | 'arquivo'
  files: {
    'responsavel-frente': null,
    'responsavel-verso':  null,
    'menor-frente':       null,
    'menor-verso':        null,
  },
  // Câmera
  camera: {
    stream:        null,
    facingMode:    'environment', // traseira por padrão
    targetKey:     null,          // qual slot está sendo fotografado
    capturedBlob:  null,          // blob da foto tirada antes de confirmar
  },
};

// ─── Init ─────────────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
  const params = new URLSearchParams(window.location.search);
  state.verificationId = params.get('id');

  if (!state.verificationId) {
    showFatalError('Link inválido. O parâmetro de verificação está ausente. Verifique o link recebido por e-mail.');
    return;
  }

  setupForms();
});

// ─── Step 0: Boas-vindas ──────────────────────────────────────────────────────

function selecionarModo(modo) {
  state.modo = modo;

  // Destaca o botão selecionado
  document.querySelectorAll('.modo-btn').forEach((btn) => {
    btn.classList.remove('border-brand-500', 'bg-brand-50');
    btn.classList.add('border-gray-200');
  });

  const btnSelecionado = document.getElementById(`btn-modo-${modo}`);
  btnSelecionado.classList.remove('border-gray-200');
  btnSelecionado.classList.add('border-brand-500', 'bg-brand-50');

  document.getElementById('btn-comecar').disabled = false;
}

function comecar() {
  if (!state.modo) return;

  // Renderiza as áreas de upload de acordo com o modo escolhido
  const slots = [
    'responsavel-frente',
    'responsavel-verso',
    'menor-frente',
    'menor-verso',
  ];

  slots.forEach((key) => {
    const container = document.getElementById(`area-${key}`);
    if (!container) return;
    container.innerHTML = renderUploadArea(key);
  });

  if (state.modo === 'arquivo') {
    setupDragAndDrop();
  }

  goToStep(1);
}

// ─── Renderização das áreas de upload ─────────────────────────────────────────

/**
 * Retorna o HTML da área de upload para um slot,
 * adaptado ao modo escolhido (câmera ou arquivo).
 */
function renderUploadArea(key) {
  const labelMap = {
    'responsavel-frente': 'Frente do RG',
    'responsavel-verso':  'Verso do RG',
    'menor-frente':       'Frente do RG do menor',
    'menor-verso':        'Verso do RG do menor',
  };

  const previewHtml = `
    <div id="preview-${key}" class="hidden text-center">
      <img id="preview-img-${key}" class="mx-auto max-h-40 rounded-lg object-contain mb-2" alt="Preview" />
      <p id="preview-name-${key}" class="text-sm text-gray-500"></p>
      <button type="button" onclick="limparSlot('${key}')"
        class="mt-2 text-xs text-red-500 hover:text-red-700 underline">
        Remover e escolher outra
      </button>
    </div>
  `;

  if (state.modo === 'camera') {
    return `
      <div id="drop-${key}" class="border-2 border-dashed border-gray-300 rounded-xl p-6 text-center">
        ${previewHtml}
        <div id="placeholder-${key}">
          <svg class="w-10 h-10 text-gray-400 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
              d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <p class="text-sm text-gray-600 font-medium mb-3">Nenhuma foto tirada</p>
          <button type="button" onclick="abrirCamera('${key}')"
            class="inline-flex items-center gap-2 bg-brand-600 hover:bg-brand-700 text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            Abrir câmera
          </button>
        </div>
      </div>
    `;
  }

  // Modo arquivo
  return `
    <div id="drop-${key}"
      class="upload-area border-2 border-dashed border-gray-300 rounded-xl p-6 text-center cursor-pointer hover:border-brand-500 hover:bg-brand-50 transition-colors"
      onclick="document.getElementById('input-${key}').click()">
      ${previewHtml}
      <div id="placeholder-${key}">
        <svg class="w-10 h-10 text-gray-400 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        <p class="text-sm text-gray-600 font-medium">Clique para selecionar ou arraste a imagem</p>
        <p class="text-xs text-gray-400 mt-1">JPG, PNG ou WEBP — máx. 10 MB</p>
      </div>
    </div>
    <input id="input-${key}" type="file" accept="image/*" class="hidden"
      onchange="handleFileSelect(this, '${key}')" />
  `;
}

// ─── Câmera ───────────────────────────────────────────────────────────────────

async function abrirCamera(key) {
  state.camera.targetKey = key;
  state.camera.capturedBlob = null;

  const labelMap = {
    'responsavel-frente': 'Frente do RG — Responsável',
    'responsavel-verso':  'Verso do RG — Responsável',
    'menor-frente':       'Frente do RG — Menor',
    'menor-verso':        'Verso do RG — Menor',
  };

  document.getElementById('camera-modal-title').textContent = labelMap[key] || 'Fotografar documento';
  document.getElementById('camera-modal-subtitle').textContent = 'Centralize o documento no quadro e fotografe';

  // Mostra viewfinder, esconde preview
  document.getElementById('camera-video').classList.remove('hidden');
  document.getElementById('camera-preview-container').classList.add('hidden');
  document.getElementById('camera-controls-live').classList.remove('hidden');
  document.getElementById('camera-controls-preview').classList.add('hidden');

  document.getElementById('camera-modal').classList.remove('hidden');
  document.body.classList.add('overflow-hidden');

  await iniciarStream();
}

async function iniciarStream() {
  pararStream();

  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: {
        facingMode: state.camera.facingMode,
        width:  { ideal: 1920 },
        height: { ideal: 1080 },
      },
    });

    state.camera.stream = stream;
    const video = document.getElementById('camera-video');
    video.srcObject = stream;
  } catch (err) {
    fecharCamera();
    alert('Não foi possível acessar a câmera. Verifique as permissões do navegador ou use a opção "Enviar arquivo".');
    console.error('Erro ao acessar câmera:', err);
  }
}

function pararStream() {
  if (state.camera.stream) {
    state.camera.stream.getTracks().forEach((t) => t.stop());
    state.camera.stream = null;
  }
}

async function alternarCamera() {
  state.camera.facingMode = state.camera.facingMode === 'environment' ? 'user' : 'environment';
  await iniciarStream();
}

function tirarFoto() {
  const video  = document.getElementById('camera-video');
  const canvas = document.getElementById('camera-canvas');

  canvas.width  = video.videoWidth  || 1280;
  canvas.height = video.videoHeight || 720;

  const ctx = canvas.getContext('2d');
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

  canvas.toBlob((blob) => {
    state.camera.capturedBlob = blob;

    // Mostra preview da foto
    const previewImg = document.getElementById('camera-preview-img');
    previewImg.src = URL.createObjectURL(blob);

    document.getElementById('camera-video').classList.add('hidden');
    document.getElementById('camera-preview-container').classList.remove('hidden');
    document.getElementById('camera-controls-live').classList.add('hidden');
    document.getElementById('camera-controls-preview').classList.remove('hidden');
  }, 'image/jpeg', 0.92);
}

function repetirFoto() {
  state.camera.capturedBlob = null;
  document.getElementById('camera-video').classList.remove('hidden');
  document.getElementById('camera-preview-container').classList.add('hidden');
  document.getElementById('camera-controls-live').classList.remove('hidden');
  document.getElementById('camera-controls-preview').classList.add('hidden');
}

function confirmarFoto() {
  const key  = state.camera.targetKey;
  const blob = state.camera.capturedBlob;
  if (!blob || !key) return;

  // Converte blob para File para manter compatibilidade com o envio
  const file = new File([blob], `foto_${key}_${Date.now()}.jpg`, { type: 'image/jpeg' });
  applyFile(file, key);
  fecharCamera();
}

function fecharCamera() {
  pararStream();
  document.getElementById('camera-modal').classList.add('hidden');
  document.body.classList.remove('overflow-hidden');
  state.camera.targetKey    = null;
  state.camera.capturedBlob = null;
}

// ─── Drag & Drop (modo arquivo) ───────────────────────────────────────────────

function setupDragAndDrop() {
  const keys = ['responsavel-frente', 'responsavel-verso', 'menor-frente', 'menor-verso'];

  keys.forEach((key) => {
    const area = document.getElementById(`drop-${key}`);
    if (!area) return;

    area.addEventListener('dragover', (e) => {
      e.preventDefault();
      area.classList.add('drag-over');
    });
    area.addEventListener('dragleave', () => area.classList.remove('drag-over'));
    area.addEventListener('drop', (e) => {
      e.preventDefault();
      area.classList.remove('drag-over');
      const file = e.dataTransfer.files[0];
      if (file) applyFile(file, key);
    });
  });
}

// ─── File handling ────────────────────────────────────────────────────────────

function handleFileSelect(input, key) {
  const file = input.files[0];
  if (file) applyFile(file, key);
}

function applyFile(file, key) {
  if (!file.type.startsWith('image/')) {
    alert('Por favor, selecione um arquivo de imagem (JPG, PNG ou WEBP).');
    return;
  }
  if (file.size > 10 * 1024 * 1024) {
    alert('O arquivo excede o limite de 10 MB.');
    return;
  }

  state.files[key] = file;

  const preview     = document.getElementById(`preview-${key}`);
  const placeholder = document.getElementById(`placeholder-${key}`);
  const previewImg  = document.getElementById(`preview-img-${key}`);
  const previewName = document.getElementById(`preview-name-${key}`);

  const reader = new FileReader();
  reader.onload = (e) => {
    previewImg.src       = e.target.result;
    previewName.textContent = file.name;
    preview.classList.remove('hidden');
    placeholder.classList.add('hidden');
  };
  reader.readAsDataURL(file);
}

function limparSlot(key) {
  state.files[key] = null;

  const preview     = document.getElementById(`preview-${key}`);
  const placeholder = document.getElementById(`placeholder-${key}`);
  const previewImg  = document.getElementById(`preview-img-${key}`);

  if (previewImg) previewImg.src = '';
  preview.classList.add('hidden');
  placeholder.classList.remove('hidden');

  // Limpa o input file se existir (modo arquivo)
  const input = document.getElementById(`input-${key}`);
  if (input) input.value = '';
}

// ─── Forms ────────────────────────────────────────────────────────────────────

function setupForms() {
  document.getElementById('form-responsavel').addEventListener('submit', handleStep1Submit);
  document.getElementById('form-menor').addEventListener('submit', handleStep2Submit);
}

async function handleStep1Submit(e) {
  e.preventDefault();
  hideError('step1');

  const frente = state.files['responsavel-frente'];
  const verso  = state.files['responsavel-verso'];

  if (!frente || !verso) {
    showError('step1', 'Adicione as imagens da frente e do verso do RG antes de continuar.');
    return;
  }

  setLoading('step1', true);

  try {
    const formData = new FormData();
    formData.append('frente', frente);
    formData.append('verso', verso);

    const response = await fetch(
      `${API_BASE}/api/verificacoes/${state.verificationId}/documento-responsavel`,
      { method: 'POST', body: formData }
    );

    if (!response.ok) {
      const err = await parseError(response);
      throw new Error(err);
    }

    const data = await response.json();
    populateResponsavelCard(data);
    goToStep(2);

  } catch (err) {
    showError('step1', err.message || 'Erro ao enviar o documento. Tente novamente.');
  } finally {
    setLoading('step1', false);
  }
}

async function handleStep2Submit(e) {
  e.preventDefault();
  hideError('step2');

  const frente = state.files['menor-frente'];
  const verso  = state.files['menor-verso'];

  if (!frente || !verso) {
    showError('step2', 'Adicione as imagens da frente e do verso do RG do menor antes de continuar.');
    return;
  }

  setLoading('step2', true);

  try {
    const formData = new FormData();
    formData.append('frente', frente);
    formData.append('verso', verso);

    const response = await fetch(
      `${API_BASE}/api/verificacoes/${state.verificationId}/documento-menor`,
      { method: 'POST', body: formData }
    );

    if (!response.ok) {
      const err = await parseError(response);
      throw new Error(err);
    }

    const data = await response.json();
    populateMenorCard(data);
    goToStep(3);

  } catch (err) {
    showError('step2', err.message || 'Erro ao enviar o documento. Tente novamente.');
  } finally {
    setLoading('step2', false);
  }
}

// ─── UI helpers ───────────────────────────────────────────────────────────────

function goToStep(step) {
  document.querySelectorAll('.step-section').forEach((s) => s.classList.add('hidden'));
  document.getElementById(`step-${step}`).classList.remove('hidden');

  // Barra de progresso só aparece a partir do step 1
  const progressSection = document.getElementById('progress-section');
  if (step === 0) {
    progressSection.classList.add('hidden');
  } else {
    progressSection.classList.remove('hidden');
    const progress = { 1: '33%', 2: '66%', 3: '100%' };
    const labels   = { 1: 'Etapa 1 de 3', 2: 'Etapa 2 de 3', 3: 'Concluído' };
    document.getElementById('progress-bar').style.width = progress[step];
    document.getElementById('step-label').textContent   = labels[step];
  }

  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function setLoading(stepKey, loading) {
  const btn     = document.getElementById(`btn-${stepKey}`);
  const text    = document.getElementById(`btn-${stepKey}-text`);
  const spinner = document.getElementById(`btn-${stepKey}-spinner`);

  btn.disabled = loading;
  text.textContent = loading ? 'Processando...' : (stepKey === 'step1' ? 'Enviar e continuar' : 'Enviar e finalizar');
  spinner.classList.toggle('hidden', !loading);
}

function showError(stepKey, message) {
  const box = document.getElementById(`error-${stepKey}`);
  const msg = document.getElementById(`error-${stepKey}-msg`);
  msg.textContent = message;
  box.classList.remove('hidden');
}

function hideError(stepKey) {
  document.getElementById(`error-${stepKey}`).classList.add('hidden');
}

function showFatalError(message) {
  document.querySelector('main').innerHTML = `
    <div class="flex flex-col items-center justify-center py-20 text-center">
      <div class="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mb-4">
        <svg class="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <h2 class="text-xl font-bold text-gray-900 mb-2">Link inválido</h2>
      <p class="text-gray-500 max-w-sm">${message}</p>
    </div>
  `;
}

// ─── Data population ──────────────────────────────────────────────────────────

function populateResponsavelCard(data) {
  setText('resp-nome',       data.nome             || '—');
  setText('resp-cpf',        data.cpf              || '—');
  setText('resp-nascimento', data.dataNascimento    || '—');
  setText('resp-idade',      data.idade != null ? `${data.idade} anos` : '—');
}

function populateMenorCard(data) {
  setText('menor-nome',       data.nome             || '—');
  setText('menor-cpf',        data.cpf              || '—');
  setText('menor-nascimento', data.dataNascimento    || '—');
  setText('menor-idade',      data.idade != null ? `${data.idade} anos` : '—');
  setText('menor-rg',         data.registroGeral    || '—');
  setText('menor-orgao',      data.orgaoExpedidor   || '—');
}

function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value;
}

// ─── Error parsing ────────────────────────────────────────────────────────────

async function parseError(response) {
  try {
    const body = await response.json();
    if (body.camposFaltantes && body.camposFaltantes.length > 0) {
      const campos = body.camposFaltantes.map(traduzirCampo).join(', ');
      return `${body.erro}. Campos não extraídos: ${campos}. Verifique a qualidade das imagens e tente novamente.`;
    }
    if (body.erro) return `${body.erro}${body.detalhe ? ` — ${body.detalhe}` : ''}`;
    if (body.message) return body.message;
  } catch (_) { /* não era JSON */ }
  return `Erro ${response.status}: ${response.statusText}`;
}

function traduzirCampo(campo) {
  const map = {
    nome:           'Nome',
    cpf:            'CPF',
    dataNascimento: 'Data de nascimento',
    naturalidade:   'Naturalidade',
    nomePai:        'Nome do pai',
    nomeMae:        'Nome da mãe',
    orgaoExpedidor: 'Órgão expedidor',
    registroGeral:  'Registro Geral (RG)',
    dataExpedicao:  'Data de expedição',
  };
  return map[campo] || campo;
}
