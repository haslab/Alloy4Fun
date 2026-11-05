/**
 * Minimal DDP client (method calls and subscriptions) over the native
 * WebSocket available in Node 22+. Enough to exercise Meteor methods and
 * publications exactly as the browser client does.
 */
export class DdpClient {
    constructor(url, timeoutMs = 20000) {
        this.url = url
        this.timeoutMs = timeoutMs
        this.nextId = 1
        this.pending = new Map()
        this.subs = new Map()
    }

    connect() {
        return new Promise((resolve, reject) => {
            this.ws = new WebSocket(this.url)
            this.ws.onerror = (e) => reject(new Error(`DDP connection failed: ${e.message || this.url}`))
            this.ws.onopen = () => this.send({ msg: 'connect', version: '1', support: ['1'] })
            this.ws.onmessage = (ev) => {
                const m = JSON.parse(ev.data)
                if (m.msg === 'connected') resolve()
                else this.handle(m)
            }
        })
    }

    send(m) {
        this.ws.send(JSON.stringify(m))
    }

    handle(m) {
        if (m.msg === 'ping') return this.send({ msg: 'pong', id: m.id })
        if (m.msg === 'result') {
            const p = this.pending.get(m.id)
            this.pending.delete(m.id)
            if (m.error) p.resolve({ error: { error: m.error.error, reason: m.error.reason } })
            else p.resolve({ result: m.result })
        }
        if (m.msg === 'added' || m.msg === 'changed' || m.msg === 'removed') {
            for (const s of this.subs.values()) s.events.push(m)
        }
        if (m.msg === 'ready') {
            for (const id of m.subs) this.subs.get(id)?.ready()
        }
        if (m.msg === 'nosub') this.subs.get(m.id)?.ready(m.error)
    }

    /**
     * Calls a method; always resolves with { result }, { error } or, if the
     * server never answers, { timeout: true }. A method that never answers
     * blocks later calls on the same connection, so use a fresh client after.
     */
    call(method, ...params) {
        const id = String(this.nextId++)
        return new Promise((resolve) => {
            const timer = setTimeout(() => {
                this.pending.delete(id)
                resolve({ timeout: true })
            }, this.timeoutMs)
            this.pending.set(id, { resolve: (r) => { clearTimeout(timer); resolve(r) } })
            this.send({ msg: 'method', method, params, id })
        })
    }

    /** Subscribes and resolves with the documents received until ready. */
    subscribe(name, ...params) {
        const id = String(this.nextId++)
        return new Promise((resolve) => {
            const timer = setTimeout(() => resolve({ timeout: true, events: sub.events }), this.timeoutMs)
            const sub = { events: [], ready: (error) => { clearTimeout(timer); resolve({ events: sub.events, error }) } }
            this.subs.set(id, sub)
            this.send({ msg: 'sub', id, name, params })
        })
    }

    close() {
        this.ws.close()
    }
}
