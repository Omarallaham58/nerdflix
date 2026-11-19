function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET,HEAD,OPTIONS",
    "Access-Control-Allow-Headers": "Range,Content-Type",
    "Access-Control-Expose-Headers":
      "Content-Length,Content-Range,Accept-Ranges,Content-Type",
  };
}

function isDebug(url) {
  return url.searchParams.get("debug") === "1";
}

function safeJson(obj) {
  return JSON.stringify(obj, null, 2);
}

function inspectString(label, s) {
  const str = String(s ?? "");
  const codes = Array.from(str).map((ch) => ch.charCodeAt(0));
  return {
    label,
    value_json: JSON.stringify(str),
    length: str.length,
    charCodes: codes,
  };
}

function b64UrlToBytes(b64url) {
  const b64 =
    b64url.replace(/-/g, "+").replace(/_/g, "/") +
    "===".slice((b64url.length + 3) % 4);
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes;
}

function bytesToB64Url(bytes) {
  let bin = "";
  for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i]);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

async function hmacSha256B64Url(secret, msg) {
  const raw = new TextEncoder().encode(secret);
  const key = await crypto.subtle.importKey(
    "raw",
    raw,
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const sig = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(msg)
  );
  return bytesToB64Url(new Uint8Array(sig));
}

async function verifyWorkerToken(token, secret, expectedVideoId = null) {
  if (!secret) return { ok: false, reason: "WORKER_TOKEN_SECRET missing" };

  token = token.trim().replace(/^"+|"+$/g, "");

  const dot = token.lastIndexOf(".");
  if (dot < 0) return { ok: false, reason: "token format" };

  const payloadB64 = token.slice(0, dot);
  const sigB64 = token.slice(dot + 1);

  const expectedSig = await hmacSha256B64Url(secret, payloadB64);
  if (expectedSig !== sigB64) return { ok: false, reason: "bad signature" };

  let payloadStr;
  try {
    payloadStr = new TextDecoder().decode(b64UrlToBytes(payloadB64));
  } catch {
    return { ok: false, reason: "payload decode" };
  }

  const params = new URLSearchParams(payloadStr);
  const vid = params.get("vid");
  const uid = params.get("uid");
  const exp = Number(params.get("exp") || "0");

  if (!vid || !uid || !exp) return { ok: false, reason: "missing claims" };
  if (expectedVideoId !== null && vid !== expectedVideoId) return { ok: false, reason: "vid mismatch" };

  const now = Math.floor(Date.now() / 1000);
  if (exp <= now) return { ok: false, reason: "expired" };

  return { ok: true, uid: Number(uid), vid, exp };
}

// ------------------- Backblaze B2 (cached) -------------------
let B2_AUTH_CACHE = {
  authToken: null,
  apiUrl: null,
  downloadUrl: null,
  accountId: null,
  expMs: 0,
};

let B2_BUCKET_CACHE = {
  bucketId: null,
  bucketName: null,
};

// Cache for download tokens per videoId
let B2_DOWNLOAD_TOKEN_CACHE = new Map(); // key: videoId, value: { token, expiresAt }

async function b2Authorize(env) {
  const now = Date.now();
  if (B2_AUTH_CACHE.authToken && now < B2_AUTH_CACHE.expMs) return B2_AUTH_CACHE;

  if (!env.B2_KEY_ID || !env.B2_APP_KEY) {
    throw new Error("Missing B2_KEY_ID / B2_APP_KEY");
  }

  const basic = btoa(`${env.B2_KEY_ID}:${env.B2_APP_KEY}`);
  const res = await fetch(
    "https://api.backblazeb2.com/b2api/v2/b2_authorize_account",
    { headers: { Authorization: `Basic ${basic}` } }
  );

  const text = await res.text();
  if (!res.ok) throw new Error(`b2_authorize_account failed: ${res.status} ${text}`);

  const data = JSON.parse(text);
  B2_AUTH_CACHE = {
    authToken: data.authorizationToken,
    apiUrl: data.apiUrl,
    downloadUrl: data.downloadUrl,
    accountId: data.accountId,
    expMs: now + 20 * 60 * 1000,//20 minutes cache (tokens are valid for 24 hours, but we refresh more often to be safe)
  };
  return B2_AUTH_CACHE;
}

