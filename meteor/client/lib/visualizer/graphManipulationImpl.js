import { createVisualizer } from 'alloy-a4f-visualizer'
import 'alloy-a4f-visualizer/src/visualizer.css'
import { getCurrentTrace, instChanged, themeChanged } from '../editor/state'

let mountedVisualizer

function currentDocument() {
    const trace = getCurrentTrace()
    const states = trace && (trace.states || trace.instance)
    if (!states || states.length === 0) return undefined

    // A4F's static solver results may still carry loop: 0. The static flag is
    // authoritative: only temporal results should expose trace navigation.
    let loop = trace.loop
    if (trace.static) {
        loop = -1
    } else if (!Number.isInteger(loop) || loop < 0 || loop >= states.length) {
        loop = states.length === 1 ? -1 : 0
    }
    return { states, loop }
}

export function updateGraph(instance, skipLayout, replaceDocument = false) {
    if (!mountedVisualizer || !instance) return

    if (replaceDocument) {
        const document = currentDocument()
        if (document) {
            mountedVisualizer.setDocument(document)
        } else {
            mountedVisualizer.render(instance, { layout: !skipLayout })
        }
    } else {
        mountedVisualizer.render(instance, { layout: !skipLayout })
    }

    mountedVisualizer.refresh()
}

export function initGraphViewer(element, options = {}) {
    const container = document.getElementById(element)
    container.parentElement.parentElement.removeAttribute('hidden')

    if (mountedVisualizer) mountedVisualizer.destroy()

    mountedVisualizer = createVisualizer(container, {
        preferences: options.preferences || Session.get('visualizer-preferences'),
        onPreferencesChange(value) {
            Session.set('visualizer-preferences', value)
            themeChanged()
        },
        onRender: instChanged
    })
}

export function visualizerPreferences() {
    return mountedVisualizer ? mountedVisualizer.preferences() : undefined
}

export function visualizerIsEmpty() {
    return !mountedVisualizer || mountedVisualizer.isEmpty()
}

export function shareInstanceDocument() {
    return mountedVisualizer ? mountedVisualizer.shareInstanceDocument() : undefined
}

export function captureVisualizerPositions() {
    return mountedVisualizer ? mountedVisualizer.capturePositions() : undefined
}

export function clearVisualizerPositions() {
    if (mountedVisualizer) mountedVisualizer.clearPositions()
}
