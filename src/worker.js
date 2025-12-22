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
        const apiKey = env.GEMINI_API_KEY;

        if (!apiKey) {
            return new Response(JSON.stringify({ error: "Missing GEMINI_API_KEY on server" }), {
                headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                status: 500
            });
        }

        // ==================== GEMINI NON-STREAMING (/gemini) ====================
        if (url.pathname === '/gemini' && request.method === 'POST') {
            try {
                const geminiUrl = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent';

                const body = await request.json();
                const response = await fetch(`${geminiUrl}?key=${apiKey}`, {
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

                if (!response.ok) {
                    const errorText = await response.text();
                    console.error("Gemini API Error:", errorText);
                    return new Response(errorText, {
                        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                        status: response.status
                    });
                }

                const data = await response.json();
                return new Response(JSON.stringify(data), {
                    headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                    status: 200
                });

            } catch (error) {
                return new Response(JSON.stringify({ error: error.message }), {
                    headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                    status: 500
                });
            }
        }

        // ==================== GEMINI STREAMING (/gemini/stream) ====================
        if (url.pathname === '/gemini/stream' && request.method === 'POST') {
            try {
                const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=${apiKey}`;

                const body = await request.json();
                const geminiResponse = await fetch(geminiUrl, {
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

                if (!geminiResponse.ok) {
                    const errorText = await geminiResponse.text();
                    console.error("Gemini Streaming API Error:", errorText);
                    return new Response(JSON.stringify({ error: 'Gemini API error', details: errorText }), {
                        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                        status: geminiResponse.status
                    });
                }

                // Stream trực tiếp về client
                return new Response(geminiResponse.body, {
                    headers: {
                        'Content-Type': 'text/event-stream',
                        'Cache-Control': 'no-cache',
                        'Connection': 'keep-alive',
                        ...corsHeaders
                    }
                });

            } catch (error) {
                return new Response(JSON.stringify({ error: error.message }), {
                    headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                    status: 500
                });
            }
        }

        // ==================== iNaturalist API Proxy ====================
        if (url.pathname.startsWith('/inaturalist/')) {
            try {
                const inaturalistPath = url.pathname.replace('/inaturalist', '');
                const inaturalistUrl = `https://api.inaturalist.org${inaturalistPath}${url.search}`;
                const token = env.INATURALIST_API_TOKEN;

                const headers = new Headers();
                if (token) {
                    headers.set('Authorization', `Bearer ${token}`);
                }

                let response;

                if (request.method === 'POST') {
                    // Forward form data (hỗ trợ upload file)
                    const formData = await request.formData();
                    response = await fetch(inaturalistUrl, {
                        method: 'POST',
                        headers: headers,
                        body: formData
                    });
                } else {
                    headers.set('Accept', 'application/json');
                    response = await fetch(inaturalistUrl, {
                        method: request.method,
                        headers: headers
                    });
                }

                const responseData = await response.json();

                return new Response(JSON.stringify(responseData), {
                    headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                    status: response.status
                });

            } catch (error) {
                return new Response(JSON.stringify({
                    error: error.message
                }), {
                    headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                    status: 500
                });
            }
        }

        // ==================== Root / Default ====================
        return new Response('API Proxy Active – /gemini, /gemini/stream, /inaturalist/*', {
            headers: corsHeaders
        });
    }
}