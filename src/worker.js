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
            const geminiUrl = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent';
            const apiKey = env.GEMINI_API_KEY;

            const body = await request.json();

            const response = await fetch(`${geminiUrl}?key=${apiKey}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });

            const data = await response.json();
            return new Response(JSON.stringify(data), {
                headers: { ...corsHeaders, 'Content-Type': 'application/json' }
            });
        }

        // iNaturalist API Proxy
        if (url.pathname.startsWith('/inaturalist/')) {
            const inaturalistPath = url.pathname.replace('/inaturalist', '');
            const inaturalistUrl = `https://api.inaturalist.org${inaturalistPath}${url.search}`;
            const token = env.INATURALIST_API_TOKEN;

            const headers = {
                'Authorization': `Bearer ${token}`,
                'Accept': 'application/json'
            };

            if (request.method === 'POST') {
                const formData = await request.formData();
                const response = await fetch(inaturalistUrl, {
                    method: 'POST',
                    headers: headers,
                    body: formData
                });

                const data = await response.json();
                return new Response(JSON.stringify(data), {
                    headers: { ...corsHeaders, 'Content-Type': 'application/json' }
                });
            } else {
                const response = await fetch(inaturalistUrl, {
                    method: request.method,
                    headers: headers
                });

                const data = await response.json();
                return new Response(JSON.stringify(data), {
                    headers: { ...corsHeaders, 'Content-Type': 'application/json' }
                });
            }
        }

        return new Response('API Proxy Active', { headers: corsHeaders });
    }
}