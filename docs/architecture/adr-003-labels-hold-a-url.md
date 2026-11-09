# ADR 003: QR labels hold a URL with only the asset tag

**Context.** A printed label must open the asset's record from a phone (proposal objective 4). The label could hold only the tag (`IT-000042`), or a full URL (`https://<host>/a/IT-000042`). Labels are printed once and stay on the devices for years.

**Decision.**
- The label holds **`https://<APP_BASE_URL>/a/<TAG>`**, and shows the tag as text under the code.
- The in-app scanner extracts the tag **on the server** from either a full URL (any host) or a bare tag, and checks it against the tag pattern.
- `/a/{tag}` requires a login; after login the user lands on that asset's page.

**Consequences.**
- Any phone camera app opens the label directly, without the in-app scanner. That's simpler for usability tests and for IT staff.
- If the host ever changes, the in-app scanner still works with old labels, because it only uses the tag. Only the "open with the phone's own camera" path would need a redirect from the old host.
- The label holds an identifier only: no personal data, no secret. Somebody outside scanning it sees a login page. So the label is safe even when it's visible to everyone.
- The production URL must be final before labels are printed for the evaluation (see docs/deployment.md).
