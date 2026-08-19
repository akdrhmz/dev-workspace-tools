/**
 * HLS and Subtitle helper utilities
 */

export function detectQuality(text) {
    if (!text) return 'Auto';
    const lower = text.toLowerCase();
    if (lower.includes('4k') || lower.includes('2160p')) return '4K';
    if (lower.includes('1080p') || lower.includes('fhd')) return '1080p';
    if (lower.includes('720p') || lower.includes('hd')) return '720p';
    if (lower.includes('480p') || lower.includes('sd')) return '480p';
    if (lower.includes('360p')) return '360p';
    return '1080p';
}

export function parsePlayerJsSubtitles(subtitleStr) {
    if (!subtitleStr) return [];
    // Formats like: "[Türkçe]https://.../tr.vtt,[İngilizce]https://.../en.vtt"
    return subtitleStr.split(',').map(part => {
        const match = part.match(/^\s*\[([^\]]*)\]\s*(\S+)\s*$/);
        if (!match) return null;
        const label = match[1].trim();
        const url = match[2].trim();
        const lowerLabel = label.toLowerCase();
        const lang = (lowerLabel.includes('turk') || lowerLabel.includes('tr')) ? 'tr' :
                     (lowerLabel.includes('ing') || lowerLabel.includes('en')) ? 'en' : 'tr';
        return { url, label, lang };
    }).filter(Boolean);
}
