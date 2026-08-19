import { getTmdbInfo, tmdbApiKeySettingsLayout } from '../shared/tmdb.js';
import { fetchJson, fetchText } from '../shared/http.js';
import { parsePlayerJsSubtitles } from '../shared/hls.js';

const DOMAINS = ['https://dizibal.com', 'https://dizibal.net'];
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
        const mediaType = isMovie ? 'movie' : 'tv';

        let targetDomain = DOMAINS[0];
        let searchResults = [];

        for (const domain of DOMAINS) {
            try {
                const searchPath = `/api/search?q=${encodeURIComponent(tmdb.originalTitle || tmdb.title)}`;
                const data = await fetchJson(`${domain}${searchPath}`, {
                    headers: { ...HEADERS, 'Referer': `${domain}/` }
                });
                if (Array.isArray(data) && data.length > 0) {
                    targetDomain = domain;
                    searchResults = data;
                    break;
                }
            } catch {}
        }

        if (searchResults.length === 0) return [];

        // Match item by TMDB ID or title
        const item = searchResults.find(it => String(it.id) === String(id) || String(it.tmdb_id) === String(id)) || searchResults[0];
        if (!item || !item.slug) return [];

        let streamUrl = null;
        let subtitles = [];

        if (isMovie) {
            const detailUrl = `${targetDomain}/api/movie/${item.slug}`;
            const detail = await fetchJson(detailUrl, {
                headers: { ...HEADERS, 'Referer': `${targetDomain}/` }
            });
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
            const epDetail = await fetchJson(epUrl, {
                headers: { ...HEADERS, 'Referer': `${targetDomain}/` }
            });
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
                },
                subtitles
            }
        ];
    } catch (e) {
        console.error('[DiziBal] getStreams error:', e.message);
        return [];
    }
}

export function onSettings() {
    return tmdbApiKeySettingsLayout();
}
