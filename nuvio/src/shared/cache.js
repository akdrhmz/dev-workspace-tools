/**
 * Simple in-memory TTL cache for Nuvio providers
 */
export function createTtlCache(ttlMs = 30 * 60 * 1000, maxEntries = 300) {
    const store = new Map();

    return {
        get(key) {
            const entry = store.get(key);
            if (!entry) return null;
            if (Date.now() > entry.expiry) {
                store.delete(key);
                return null;
            }
            return entry.value;
        },
        set(key, value) {
            if (store.size >= maxEntries) {
                const oldest = store.keys().next().value;
                if (oldest) store.delete(oldest);
            }
            store.set(key, { value, expiry: Date.now() + ttlMs });
        },
        has(key) {
            return this.get(key) !== null;
        },
        delete(key) {
            store.delete(key);
        },
        clear() {
            store.clear();
        }
    };
}
