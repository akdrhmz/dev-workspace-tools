/**
 * AtlasStream Nuvio Provider: SineWix API
 * Generated for Nuvio native JS runtime
 */

const API_BASE = 'https://ydfvfdizipanel.ru/public/api';
const API_KEY = '9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA';
const DEFAULT_TMDB_KEY = '8c598c9af9b0badc281e95b1890834bc';
const HEADERS = {
    'User-Agent': 'EasyPlex (Android 13; SM-A546E; samsung; tr)',
    'signature': '308202c3308201aba0030201020204075cec01300d06092a864886f70d01010b050030123110300e0603550403130753696e65776978',
    'hash256': 'f4d4bc98a3fc4600e7f2c2bab7533f1f03d8a70ff03c256bb11dc57050536bd0'
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
        const searchUrl = `${API_BASE}/search/${encodeURIComponent(query)}/${API_KEY}`;

        let searchData;
        try {
            const res = await fetch(searchUrl, { headers: HEADERS });
            searchData = await res.json();
        } catch {
            return [];
        }

        const items = searchData?.search || [];
        if (items.length === 0) return [];

        const isMovie = type !== 'tv';
        const matched = items.find(it => isMovie ? (it.type === 'movie') : (it.type === 'serie')) || items[0];
        if (!matched || !matched.id) return [];

        const streams = [];

        if (isMovie) {
            const detailUrl = `${API_BASE}/media/detail/${matched.id}/${API_KEY}`;
            const res = await fetch(detailUrl, { headers: HEADERS });
            const detail = await res.json();
            if (detail?.videos) {
                for (const vid of detail.videos) {
                    if (vid.link) {
                        streams.push({
                            name: `SineWix - ${vid.server || 'Hızlı Sunucu'}`,
                            title: `${tmdb.title || matched.title || 'Film'} - ${vid.lang || 'Türkçe'}`,
                            url: vid.link,
                            quality: '1080p',
                            headers: HEADERS
                        });
                    }
                }
            }
        } else {
            const detailUrl = `${API_BASE}/series/show/${matched.id}/${API_KEY}`;
            const res = await fetch(detailUrl, { headers: HEADERS });
            const detail = await res.json();
            const targetSeason = season || 1;
            const targetEpisode = episode || 1;

            const seasonObj = detail?.seasons?.find(s => s.season_number === targetSeason) || detail?.seasons?.[0];
            const epObj = seasonObj?.episodes?.find(e => e.episode_number === targetEpisode) || seasonObj?.episodes?.[0];

            if (epObj?.videos) {
                for (const vid of epObj.videos) {
                    if (vid.link) {
                        streams.push({
                            name: `SineWix - ${vid.server || 'Hızlı Sunucu'}`,
                            title: `${tmdb.title || matched.name || 'Dizi'} S${targetSeason}E${targetEpisode} - ${vid.lang || 'Türkçe'}`,
                            url: vid.link,
                            quality: '1080p',
                            headers: HEADERS
                        });
                    }
                }
            }
        }

        return streams;
    } catch (e) {
        console.error('[SineWix] error:', e.message);
        return [];
    }
}

module.exports = { getStreams };
