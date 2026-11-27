# Domain catalogs

The frontend can load the active values without authentication:

- `GET /api/catalogs/OPPORTUNITY_MODALITY`
- `GET /api/catalogs/RECRUITER_TYPE`

Each item contains `id`, `category`, `code`, `label`, and `active`. Use `code` in opportunity and recruiter requests; labels are presentation text. Existing enum values were migrated into these catalogs.

Administrators can add options with `POST /api/catalogs/{category}` and JSON `{ "code": "NEW_CODE", "label": "Display label" }`. They can rename or activate/deactivate an option with `PATCH /api/catalogs/{category}/{code}` and JSON `{ "label": "New label", "active": false }`. Inactive options are omitted from public catalog responses. Existing records retain their stored code.

Supported categories are `OPPORTUNITY_MODALITY` and `RECRUITER_TYPE`. Codes are uppercase identifiers and remain stable when labels change.
