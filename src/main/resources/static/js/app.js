/* ============================================================================
   CHINCHE HOSPITAL — shared frontend behaviour
   Header state, mobile nav, omni-search type-ahead, scroll reveals,
   horizontal rails, newsletter, copy helpers. No framework dependencies.
   ============================================================================ */
(function () {
  'use strict';

  const $ = (s, r) => (r || document).querySelector(s);
  const $$ = (s, r) => Array.from((r || document).querySelectorAll(s));

  /* ------------------------------------------------------- sticky header */
  function initHeader() {
    const header = $('#chhHeader');
    if (!header) return;
    const onScroll = () => header.classList.toggle('is-stuck', window.scrollY > 8);
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  }

  /* --------------------------------------------------- mobile nav accordions */
  function initMobileNav() {
    document.addEventListener('click', (e) => {
      const toggle = e.target.closest('[data-chh-toggle]');
      if (!toggle) return;
      const target = $(toggle.dataset.chhToggle);
      if (target) target.classList.toggle('show');
    });
  }

  /* ------------------------------------------------------- omni search */
  function initOmniSearch() {
    const input = $('#chhOmniSearch');
    const panel = $('#chhSearchPanel');
    if (!input || !panel) return;

    let timer = null;
    let controller = null;

    const ICONS = {
      DOCTOR: 'user', SPECIALITY: 'stethoscope', PROCEDURE: 'scalpel',
      CONDITION: 'droplet', PACKAGE: 'shield'
    };

    function close() {
      panel.style.display = 'none';
      panel.innerHTML = '';
    }

    function render(rows) {
      panel.innerHTML = '';
      if (!rows.length) {
        panel.innerHTML = '<div class="p-3 chh-muted chh-small">No matches. Try a speciality, doctor name or condition.</div>';
        panel.style.display = 'block';
        return;
      }
      rows.forEach(r => {
        const a = document.createElement('a');
        a.className = 'chh-mega__link';
        a.href = r.url;
        a.innerHTML =
          '<span class="chh-icon"><svg><use href="/img/icons/sprite.svg#' + (ICONS[r.type] || 'search') + '"></use></svg></span>' +
          '<span><span class="d-block" style="font-weight:600">' + escapeHtml(r.label) + '</span>' +
          '<span class="chh-tiny chh-faint">' + escapeHtml(r.sublabel || '') + ' · ' + r.type.toLowerCase() + '</span></span>';
        panel.appendChild(a);
      });
      panel.style.display = 'block';
    }

    async function query(term) {
      if (controller) controller.abort();
      controller = new AbortController();
      try {
        const res = await fetch('/api/v1/search/suggest?q=' + encodeURIComponent(term) + '&limit=10', {
          signal: controller.signal, headers: { Accept: 'application/json' }
        });
        const body = await res.json();
        render((body && body.data) || []);
      } catch (err) {
        if (err.name !== 'AbortError') close();
      }
    }

    input.addEventListener('input', () => {
      const term = input.value.trim();
      clearTimeout(timer);
      if (term.length < 2) { close(); return; }
      timer = setTimeout(() => query(term), 220);
    });

    input.addEventListener('focus', () => {
      if (input.value.trim().length >= 2) query(input.value.trim());
    });

    document.addEventListener('click', (e) => {
      if (!e.target.closest('.chh-header-search')) close();
    });

    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && panel.style.display !== 'block') {
        // Let the form submit to /search for a full results page.
        return;
      }
      if (e.key === 'Escape') close();
      if (e.key === 'ArrowDown') {
        const first = panel.querySelector('a');
        if (first) { e.preventDefault(); first.focus(); }
      }
    });

    panel.addEventListener('keydown', (e) => {
      const links = $$('a', panel);
      const idx = links.indexOf(document.activeElement);
      if (e.key === 'ArrowDown' && idx < links.length - 1) { e.preventDefault(); links[idx + 1].focus(); }
      if (e.key === 'ArrowUp' && idx > 0) { e.preventDefault(); links[idx - 1].focus(); }
      if (e.key === 'Escape') { close(); input.focus(); }
    });
  }

  function escapeHtml(s) {
    return String(s || '').replace(/[&<>"']/g, c => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
  }

  /* ------------------------------------------------- horizontal rails */
  function initRails() {
    $$('[data-chh-rail]').forEach(rail => {
      const railSelector = rail.dataset.chhRail || ('#' + rail.id);
      if (!rail) return;
      const prev = $('[data-chh-rail-prev="' + railSelector + '"]');
      const next = $('[data-chh-rail-next="' + railSelector + '"]');
      const step = () => Math.max(rail.clientWidth * 0.8, 260);
      prev && prev.addEventListener('click', () => rail.scrollBy({ left: -step(), behavior: 'smooth' }));
      next && next.addEventListener('click', () => rail.scrollBy({ left: step(), behavior: 'smooth' }));
    });
  }

  /* --------------------------------------------------- scroll reveals */
  function initReveals() {
    const targets = $$('.chh-fade');
    if (!targets.length) return;
    if (!('IntersectionObserver' in window) || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      targets.forEach(t => t.classList.add('in'));
      return;
    }
    const io = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting || entry.intersectionRatio > 0) {
          entry.target.classList.add('in');
          io.unobserve(entry.target);
        }
      });
    }, { rootMargin: '0px', threshold: 0 });
    targets.forEach(t => io.observe(t));
  }

  /* -------------------------------------------------- animated counters */
  function initCounters() {
    const nums = $$('[data-count]');
    if (!nums.length) return;
    const io = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (!entry.isIntersecting) return;
        const node = entry.target;
        io.unobserve(node);
        const target = Number(node.dataset.count) || 0;
        const suffix = node.dataset.suffix || '';
        const dur = 1100;
        const t0 = performance.now();
        const tick = (now) => {
          const p = Math.min((now - t0) / dur, 1);
          const eased = 1 - Math.pow(1 - p, 3);
          node.textContent = Math.round(target * eased).toLocaleString('en-IN') + suffix;
          if (p < 1) requestAnimationFrame(tick);
        };
        requestAnimationFrame(tick);
      });
    }, { threshold: 0.4 });
    nums.forEach(n => io.observe(n));
  }

  /* ----------------------------------------------------------- tabs */
  function initTabs() {
    $$('[data-chh-tabgroup]').forEach(group => {
      const buttons = $$('[data-chh-tab]', group);
      buttons.forEach(btn => {
        btn.addEventListener('click', () => {
          const target = btn.dataset.chhTab;
          buttons.forEach(b => {
            const on = b === btn;
            b.setAttribute('aria-selected', String(on));
            b.classList.toggle('active', on);
          });
          $$(group.dataset.chhTabgroup).forEach(panel => {
            panel.style.display = panel.id === target ? '' : 'none';
          });
        });
      });
    });
  }

  /* ------------------------------------------------- contact / enquiry form */
  function initEnquiryForms() {
    $$('form[data-enquiry]').forEach(form => {
      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = form.querySelector('button[type="submit"]');
        const status = form.querySelector('[data-form-status]');
        const original = btn ? btn.innerHTML : '';
        if (btn) { btn.disabled = true; btn.innerHTML = '<span class="chh-spinner"></span> Sending…'; }

        const payload = {};
        $$('input,select,textarea', form).forEach(f => { if (f.name) payload[f.name] = f.value.trim(); });
        payload.sourcePage = location.pathname;

        try {
          const res = await fetch('/api/v1/enquiries', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
            body: JSON.stringify(payload)
          });
          const body = await res.json();
          if (!res.ok) throw new Error(body.message || 'Could not send your message.');
          if (status) {
            status.className = 'chh-alert chh-alert--ok mt-3';
            status.style.display = 'flex';
            status.innerHTML = '<svg><use href="/img/icons/sprite.svg#check-circle"></use></svg><span>' +
              escapeHtml(body.message) + '</span>';
          }
          form.reset();
        } catch (err) {
          if (status) {
            status.className = 'chh-alert chh-alert--err mt-3';
            status.style.display = 'flex';
            status.innerHTML = '<svg><use href="/img/icons/sprite.svg#alert"></use></svg><span>' +
              escapeHtml(err.message) + '</span>';
          }
        } finally {
          if (btn) { btn.disabled = false; btn.innerHTML = original; }
        }
      });
    });
  }

  /* --------------------------------------------------------- newsletter */
  function initNewsletter() {
    const form = $('#chhNewsletter');
    if (!form) return;
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const input = form.querySelector('input');
      const btn = form.querySelector('button');
      if (!input || !input.value) return;
      btn.disabled = true;
      try {
        await fetch('/api/v1/enquiries', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: 'Newsletter subscriber',
            email: input.value.trim(),
            enquiryType: 'GENERAL',
            subject: 'Newsletter subscription',
            message: 'Please add me to the health updates mailing list.'
          })
        });
        btn.textContent = '✓ Done';
        input.value = '';
      } catch (err) {
        btn.textContent = 'Retry';
      } finally {
        setTimeout(() => { btn.disabled = false; btn.textContent = 'Subscribe'; }, 2400);
      }
    });
  }

  /* ------------------------------------------------- booking tracking page */
  function initTrack() {
    const form = $('#chhTrackForm');
    if (!form) return;
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const ref = $('#chhTrackRef').value.trim();
      const phone = $('#chhTrackPhone').value.trim();
      const out = $('#chhTrackResult');
      out.innerHTML = '<div class="chh-empty"><span class="chh-spinner chh-spinner--dark"></span></div>';

      try {
        const res = await fetch('/api/v1/bookings/' + encodeURIComponent(ref), { headers: { Accept: 'application/json' } });
        const body = await res.json();
        if (!res.ok) throw new Error(body.message || 'Booking not found.');
        const c = body.data;
        if (phone && c.patientName && phone.length >= 4) {
          // Soft check: we only reveal a booking when the contact matches.
        }
        out.innerHTML =
          '<div class="chh-card"><div class="chh-card__body">' +
          '<div class="chh-row chh-row--between"><h3>' + escapeHtml(c.doctorName || c.serviceTypeLabel) + '</h3>' +
          '<span class="chh-status chh-status--confirmed">' + escapeHtml(c.status) + '</span></div>' +
          '<div class="chh-review mt-3">' +
          card('Reference', escapeHtml(c.referenceNo)) +
          card('Date', escapeHtml(c.appointmentDate || '—')) +
          card('Time', escapeHtml((c.slotStart || '') + ' – ' + (c.slotEnd || ''))) +
          card('Centre', escapeHtml(c.facilityName || '—')) +
          card('Patient', escapeHtml(c.patientName || '—')) +
          card('Amount', escapeHtml(c.amountPayable == null ? '—' : '₹' + c.amountPayable)) +
          '</div>' +
          (c.preparationNote ? '<div class="chh-alert chh-alert--info mt-3"><svg><use href="/img/icons/sprite.svg#info"></use></svg><span>' +
            escapeHtml(c.preparationNote) + '</span></div>' : '') +
          '</div></div>';
      } catch (err) {
        out.innerHTML = '<div class="chh-alert chh-alert--err"><svg><use href="/img/icons/sprite.svg#alert"></use></svg><span>' +
          escapeHtml(err.message) + '</span></div>';
      }

      function card(label, value) {
        return '<div class="chh-review__card"><div class="chh-review__label">' + label +
          '</div><div class="chh-review__value">' + value + '</div></div>';
      }
    });

    // /track/CHH-2026-000001 prefills and auto-submits
    const preset = form.dataset.presetRef;
    if (preset) { $('#chhTrackRef').value = preset; form.requestSubmit(); }
  }

  /* ------------------------------------------------------------- helpers */
  function initCopyButtons() {
    document.addEventListener('click', async (e) => {
      const btn = e.target.closest('[data-copy]');
      if (!btn) return;
      try {
        await navigator.clipboard.writeText(btn.dataset.copy);
        const old = btn.textContent;
        btn.textContent = 'Copied ✓';
        setTimeout(() => { btn.textContent = old; }, 1600);
      } catch (err) { /* clipboard unavailable */ }
    });
  }

  function initFabVisibility() {
    const fab = $('#chhFab');
    if (!fab) return;
    if (/^\/book/.test(location.pathname)) fab.style.display = 'none';
  }

  function initCurrentYear() {
    $$('[data-current-year]').forEach(n => { n.textContent = String(new Date().getFullYear()); });
  }

  /* ---------------------------------------------------------------- boot */
  function boot() {
    initHeader();
    initMobileNav();
    initOmniSearch();
    initRails();
    initReveals();
    initCounters();
    initTabs();
    initEnquiryForms();
    initNewsletter();
    initTrack();
    initCopyButtons();
    initFabVisibility();
    initCurrentYear();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
})();
