/**
 * Makes recorded outputs comparable across runs: random Meteor ids become
 * stable placeholders (numbered by first appearance) and timestamps are masked.
 */
const ID_RE = /\b[23456789ABCDEFGHJKLMNPQRSTWXYZabcdefghijkmnopqrstuvwxyz]{17}\b/g
const LOCALE_TIME_RE = /^(\d{1,2}\/\d{1,2}\/\d{4},? \d{1,2}:\d{2}:\d{2}|\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})/

export function makeNormalizer() {
    const ids = new Map()
    const mask = (s) => s.replace(ID_RE, (id) => {
        if (!ids.has(id)) ids.set(id, `<ID:${ids.size + 1}>`)
        return ids.get(id)
    })

    const walk = (v) => {
        if (typeof v === 'string') return LOCALE_TIME_RE.test(v) ? '<TIME>' : mask(v)
        if (Array.isArray(v)) return v.map(walk)
        if (v instanceof Date) return '<DATE>'
        if (v && typeof v === 'object') {
            if ('$date' in v) return '<DATE>'
            return Object.fromEntries(Object.entries(v).map(([k, x]) => [mask(k), walk(x)]))
        }
        return v
    }
    return walk
}

/**
 * Canonical form applied to both the recorded and the current result before
 * comparing: day buckets (YYYY-MM-DD) depend on the run date, and the API does
 * not guarantee the order of named entries such as the stats scalars.
 */
export function canonicalize(v) {
    if (typeof v === 'string') return /^\d{4}-\d{2}-\d{2}$/.test(v) ? '<DAY>' : v
    if (Array.isArray(v)) {
        const items = v.map(canonicalize)
        const named = items.length > 1 && items.every((x) => x && typeof x === 'object' && typeof x.name === 'string')
        return named ? items.sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0)) : items
    }
    if (v && typeof v === 'object') return Object.fromEntries(Object.entries(v).map(([k, x]) => [k, canonicalize(x)]))
    return v
}
