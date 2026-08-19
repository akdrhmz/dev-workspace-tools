/**
 * AtlasStream Nuvio Provider: DiziBal
 * Generated for Nuvio native JS runtime
 */

const DOMAINS = ['https://dizibal.com', 'https://dizibal.net'];
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
        let targetDomain = DOMAINS[0];
        let searchResults = [];

        for (const domain of DOMAINS) {
            try {
                const searchPath = `/api/search?q=${encodeURIComponent(tmdb.originalTitle || tmdb.title)}`;
                const res = await fetch(`${domain}${searchPath}`, {
                    headers: { ...HEADERS, 'Referer': `${domain}/` }
                });
                const data = await res.json();
                if (Array.isArray(data) && data.length > 0) {
                    targetDomain = domain;
                    searchResults = data;
                    break;
                }
            } catch {}
        }

        if (searchResults.length === 0) return [];

        const item = searchResults.find(it => String(it.id) === String(id) || String(it.tmdb_id) === String(id)) || searchResults[0];
        if (!item || !item.slug) return [];

        let streamUrl = null;

        if (isMovie) {
            const detailUrl = `${targetDomain}/api/movie/${item.slug}`;
            const res = await fetch(detailUrl, {
                headers: { ...HEADERS, 'Referer': `${targetDomain}/` }
            });
            const detail = await res.json();
            if (detail?.sources) {
                for (const src of detail.sources) {
                    if (src.file && (src.file.includes('.m3u8') || src.file.includes('.mp4'))) {
                        streamUrl = src.file;
                        break;
                    }
                }
            }
        } else {
            const targetSeason = season || 1;
            const targetEpisode = episode || 1;
            const epUrl = `${targetDomain}/api/tv/${item.slug}/season/${targetSeason}/episode/${targetEpisode}`;
            const res = await fetch(epUrl, {
                headers: { ...HEADERS, 'Referer': `${targetDomain}/` }
            });
            const epDetail = await res.json();
            if (epDetail?.sources) {
                for (const src of epDetail.sources) {
                    if (src.file && (src.file.includes('.m3u8') || src.file.includes('.mp4'))) {
                        streamUrl = src.file;
                        break;
                    }
                }
            }
        }

        if (!streamUrl) return [];

        return [
            {
                name: 'DiziBal',
                title: `${tmdb.title || item.title || 'İçerik'} - 1080p`,
                url: streamUrl,
                quality: '1080p',
                headers: {
                    'Referer': `${targetDomain}/`,
                    'User-Agent': HEADERS['User-Agent']
                }
            }
        ];
    } catch (e) {
        console.error('[DiziBal] error:', e.message);
        return [];
    }
}

module.exports = { getStreams };
