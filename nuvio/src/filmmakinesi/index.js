import { getTmdbInfo, tmdbApiKeySettingsLayout } from '../shared/tmdb.js';
import { fetchText } from '../shared/http.js';
import { detectQuality } from '../shared/hls.js';

const MAIN_URL = 'https://filmmakinesi.to';

function cleanTitle(str) {
    return (str || '')
        .replace(/[:!?\"'\-]/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();
}

export async function getStreams({ id, type, season, episode, settings }) {
    try {
        const tmdb = await getTmdbInfo(id, type);
        if (!tmdb.title && !tmdb.originalTitle) return [];

        const queries = [tmdb.originalTitle, tmdb.title].filter(Boolean);
        let foundUrl = null;

        for (const query of queries) {
            const searchUrl = `${MAIN_URL}/?s=${encodeURIComponent(cleanTitle(query))}`;
            const html = await fetchText(searchUrl, {
                headers: { 'Referer': `${MAIN_URL}/` }
            });

            // Parse search results
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

        // If it's a TV series and season/episode specified
        let targetPageUrl = foundUrl;
        if (type === 'tv' && season && episode) {
            targetPageUrl = `${foundUrl.replace(/\/$/, '')}/sezon-${season}/bolum-${episode}`;
        }

        const pageHtml = await fetchText(targetPageUrl, {
            headers: { 'Referer': `${MAIN_URL}/` }
        });

        // Extract iframes / video sources
        const streams = [];
        const iframeMatches = pageHtml.match(/<iframe[^>]+src=["']([^"']+)["']/gi) || [];

        for (const iframeTag of iframeMatches) {
            const srcMatch = iframeTag.match(/src=["']([^"']+)["']/i);
            if (!srcMatch) continue;
            let src = srcMatch[1];
            if (src.startsWith('//')) src = 'https:' + src;

            if (src.includes('vidmoly') || src.includes('closeload') || src.includes('player')) {
                streams.push({
                    name: 'FilmMakinesi',
                    title: `${tmdb.title || 'İçerik'} - Türkçe`,
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
        console.error('[FilmMakinesi] getStreams error:', e.message);
        return [];
    }
}

export function onSettings() {
    return tmdbApiKeySettingsLayout();
}
