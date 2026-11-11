(function () {
  'use strict';

  let results = document.getElementById('appointmentResults');
  if (!results || !window.EventSource) return;

  const events = new EventSource('/admin/api/appointments/events');
  document.addEventListener('submit', async function (event) {
    const form = event.target.closest('form[data-live-action]');
    if (!form) return;
    event.preventDefault();

    const button = form.querySelector('button[type="submit"]');
    if (button) button.disabled = true;
    try {
      const response = await fetch(form.dataset.liveAction, {
        method: 'POST',
        body: new FormData(form),
        headers: { Accept: 'application/json' },
        credentials: 'same-origin'
      });
      const body = await response.json();
      if (!response.ok || !body || !body.success) {
        throw new Error((body && body.message) || 'Could not update this appointment.');
      }
    } catch (error) {
      window.alert(error.message);
    } finally {
      if (button) button.disabled = false;
    }
  });

  events.addEventListener('appointment-changed', async function () {
    try {
      const response = await fetch('/admin/appointments/fragment' + window.location.search, {
        headers: { Accept: 'text/html' },
        credentials: 'same-origin'
      });
      if (!response.ok) throw new Error('Appointment refresh failed (' + response.status + ')');
      const html = await response.text();
      const documentFragment = new DOMParser().parseFromString(html, 'text/html')
        .getElementById('appointmentResults');
      if (!documentFragment) throw new Error('Appointment refresh response was incomplete');
      results.replaceWith(documentFragment);
      results = documentFragment;
    } catch (error) {
      console.error('Could not update the appointment list live.', error);
    }
  });
})();
