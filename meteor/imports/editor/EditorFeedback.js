import { EditorState, StateEffect, StateField } from '@codemirror/state'
import { Decoration, EditorView } from '@codemirror/view'

const addEditorMark = StateEffect.define()
const clearEditorMarksEffect = StateEffect.define()

const editorMarks = StateField.define({
    create() {
        return Decoration.none
    },

    update(marks, transaction) {
        // Keep existing marks aligned with document changes.
        marks = marks.map(transaction.changes)

        // Clear error and warning marks on every document edit.
        if (transaction.docChanged) {
            marks = marks.update({
                filter: (_from, _to, decoration) =>
                    decoration.spec.class?.includes('-info-')
            })
        }

        for (const effect of transaction.effects) {
            if (effect.is(clearEditorMarksEffect)) {
                // Explicit cleanup: preserve info marks.
                marks = marks.update({
                    filter: (_from, _to, decoration) =>
                        decoration.spec.class?.includes('-info-')
                })
            }

            if (effect.is(addEditorMark)) {
                const { from, to, className, readOnly } = effect.value

                if (from < to) {
                    marks = marks.update({
                        add: [
                            Decoration.mark({
                                class: className,
                                inclusive: true,
                                readOnly
                            }).range(from, to)
                        ]
                    })
                }
            }
        }

        return marks
    },

    provide: field => EditorView.decorations.from(field)
})

const preventReadOnlyEdits = EditorState.changeFilter.of(transaction => {
    const marks = transaction.startState.field(editorMarks)
    let blocked = false

    transaction.changes.iterChanges((from, to) => {
        marks.between(
            0,
            transaction.startState.doc.length,
            (markFrom, markTo, value) => {
                if (!value.spec.readOnly) return

                if (from <= markTo && to >= markFrom) {
                    blocked = true
                }
            }
        )
    })

    return !blocked
})

export const editorFeedbackExtensions = [
    editorMarks,
    preventReadOnlyEdits
]

export function clearEditorMarks(view) {
    if (!view) return

    view.dispatch({
        effects: clearEditorMarksEffect.of(null)
    })
}

export function markEditorRange(view, from, to, className, readOnly = false) {
    if (!view || !view.state) return

    const doc = view.state.doc

    const toOffset = position => {
        const lineNumber = Math.max(1, Math.min(position.line + 1, doc.lines))
        const line = doc.line(lineNumber)
        const column = Math.max(0, Math.min(position.ch, line.length))

        return line.from + column
    }

    const start = toOffset(from)
    const end = toOffset(to)

    if (end <= start) return

    view.dispatch({
        effects: addEditorMark.of({
            from: start,
            to: end,
            className,
            readOnly
        })
    })
}