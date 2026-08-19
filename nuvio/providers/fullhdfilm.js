/**
 * AtlasStream Nuvio Provider: FullHDFilmizlesene
 * Generated for Nuvio native JS runtime
 */

const DOMAIN = 'https://www.fullhdfilmizlesene.pw';
const DEFAULT_TMDB_KEY = '8c598c9af9b0badc281e95b1890834bc';
const HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'tr-TR,tr;q=0.9,en;q=0.8'
};

async function getTmdbInfo(tmdbId, mediaType) {
    const url = `https://api.themoviedb.org/3/movie/${tmdbId}?api_key=${DEFAULT_TMDB_KEY}&language=tr-TR&append_to_response=external_ids`;
    try {
        const res = await fetch(url);
        const data = await res.json();
        return {
            title: data.title || '',
            originalTitle: data.original_title || ''
        };
    } catch (e) {
        return { title: '', originalTitle: '' };
    }
}

async function getStreams({ id, type, season, episode, settings }) {
    if (type === 'tv') return [];

    try {
        const tmdb = await getTmdbInfo(id, type);
        if (!tmdb.title && !tmdb.originalTitle) return [];

        const query = tmdb.originalTitle || tmdb.title;
        const searchUrl = `${DOMAIN}/arama/${encodeURIComponent(query)}`;

        const searchRes = await fetch(searchUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });
        const html = await searchRes.text();

        const movieLinks = html.match(/href=["'](https?:\/\/www\.fullhdfilmizlesene\.pw\/film\/[^"']+)["']/gi) || [];
        if (movieLinks.length === 0) return [];

        const movieUrl = movieLinks[0].match(/href=["']([^"']+)["']/i)[1];
        const movieRes = await fetch(movieUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });
        const movieHtml = await movieRes.text();

        const iframes = movieHtml.match(/<iframe[^>]+src=["']([^"']+)["']/gi) || [];
        const streams = [];

        for (const ifr of iframes) {
            const src = ifr.match(/src=["']([^"']+)["']/i)?.[1];
            if (src && (src.includes('player') || src.includes('embed') || src.includes('m3u8') || src.includes('video'))) {
                streams.push({
                    name: 'FullHDFilmizlesene',
                    title: `${tmdb.title || 'Film'} - 1080p Türkçe`,
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
        console.error('[FullHDFilm] error:', e.message);
        return [];
    }
}

module.exports = { getStreams };
