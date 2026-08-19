import { getTmdbInfo, tmdbApiKeySettingsLayout } from '../shared/tmdb.js';
import { fetchText } from '../shared/http.js';

const DOMAIN = 'https://sezonlukdizi.vip';
const HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'tr-TR,tr;q=0.9,en;q=0.8'
};

export async function getStreams({ id, type, season, episode, settings }) {
    if (type === 'movie') return []; // SezonlukDizi is TV series only

    try {
        const tmdb = await getTmdbInfo(id, type);
        if (!tmdb.title && !tmdb.originalTitle) return [];

        const query = tmdb.originalTitle || tmdb.title;
        const searchUrl = `${DOMAIN}/diziler?adi=${encodeURIComponent(query)}`;

        const html = await fetchText(searchUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });

        // Match series url
        const match = html.match(/href=["'](https?:\/\/sezonlukdizi\.vip\/diziler\/[^"']+\.html)["']/i);
        if (!match) return [];

        const s = season || 1;
        const e = episode || 1;
        const epUrl = match[1].replace('.html', `/${s}-sezon-${e}-bolum.html`);

        const epHtml = await fetchText(epUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });

        // Extract embed player iframes
        const iframes = epHtml.match(/<iframe[^>]+src=["']([^"']+)["']/gi) || [];
        const streams = [];

        for (const ifr of iframes) {
            const src = ifr.match(/src=["']([^"']+)["']/i)?.[1];
            if (src && (src.includes('player') || src.includes('embed') || src.includes('m3u8') || src.includes('vidmoly'))) {
                streams.push({
                    name: 'SezonlukDizi',
                    title: `${tmdb.title || 'Dizi'} S${s}E${e} - 1080p`,
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
        console.error('[SezonlukDizi] getStreams error:', e.message);
        return [];
    }
}

export function onSettings() {
    return tmdbApiKeySettingsLayout();
}
