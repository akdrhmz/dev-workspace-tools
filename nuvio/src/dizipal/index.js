import { getTmdbInfo, tmdbApiKeySettingsLayout } from '../shared/tmdb.js';
import { fetchText } from '../shared/http.js';

const DOMAIN = 'https://dizipal.org';
const HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'tr-TR,tr;q=0.9,en;q=0.8'
};

export async function getStreams({ id, type, season, episode, settings }) {
    try {
        const tmdb = await getTmdbInfo(id, type);
        if (!tmdb.title && !tmdb.originalTitle) return [];

        const query = tmdb.originalTitle || tmdb.title;
        const searchUrl = `${DOMAIN}/search?q=${encodeURIComponent(query)}`;

        const html = await fetchText(searchUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });

        // Find result page
        const match = html.match(/href=["'](\/dizi\/[^"']+|\/film\/[^"']+)["']/i);
        if (!match) return [];

        let targetUrl = `${DOMAIN}${match[1]}`;
        if (type === 'tv' && season && episode) {
            targetUrl = `${targetUrl}/sezon-${season}/bolum-${episode}`;
        }

        const pageHtml = await fetchText(targetUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });

        // Extract player links
        const iframeMatch = pageHtml.match(/<iframe[^>]+src=["']([^"']+)["']/i);
        if (!iframeMatch) return [];

        const iframeUrl = iframeMatch[1].startsWith('//') ? `https:${iframeMatch[1]}` : iframeMatch[1];

        return [
            {
                name: 'DiziPal',
                title: `${tmdb.title || 'İçerik'} - 1080p`,
                url: iframeUrl,
                quality: '1080p',
                headers: {
                    'Referer': `${DOMAIN}/`,
                    'User-Agent': HEADERS['User-Agent']
                }
            }
        ];
    } catch (e) {
        console.error('[DiziPal] getStreams error:', e.message);
        return [];
    }
}

export function onSettings() {
    return tmdbApiKeySettingsLayout();
}
