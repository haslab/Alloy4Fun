/**
 * Minimal client-side router exposing the subset of the former iron:router
 * API used by the application: Router.current() (with params and data()) and
 * Router.go(). The matched template is rendered by the `router` template.
 *
 * @module client/lib/router
 */

import { Meteor } from 'meteor/meteor'
import { Tracker } from 'meteor/tracker'
import { ReactiveVar } from 'meteor/reactive-var'
import { Model } from '../../lib/collections/model'
import { Stats } from '../../lib/collections/stats'

// order matters: the first matching route wins
const routes = [
    { pattern: /^\/$/, template: 'alloyEditor' },
    {
        pattern: /^\/stats\/?$/,
        template: 'modelStats'
    },
    {
        pattern: /^\/stats\/([^/]+)\/?$/,
        template: 'modelStats',
        subscription: 'statsFromLink',
        data: (id) => Stats.findOne(id) || { code: 'Unable to retrieve Model from Link' }
    },
    {
        pattern: /^\/([^/]+)\/?$/,
        template: 'alloyEditor',
        subscription: 'modelFromLink',
        data: (id) => Model.findOne(id) || { code: 'Unable to retrieve Model from Link' }
    }
]

const currentRoute = new ReactiveVar(null)
const currentView = new ReactiveVar({ template: 'loading' })

function match(path) {
    for (const def of routes) {
        const m = def.pattern.exec(path)
        if (m) {
            const route = { def, params: { _id: m[1] ? decodeURIComponent(m[1]) : undefined } }
            if (def.data) route.data = () => def.data(route.params._id)
            return route
        }
    }
    return null
}

function resolve() {
    currentRoute.set(match(window.location.pathname))
}

export const Router = {
    /** The active route: { params, data? } (undefined data if it has none). */
    current() {
        return currentRoute.get()
    },

    /** Navigates to a path without reloading the page. */
    go(path) {
        const url = new URL(path, window.location.origin)
        if (url.pathname + url.search + url.hash !== window.location.pathname + window.location.search + window.location.hash) {
            window.history.pushState({}, '', url.pathname + url.search + url.hash)
        }
        resolve()
    },

    /** Reactive description of what the `router` template must render. */
    view() {
        return currentView.get()
    }
}

window.Router = Router

window.addEventListener('popstate', resolve)

// same-origin link clicks are handled client-side, as page reloads would trigger onbeforeunload
document.addEventListener('click', (e) => {
    if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return
    const a = e.target.closest && e.target.closest('a[href]')
    if (!a || (a.target && a.target !== '_self') || a.hasAttribute('download')) return
    if (a.origin !== window.location.origin || !match(a.pathname)) return
    e.preventDefault()
    Router.go(a.pathname + a.search + a.hash)
})

Meteor.startup(() => {
    resolve()

    // template selection must not depend on the data, only on the route and subscription
    Tracker.autorun(() => {
        const route = currentRoute.get()
        if (!route) return currentView.set({ template: 'notFound' })
        if (route.def.subscription) {
            const handle = Meteor.subscribe(route.def.subscription, route.params._id)
            if (!handle.ready()) return currentView.set({ template: 'loading' })
        }
        currentView.set({ template: route.def.template })
    })
})
