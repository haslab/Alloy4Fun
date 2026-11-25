/**
 * Module with functions that handle the internal state of the app.
 *
 * @module client/lib/editor/state
 */

// final questionnaire link of a study; set it per deployment with settings.public.QUESTIONNAIRE_URL
export const QUESTIONNAIRE_URL = (Meteor.settings && Meteor.settings.public && Meteor.settings.public.QUESTIONNAIRE_URL) || '#'

/**
 * Updates the state when the model has been changed.
 */
export function modelChanged() {
    Session.set('model-updated', true)
    Session.set('model-shared', false)
    Session.set('log-message', '')
    Session.set('log-class', '')
    Session.set('currentInstance', 0)
    Session.set('maxInstance', -1)
}

/**
 * Updates the state when the selected command has been changed.
 */
export function cmdChanged() {
    Session.set('model-updated', true)
}

/**
 * Updates the state when the model has been executed.
 */
export function modelExecuted() {
    Session.set('model-updated', false)
    instChanged()
    if (!Session.get('frozen-message'))
        Session.set('from-instance', false)
}

/**
 * Updates the state when the instance has been changed.
 */
export function instChanged() {
    Session.set('inst-updated',!Session.get('inst-updated'))
    Session.set('inst-shared', false)
}

/**
 * Updates the state when the theme has been changed.
 */
export function themeChanged() {
    Session.set('model-shared', false)
    Session.set('inst-shared', false)
}

/**
 * Updates the state when the model has been shared.
 */
export function modelShared() {
    Session.set('model-shared', true)
}

/**
 * Updates the state when the instance has been shared.
 */
export function instShared() {
    Session.set('inst-shared', true)
}

/**
 * Updates the state when a new batch of instances has been received.
 */
export function storeInstances(allInstances) {
    const instanceIndex = Session.get('currentInstance')
    const maxInstanceNumber = Session.get('maxInstance')
    if (allInstances.alloy_error || allInstances[0].cnt == 0 || Array.isArray(allInstances[0].states)) {
        Session.set('instances',allInstances)
        Session.set('currentInstance', 0)
        Session.set('maxInstance', (allInstances[0] && !allInstances[0].unsat)?allInstances.length:-1)
    } else { // a continuation batch of instances

        Session.set('instances',Session.get('instances').concat(allInstances))
        Session.set('maxInstance', maxInstanceNumber + allInstances.length)
    }
}

/**
 * Returns the current instance.
 *
 * @returns the current stored instance.
 */
export function getCurrentTrace() {
    const instances = Session.get('instances')
    if (!instances) return undefined
    const instanceIndex = Session.get('currentInstance')
    return instances[instanceIndex]
}

export function disableExec() {
    if (Session.get('unsat')) return
    console.log("timed out")
    Session.set('timeout',true)

    log_messages = []
    log_classes = []

    log_messages.push('This challenge has timed out. '+(Session.get('frozen-next')?' Please skip to the next challenge.':' You have completed all the challenges. Please proceed to the <a href="'+QUESTIONNAIRE_URL+'">final questionnaire</a>.'))
    log_classes.push('log-warning')
    textEditor.setOption("readOnly",true)

    Session.set('log-message', log_messages)
    Session.set('log-class', log_classes)
}

/**
 * Sets the next known instance as the current one and returns it.
 *
 * @returns the next stored instance.
 */
export function getNextInstance() {
    const instanceIndex = Session.get('currentInstance')
    Session.set('currentInstance', instanceIndex + 1)
    const instances = Session.get('instances')
    if (!instances[instanceIndex + 1])
        return undefined
    return instances[instanceIndex + 1]
}

/**
 * Sets the previous known instance as the current one and returns it.
 *
 * @returns the next stored instance.
 */
export function getPreviousInstance() {
    const instanceIndex = Session.get('currentInstance')
    Session.set('currentInstance', instanceIndex - 1)
    const instances = Session.get('instances')
    return instances[instanceIndex - 1]
}

/**
 * Whether an instance is unsat.
 *
 * @param {Number} the index of the instance to be tested
 * @returns whether the instance is unsat
 */
export function isUnsatInstance(i) {
    const instances = Session.get('instances')
    return instances[i].unsat
}

/**
 * The index of the currently selected command.
 *
 * @returns the index of the selected command
 */
export function getCommandIndex() {
    let i = -1
    if (Session.get('commands').length == 1) { i = 0 } else if (Session.get('commands').length > 0) { i = $('.command-selection > select option:selected').index() }
    return i
}

/**
 * The label of the currently selected command.
 *
 * @returns the label of the selected command
 */
export function getCommandLabel() {
    let command
    if (Session.get('commands').length <= 1) command = Session.get('commands')[0]
    else { command = $('.command-selection > select option:selected').text() }
    return command
}
