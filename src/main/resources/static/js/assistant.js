/* Bilingual, source-cited hospital assistant. Chat turns stay in the page and are not persisted. */
(function () {
  'use strict';

  const root = document.getElementById('chhAssistant');
  if (!root) return;

  const toggle = document.getElementById('chhAssistantToggle');
  const panel = document.getElementById('chhAssistantPanel');
  const close = document.getElementById('chhAssistantClose');
  const form = document.getElementById('chhAssistantForm');
  const input = document.getElementById('chhAssistantInput');
  const send = document.getElementById('chhAssistantSend');
  const messages = document.getElementById('chhAssistantMessages');
  const status = document.getElementById('chhAssistantStatus');
  const REQUEST_TIMEOUT_MS = 125000;
  const isMr = root.dataset.language === 'mr';
  const labels = isMr ? {
    emergency: 'तातडीची मदत',
    verified: 'रुग्णालयाने मंजूर केलेली माहिती',
    sample: 'ही नमुना माहिती आहे. रुग्णालयाकडून तपासून घ्या.',
    generalHealth: 'सामान्य आरोग्य माहिती; रुग्णालयाची विशिष्ट सूचना नाही',
    source: 'स्रोत',
    doctors: 'संबंधित डॉक्टर',
    profile: 'प्रोफाइल',
    book: 'बुकिंग सुरू करा',
    error: 'सध्या उत्तर मिळू शकले नाही. कृपया रुग्णालयाशी संपर्क साधा.',
    loading: 'माहिती शोधत आहे…'
  } : {
    emergency: 'Emergency help',
    verified: 'Hospital-approved information',
    sample: 'This is sample information. Please confirm it with hospital staff.',
    generalHealth: 'General health information; not hospital-specific advice',
    source: 'Sources',
    doctors: 'Related doctors',
    profile: 'Profile',
    book: 'Continue to booking',
    error: 'A reply is unavailable right now. Please contact the hospital.',
    loading: 'Checking hospital information…'
  };

  function setOpen(open) {
    panel.hidden = !open;
    toggle.setAttribute('aria-expanded', String(open));
    if (open) input.focus();
    else toggle.focus();
  }

  function addText(parent, tag, text, className) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    element.textContent = text || '';
    parent.appendChild(element);
    return element;
  }

  function addLink(parent, url, text, className) {
    if (!url) return;
    const local = url.startsWith('/') && !url.startsWith('//');
    let external = false;
    try {
      const parsed = new URL(url, window.location.origin);
      external = parsed.protocol === 'https:'
        && ['medlineplus.gov', 'www.cdc.gov'].includes(parsed.hostname);
    } catch (error) {
      return;
    }
    if (!local && !external) return;
    const link = document.createElement('a');
    link.href = url;
    link.className = className || '';
    link.textContent = text;
    if (external) {
      link.target = '_blank';
      link.rel = 'noopener noreferrer';
    }
    parent.appendChild(link);
  }

  function addUserMessage(text) {
    addText(messages, 'p', text, 'chh-assistant__user');
  }

  function addAssistantResponse(result) {
    const article = document.createElement('article');
    article.className = 'chh-assistant__reply';
    if (result.emergency) {
      article.classList.add('chh-assistant__reply--emergency');
      article.setAttribute('role', 'alert');
      addText(article, 'strong', labels.emergency, 'chh-assistant__emergency-title');
    } else if (result.sources && result.sources.some(source => source.category === 'general-health')) {
      addText(article, 'span', labels.generalHealth, 'chh-assistant__verification');
    } else if (result.verified) {
      addText(article, 'span', labels.verified, 'chh-assistant__verification');
    } else if (result.sources && result.sources.length) {
      addText(article, 'span', labels.sample, 'chh-assistant__verification');
    }

    addText(article, 'p', result.answer, 'chh-assistant__answer');
    if (result.emergency) {
      const call = document.createElement('a');
      call.href = 'tel:108';
      call.className = 'chh-btn chh-btn--danger';
      call.textContent = '108';
      article.appendChild(call);
      addLink(article, result.emergencyUrl, labels.emergency, 'chh-assistant__action');
    }

    if (result.sources && result.sources.length) {
      const section = document.createElement('div');
      addText(section, 'p', labels.source, 'chh-assistant__section-label');
      const list = document.createElement('ul');
      list.className = 'chh-assistant__sources';
      result.sources.forEach(source => {
        const item = document.createElement('li');
        addLink(item, source.url, source.title);
        list.appendChild(item);
      });
      section.appendChild(list);
      article.appendChild(section);
    }

    if (result.doctors && result.doctors.length) {
      const section = document.createElement('div');
      addText(section, 'p', labels.doctors, 'chh-assistant__section-label');
      const list = document.createElement('ul');
      list.className = 'chh-assistant__doctors';
      result.doctors.forEach(doctor => {
        const item = document.createElement('li');
        addLink(item, doctor.profileUrl, doctor.name);
        addText(item, 'span', doctor.speciality, 'chh-assistant__doctor-speciality');
        addLink(item, doctor.bookingUrl, labels.book, 'chh-assistant__action');
        list.appendChild(item);
      });
      section.appendChild(list);
      article.appendChild(section);
    }

    if (result.bookingUrl) addLink(article, result.bookingUrl, labels.book, 'chh-assistant__action');
    messages.appendChild(article);
    messages.scrollTop = messages.scrollHeight;
  }

  toggle.addEventListener('click', () => setOpen(panel.hidden));
  close.addEventListener('click', () => setOpen(false));
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !panel.hidden) setOpen(false);
  });

  form.addEventListener('submit', async event => {
    event.preventDefault();
    const message = input.value.trim();
    if (!message || send.disabled) return;

    addUserMessage(message);
    input.value = '';
    send.disabled = true;
    status.textContent = labels.loading;
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
    try {
      const response = await fetch('/api/v1/assistant/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ message, language: isMr ? 'mr' : 'en' }),
        signal: controller.signal
      });
      const body = await response.json();
      if (!response.ok || !body || !body.success || !body.data) throw new Error('Assistant request failed');
      addAssistantResponse(body.data);
      status.textContent = '';
    } catch (error) {
      addText(messages, 'p', labels.error, 'chh-assistant__reply');
      status.textContent = '';
    } finally {
      window.clearTimeout(timeout);
      send.disabled = false;
      input.focus();
    }
  });
})();
