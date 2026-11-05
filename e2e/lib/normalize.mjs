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
