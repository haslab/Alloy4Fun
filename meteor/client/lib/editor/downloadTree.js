/**
 * Module that handles the downloading of the derivation tree.
 *
 * @module client/lib/editor/genUrl
 */

import { displayError } from './feedback'

/**
 * Creates the descendants tree for the current model and triggers its
 * download as a text file.
 */
export function downloadTree() {
    const linkId = Router.current().params._id
    Meteor.call('downloadTree', linkId, (err, res) => {
        if (err) return displayError(err)
        const d = new Date()
        download(`tree_${linkId}_${d.getFullYear()}_${lz(d.getMonth() + 1)}_${lz(d.getDate())}_${lz(d.getHours())}_${lz(d.getMinutes())}_${lz(d.getSeconds())}.json`, JSON.stringify(descendantsToTree(res)))
    })
}

export function createTree() {
    const linkId = Router.current().params._id
    Meteor.call('downloadTree', linkId, (err, res) => {
        if (err) return displayError(err)
        const d = new Date()
        Session.set('derivation',descendantsToTree(res))
    })
}

/**
 * Converts a list of flat descendants into a tree object using Hashmap and
 * DFS.
 *
 * @param {Object} res with descendants and root as properties
 * @returns the root of the tree
 */
export function descendantsToTree(res) {
    const { descendants } = res
    const { root } = res
    // get all the ids
    const ids = descendants.map(x => x._id)
    ids.push(root._id)
    // generate a hashmap of id -> direct child
    const hashmap = {}
    ids.forEach(id => hashmap[id] = [])
    descendants.forEach((model) => {
        // the root may derive from other model
        if (hashmap[model.derivationOf]) hashmap[model.derivationOf].push(model)
    })
    // depth first search to obtain recursive tree structure
    let current; const
        queue = [root]
    root.depth = 0
    while (queue.length) {
        current = queue.shift()
        current.children = current.children || []
        hashmap[current._id].forEach((model) => {
            queue.push(model)
            model.depth = current.depth + 1
            current.children.push(model)
        })
    }
    return root
}

/**
 * Triggers the download of a file with a given content.
 *
 * @param {String} filename the name of the file to be downloaded
 * @param {String} text the content of the file
 */
function download(filename, text) {
    const anchor = document.createElement('a')
    anchor.setAttribute('href', `data:text/plain;charset=utf-8,${encodeURIComponent(text)}`)
    anchor.setAttribute('download', filename)

    anchor.style.display = 'none'
    document.body.appendChild(anchor)

    anchor.click()

    document.body.removeChild(anchor)
}

/**
 * Adds leading zeros to string: 9->09, 19->19.
 *
 * @param {String} s
 * @returns s with leading zeros
 */
function lz(s) {
    return (`0${s}`).slice(-2)
}
