// Counter toàn cục (reset khi worker restart, nhưng đủ dùng)
let apiKeyIndex = 0;

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

        // ==================== Helper: Get All API Keys ====================
        function getAllGeminiKeys(env) {
            return [
                env.GEMINI_API_KEY,
                env.GEMINI_API_KEY_2,
                env.GEMINI_API_KEY_3,
                env.GEMINI_API_KEY_4,
                env.GEMINI_API_KEY_5
            ].filter(k => k); // Lọc bỏ key undefined/null
        }

        // ==================== Helper: Try API with Retry ====================
        async function callGeminiWithRetry(geminiUrl, body, env, isStreaming = false) {
            const keys = getAllGeminiKeys(env);

            if (keys.length === 0) {
                throw new Error("No Gemini API keys configured");
            }

            let lastError = null;
            const maxAttempts = keys.length; // Thử tất cả keys
            let retryInfo = {
                totalAttempts: 0,
                failedKeys: [],
                successKeyIndex: null
            };

            for (let attempt = 0; attempt < maxAttempts; attempt++) {
                // Lấy key hiện tại và tăng index cho lần sau
                const currentKey = keys[apiKeyIndex % keys.length];
                const currentKeyIndex = apiKeyIndex % keys.length;
                apiKeyIndex++; // Tăng index cho lần gọi tiếp theo

                retryInfo.totalAttempts++;
                console.log(`Attempt ${attempt + 1}/${maxAttempts} - Using key index: ${currentKeyIndex}`);

                try {
                    // Build URL với key parameter
                    const urlWithKey = `${geminiUrl}${geminiUrl.includes('?') ? '&' : '?'}key=${currentKey}`;

                    const response = await fetch(urlWithKey, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'x-goog-api-client': 'genai-js/0.1.0',
                            'X-Forwarded-For': '8.8.8.8',
                            'CF-IPCountry': 'US'
                        },
                        body: JSON.stringify(body),
                        cf: {
                            resolveOverride: 'generativelanguage.googleapis.com'
                        }
                    });

                    // Nếu thành công (không phải 429), return luôn
                    if (response.ok) {
                        retryInfo.successKeyIndex = currentKeyIndex;
                        console.log(`✓ Success with key index: ${currentKeyIndex}`);
                        return { response, retryInfo };
                    }

                    // Nếu bị 429 (quota exceeded), thử key tiếp theo
                    if (response.status === 429) {
                        const errorText = await response.text();
                        retryInfo.failedKeys.push(currentKeyIndex);
                        console.warn(`✗ Key ${currentKeyIndex} quota exceeded (429), trying next key...`);
                        lastError = { status: 429, text: errorText };
                        continue; // Thử key tiếp theo
                    }

                    // Lỗi khác (không phải 429), return luôn (không retry)
                    console.error(`✗ API error ${response.status} with key ${currentKeyIndex}`);
                    return { response, retryInfo };

                } catch (error) {
                    console.error(`✗ Network error with key ${currentKeyIndex}:`, error.message);
                    retryInfo.failedKeys.push(currentKeyIndex);
                    lastError = { status: 500, text: error.message };
                    continue; // Thử key tiếp theo
                }
            }

            // Nếu tất cả keys đều fail
            console.error('✗ All API keys exhausted');
            retryInfo.allFailed = true;
            throw { error: lastError ? `All API keys failed. Last error: ${lastError.text}` : 'All API keys failed', retryInfo };
        }

        // ==================== GEMINI NON-STREAMING (/gemini) ====================
        if (url.pathname === '/gemini' && request.method === 'POST') {
            try {
                const geminiUrl = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent';
                const body = await request.json();

                const { response, retryInfo } = await callGeminiWithRetry(geminiUrl, body, env, false);

                // Thêm custom headers để client biết trạng thái retry
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
                // Trường hợp tất cả keys đều fail
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

        // ==================== GEMINI STREAMING (/gemini/stream) ====================
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
                    console.error("Gemini Streaming API Error:", errorText);

                    return new Response(JSON.stringify({ error: 'Gemini API error', details: errorText }), {
                        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                        status: response.status
                    });
                }

                // Stream trực tiếp về client
                return new Response(response.body, {
                    headers: customHeaders
                });

            } catch (error) {
                // Trường hợp tất cả keys đều fail
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

        // ==================== iNaturalist API Proxy (NEW: Fetch token from renewer worker) ====================
        if (url.pathname.startsWith('/inaturalist/')) {
            try {
                // URL của worker renewer - THAY ĐỔI NẾU DOMAIN KHÁC
                const TOKEN_RENEWER_URL = 'https://inaturalist-token-renewer.tainguyen.dev/token';

                // Lấy token từ renewer worker (có cache 5 phút để tránh gọi quá nhiều)
                const tokenResponse = await fetch(TOKEN_RENEWER_URL, {
                    headers: {
                        'Accept': 'application/json'
                    },
                    // Cache 300 giây (5 phút) ở edge Cloudflare
                    cf: { cacheTtl: 300, cacheEverything: true }
                });

                if (!tokenResponse.ok) {
                    throw new Error(`Failed to fetch token: ${tokenResponse.status}`);
                }

                const tokenData = await tokenResponse.json();
                const token = tokenData.token;

                if (!token || typeof token !== 'string' || token.length < 20) {
                    throw new Error('Invalid or empty token received from renewer');
                }

                // Xây dựng URL gốc iNaturalist
                const inaturalistPath = url.pathname.replace('/inaturalist', '');
                const inaturalistUrl = `https://api.inaturalist.org${inaturalistPath}${url.search}`;

                const headers = new Headers();
                headers.set('Authorization', `Bearer ${token}`);
                headers.set('Accept', 'application/json');

                let response;

                if (request.method === 'POST' || request.method === 'PUT' || request.method === 'PATCH') {
                    // Đối với POST/PUT thường là upload observation → forward body nguyên
                    response = await fetch(inaturalistUrl, {
                        method: request.method,
                        headers: headers,
                        body: request.body
                    });
                } else {
                    response = await fetch(inaturalistUrl, {
                        method: request.method,
                        headers: headers
                    });
                }

                // Forward response về client (giữ nguyên status, headers, body)
                const responseHeaders = new Headers(corsHeaders);
                responseHeaders.set('Content-Type', 'application/json');

                // Copy một số header hữu ích từ iNaturalist (nếu có)
                const contentType = response.headers.get('content-type');
                if (contentType) {
                    responseHeaders.set('Content-Type', contentType);
                }

                return new Response(response.body, {
                    status: response.status,
                    statusText: response.statusText,
                    headers: responseHeaders
                });

            } catch (error) {
                console.error('iNaturalist proxy error:', error);

                return new Response(JSON.stringify({
                    error: 'Failed to proxy iNaturalist request',
                    details: error.message
                }), {
                    headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                    status: 502  // Bad Gateway
                });
            }
        }

        // ==================== Root / Default ====================
        return new Response('API Proxy Active – /gemini, /gemini/stream, /inaturalist/*', {
            headers: corsHeaders
        });
    }
}