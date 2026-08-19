/**
 * AtlasStream Nuvio Provider: DiziPal
 * Generated for Nuvio native JS runtime
 */

const DOMAIN = 'https://dizipal.org';
const DEFAULT_TMDB_KEY = '8c598c9af9b0badc281e95b1890834bc';
const HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
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

        const query = tmdb.originalTitle || tmdb.title;
        const searchUrl = `${DOMAIN}/search?q=${encodeURIComponent(query)}`;

        const searchRes = await fetch(searchUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });
        const html = await searchRes.text();

        const match = html.match(/href=["'](\/dizi\/[^"']+|\/film\/[^"']+)["']/i);
        if (!match) return [];

        let targetUrl = `${DOMAIN}${match[1]}`;
        if (type === 'tv' && season && episode) {
            targetUrl = `${targetUrl}/sezon-${season}/bolum-${episode}`;
        }

        const pageRes = await fetch(targetUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });
        const pageHtml = await pageRes.text();

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
        console.error('[DiziPal] error:', e.message);
        return [];
    }
}

module.exports = { getStreams };
