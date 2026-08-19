/**
 * AtlasStream Nuvio Provider: SezonlukDizi
 * Generated for Nuvio native JS runtime
 */

const DOMAIN = 'https://sezonlukdizi.vip';
const DEFAULT_TMDB_KEY = '8c598c9af9b0badc281e95b1890834bc';
const HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'tr-TR,tr;q=0.9,en;q=0.8'
};

async function getTmdbInfo(tmdbId, mediaType) {
    const url = `https://api.themoviedb.org/3/tv/${tmdbId}?api_key=${DEFAULT_TMDB_KEY}&language=tr-TR&append_to_response=external_ids`;
    try {
        const res = await fetch(url);
        const data = await res.json();
        return {
            title: data.name || '',
            originalTitle: data.original_name || ''
        };
    } catch (e) {
        return { title: '', originalTitle: '' };
    }
}

async function getStreams({ id, type, season, episode, settings }) {
    if (type === 'movie') return [];

    try {
        const tmdb = await getTmdbInfo(id, type);
        if (!tmdb.title && !tmdb.originalTitle) return [];

        const query = tmdb.originalTitle || tmdb.title;
        const searchUrl = `${DOMAIN}/diziler?adi=${encodeURIComponent(query)}`;

        const searchRes = await fetch(searchUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });
        const html = await searchRes.text();

        const match = html.match(/href=["'](https?:\/\/sezonlukdizi\.vip\/diziler\/[^"']+\.html)["']/i);
        if (!match) return [];

        const s = season || 1;
        const e = episode || 1;
        const epUrl = match[1].replace('.html', `/${s}-sezon-${e}-bolum.html`);

        const epRes = await fetch(epUrl, { headers: { ...HEADERS, 'Referer': `${DOMAIN}/` } });
        const epHtml = await epRes.text();

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
        console.error('[SezonlukDizi] error:', e.message);
        return [];
    }
}

module.exports = { getStreams };