async function b2ResolveBucketId(env, auth, bucketName) {
  if (env.B2_BUCKET_ID) return env.B2_BUCKET_ID;

  if (!bucketName) throw new Error("Missing B2_BUCKET (bucket name)");

  if (B2_BUCKET_CACHE.bucketId && B2_BUCKET_CACHE.bucketName === bucketName) {
    return B2_BUCKET_CACHE.bucketId;
  }

  const res = await fetch(`${auth.apiUrl}/b2api/v2/b2_list_buckets`, {
    method: "POST",
    headers: {
      Authorization: auth.authToken,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      accountId: auth.accountId,
      bucketName,
    }),
  });

  const text = await res.text();
  if (!res.ok) throw new Error(`b2_list_buckets failed: ${res.status} ${text}`);

  const data = JSON.parse(text);
  const bucket = (data.buckets || [])[0];
  if (!bucket?.bucketId) throw new Error("Could not resolve bucketId (check B2_BUCKET)");

  B2_BUCKET_CACHE = { bucketId: bucket.bucketId, bucketName };
  return bucket.bucketId;
}

async function b2GetDownloadAuthorization(env, auth, bucketName, videoId) {
  // Check cache first
  const now = Date.now();
  const cached = B2_DOWNLOAD_TOKEN_CACHE.get(videoId);
  if (cached && cached.expiresAt > now) {
    return cached.token;
  }

  const bucketId = await b2ResolveBucketId(env, auth, bucketName);
  const prefix = `videos/${videoId}/`;

  const res = await fetch(`${auth.apiUrl}/b2api/v2/b2_get_download_authorization`, {
    method: "POST",
    headers: {
      Authorization: auth.authToken,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      bucketId,
      fileNamePrefix: prefix,
      validDurationInSeconds: 300,
    }),
  });

  const text = await res.text();
  if (!res.ok) throw new Error(`b2_get_download_authorization failed: ${res.status} ${text}`);

  const data = JSON.parse(text);
  const token = data.authorizationToken;
  // Cache for 5 minutes (validDurationInSeconds - 10 seconds safety margin)
  B2_DOWNLOAD_TOKEN_CACHE.set(videoId, { token, expiresAt: now + 290 * 1000 });
  return token;
}

// ------------------- M3U8 rewriting -------------------
function appendTokenToUrl(uri, token) {
  if (!uri) return uri;
  if (uri.includes("token=")) return uri;

  const hashIndex = uri.indexOf("#");
  const base = hashIndex >= 0 ? uri.slice(0, hashIndex) : uri;
  const hash = hashIndex >= 0 ? uri.slice(hashIndex) : "";

  const sep = base.includes("?") ? "&" : "?";
  return `${base}${sep}token=${encodeURIComponent(token)}${hash}`;
}

/**
 * Rewrite M3U8 lines:
 * - For EXT-X-KEY, ensure the URI points to the worker's origin (replace any absolute URI
 *   that does not match the worker's origin with the worker's origin, preserving path/query).
 * - For all non‑comment lines, append the token parameter.
 */
function rewriteM3U8(text, token, workerOrigin) {
  const lines = text.split(/\r?\n/);
  return lines
    .map((line) => {
      if (!line) return line;

      if (line.startsWith("#EXT-X-KEY") && line.includes('URI="')) {
        return line.replace(/URI="([^"]+)"/g, (_m, uri) => {
          let newUri = uri;
          try {
            const uriUrl = new URL(uri, workerOrigin); // resolves relative URIs
            if (uriUrl.origin !== workerOrigin) {
              // Absolute URI pointing to a different host – rewrite to worker's origin
              const path = uriUrl.pathname;
              const search = uriUrl.search;
              newUri = `${workerOrigin}${path}${search}`;
            } else {
              // Already on the worker origin (or relative)
              newUri = uri;
            }
          } catch {
            // If URI is malformed, keep as is
          }
          return `URI="${appendTokenToUrl(newUri, token)}"`;
        });
      }

      if (line.startsWith("#")) return line;

      return appendTokenToUrl(line, token);
    })
    .join("\n");
}

