/**
 * TMDB API Helper for Nuvio Providers
 */

import { fetchJson } from './http.js';
import { createTtlCache } from './cache.js';

const tmdbCache = createTtlCache(30 * 60 * 1000, 300);
const DEFAULT_API_KEY = '8c598c9af9b0badc281e95b1890834bc';

export function getTmdbApiKey() {
    try {
        const settings = typeof globalThis !== 'undefined' ? globalThis.SCRAPER_SETTINGS : null;
        if (settings?.tmdbApiKey) return String(settings.tmdbApiKey).trim();
    } catch {}
    return DEFAULT_API_KEY;
}

export async function getTmdbInfo(tmdbId, mediaType = 'movie') {
    const isMovie = mediaType === 'movie';
    const type = isMovie ? 'movie' : 'tv';
    const cacheKey = `${type}:${tmdbId}`;

    const cached = tmdbCache.get(cacheKey);
    if (cached) return cached;

    const apiKey = getTmdbApiKey();
    const url = `https://api.themoviedb.org/3/${type}/${tmdbId}?api_key=${apiKey}&language=tr-TR&append_to_response=external_ids`;

    try {
        const data = await fetchJson(url);

        const trTitle = isMovie ? (data.title || '') : (data.name || '');
        const origTitle = isMovie ? (data.original_title || '') : (data.original_name || '');
        const releaseDate = isMovie ? data.release_date : data.first_air_date;
        const year = releaseDate ? parseInt(releaseDate.slice(0, 4), 10) : null;
        const imdbId = data.external_ids?.imdb_id || null;

        const info = {
            id: tmdbId,
            type,
            title: trTitle || origTitle,
            originalTitle: origTitle,
            year,
            imdbId
        };

        tmdbCache.set(cacheKey, info);
        return info;
    } catch (e) {
        console.warn(`[TMDB] Failed to fetch info for ${type} ${tmdbId}:`, e.message);
        return {
            id: tmdbId,
            type,
            title: '',
            originalTitle: '',
            year: null,
            imdbId: null
        };
    }
}

export function tmdbApiKeySettingsLayout() {
    return [
        {
            key: 'tmdbApiKey',
            label: 'Özel TMDB API Anahtarı',
            description: 'Kendi ücretsiz themoviedb.org API anahtarınızı girebilirsiniz. Boş bırakılırsa genel anahtar kullanılır.',
            type: 'text',
            default: ''
        }
    ];
}
