/**
 * CodeMirror 6 language and Alloy4Fun-specific secret paragraph highlighting.
 *
 * alloy.js is generated from the canonical TextMate grammar. Do not edit it
 * manually; regenerate it in the alloy-syntax-highlighting project.
 */
import { alloy } from './alloy.js'
import { secretTag } from '../../lib/editor/text'
import { Decoration, ViewPlugin } from '@codemirror/view'

function escapeRegExp(value) {
    return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function secretDecorations(doc) {
    const ranges = []
    const marker = new RegExp(`[\\t ]*${escapeRegExp(secretTag)}[^\\r\\n]*`, 'g')
    const source = doc.toString()
    let match

    while ((match = marker.exec(source)) !== null) {
        if (match[0].length > 0) {
            ranges.push(Decoration.mark({ class: 'cm-secret' }).range(
                match.index,
                match.index + match[0].length
            ))
        }
    }

    return Decoration.set(ranges, true)
}

const secretParagraphHighlight = ViewPlugin.fromClass(class {
    constructor(view) {
        this.decorations = secretDecorations(view.state.doc)
    }

    update(update) {
        if (update.docChanged) {
            this.decorations = secretDecorations(update.state.doc)
        }
    }
}, {
    decorations: value => value.decorations
})

export const alloyLanguageExtensions = [
    alloy,
    secretParagraphHighlight
]
