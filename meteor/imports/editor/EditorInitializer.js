import { EditorState } from '@codemirror/state'
import { oneDark,     oneDarkTheme,
    oneDarkHighlightStyle } from '@codemirror/theme-one-dark'
import {
    closeBrackets,
    closeBracketsKeymap
} from '@codemirror/autocomplete'
import {
    bracketMatching,
    defaultHighlightStyle,
    HighlightStyle,
    indentOnInput,
    syntaxHighlighting
} from '@codemirror/language'
import {
    defaultKeymap,
    history,
    historyKeymap,
    indentWithTab
} from '@codemirror/commands'
import {
    drawSelection,
    dropCursor,
    EditorView,
    highlightActiveLine,
    highlightActiveLineGutter,
    keymap,
    lineNumbers,
    placeholder
} from '@codemirror/view'
import { tags } from '@lezer/highlight'

import { alloyLanguageExtensions } from '/imports/editor/AlloyEditorMode'
import { editorFeedbackExtensions } from '/imports/editor/EditorFeedback'
import {
    getCommandsFromCode,
    containsValidSecret
} from '../../lib/editor/text'
import 'qtip2'
import { modelChanged } from '../../client/lib/editor/state'

export { initializeAlloyEditor }

/**
 * A Twilight-inspired CodeMirror 6 theme. The old CodeMirror 5 theme CSS
 * cannot style CodeMirror 6, so the editor theme is defined with CM6 APIs.
 */
const alloyHighlightStyle = HighlightStyle.define([
    { tag: tags.keyword, color: '#f9ee98' },
    { tag: tags.operator, color: '#cda869' },
    { tag: tags.punctuation, color: '#e9e9e9' },
    { tag: tags.comment, color: '#777777', fontStyle: 'italic' },
    { tag: tags.docComment, color: '#777777', fontStyle: 'italic'  },
    { tag: tags.string, color: '#8f9d6a' },
    { tag: tags.number, color: '#cf6a4c' },
    { tag: tags.constant(tags.variableName), color: '#cf6a4c' },
    { tag: tags.typeName, color: '#9b703f' },
    { tag: tags.function(tags.variableName), color: '#9b859d' },
    { tag: tags.variableName, color: '#e9e9e9' },
    { tag: tags.atom, color: '#cf6a4c' }
])

/**
 * Initialize the Alloy editor.
 *
 * The returned object exposes the common CodeMirror 5 methods used by this
 * initializer's callers. The native CodeMirror 6 EditorView is available as
 * editor.view.
 *
 * htmlElement is the original textarea. It is kept in the DOM (hidden) and
 * synchronized so existing form submission and textarea-based code continue
 * to receive the current model text.
 */
function initializeAlloyEditor(htmlElement) {
    const initialValue = htmlElement.value || htmlElement.textContent || ''
    const host = document.createElement('div')
    host.className = 'alloy-codemirror6'
    htmlElement.insertAdjacentElement('afterend', host)
    htmlElement.hidden = true

    let editor
    const changeHandlers = new Set()

    const updateListener = EditorView.updateListener.of(update => {
        if (!update.docChanged) return

        const value = update.state.doc.toString()
        htmlElement.value = value

        $('.qtip').remove()

        if ($.trim(value) === '') {
            Session.set('empty-model', true)
        } else {
            Session.set('empty-model', false)
            const localSecrets = containsValidSecret(value)
            Session.set('local-secrets', localSecrets)

            const hiddenCommands = localSecrets
                ? []
                : (Session.get('hidden_commands') || [])

            Session.set(
                'commands',
                getCommandsFromCode(value).concat(hiddenCommands)
            )
        }

        modelChanged()

        for (const handler of changeHandlers) {
            handler(editor, update)
        }
    })

    const state = EditorState.create({
        doc: initialValue,
        extensions: [
            ...alloyLanguageExtensions,
            oneDarkTheme,
            syntaxHighlighting(oneDarkHighlightStyle),
            ...editorFeedbackExtensions,
            lineNumbers(),
            highlightActiveLineGutter(),
            drawSelection(),
            dropCursor(),
            history(),
            indentOnInput(),
            bracketMatching(),
            highlightActiveLine(),
            closeBrackets(),
            placeholder('Type your Alloy model here...'),
            EditorView.lineWrapping,
            keymap.of([
                ...defaultKeymap,
                ...historyKeymap,
                indentWithTab,
                closeBracketsKeymap
            ]),
            updateListener
        ]
    })

    const view = new EditorView({ state, parent: host })

    // Keep the hidden textarea synchronized from the initial render onward.
    htmlElement.value = initialValue

    editor = {
        view,

        getValue() {
            return view.state.doc.toString()
        },

        setValue(value) {
            const current = view.state.doc
            view.dispatch({
                changes: {
                    from: 0,
                    to: current.length,
                    insert: String(value)
                }
            })
        },

        on(eventName, handler) {
            if (eventName === 'change' && typeof handler === 'function') {
                changeHandlers.add(handler)
            }
            return this
        },

        off(eventName, handler) {
            if (eventName === 'change') changeHandlers.delete(handler)
            return this
        },

        focus() {
            view.focus()
        },

        setSize(width, height) {
            if (width != null) view.dom.style.width = typeof width === 'number' ? `${width}px` : width
            if (height != null) view.dom.style.height = typeof height === 'number' ? `${height}px` : height
            view.requestMeasure()
        },

        getWrapperElement() {
            return view.dom
        },

        refresh() {
            view.requestMeasure()
        },

        destroy() {
            htmlElement.value = view.state.doc.toString()
            view.destroy()
            host.remove()
            htmlElement.hidden = false
            changeHandlers.clear()
        }
    }

    editor.setSize('100%', 400)
    return editor
}
