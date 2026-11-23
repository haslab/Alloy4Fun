/**
 * This file specifies the syntax highlighting rules for the codemirror editor
 * each rule is composed by regex and token, which allows for css rules on the tokens
 * such as .cm.comment {...} and also to refer to those tokens after parsing the syntax
 * Additionaly "sol: true" attribute means that the match should happen only at line start
 */
import CodeMirror from 'codemirror'
import * as simpleMode from 'codemirror/addon/mode/simple' // do not remove despite unused warning
import { paragraphKeywords, secretTag } from '../../lib/editor/text'

export { defineAlloyMode }

function defineAlloyMode() {
    const keywords = `${paragraphKeywords}|one|lone|none|some|abstract|all|iff|but|else|extends|set|implies|module|open|and|disj|for|in|no|or|as|Int|String|sum|exactly|iden|let|not|univ|enum|var|steps|always|historically|eventually|once|after|before|until|since|releases|triggered`
    const tag = secretTag.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    CodeMirror.defineSimpleMode('alloy', {
        start: [{
            regex: `(\\W)(${keywords})(?:\\b)`,
            token: [null, 'keyword']
        }, {
            regex: `(${keywords})(?:\\b)`,
            token: 'keyword',
            sol: true
        }, {
            regex: new RegExp(`\s*${tag}.*`, 'mg'),
            token: 'secret',
            sol: false
        }, {
            regex: /\/\*\*/,
            token: 'commentB',
            next: 'commentB' // Jump to comment mode.
        }, {
            regex: /\/\*/,
            token: 'comment',
            next: 'comment' // Jump to comment mode.
        }, {
            // Line comment.
            regex: /(\/\/.*)|(--.*)/,
            token: 'comment'
        }, {
            regex: /(\+\+ )|=>|<=>|\+\+|=<|->|>=|\|\||<:|:>|&&|!=|\+|-|&|\.|~|\*|\^|!|#|\'/,
            token: 'operator'
        }, {
            regex: /(\s+|\||{|})[0-9]+](?:\b)/,
            token: [null, 'number']
        }, {
            regex: /(\s+|\||{|})[0-9]+](?:\b)/,
            token: 'number',
            // Rule that only applies if the match is at the start of some line(workaround).
            sol: true
        }, {
            regex: /[{(]/,
            indent: true
        }, {
            regex: /[})]/,
            dedent: true
        }],
        // Modes allow applying a different set of rules to different contexts.
        commentB: [{
            // When the comment block end tag is found...
            regex: /.*?\*\//,
            token: 'commentB',
            next: 'start' // ...go back to start mode.
        }, {
            regex: /.*/,
            token: 'commentB'
        }],
        comment: [{
            // When the comment block end tag is found...
            regex: /.*?\*\//,
            token: 'comment',
            next: 'start' // ...go back to start mode.
        }, {
            regex: /.*/,
            token: 'comment'
        }
        ],
        // Simple Mode additional settings, check documentation for more information.
        meta: {
            // Prevent indentation inside comments.
            dontIndentStates: ['comment'],
            lineComment: '//'
        }
    })
}
