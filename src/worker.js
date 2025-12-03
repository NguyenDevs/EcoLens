export default {
    async fetch(request, env) {
        const corsHeaders = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type',
        };

        if (request.method === 'OPTIONS') {
            return new Response(null, { headers: corsHeaders });
        }

        const url = new URL(request.url);

        if (url.pathname === '/gemini') {
            const geminiUrl = 'https://generativelanguage.googleapis.com/v1/models/gemini-pro-vision:generateContent';
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

        return new Response('API Proxy Active', { headers: corsHeaders });
    }
}