let apiKeyIndex = 0;

export default {
    async fetch(request, env) {
        const corsHeaders = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Gemini-Retry-Count',
        };

        if (request.method === 'OPTIONS') {
            return new Response(null, { headers: corsHeaders });
        }

        const url = new URL(request.url);

        function getAllGeminiKeys(env) {
            return [
                env.GEMINI_API_KEY,
                env.GEMINI_API_KEY_2,
                env.GEMINI_API_KEY_3,
                env.GEMINI_API_KEY_4,
                env.GEMINI_API_KEY_5
            ].filter(k => k);
        }

        async function callGeminiWithRetry(geminiUrl, body, env, isStreaming = false) {
            const keys = getAllGeminiKeys(env);

            if (keys.length === 0) {
                throw new Error("No Gemini API keys configured");
            }

            let lastError = null;
            const maxAttempts = keys.length;
            let retryInfo = {
                totalAttempts: 0,
                failedKeys: [],
                successKeyIndex: null
            };

            for (let attempt = 0; attempt < maxAttempts; attempt++) {
                const currentKey = keys[apiKeyIndex % keys.length];
                const currentKeyIndex = apiKeyIndex % keys.length;
                apiKeyIndex++;

                retryInfo.totalAttempts++;

                try {
                    const urlWithKey = `${geminiUrl}${geminiUrl.includes('?') ? '&' : '?'}key=${currentKey}`;

                    const response = await fetch(urlWithKey, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'x-goog-api-client': 'genai-js/0.1.0'
                        },
                        body: JSON.stringify(body)
                    });

                    if (response.ok) {
                        retryInfo.successKeyIndex = currentKeyIndex;
                        return { response, retryInfo };
                    }

                    if (response.status === 429) {
                        const errorText = await response.text();
                        retryInfo.failedKeys.push(currentKeyIndex);
                        lastError = { status: 429, text: errorText };
                        continue;
                    }

                    return { response, retryInfo };

                } catch (error) {
                    retryInfo.failedKeys.push(currentKeyIndex);
                    lastError = { status: 500, text: error.message };
                    continue;
                }
            }

            retryInfo.allFailed = true;
            throw { error: lastError ? `All API keys failed. Last error: ${lastError.text}` : 'All API keys failed', retryInfo };
        }

        if (url.pathname === '/gemini' && request.method === 'POST') {
            try {
                const geminiUrl = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent';
                const body = await request.json();

                const { response, retryInfo } = await callGeminiWithRetry(geminiUrl, body, env, false);

                const customHeaders = {
                    ...corsHeaders,
                    'Content-Type': 'application/json',
                    'X-Gemini-Retry-Count': retryInfo.totalAttempts.toString(),
                    'X-Gemini-Failed-Keys': retryInfo.failedKeys.join(','),
                    'X-Gemini-Success-Key': retryInfo.successKeyIndex !== null ? retryInfo.successKeyIndex.toString() : 'none'
                };

                if (!response.ok) {
                    const errorText = await response.text();
                    return new Response(errorText, {
                        headers: customHeaders,
                        status: response.status
                    });
                }

                const data = await response.json();
                return new Response(JSON.stringify(data), {
                    headers: customHeaders,
                    status: 200
                });

            } catch (error) {
                const customHeaders = {
                    ...corsHeaders,
                    'Content-Type': 'application/json',
                    'X-Gemini-All-Failed': 'true'
                };

                if (error.retryInfo) {
                    customHeaders['X-Gemini-Retry-Count'] = error.retryInfo.totalAttempts.toString();
                    customHeaders['X-Gemini-Failed-Keys'] = error.retryInfo.failedKeys.join(',');
                }

                return new Response(JSON.stringify({ error: error.error || error.message }), {
                    headers: customHeaders,
                    status: 429
                });
            }
        }

        if (url.pathname === '/gemini/stream' && request.method === 'POST') {
            try {
                const geminiUrl = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent?alt=sse';
                const body = await request.json();

                const { response, retryInfo } = await callGeminiWithRetry(geminiUrl, body, env, true);

                const customHeaders = {
                    'Content-Type': 'text/event-stream',
                    'Cache-Control': 'no-cache',
                    'Connection': 'keep-alive',
                    ...corsHeaders,
                    'X-Gemini-Retry-Count': retryInfo.totalAttempts.toString(),
                    'X-Gemini-Failed-Keys': retryInfo.failedKeys.join(','),
                    'X-Gemini-Success-Key': retryInfo.successKeyIndex !== null ? retryInfo.successKeyIndex.toString() : 'none'
                };

                if (!response.ok) {
                    const errorText = await response.text();
                    return new Response(JSON.stringify({ error: 'Gemini API error', details: errorText }), {
                        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                        status: response.status
                    });
                }

                return new Response(response.body, {
                    headers: customHeaders
                });

            } catch (error) {
                const customHeaders = {
                    ...corsHeaders,
                    'Content-Type': 'application/json',
                    'X-Gemini-All-Failed': 'true'
                };

                if (error.retryInfo) {
                    customHeaders['X-Gemini-Retry-Count'] = error.retryInfo.totalAttempts.toString();
                    customHeaders['X-Gemini-Failed-Keys'] = error.retryInfo.failedKeys.join(',');
                }

                return new Response(JSON.stringify({ error: error.error || error.message }), {
                    headers: customHeaders,
                    status: 429
                });
            }
        }

        if (url.pathname.startsWith('/inaturalist/')) {
            try {
                if (!env.INATURALIST_KV) {
                    throw new Error("Missing INATURALIST_KV binding. Please configure it in Worker Settings.");
                }

                const token = await env.INATURALIST_KV.get('API_TOKEN');

                if (!token) {
                    throw new Error("Token not found in KV. Please ensure the Renewer worker is running.");
                }

                const targetPath = url.pathname.replace('/inaturalist', '');
                const targetUrl = `https://api.inaturalist.org${targetPath}${url.search}`;

                const proxyHeaders = new Headers(request.headers);
                proxyHeaders.set('Authorization', `Bearer ${token}`);
                proxyHeaders.set('Host', 'api.inaturalist.org');

                ['cf-connecting-ip', 'cf-ipcountry', 'x-forwarded-proto', 'x-real-ip'].forEach(h => proxyHeaders.delete(h));

                const response = await fetch(targetUrl, {
                    method: request.method,
                    headers: proxyHeaders,
                    body: request.body
                });

                const newResponse = new Response(response.body, response);
                Object.keys(corsHeaders).forEach(key => newResponse.headers.set(key, corsHeaders[key]));

                return newResponse;

            } catch (error) {
                return new Response(JSON.stringify({
                    error: 'Proxy Error',
                    details: error.message
                }), {
                    status: 500,
                    headers: { ...corsHeaders, 'Content-Type': 'application/json' }
                });
            }
        }

        return new Response('API Proxy Active', {
            headers: corsHeaders
        });
    }
}