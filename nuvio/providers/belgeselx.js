/**
 * AtlasStream Nuvio Provider: BelgeselX
 * Generated for Nuvio native JS runtime
 */

const DOMAIN = 'https://belgeselx.com';
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

        const query = tmdb.title || tmdb.originalTitle;
        const searchUrl = `${DOMAIN}/?s=${encodeURIComponent(query)}`;

        const searchRes = await fetch(searchUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });
        const html = await searchRes.text();

        const matches = html.match(/href=["'](https?:\/\/belgeselx\.com\/[^"']+)["']/gi) || [];
        if (matches.length === 0) return [];

        const pageUrl = matches[0].match(/href=["']([^"']+)["']/i)[1];
        const pageRes = await fetch(pageUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });
        const pageHtml = await pageRes.text();

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
        console.error('[BelgeselX] error:', e.message);
        return [];
    }
}

module.exports = { getStreams };
