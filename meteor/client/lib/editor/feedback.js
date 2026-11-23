/**
 * Module with feedback functions.
 *
 * @module client/lib/editor/feedback
 */

import swal from 'sweetalert'
import 'sweetalert/dist/sweetalert.css'
import {
    markEditorRange,
    clearEditorMarks as clearMarks
} from '/imports/editor/EditorFeedback'

export function clearEditorMarks() {
    clearMarks(textEditor.view)
}
/**
 * Display meteor error to the user and report in the console.
 *
 * @param {Error} err the meteor error
 * @returns {Error} the meteor error
 */
export function displayError(err) {
    console.error(err)
    swal({
        type: 'error',
        title: err.error,
        text: err.reason
    })
    return err
}

/**
 * Mark text in the CodeMirror 6 editor. Positions retain the CodeMirror 5
 * convention used by the existing Alloy4Fun callers: zero-based line/ch.
 */
export function markEditor(from, to, className, readOnly) {
    markEditorRange(textEditor.view, from, to, className, readOnly)
}

/** Mark the text in the editor with an error. */
export function markEditorError(line, col, line2, col2) {
    markEditor({ line, ch: col }, { line: line2, ch: col2 + 1 }, 'editor-error-mark', false)
}

export function markEditorInfo(line, col, line2, col2) {
    markEditor({ line, ch: 1 }, { line: line2, ch: 999 }, 'editor-info-freeze', true)
    markEditor({ line, ch: col }, { line: line2, ch: col2 + 1 }, 'editor-info-mark', true)
}

/** Mark the text in the editor with a warning. */
export function markEditorWarning(line, col, line2, col2) {
    markEditor({ line, ch: col }, { line: line2, ch: col2 + 1 }, 'editor-warning-mark', false)
}
