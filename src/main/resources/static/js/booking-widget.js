/* ============================================================================
   CHINCHE HOSPITAL — Multi-step booking widget
   ----------------------------------------------------------------------------
   Vanilla ES2020, no dependencies beyond Bootstrap (used only for toasts).
   Driven entirely by /api/v1 so the same markup works embedded elsewhere.

   State machine:  service → doctor → datetime → details → review → done
   ============================================================================ */
(function () {
  'use strict';

  const API = '/api/v1';
  const TOTAL_STEPS = 5;

  /* Services that require a specific doctor to be chosen. */
  const NEEDS_DOCTOR = new Set([
    'OPD_CONSULTATION', 'TELE_CONSULTATION', 'SECOND_OPINION', 'SURGERY_PLANNING'
  ]);
  const NEEDS_PACKAGE = new Set(['HEALTH_CHECKUP']);
  const DEFAULT_VISIT_MODE = {
    OPD_CONSULTATION: 'IN_HOSPITAL',
    TELE_CONSULTATION: 'VIDEO',
    SECOND_OPINION: 'IN_HOSPITAL',
    SURGERY_PLANNING: 'IN_HOSPITAL',
    HEALTH_CHECKUP: 'IN_HOSPITAL',
    LAB_TEST: 'IN_HOSPITAL',
    HOME_CARE: 'HOME_VISIT'
  };

  const state = {
    step: 1,
    serviceType: null,
    visitMode: 'IN_HOSPITAL',
    specialityId: null,
    facilityId: null,
    teleOnly: false,
    doctorId: null,
    healthPackageId: null,
    date: null,
    slotStart: null,
    slotEnd: null,
    paymentMode: 'PAY_AT_HOSPITAL',
    availability: [],
    doctors: [],
    packages: [],
    submitting: false
  };

  let data = null;
  const $ = (sel, root) => (root || document).querySelector(sel);
  const $$ = (sel, root) => Array.from((root || document).querySelectorAll(sel));

  /* ---------------------------------------------------------------- helpers */
  const t = (key) => (data && data.i18n && data.i18n[key]) || key;
  const isMr = () => !!(data && data.i18n && data.i18n.marathi);

  function money(v) {
    if (v === null || v === undefined || v === '') return '—';
    const n = Number(v);
    if (!n) return isMr() ? 'मोफत' : 'Free';
    return '₹' + n.toLocaleString('en-IN');
  }

  function initials(name) {
    if (!name) return '?';
    const clean = name.replace(/^Dr\.?\s*/i, '').trim();
    const parts = clean.split(/\s+/);
    return ((parts[0] || '')[0] || '') + ((parts[1] || '')[0] || '');
  }

  function fmtDate(iso) {
    const d = new Date(iso + 'T00:00:00');
    return d.toLocaleDateString(isMr() ? 'mr-IN' : 'en-IN', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    });
  }

  function fmtTime(hhmm) {
    if (!hhmm) return '';
    const [h, m] = hhmm.split(':').map(Number);
    const d = new Date(); d.setHours(h, m, 0, 0);
    return d.toLocaleTimeString(isMr() ? 'mr-IN' : 'en-IN', { hour: 'numeric', minute: '2-digit' });
  }

  function el(tag, cls, text) {
    const n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text !== undefined && text !== null) n.textContent = text;
    return n;
  }

  function svgIcon(id, cls) {
    return '<svg class="' + (cls || '') + '"><use href="/img/icons/sprite.svg#' + id + '"></use></svg>';
  }

  async function api(path, options) {
    const res = await fetch(API + path, Object.assign({
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' }
    }, options || {}));
    let body = null;
    try { body = await res.json(); } catch (e) { /* non-JSON */ }
    if (!res.ok) {
      const msg = (body && body.message) || ('Request failed (' + res.status + ')');
      const err = new Error(msg);
      err.payload = body;
      err.status = res.status;
      throw err;
    }
    return body;
  }

  function showError(message) {
    const box = $('#chhGlobalError');
    const txt = $('#chhGlobalErrorText');
    if (!box || !txt) return;
    if (!message) { box.style.display = 'none'; txt.textContent = ''; return; }
    txt.textContent = message;
    box.style.display = 'flex';
  }

  function fieldError(name, message) {
    const slot = $('[data-error-for="' + name + '"]');
    const input = $('[name="' + name + '"]');
    if (slot) { slot.textContent = message || ''; slot.classList.toggle('show', !!message); }
    if (input) input.classList.toggle('is-invalid', !!message);
  }

  function clearFieldErrors() {
    $$('.chh-error').forEach(e => { e.textContent = ''; e.classList.remove('show'); });
    $$('.chh-input.is-invalid, .chh-select.is-invalid').forEach(e => e.classList.remove('is-invalid'));
  }

  /* ------------------------------------------------------- step navigation */
  function canAdvance() {
    switch (state.step) {
      case 1: return !!state.serviceType;
      case 2: return NEEDS_PACKAGE.has(state.serviceType) ? !!state.healthPackageId : !!state.doctorId;
      case 3: return !!state.date && !!state.slotStart;
      case 4: return validateDetails(false);
      case 5: return true;
      default: return false;
    }
  }

  function renderStep() {
    $$('.chh-step-pane').forEach(p => p.classList.toggle('is-active', Number(p.dataset.pane) === state.step));
    $$('#chhSteps .chh-steps__item').forEach(item => {
      const n = Number(item.dataset.step);
      item.classList.toggle('is-active', n === state.step);
      item.classList.toggle('is-done', n < state.step);
    });

    const counter = $('#chhStepCounter b');
    if (counter) counter.textContent = Math.min(state.step, TOTAL_STEPS);

    const back = $('#chhBack');
    const next = $('#chhNext');
    const foot = $('#chhFoot');
    if (back) back.style.visibility = (state.step > 1 && state.step <= TOTAL_STEPS) ? 'visible' : 'hidden';
    if (next) {
      next.disabled = !canAdvance();
      const label = next.querySelector('span');
      if (label) {
        if (state.step === TOTAL_STEPS) label.textContent = t('confirm');
        else if (state.step === 6) label.textContent = t('newBooking');
        else label.textContent = t('next');
      }
      next.style.display = state.step === 6 ? 'none' : '';
    }
    if (foot) foot.style.display = state.step === 6 ? 'none' : '';
    showError(null);

    if (state.step === 2) renderStep2();
    if (state.step === 3) renderStep3();
    if (state.step === 5) renderReview();

    const shell = $('.chh-booking__shell');
    if (shell && window.innerWidth < 992) {
      shell.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  function goTo(step) {
    if (step < 1) return;
    if (step > state.step && !canAdvance()) return;
    state.step = step;
    renderStep();
  }

  /* ------------------------------------------------------------- STEP 1 */
  function initStep1() {
    const grid = $('#chhServiceGrid');
    if (!grid) return;

    grid.addEventListener('click', (e) => {
      const btn = e.target.closest('.chh-service-opt');
      if (!btn) return;
      $$('.chh-service-opt', grid).forEach(b => {
        b.classList.remove('is-selected');
        b.setAttribute('aria-checked', 'false');
        b.setAttribute('aria-pressed', 'false');
      });
      btn.classList.add('is-selected');
      btn.setAttribute('aria-checked', 'true');
      btn.setAttribute('aria-pressed', 'true');
      state.serviceType = btn.dataset.value;
      state.visitMode = DEFAULT_VISIT_MODE[state.serviceType] || 'IN_HOSPITAL';
      // Reset downstream choices when the service changes.
      state.doctorId = null; state.healthPackageId = null;
      state.date = null; state.slotStart = null;

      syncVisitMode();
      renderStep();
    });

    const vmWrap = $('#chhVisitModeWrap');
    $('#chhVisitMode').addEventListener('click', (e) => {
      const btn = e.target.closest('[data-value]');
      if (!btn) return;
      if (btn.dataset.value === 'VIDEO' && !doctorSupportsVideo()) {
        showError(t('teleconsult'));
        return;
      }
      $$('#chhVisitMode [data-value]').forEach(b => {
        b.classList.remove('chh-chip');
        b.classList.add('chh-chip', 'chh-chip--outline');
        b.setAttribute('aria-checked', 'false');
      });
      btn.classList.remove('chh-chip--outline');
      btn.setAttribute('aria-checked', 'true');
      state.visitMode = btn.dataset.value;
      state.date = null; state.slotStart = null;
    });

    if (vmWrap) vmWrap.style.display = 'flex';

    // Deep-link presets: /book?service=TELE_CONSULTATION
    const preset = $('#chhBookingForm')?.dataset.presetService;
    if (preset) {
      const btn = grid.querySelector('[data-value="' + preset + '"]');
      if (btn) btn.click();
    }
  }

  function syncVisitMode() {
    const wrap = $('#chhVisitModeWrap');
    if (!wrap) return;
    const needsMode = ['OPD_CONSULTATION', 'SECOND_OPINION', 'SURGERY_PLANNING'].includes(state.serviceType);
    wrap.style.display = needsMode ? 'block' : 'none';
    if (!needsMode) state.visitMode = DEFAULT_VISIT_MODE[state.serviceType] || 'IN_HOSPITAL';

    $$('#chhVisitMode [data-value]').forEach(b => {
      const isSel = b.dataset.value === state.visitMode;
      b.classList.toggle('chh-chip--outline', !isSel);
      b.setAttribute('aria-checked', String(isSel));
      const disabled = b.dataset.value === 'VIDEO' && !doctorSupportsVideo();
      b.style.opacity = disabled ? '.45' : '';
    });
  }

  function doctorSupportsVideo() {
    if (state.serviceType === 'TELE_CONSULTATION') return true;
    if (!state.doctorId) return true;
    const d = state.doctors.find(x => String(x.id) === String(state.doctorId));
    return !d || d.teleconsultEnabled;
  }

  /* ------------------------------------------------------------- STEP 2 */
  function initStep2() {
    const specSel = $('#chhSpecialitySelect');
    const facSel = $('#chhFacilitySelect');
    const tele = $('#chhTeleOnly');

    specSel && specSel.addEventListener('change', () => {
      state.specialityId = specSel.value || null;
      state.doctorId = null;
      renderDoctorList();
    });
    facSel && facSel.addEventListener('change', () => {
      state.facilityId = facSel.value || null;
      state.doctorId = null;
      renderDoctorList();
    });
    tele && tele.addEventListener('change', () => {
      state.teleOnly = tele.checked;
      state.doctorId = null;
      renderDoctorList();
    });

    $('#chhDoctorList').addEventListener('click', (e) => {
      const btn = e.target.closest('.chh-doctor-opt');
      if (!btn) return;
      $$('.chh-doctor-opt').forEach(b => { b.classList.remove('is-selected'); b.setAttribute('aria-pressed', 'false'); });
      btn.classList.add('is-selected');
      btn.setAttribute('aria-pressed', 'true');
      state.doctorId = Number(btn.dataset.id);
      const doc = state.doctors.find(d => String(d.id) === btn.dataset.id);
      if (doc) {
        state.specialityId = doc.specialityId || state.specialityId;
        state.facilityId = doc.facilityId || state.facilityId;
      }
      state.date = null; state.slotStart = null;
      renderStep();
    });

    $('#chhPackageList').addEventListener('click', (e) => {
      const btn = e.target.closest('.chh-doctor-opt');
      if (!btn) return;
      $$('#chhPackageList .chh-doctor-opt').forEach(b => { b.classList.remove('is-selected'); b.setAttribute('aria-pressed', 'false'); });
      btn.classList.add('is-selected');
      btn.setAttribute('aria-pressed', 'true');
      state.healthPackageId = Number(btn.dataset.id);
      renderStep();
    });
  }

  function renderStep2() {
    const isPkg = NEEDS_PACKAGE.has(state.serviceType);
    $('#chhPackagePicker').style.display = isPkg ? 'block' : 'none';
    $('#chhDoctorPicker').style.display = isPkg ? 'none' : 'block';

    if (isPkg) renderPackageList(); else renderDoctorList();

    if (specSelValue() !== state.specialityId) {
      const s = $('#chhSpecialitySelect');
      if (s) s.value = state.specialityId || '';
    }
  }

  function specSelValue() {
    const s = $('#chhSpecialitySelect');
    return s && s.value ? Number(s.value) : null;
  }

  function renderPackageList() {
    const list = $('#chhPackageList');
    list.innerHTML = '';
    state.packages.forEach(p => {
      const btn = el('button', 'chh-doctor-opt');
      btn.type = 'button';
      btn.dataset.id = p.id;
      btn.setAttribute('aria-pressed', String(state.healthPackageId === p.id));
      if (state.healthPackageId === p.id) btn.classList.add('is-selected');

      const av = el('div', 'chh-doctor-opt__avatar');
      av.innerHTML = svgIcon(p.iconKey || 'shield');
      const main = el('div', 'chh-doctor-opt__main');
      main.appendChild(el('div', 'chh-doctor-opt__name', isMr() && p.nameMr ? p.nameMr : p.name));
      const meta = el('div', 'chh-doctor-opt__meta');
      meta.appendChild(el('span', null, (p.testCount || 0) + ' ' + t('slots').replace('slots', 'tests')));
      if (p.tier) meta.appendChild(el('span', 'chh-chip chh-chip--accent', p.tier));
      if (p.homeCollection) meta.appendChild(el('span', null, '🏠 Home collection'));
      main.appendChild(meta);

      const fee = el('div', 'chh-doctor-opt__fee');
      fee.appendChild(el('b', null, money(p.price)));
      if (p.listPrice && Number(p.listPrice) > Number(p.price)) {
        const was = el('span', null, money(p.listPrice));
        was.style.textDecoration = 'line-through';
        fee.appendChild(was);
      }

      btn.append(av, main, fee);
      list.appendChild(btn);
    });
  }

  function renderDoctorList() {
    const list = $('#chhDoctorList');
    const count = $('#chhDoctorCount');
    list.innerHTML = '';

    let rows = state.doctors.slice();
    if (state.serviceType === 'TELE_CONSULTATION' || state.teleOnly) {
      rows = rows.filter(d => d.teleconsultEnabled);
    }
    if (state.specialityId) rows = rows.filter(d => String(d.specialityId) === String(state.specialityId));
    if (state.facilityId) rows = rows.filter(d => String(d.facilityId) === String(state.facilityId));

    if (count) count.textContent = '· ' + rows.length + ' ' + t('doctor');

    if (!rows.length) {
      const empty = el('div', 'chh-empty');
      empty.innerHTML = '<span class="chh-empty__icon">' + svgIcon('user') + '</span>' +
        '<div>' + t('noSlots') + '</div>';
      list.appendChild(empty);
      return;
    }

    rows.forEach(d => {
      const btn = el('button', 'chh-doctor-opt');
      btn.type = 'button';
      btn.dataset.id = d.id;
      btn.setAttribute('aria-pressed', String(state.doctorId === d.id));
      if (state.doctorId === d.id) btn.classList.add('is-selected');

      const av = el('div', 'chh-doctor-opt__avatar', initials(d.name));
      const main = el('div', 'chh-doctor-opt__main');
      main.appendChild(el('div', 'chh-doctor-opt__name', d.name));

      const meta = el('div', 'chh-doctor-opt__meta');
      if (d.specialityName) meta.appendChild(el('span', null, d.specialityName));
      if (d.experienceYears) meta.appendChild(el('span', null, d.experienceYears + ' ' + t('years')));
      if (d.rating) meta.appendChild(el('span', null, '★ ' + Number(d.rating).toFixed(1)));
      if (d.teleconsultEnabled) meta.appendChild(el('span', 'chh-chip chh-chip--outline', t('teleconsult')));
      main.appendChild(meta);
      if (d.qualifications) main.appendChild(el('div', 'chh-doctor-opt__meta', d.qualifications));

      const fee = el('div', 'chh-doctor-opt__fee');
      fee.appendChild(el('b', null, money(d.consultationFee)));
      fee.appendChild(el('span', null, t('fee')));

      btn.append(av, main, fee);
      list.appendChild(btn);
    });
  }

  /* ------------------------------------------------------------- STEP 3 */
  async function renderStep3() {
    const nameEl = $('#chhSlotDoctorName');
    if (NEEDS_PACKAGE.has(state.serviceType)) {
      const p = state.packages.find(x => x.id === state.healthPackageId);
      if (nameEl) nameEl.textContent = p ? p.name : '';
      renderLocalCalendar(null);
      return;
    }
    const doc = state.doctors.find(d => String(d.id) === String(state.doctorId));
    if (nameEl) nameEl.textContent = doc ? doc.name + ' · ' + (doc.specialityName || '') : '';
    if (!doc) return;

    const loading = $('#chhSlotLoading');
    const strip = $('#chhDateStrip');
    if (loading) loading.style.display = 'flex';
    if (strip) strip.innerHTML = '';

    try {
      const res = await api('/availability/doctors/' + doc.id + '?days=16&visitMode=' + state.visitMode);
      state.availability = (res && res.data) || [];
    } catch (err) {
      state.availability = [];
      showError(err.message);
    } finally {
      if (loading) loading.style.display = 'none';
    }
    renderLocalCalendar(doc);
  }

  /** Fallback calendar for bookings that are not doctor-specific (packages, lab). */
  function renderLocalCalendar(doctor) {
    const strip = $('#chhDateStrip');
    strip.innerHTML = '';

    const days = doctor ? state.availability : buildGenericDays();
    if (!days.length) return;

    if (!state.date || !days.some(d => d.date === state.date)) {
      const firstOpen = days.find(d => d.clinicOpen);
      state.date = (firstOpen || days[0]).date;
      state.slotStart = null;
    }

    days.forEach(day => {
      const d = new Date(day.date + 'T00:00:00');
      const openSlots = (day.slots || []).filter(s => s.available).length;

      const chip = el('button', 'chh-datechip');
      chip.type = 'button';
      chip.dataset.date = day.date;
      chip.disabled = !day.clinicOpen || openSlots === 0;
      chip.setAttribute('aria-pressed', String(state.date === day.date));
      if (state.date === day.date) chip.classList.add('is-selected');

      chip.appendChild(el('div', 'chh-datechip__day', d.toLocaleDateString(isMr() ? 'mr-IN' : 'en-IN', { weekday: 'short' })));
      chip.appendChild(el('div', 'chh-datechip__num', String(d.getDate())));
      chip.appendChild(el('div', 'chh-datechip__mon', d.toLocaleDateString(isMr() ? 'mr-IN' : 'en-IN', { month: 'short' })));
      chip.appendChild(el('div', 'chh-datechip__slots',
        chip.disabled ? (day.reason ? t('full') : '—') : openSlots + ' ' + t('slots')));

      chip.addEventListener('click', () => {
        state.date = day.date;
        state.slotStart = null;
        $$('.chh-datechip').forEach(c => {
          c.classList.remove('is-selected');
          c.setAttribute('aria-pressed', 'false');
        });
        chip.classList.add('is-selected');
        chip.setAttribute('aria-pressed', 'true');
        renderSlots(day);
        renderStep();
      });
      strip.appendChild(chip);
    });

    const selected = days.find(d => d.date === state.date);
    if (selected) renderSlots(selected);
    renderStepButtonsOnly();
  }

  function buildGenericDays() {
    const out = [];
    const today = new Date();
    for (let i = 1; i <= 16; i++) {
      const d = new Date(today.getTime() + i * 86400000);
      const iso = d.toISOString().slice(0, 10);
      const slots = [];
      ['07:30', '08:30', '09:30', '10:30', '11:30', '16:30', '17:30', '18:30'].forEach((tm, idx) => {
        const [h, m] = tm.split(':').map(Number);
        const end = new Date(d); end.setHours(h, m + 60, 0, 0);
        slots.push({
          start: tm,
          end: String(end.getHours()).padStart(2, '0') + ':' + String(end.getMinutes()).padStart(2, '0'),
          available: true,
          period: h < 12 ? 'MORNING' : (h < 17 ? 'AFTERNOON' : 'EVENING')
        });
      });
      out.push({ date: iso, clinicOpen: true, slots: slots });
    }
    return out;
  }

  function renderSlots(day) {
    const area = $('#chhSlotGroups');
    const empty = $('#chhSlotEmpty');
    area.innerHTML = '';

    const slots = day.slots || [];
    const open = slots.filter(s => s.available);
    if (!open.length) { empty.style.display = 'flex'; return; }
    empty.style.display = 'none';

    const groups = { MORNING: [], AFTERNOON: [], EVENING: [] };
    slots.forEach(s => (groups[s.period] || groups.MORNING).push(s));

    Object.keys(groups).forEach(period => {
      if (!groups[period].length) return;
      const wrap = el('div', 'chh-slotgroup');
      const head = el('div', 'chh-slotgroup__head');
      head.appendChild(el('h4', null, t(period.toLowerCase())));
      const free = groups[period].filter(s => s.available).length;
      head.appendChild(el('span', 'chh-tiny', free + ' ' + t('slots')));
      wrap.appendChild(head);

      const grid = el('div', 'chh-slotgrid');
      groups[period].forEach(s => {
        const b = el('button', 'chh-slot');
        b.type = 'button';
        b.textContent = fmtTime(s.start);
        b.disabled = !s.available;
        b.dataset.start = s.start;
        b.dataset.end = s.end;
        const isSel = state.date === day.date && state.slotStart === s.start;
        b.setAttribute('aria-pressed', String(isSel));
        if (isSel) b.classList.add('is-selected');
        b.addEventListener('click', () => {
          state.slotStart = s.start;
          state.slotEnd = s.end;
          state.date = day.date;
          $$('.chh-slot').forEach(x => { x.classList.remove('is-selected'); x.setAttribute('aria-pressed', 'false'); });
          b.classList.add('is-selected');
          b.setAttribute('aria-pressed', 'true');
          renderStepButtonsOnly();
        });
        grid.appendChild(b);
      });
      wrap.appendChild(grid);
      area.appendChild(wrap);
    });
  }

  function renderStepButtonsOnly() {
    const next = $('#chhNext');
    if (next) next.disabled = !canAdvance();
  }

  function initStep3() {
    $('#chhDateStrip').addEventListener('click', () => renderStepButtonsOnly());
  }

  /* ------------------------------------------------------------- STEP 4 */
  function validateDetails(showMessages) {
    clearFieldErrors();
    let ok = true;
    const name = $('#chhPatientName').value.trim();
    const phone = $('#chhPatientPhone').value.trim();
    const email = $('#chhPatientEmail').value.trim();
    const age = $('#chhPatientAge').value;

    if (!name) { ok = false; if (showMessages) fieldError('patientName', 'Please enter the patient name.'); }
    if (!/^[6-9]\d{9}$/.test(phone)) {
      ok = false;
      if (showMessages) fieldError('patientPhone', 'Enter a valid 10-digit Indian mobile number.');
    }
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      ok = false;
      if (showMessages) fieldError('patientEmail', 'Enter a valid email address.');
    }
    if (age && (Number(age) < 0 || Number(age) > 120)) {
      ok = false;
      if (showMessages) fieldError('patientAge', 'Enter a valid age.');
    }
    return ok;
  }

  function initStep4() {
    ['chhPatientName', 'chhPatientPhone', 'chhPatientEmail', 'chhPatientAge'].forEach(id => {
      const input = $('#' + id);
      if (input) input.addEventListener('input', () => { fieldError(input.name, ''); renderStepButtonsOnly(); });
    });
    const phone = $('#chhPatientPhone');
    if (phone) phone.addEventListener('input', () => { phone.value = phone.value.replace(/\D/g, '').slice(0, 10); });
  }

  /* ------------------------------------------------------------- STEP 5 */
  function currentDoctor() { return state.doctors.find(d => String(d.id) === String(state.doctorId)) || null; }
  function currentPackage() { return state.packages.find(p => p.id === state.healthPackageId) || null; }

  function payableAmount() {
    const pkg = currentPackage();
    if (pkg) return pkg.price;
    const doc = currentDoctor();
    if (doc && doc.consultationFee) return doc.consultationFee;
    return 0;
  }

  function renderReview() {
    const wrap = $('#chhReview');
    if (!wrap) return;
    wrap.innerHTML = '';
    const doc = currentDoctor();
    const pkg = currentPackage();

    const cards = [
      { label: t('service'), value: serviceLabel(), sub: visitLabel() },
      doc ? { label: t('doctor'), value: doc.name, sub: [doc.designation, doc.specialityName].filter(Boolean).join(' · ') }
          : { label: t('choosePackage'), value: pkg ? (isMr() && pkg.nameMr ? pkg.nameMr : pkg.name) : '—', sub: pkg ? pkg.tier : '' },
      { label: t('date'), value: state.date ? fmtDate(state.date) : '—', sub: '' },
      { label: t('time'), value: state.slotStart ? fmtTime(state.slotStart) + ' – ' + fmtTime(state.slotEnd) : '—', sub: '' },
      { label: t('centre'), value: facilityName(doc), sub: facilityAddress(doc) },
      { label: t('patient'), value: $('#chhPatientName').value.trim() || '—',
        sub: [$('#chhPatientPhone').value.trim(), $('#chhPatientAge').value ? $('#chhPatientAge').value + ' yrs' : '']
          .filter(Boolean).join(' · ') },
      { label: t('fee'), value: money(payableAmount()), sub: state.paymentMode }
    ];

    cards.forEach(c => {
      const box = el('div', 'chh-review__card');
      box.appendChild(el('div', 'chh-review__label', c.label));
      box.appendChild(el('div', 'chh-review__value', c.value));
      if (c.sub) box.appendChild(el('div', 'chh-review__sub', c.sub));
      wrap.appendChild(box);
    });
  }

  function serviceLabel() {
    const btn = $('#chhServiceGrid [data-value="' + state.serviceType + '"] .chh-service-opt__name');
    return btn ? btn.textContent.trim() : state.serviceType || '—';
  }

  function visitLabel() {
    const map = { IN_HOSPITAL: 'At hospital', VIDEO: 'Video call', HOME_VISIT: 'Home visit' };
    return map[state.visitMode] || state.visitMode;
  }

  function facilityName(doc) {
    if (doc && doc.facilityName) return doc.facilityName;
    const sel = $('#chhFacilitySelect');
    if (sel && sel.value) return sel.options[sel.selectedIndex].textContent;
    return 'Chinche Hospital, Katol';
  }

  function facilityAddress(doc) {
    if (doc && doc.facilityCity) return doc.facilityCity + ', Maharashtra';
    return 'Mulai Road, Katol 441302';
  }

  function initStep5() {
    $('#chhPaymentMode').addEventListener('click', (e) => {
      const btn = e.target.closest('[data-value]');
      if (!btn) return;
      $$('#chhPaymentMode [data-value]').forEach(b => {
        b.classList.add('chh-chip--outline');
        b.setAttribute('aria-checked', 'false');
      });
      btn.classList.remove('chh-chip--outline');
      btn.setAttribute('aria-checked', 'true');
      state.paymentMode = btn.dataset.value;
      renderReview();
    });
  }

  /* ------------------------------------------------------------- SUBMIT */
  async function submitBooking() {
    if (state.submitting) return;
    if (!validateDetails(true)) { goTo(4); return; }

    const doc = currentDoctor();
    const payload = {
      serviceType: state.serviceType,
      visitMode: state.visitMode,
      specialityId: state.specialityId || (doc ? doc.specialityId : null),
      facilityId: state.facilityId || (doc ? doc.facilityId : null),
      doctorId: state.doctorId,
      healthPackageId: state.healthPackageId,
      appointmentDate: state.date,
      slotStart: state.slotStart,
      patientName: $('#chhPatientName').value.trim(),
      patientPhone: $('#chhPatientPhone').value.trim(),
      patientEmail: $('#chhPatientEmail').value.trim() || null,
      patientGender: $('#chhPatientGender').value || null,
      patientAge: $('#chhPatientAge').value ? Number($('#chhPatientAge').value) : null,
      address: $('#chhPatientAddress').value.trim() || null,
      city: $('#chhPatientCity').value.trim() || null,
      pincode: null,
      symptoms: $('#chhSymptoms').value.trim() || null,
      allergies: null,
      consentSms: $('#chhConsentSms').checked,
      consentEmail: $('#chhConsentEmail').checked,
      paymentMode: state.paymentMode
    };

    const next = $('#chhNext');
    state.submitting = true;
    next.disabled = true;
    next.innerHTML = '<span class="chh-spinner"></span> <span>' + t('loading') + '</span>';

    try {
      const res = await api('/bookings', { method: 'POST', body: JSON.stringify(payload) });
      renderSuccess(res.data);
      state.step = 6;
      renderStep();
    } catch (err) {
      showError(err.message);
      // A lost race on the slot: send the user back to pick a fresh time.
      if (err.status === 409) {
        state.slotStart = null;
        goTo(3);
        renderStep3();
      }
      next.disabled = false;
      next.innerHTML = '<span>' + t('confirm') + '</span> ' + svgIcon('arrow-right');
    } finally {
      state.submitting = false;
    }
  }

  function renderSuccess(c) {
    const host = $('#chhSuccess');
    if (!host || !c) return;
    host.innerHTML = '';

    const tick = el('div', 'chh-success__tick');
    tick.innerHTML = svgIcon('check');
    host.appendChild(tick);

    host.appendChild(el('h3', null, t('successTitle')));
    host.appendChild(el('p', 'chh-lead mt-2', c.preparationNote || t('successNote')));

    const ref = el('div', 'chh-reference');
    ref.appendChild(el('span', null, c.referenceNo));
    const copy = el('button', null, 'Copy');
    copy.type = 'button';
    copy.addEventListener('click', async () => {
      try { await navigator.clipboard.writeText(c.referenceNo); copy.textContent = t('copied'); }
      catch (e) { copy.textContent = c.referenceNo; }
      setTimeout(() => { copy.textContent = 'Copy'; }, 1800);
    });
    ref.appendChild(copy);
    host.appendChild(ref);

    const grid = el('div', 'chh-review mt-4');
    [
      { l: t('doctor'), v: c.doctorName || '—', s: c.doctorDesignation || '' },
      { l: t('service'), v: c.serviceTypeLabel || '', s: c.visitModeLabel || '' },
      { l: t('date'), v: c.appointmentDate ? fmtDate(c.appointmentDate) : '—', s: '' },
      { l: t('time'), v: fmtTime(c.slotStart) + ' – ' + fmtTime(c.slotEnd), s: '' },
      { l: t('centre'), v: c.facilityName || '—', s: c.facilityAddress || '' },
      { l: t('patient'), v: c.patientName || '—', s: c.status || '' },
      { l: t('fee'), v: money(c.amountPayable), s: c.paymentMode || '' }
    ].forEach(item => {
      const box = el('div', 'chh-review__card');
      box.appendChild(el('div', 'chh-review__label', item.l));
      box.appendChild(el('div', 'chh-review__value', item.v));
      if (item.s) box.appendChild(el('div', 'chh-review__sub', item.s));
      grid.appendChild(box);
    });
    host.appendChild(grid);

    const actions = el('div', 'chh-row mt-4');
    actions.style.justifyContent = 'center';

    const print = el('a', 'chh-btn chh-btn--outline', t('print'));
    print.href = '#';
    print.addEventListener('click', (e) => { e.preventDefault(); window.print(); });

    const wa = el('a', 'chh-btn chh-btn--accent');
    wa.innerHTML = svgIcon('whatsapp') + '<span>' + t('whatsapp') + '</span>';
    wa.target = '_blank';
    wa.rel = 'noopener';
    wa.href = 'https://wa.me/919422100108?text=' + encodeURIComponent(
      'Chinche Hospital booking ' + c.referenceNo + ' — ' + (c.doctorName || '') + ' on ' + c.appointmentDate);

    const track = el('a', 'chh-btn chh-btn--ghost');
    track.href = c.trackingUrl || '/track';
    track.innerHTML = svgIcon('search-doc') + '<span>Track</span>';

    actions.append(print, wa, track);
    host.appendChild(actions);
  }

  /* ------------------------------------------------------------- wiring */
  function initChrome() {
    const next = $('#chhNext');
    const back = $('#chhBack');

    next && next.addEventListener('click', () => {
      if (state.step === TOTAL_STEPS) { submitBooking(); return; }
      if (state.step === 4 && !validateDetails(true)) return;
      if (!canAdvance()) { showError(t('selectToContinue')); return; }
      goTo(state.step + 1);
    });

    back && back.addEventListener('click', () => goTo(state.step - 1));

    $$('#chhSteps .chh-steps__item').forEach(item => {
      item.addEventListener('click', () => {
        const n = Number(item.dataset.step);
        if (item.classList.contains('is-done')) goTo(n);
      });
    });

    document.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && e.target.tagName !== 'TEXTAREA' && $('#chhBookingForm')) {
        const form = $('#chhBookingForm');
        if (form.contains(e.target)) { e.preventDefault(); next && next.click(); }
      }
    });
  }

  function boot() {
    const island = $('#chhBookingData');
    if (!island) return;
    try { data = JSON.parse(island.textContent); }
    catch (e) { console.error('Booking data island could not be parsed', e); return; }

    state.doctors = data.doctors || [];
    state.packages = data.packages || [];

    initStep1();
    initStep2();
    initStep3();
    initStep4();
    initStep5();
    initChrome();
    renderStep();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
})();