export default {
  async fetch(request, env) {
    try {
      if (request.method === "OPTIONS") {
        return new Response(null, { status: 204, headers: corsHeaders() });
      }

      const url = new URL(request.url);
      const workerOrigin = url.origin;


      // LICENSE ENDPOINT - Proxy to backend (handles both /license and /api/license)
     
      if (url.pathname === "/license" || url.pathname === "/api/license") {
        console.log(" Proxying license request to backend");

        const token = url.searchParams.get("token");
        if (!token) {
          return new Response("Missing token", { status: 401, headers: corsHeaders() });
        }

        const payload = await verifyWorkerToken(token, env.WORKER_TOKEN_SECRET, null);
        if (!payload.ok) {
          console.error("License token verification failed:", payload.reason);
          return new Response(`Invalid token: ${payload.reason}`, { status: 403, headers: corsHeaders() });
        }

        console.log("License token valid for user:", payload.uid);

        // Use environment variable for backend URL
        const backendBase = env.BACKEND_URL || "https://your-backend.com"; // set this in Cloudflare
        const backendUrl = `${backendBase}/api/license${url.search}`;
        console.log("Forwarding to:", backendUrl);

        try {
          const backendResponse = await fetch(backendUrl, {
            headers: { 'Content-Type': 'application/json' }
          });

          console.log("Backend response status:", backendResponse.status);

          return new Response(backendResponse.body, {
            status: backendResponse.status,
            headers: {
              'Content-Type': 'application/octet-stream',
              'Access-Control-Allow-Origin': '*',
              'Cache-Control': 'no-cache'
            }
          });
        } catch (fetchError) {
          console.error("Failed to reach backend:", fetchError.message);
          return new Response("Backend unavailable", { status: 502, headers: corsHeaders() });
        }
      }

 
      // STREAM ENDPOINT - Serves video files from B2 with token verification and M3U8 rewriting
      const parts = url.pathname.split("/").filter(Boolean);

      if (parts.length < 3 || parts[0] !== "stream") {
        return new Response("Not found", { status: 404, headers: corsHeaders() });
      }

      const videoId = parts[1];
      const objectPath = parts.slice(2).join("/");

      const token = url.searchParams.get("token");
      if (!token) return new Response("Missing token", { status: 401, headers: corsHeaders() });

      const v = await verifyWorkerToken(token, env.WORKER_TOKEN_SECRET, videoId);
      if (!v.ok) return new Response(`Invalid token: ${v.reason}`, { status: 403, headers: corsHeaders() });

      const bucketNameRaw = env.B2_BUCKET;
      const bucketName = (env.B2_BUCKET || "").trim();

      if (isDebug(url)) {
        const info = {
          parsed: { videoId, objectPath },
          env: {
            bucketRaw: inspectString("B2_BUCKET raw", bucketNameRaw),
            bucketTrimmed: inspectString("B2_BUCKET trimmed", bucketName),
            hasBucketIdVar: Boolean(env.B2_BUCKET_ID),
            hasKeyId: Boolean(env.B2_KEY_ID),
            hasAppKey: Boolean(env.B2_APP_KEY),
            hasWorkerSecret: Boolean(env.WORKER_TOKEN_SECRET),
          },
        };
        console.log("DEBUG INFO:", info);
        return new Response(safeJson(info), {
          status: 200,
          headers: { ...corsHeaders(), "Content-Type": "application/json; charset=utf-8" },
        });
      }

      if (!bucketName) {
        throw new Error("Worker misconfig: Missing B2_BUCKET (bucket name)");
      }

      const auth = await b2Authorize(env);
      const dlAuthToken = await b2GetDownloadAuthorization(env, auth, bucketName, videoId);

      const b2FileUrl = `${auth.downloadUrl}/file/${encodeURIComponent(bucketName)}/videos/${encodeURIComponent(videoId)}/${objectPath}`;

      const upstreamHeaders = new Headers();
      upstreamHeaders.set("Authorization", dlAuthToken);

      const range = request.headers.get("Range");
      if (range) upstreamHeaders.set("Range", range);

      const upstream = await fetch(b2FileUrl, { headers: upstreamHeaders });

      if (!upstream.ok) {
        const errText = await upstream.text();
        console.log("UPSTREAM FAIL", { status: upstream.status, b2FileUrl, errText });

        const h = new Headers(corsHeaders());
        h.set("Content-Type", "text/plain; charset=utf-8");
        return new Response(
          `Upstream B2 error ${upstream.status}:\n${errText}\n\nURL: ${b2FileUrl}\nBucketRaw: ${JSON.stringify(bucketNameRaw)}\nBucketTrim: ${JSON.stringify(bucketName)}`,
          { status: upstream.status, headers: h }
        );
      }

      const headers = new Headers(upstream.headers);
      Object.entries(corsHeaders()).forEach(([k, val]) => headers.set(k, val));

      const playlistMaxAge = Number(env.PLAYLIST_CACHE_SECONDS || 10);
      const segmentMaxAge = Number(env.SEGMENT_CACHE_SECONDS || 60);

      if (objectPath.endsWith(".m3u8")) {
        headers.set("Cache-Control", `private, max-age=${playlistMaxAge}`);
        headers.set("Content-Type", "application/vnd.apple.mpegurl; charset=utf-8");

        const text = await upstream.text();
        const rewritten = rewriteM3U8(text, token, workerOrigin);
        return new Response(rewritten, { status: upstream.status, headers });
      }

      if (objectPath.endsWith(".ts")) {
        headers.set("Cache-Control", `private, max-age=${segmentMaxAge}`);
      }

      return new Response(upstream.body, { status: upstream.status, headers });
    } catch (e) {
      console.log("WORKER ERROR", e);
      return new Response(`Worker error:\n${e?.stack || e}`, {
        status: 500,
        headers: corsHeaders(),
      });
    }
  },
};