import { getTmdbInfo, tmdbApiKeySettingsLayout } from '../shared/tmdb.js';
import { fetchText } from '../shared/http.js';

const DOMAIN = 'https://belgeselx.com';
const HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'tr-TR,tr;q=0.9,en;q=0.8'
};

export async function getStreams({ id, type, season, episode, settings }) {
    try {
        const tmdb = await getTmdbInfo(id, type);
        if (!tmdb.title && !tmdb.originalTitle) return [];

        const query = tmdb.title || tmdb.originalTitle;
        const searchUrl = `${DOMAIN}/?s=${encodeURIComponent(query)}`;

        const html = await fetchText(searchUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });

        const matches = html.match(/href=["'](https?:\/\/belgeselx\.com\/[^"']+)["']/gi) || [];
        if (matches.length === 0) return [];

        const pageUrl = matches[0].match(/href=["']([^"']+)["']/i)[1];
        const pageHtml = await fetchText(pageUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });

        const iframes = pageHtml.match(/<iframe[^>]+src=["']([^"']+)["']/gi) || [];
        const streams = [];

        for (const ifr of iframes) {
            const src = ifr.match(/src=["']([^"']+)["']/i)?.[1];
            if (src && (src.includes('player') || src.includes('embed') || src.includes('youtube') || src.includes('m3u8'))) {
                streams.push({
                    name: 'BelgeselX',
                    title: `${tmdb.title || 'Belgesel'} - Türkçe`,
                    url: src.startsWith('//') ? `https:${src}` : src,
                    quality: '1080p',
                    headers: {
                        'Referer': `${DOMAIN}/`,
                        'User-Agent': HEADERS['User-Agent']
                    }
                });
            }
        }

        return streams;
    } catch (e) {
        console.error('[BelgeselX] getStreams error:', e.message);
        return [];
    }
}

export function onSettings() {
    return tmdbApiKeySettingsLayout();
}
