import { getTmdbInfo, tmdbApiKeySettingsLayout } from '../shared/tmdb.js';
import { fetchText } from '../shared/http.js';

const DOMAIN = 'https://www.fullhdfilmizlesene.pw';
const HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'tr-TR,tr;q=0.9,en;q=0.8'
};

export async function getStreams({ id, type, season, episode, settings }) {
    if (type === 'tv') return []; // FullHDFilmizlesene is movie only

    try {
        const tmdb = await getTmdbInfo(id, type);
        if (!tmdb.title && !tmdb.originalTitle) return [];

        const query = tmdb.originalTitle || tmdb.title;
        const searchUrl = `${DOMAIN}/arama/${encodeURIComponent(query)}`;

        const html = await fetchText(searchUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });

        // Match movie url
        const movieLinks = html.match(/href=["'](https?:\/\/www\.fullhdfilmizlesene\.pw\/film\/[^"']+)["']/gi) || [];
        if (movieLinks.length === 0) return [];

        const movieUrl = movieLinks[0].match(/href=["']([^"']+)["']/i)[1];
        const movieHtml = await fetchText(movieUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });

        // Extract iframes/players
        const iframes = movieHtml.match(/<iframe[^>]+src=["']([^"']+)["']/gi) || [];
        const streams = [];

        for (const ifr of iframes) {
            const src = ifr.match(/src=["']([^"']+)["']/i)?.[1];
            if (src && (src.includes('player') || src.includes('embed') || src.includes('m3u8') || src.includes('video'))) {
                streams.push({
                    name: 'FullHDFilmizlesene',
                    title: `${tmdb.title || 'Film'} - HD Türkçe`,
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
        console.error('[FullHDFilm] getStreams error:', e.message);
        return [];
    }
}

export function onSettings() {
    return tmdbApiKeySettingsLayout();
}
