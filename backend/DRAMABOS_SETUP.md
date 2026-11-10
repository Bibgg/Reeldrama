# DramaBos catalog search integration

ReelDrama has an **admin-only metadata search proxy** for testing DramaBos. It does not automatically import, download, copy, or re-host any third-party videos.

## Configure the API key

1. Create a DramaBos account and obtain an API key from https://dramabos.live/register.
2. In the Render dashboard, open the existing ReelDrama backend service and add an environment variable:
   - Key: `DRAMABOS_API_KEY`
   - Value: your private DramaBos API key
3. Save the environment variable and wait for Render to redeploy.
4. Keep the key in Render only. Never put it in the Android app, GitHub, screenshots, or a public URL.

The API key is not committed to this repository.

## Test through Hoppscotch

Send a GET request to:

```
https://YOUR-REELDRAMA-BACKEND/api/admin/catalog/search?provider=dramabox&keyword=love
```

Headers:

- `x-admin-key: YOUR_EXISTING_REELDRAMA_ADMIN_API_KEY`
- `Accept: application/json`

Documented provider values currently wired:

- `dramabox`
- `shortmax`
- `reelshort`
- `starshort`
- `flickreels`

Example query:

```
GET /api/admin/catalog/search?provider=reelshort&keyword=CEO
```

This proxy keeps the DramaBos key on the backend and applies a 12-second upstream timeout. A missing key returns HTTP 503; an invalid key is reported as an upstream authentication error; a provider quota response is returned as HTTP 429.

## Important content-rights check

Search results, posters, episode metadata, and playable stream URLs do **not by themselves prove** that ReelDrama has permission to publicly stream, monetize, download, or re-host the content. Before adding any returned episode to the public app, obtain written distribution/streaming rights from the relevant rights holder and confirm that DramaBos terms permit this use. Do not mirror or upload third-party videos to Cloudinary without those rights.

## Free tier

DramaBos currently advertises a free tier of 1,000 requests per month. Verify the current plan, rate limits, endpoint availability, and language coverage in its official documentation before relying on it:
- https://dramabos.live/docs
- https://dramabos.live/pricing
