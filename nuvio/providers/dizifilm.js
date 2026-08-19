/**
 * AtlasStream Nuvio Provider: DiziFilm
 * Generated for Nuvio native JS runtime
 */

const DOMAIN = 'https://dizifilm.life';
const DEFAULT_TMDB_KEY = '8c598c9af9b0badc281e95b1890834bc';
const HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'application/json,text/plain,*/*',
    'Accept-Language': 'tr-TR,tr;q=0.9,en;q=0.8'
};

async function getTmdbInfo(tmdbId, mediaType) {
    const isMovie = mediaType === 'movie';
    const type = isMovie ? 'movie' : 'tv';
    const url = `https://api.themoviedb.org/3/${type}/${tmdbId}?api_key=${DEFAULT_TMDB_KEY}&language=tr-TR&append_to_response=external_ids`;
    try {
        const res = await fetch(url);
        const data = await res.json();
        return {
            title: isMovie ? (data.title || '') : (data.name || ''),
            originalTitle: isMovie ? (data.original_title || '') : (data.original_name || '')
        };
    } catch (e) {
        return { title: '', originalTitle: '' };
    }
}

async function getStreams({ id, type, season, episode, settings }) {
    try {
        const tmdb = await getTmdbInfo(id, type);
        if (!tmdb.title && !tmdb.originalTitle) return [];

        const isMovie = type !== 'tv';
        const searchPath = `${DOMAIN}/api/search?q=${encodeURIComponent(tmdb.originalTitle || tmdb.title)}`;
        
        let searchResults = [];
        try {
            const res = await fetch(searchPath, {
                headers: { ...HEADERS, 'Referer': `${DOMAIN}/` }
            });
            searchResults = await res.json();
        } catch {
            return [];
        }

        if (!Array.isArray(searchResults) || searchResults.length === 0) return [];

        const item = searchResults[0];
        if (!item || !item.id) return [];

        let streamUrl = null;
        if (isMovie) {
            const res = await fetch(`${DOMAIN}/api/movies/${item.id}`, {
                headers: { ...HEADERS, 'Referer': `${DOMAIN}/` }
            });
            const detail = await res.json();
            streamUrl = detail?.stream_url || detail?.video_url || detail?.sources?.[0]?.file;
        } else {
            const s = season || 1;
            const e = episode || 1;
            const res = await fetch(`${DOMAIN}/api/series/${item.id}/season/${s}/episode/${e}`, {
                headers: { ...HEADERS, 'Referer': `${DOMAIN}/` }
            });
            const detail = await res.json();
            streamUrl = detail?.stream_url || detail?.video_url || detail?.sources?.[0]?.file;
        }

        if (!streamUrl) return [];

        return [
            {
                name: 'DiziFilm',
                title: `${tmdb.title || 'İçerik'} - HD Türkçe`,
                url: streamUrl,
                quality: '1080p',
                headers: {
                    'Referer': `${DOMAIN}/`,
                    'User-Agent': HEADERS['User-Agent']
                }
            }
        ];
    } catch (e) {
        console.error('[DiziFilm] error:', e.message);
        return [];
    }
}

module.exports = { getStreams };
