# Live appointment updates

The appointment list in the staff console and the public booking calendar use
Server-Sent Events (SSE). SSE is a simple one-way connection from the server to
the browser; it is sufficient for announcing a booking/status/availability
change without sending patient data through the event stream.

- New bookings and appointment status changes update the open admin appointment
  list without a full-page reload.
- New bookings, cancellations, doctor attendance/break changes, and leave changes
  cause an open booking calendar to re-fetch live slot availability.
- Availability and appointment writes publish their event only after the
  database transaction commits.
- The streams send periodic keep-alive comments and reconnect automatically.
- Events are held in memory, so the real-time broadcasts reach browsers connected
  to the same application instance. A multi-instance EC2 deployment needs a
  shared event broker such as Redis; the current single-instance setup does not.

## Existing MySQL database migration

The old `uk_appt_slot` index reserved the slot even when its appointment was
cancelled. Before deploying this version against an existing MySQL database,
back up the database, select the `chinche_hospital` database, and run
[`V20261031__active_appointment_slot_unique.sql`](./sql/migrations/V20261031__active_appointment_slot_unique.sql)
once. The script replaces that index with a nullable unique key: active
appointments still cannot double-book a doctor/time, while cancelled and no-show
appointments remain in the history and release their slot. The script is safe
to re-run if the application has already added the new column through
`ddl-auto=update`.

Do not delete cancelled appointment rows or edit their original date/time to
release a slot. The new key handles slot claiming separately from the retained
appointment history.

For EC2, one Spring Boot instance can use this in-memory SSE setup. If a reverse
proxy is introduced, it must allow long-lived event streams and avoid buffering
`text/event-stream` responses.
