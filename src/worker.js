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

        // Gemini API Proxy
        if (url.pathname === '/gemini') {
            try {
                const geminiUrl = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent';
                const apiKey = env.GEMINI_API_KEY;

                if (!apiKey) {
                    return new Response(JSON.stringify({ error: "Missing GEMINI_API_KEY on server" }), {
                        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                        status: 500
                    });
                }

                const body = await request.json();

                const response = await fetch(`${geminiUrl}?key=${apiKey}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        // ✅ THÊM HEADER ĐỂ BYPASS REGION LOCK
                        'x-goog-api-client': 'genai-js/0.1.0'
                    },
                    body: JSON.stringify(body)
                });

                // Xử lý khi Google trả về lỗi (ví dụ sai API Key, sai Model)
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
                return new Response(JSON.stringify({ error: error.message, stack: error.stack }), {
                    headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                    status: 500
                });
            }
        }

        // iNaturalist API Proxy
        if (url.pathname.startsWith('/inaturalist/')) {
            try {
                const inaturalistPath = url.pathname.replace('/inaturalist', '');
                const inaturalistUrl = `https://api.inaturalist.org${inaturalistPath}${url.search}`;
                const token = env.INATURALIST_API_TOKEN;

                // Clone request để forward
                const headers = new Headers();
                headers.set('Authorization', `Bearer ${token}`);

                let responseData;

                if (request.method === 'POST') {
                    // Forward multipart form data as-is
                    const formData = await request.formData();

                    const response = await fetch(inaturalistUrl, {
                        method: 'POST',
                        headers: headers,
                        body: formData
                    });

                    responseData = await response.json();

                    return new Response(JSON.stringify(responseData), {
                        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                        status: response.status
                    });
                } else {
                    headers.set('Accept', 'application/json');

                    const response = await fetch(inaturalistUrl, {
                        method: request.method,
                        headers: headers
                    });

                    responseData = await response.json();

                    return new Response(JSON.stringify(responseData), {
                        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                        status: response.status
                    });
                }
            } catch (error) {
                return new Response(JSON.stringify({
                    error: error.message,
                    stack: error.stack
                }), {
                    headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                    status: 500
                });
            }
        }

        return new Response('API Proxy Active', { headers: corsHeaders });
    }
}