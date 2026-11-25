/**
 * Module that handles model and instance sharing.
 *
 * @module client/lib/editor/genUrl
 */

import { displayError } from './feedback'
import { modelShared,
    getCommandIndex,
    instShared } from './state'
import { captureVisualizerPositions, visualizerPreferences, shareInstanceDocument } from '../visualizer/graphManipulationImpl'

function currentThemeData() {
    return visualizerPreferences()
}

/**
 * Store and share the current model and generate the sharing URLs.
 */
export function shareModel() {
    captureVisualizerPositions()
    const themeData = currentThemeData()
    Session.set('visualizer-preferences', themeData)

    const modelToShare = textEditor.getValue()
    Meteor.call('genURL', modelToShare, Session.get('last_id'), themeData, handleShareModel)
}

/**
 * Store and share the current instance and generate the sharing URL. The
 * respective model is already stored due to the execution.
 */
export function shareInstance() {
    const document = shareInstanceDocument()
    if (!document) return displayError('There is no instance to share', '')
    captureVisualizerPositions()
    Meteor.call('storeInstance', Session.get('last_id'), getCommandIndex(), document, handleShareInstance)
}

/**
 * Handles the response to the model sharing request.
 *
 * @param {Error} err the possible meteor error
 * @param {Object} result the result to the genURL meteor call
 */
function handleShareModel(err, result) {
    if (err) return displayError(err)

    Session.set('public-model-url', `${result.public}`)
    Session.set('private-model-url', `${result.private}`)

    modelShared()
}

/**
 * Handles the response to the instance sharing request.
 *
 * @param {Error} err the possible meteor error
 * @param {Object} result the result to the storeInstance meteor call
 */
function handleShareInstance(err, result) {
    if (err) return displayError(err)

    Session.set('inst-url', `${result}`)

    instShared()
}
