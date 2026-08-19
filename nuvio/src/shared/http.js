/**
 * Shared HTTP helpers for Nuvio providers
 */

export const DEFAULT_TIMEOUT_MS = 15000;

export const DEFAULT_HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,application/json,*/*;q=0.8',
    'Accept-Language': 'tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7',
    'DNT': '1',
    'Connection': 'keep-alive'
};

export function timeoutSignal(timeoutMs = DEFAULT_TIMEOUT_MS) {
    if (typeof AbortSignal !== 'undefined' && typeof AbortSignal.timeout === 'function') {
        return AbortSignal.timeout(timeoutMs);
    }
    const controller = new AbortController();
    setTimeout(() => controller.abort(), timeoutMs);
    return controller.signal;
}

export async function withTimeout(promise, timeoutMs = DEFAULT_TIMEOUT_MS, label = 'Request') {
    let timer;
    const timeoutPromise = new Promise((_, reject) => {
        timer = setTimeout(() => reject(new Error(`${label} timed out after ${timeoutMs}ms`)), timeoutMs);
    });

    try {
        return await Promise.race([promise, timeoutPromise]);
    } finally {
        clearTimeout(timer);
    }
}

export async function fetchText(url, options = {}) {
    const headers = { ...DEFAULT_HEADERS, ...(options.headers || {}) };
    const timeout = options.timeout || DEFAULT_TIMEOUT_MS;

    return await withTimeout((async () => {
        const response = await fetch(url, {
            ...options,
            headers,
            signal: timeoutSignal(timeout)
        });
        if (!response.ok) {
            throw new Error(`HTTP ${response.status} on ${url}`);
        }
        return await response.text();
    })(), timeout, url);
}

export async function fetchJson(url, options = {}) {
    const text = await fetchText(url, options);
    return JSON.parse(text);
}
