# Authentication

Set `JWT_SECRET` to a high-entropy secret of at least 32 UTF-8 bytes. Docker Compose reads it from the project `.env` file and passes it to the app container. After changing it, recreate the app container with `docker compose up -d --build app`. All instances must use the same secret. Rotating it invalidates existing tokens. Passwords are stored as BCrypt hashes.

## Browser flow

1. GET `/api/auth/csrf` with credentials enabled. Save the returned `token` and use the returned `headerName` for the following unsafe requests. Keep the CSRF cookie sent by the server.
2. POST `/api/auth/register/student` or `/api/auth/register/recruiter` with that CSRF header. Registration returns the new account ID and role; it does not log the user in. Recruiters start unauthorized.
3. POST `/api/auth/login` with `{"email":"user@example.com","password":"..."}` and the CSRF header. A successful response is 204 and sets an HTTP-only `access_token` cookie. The JWT expires after one hour.
4. Send requests with browser credentials enabled. Include the CSRF header on POST, PUT, PATCH and DELETE requests. POST `/api/auth/logout` with the CSRF header clears the authentication cookie. Logout does not revoke a copied token before it expires.

Student registration fields: `name`, `email`, `password`, `registrationNumber`, `course`. Recruiter fields: `name`, `email`, `password`, `type` (COMPANY, PROFESSOR, ACADEMIC_LEAGUE, UNIVERSITY_DEPARTMENT or OTHER), `organizationName`. Passwords must be at least 8 characters and no more than 72 UTF-8 bytes. Administrator accounts cannot be registered publicly.

`app.auth.cookie.secure` defaults to `true`; the local profile sets it to `false` for HTTP. `app.auth.cookie.same-site` defaults to `Lax`. Cross-site deployments require `SameSite=None`, HTTPS and `secure=true`. Any frontend on a different origin also requires an explicit credentialed CORS policy, even if it is on the same site. That policy depends on the deployment origin and is not configured yet. Browser third-party cookie restrictions may also affect cross-site deployments. The unique case-insensitive email index migration requires existing emails to be unique ignoring case.

Roles are derived from profiles on every request, and inactive accounts cannot authenticate. Student and recruiter endpoints require the corresponding role and matching account ID. Account recovery, refresh tokens, administrator provisioning and recruiter approval endpoints are not implemented.

## Swagger UI

Open `/swagger-ui/index.html` from the backend origin. Execute `GET /api/auth/csrf` and copy the `token` value. For login and each other POST, PUT, PATCH or DELETE operation, paste it into the documented `X-XSRF-TOKEN` header. Log in through `POST /api/auth/login`; the browser stores and sends the HTTP-only cookie automatically. Do not paste the JWT into Swagger's Authorize dialog. Retrieve a new CSRF token if its cookie changes or expires.

## Administrator and recruiter authorization

Administrator self-registration is deliberately unavailable. To bootstrap the first administrator, register a user through a registration endpoint, then use a trusted database administrator connection to insert its existing user ID into `app_auth.administrador`:

```sql
INSERT INTO app_auth.administrador (usuario_id) VALUES (<existing-user-id>);
```

Use the ID returned at registration and verify it belongs to the intended user. This grants the existing account an additional ADMIN role. Do not expose database credentials to application users. Log in as that account, obtain a CSRF token, and call `PUT /api/admin/recruiters/{recruiterId}/authorization`. Only administrators may call it; the operation is idempotent and returns 204, or 404 if the recruiter does not exist. Public registration cannot grant the admin role. There is currently no API for creating administrators, listing pending recruiters, or revoking recruiter authorization.
