import { getTmdbInfo, tmdbApiKeySettingsLayout } from '../shared/tmdb.js';
import { fetchJson } from '../shared/http.js';

const DOMAIN = 'https://dizifilm.life';
const HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'application/json,text/plain,*/*',
    'Accept-Language': 'tr-TR,tr;q=0.9,en;q=0.8'
};

export async function getStreams({ id, type, season, episode, settings }) {
    try {
        const tmdb = await getTmdbInfo(id, type);
        if (!tmdb.title && !tmdb.originalTitle) return [];

        const isMovie = type !== 'tv';
        const searchPath = `${DOMAIN}/api/search?q=${encodeURIComponent(tmdb.originalTitle || tmdb.title)}`;
        
        let searchResults = [];
        try {
            searchResults = await fetchJson(searchPath, {
                headers: { ...HEADERS, 'Referer': `${DOMAIN}/` }
            });
        } catch {
            return [];
        }

        if (!Array.isArray(searchResults) || searchResults.length === 0) return [];

        const item = searchResults[0];
        if (!item || !item.id) return [];

        let streamUrl = null;
        if (isMovie) {
            const detail = await fetchJson(`${DOMAIN}/api/movies/${item.id}`, {
                headers: { ...HEADERS, 'Referer': `${DOMAIN}/` }
            });
            streamUrl = detail?.stream_url || detail?.video_url || detail?.sources?.[0]?.file;
        } else {
            const s = season || 1;
            const e = episode || 1;
            const detail = await fetchJson(`${DOMAIN}/api/series/${item.id}/season/${s}/episode/${e}`, {
                headers: { ...HEADERS, 'Referer': `${DOMAIN}/` }
            });
            streamUrl = detail?.stream_url || detail?.video_url || detail?.sources?.[0]?.file;
        }

        if (!streamUrl) return [];

        return [
            {
                name: 'DiziFilm',
                title: `${tmdb.title || 'İçerik'} - Türkçe`,
                url: streamUrl,
                quality: '1080p',
                headers: {
                    'Referer': `${DOMAIN}/`,
                    'User-Agent': HEADERS['User-Agent']
                }
            }
        ];
    } catch (e) {
        console.error('[DiziFilm] getStreams error:', e.message);
        return [];
    }
}

export function onSettings() {
    return tmdbApiKeySettingsLayout();
}
