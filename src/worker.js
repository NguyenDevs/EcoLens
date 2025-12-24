// ============================================================
// GLOBAL STATE (per worker instance)
// ============================================================

// Gemini key rotation
let apiKeyIndex = 0;

// iNaturalist token cache + lock
let cachedInatToken = null;
let cachedInatTokenUpdatedAt = 0;
let inatTokenPromise = null;

// ============================================================
// WORKER
// ============================================================

export default {
    async fetch(request, env) {

        const corsHeaders = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type, Authorization',
        };

        if (request.method === 'OPTIONS') {
            return new Response(null, { headers: corsHeaders });
        }

        const url = new URL(request.url);

        // ========================================================
        // GEMINI HELPERS
        // ========================================================

        function getAllGeminiKeys(env) {
            return [
                env.GEMINI_API_KEY,
                env.GEMINI_API_KEY_2,
                env.GEMINI_API_KEY_3,
                env.GEMINI_API_KEY_4,
                env.GEMINI_API_KEY_5,
            ].filter(Boolean);
        }

        async function callGeminiWithRetry(geminiUrl, body, env) {
            const keys = getAllGeminiKeys(env);
            if (!keys.length) throw new Error('No Gemini API keys');

            let lastError = null;

            for (let i = 0; i < keys.length; i++) {
                const keyIndex = apiKeyIndex % keys.length;
                const key = keys[keyIndex];
                apiKeyIndex++;

                try {
                    const resp = await fetch(
                        `${geminiUrl}${geminiUrl.includes('?') ? '&' : '?'}key=${key}`,
                        {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json',
                                'x-goog-api-client': 'genai-js/0.1.0',
                            },
                            body: JSON.stringify(body),
                            cf: { resolveOverride: 'generativelanguage.googleapis.com' }
                        }
                    );

                    if (resp.ok) return resp;
                    if (resp.status === 429) {
                        lastError = await resp.text();
                        continue;
                    }

                    return resp;

                } catch (e) {
                    lastError = e.message;
                }
            }

            throw new Error(`All Gemini keys failed: ${lastError}`);
        }

        // ========================================================
        // GEMINI ENDPOINTS
        // ========================================================

        if (url.pathname === '/gemini' && request.method === 'POST') {
            const body = await request.json();
            const resp = await callGeminiWithRetry(
                'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent',
                body,
                env
            );

            return new Response(await resp.text(), {
                status: resp.status,
                headers: { ...corsHeaders, 'Content-Type': 'application/json' }
            });
        }

        if (url.pathname === '/gemini/stream' && request.method === 'POST') {
            const body = await request.json();
            const resp = await callGeminiWithRetry(
                'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent?alt=sse',
                body,
                env
            );

            return new Response(resp.body, {
                headers: {
                    ...corsHeaders,
                    'Content-Type': 'text/event-stream',
                    'Cache-Control': 'no-cache',
                }
            });
        }

        // ========================================================
        // iNATURALIST TOKEN (CACHE + LOCK)
        // ========================================================

        async function getInaturalistToken() {
            const MAX_AGE_HOURS = 20;

            // in-flight lock
            if (inatTokenPromise) return inatTokenPromise;

            // cache hit
            if (cachedInatToken) {
                const age =
                    (Date.now() - cachedInatTokenUpdatedAt) / 3600000;
                if (age < MAX_AGE_HOURS) return cachedInatToken;
            }

            inatTokenPromise = (async () => {
                const resp = await fetch(
                    'https://inaturalist-token-renewer.tainguyen-devs.workers.dev',
                    { signal: AbortSignal.timeout(10000) }
                );

                if (!resp.ok) {
                    throw new Error(`Token fetch failed: ${resp.status}`);
                }

                const data = await resp.json();
                if (!data.token) throw new Error('Empty token');

                cachedInatToken = data.token;
                cachedInatTokenUpdatedAt = data.lastUpdated
                    ? new Date(data.lastUpdated).getTime()
                    : Date.now();

                return cachedInatToken;
            })();

            try {
                return await inatTokenPromise;
            } finally {
                inatTokenPromise = null;
            }
        }

        // ========================================================
        // iNATURALIST PROXY
        // ========================================================

        if (url.pathname.startsWith('/inaturalist/')) {
            try {
                const path = url.pathname.replace('/inaturalist', '');
                const targetUrl = `https://api.inaturalist.org${path}${url.search}`;

                let token = null;
                try {
                    token = await getInaturalistToken();
                } catch (e) {
                    console.warn('[iNat] token unavailable:', e.message);
                }

                const headers = new Headers();
                headers.set('Accept', 'application/json');
                if (token) headers.set('Authorization', `Bearer ${token}`);

                let resp;
                if (request.method === 'POST') {
                    resp = await fetch(targetUrl, {
                        method: 'POST',
                        headers,
                        body: await request.formData()
                    });
                } else {
                    resp = await fetch(targetUrl, {
                        method: request.method,
                        headers
                    });
                }

                return new Response(await resp.text(), {
                    status: resp.status,
                    headers: {
                        ...corsHeaders,
                        'Content-Type': 'application/json',
                        'X-Token-Source': token ? 'cache' : 'none'
                    }
                });

            } catch (e) {
                return new Response(
                    JSON.stringify({ error: e.message }),
                    { status: 500, headers: corsHeaders }
                );
            }
        }

        // ========================================================
        // DEFAULT
        // ========================================================

        return new Response(
            'API Proxy Active: /gemini, /gemini/stream, /inaturalist/*',
            { headers: corsHeaders }
        );
    }
};
