/**
 * AtlasStream Nuvio Provider: FilmMakinesi
 * Generated for Nuvio native JS runtime
 */

const MAIN_URL = 'https://filmmakinesi.to';
const DEFAULT_TMDB_KEY = '8c598c9af9b0badc281e95b1890834bc';

function cleanTitle(str) {
    return (str || '')
        .replace(/[:!?\"'\-]/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();
}

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

        const queries = [tmdb.originalTitle, tmdb.title].filter(Boolean);
        let foundUrl = null;

        for (const query of queries) {
            const searchUrl = `${MAIN_URL}/?s=${encodeURIComponent(cleanTitle(query))}`;
            const res = await fetch(searchUrl, {
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                    'Referer': `${MAIN_URL}/`
                }
            });
            const html = await res.text();

            const articleMatches = html.match(/<article[^>]*>[\s\S]*?<\/article>/gi) || [];
            for (const article of articleMatches) {
                const linkMatch = article.match(/href=["'](https?:\/\/filmmakinesi\.to\/[^"']+)["']/i);
                const titleMatch = article.match(/<h2[^>]*>(.*?)<\/h2>/i) || article.match(/title=["'](.*?)["']/i);

                if (linkMatch && titleMatch) {
                    const pageTitle = titleMatch[1].replace(/<[^>]*>/g, '').toLowerCase();
                    const cleanQ = cleanTitle(query).toLowerCase();

                    if (pageTitle.includes(cleanQ) || cleanQ.includes(pageTitle)) {
                        foundUrl = linkMatch[1];
                        break;
                    }
                }
            }
            if (foundUrl) break;
        }

        if (!foundUrl) return [];

        let targetPageUrl = foundUrl;
        if (type === 'tv' && season && episode) {
            targetPageUrl = `${foundUrl.replace(/\/$/, '')}/sezon-${season}/bolum-${episode}`;
        }

        const pageRes = await fetch(targetPageUrl, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                'Referer': `${MAIN_URL}/`
            }
        });
        const pageHtml = await pageRes.text();

        const streams = [];
        const iframeMatches = pageHtml.match(/<iframe[^>]+src=["']([^"']+)["']/gi) || [];

        for (const iframeTag of iframeMatches) {
            const srcMatch = iframeTag.match(/src=["']([^"']+)["']/i);
            if (!srcMatch) continue;
            let src = srcMatch[1];
            if (src.startsWith('//')) src = 'https:' + src;

            if (src.includes('vidmoly') || src.includes('closeload') || src.includes('player') || src.includes('m3u8')) {
                streams.push({
                    name: 'FilmMakinesi',
                    title: `${tmdb.title || 'İçerik'} - HD Türkçe`,
                    url: src,
                    quality: '1080p',
                    headers: {
                        'Referer': `${MAIN_URL}/`,
                        'User-Agent': 'Mozilla/5.0'
                    }
                });
            }
        }

        return streams;
    } catch (e) {
        console.error('[FilmMakinesi] error:', e.message);
        return [];
    }
}

module.exports = { getStreams };
