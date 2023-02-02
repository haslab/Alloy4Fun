/**
 * Module with functions that handle the internal state of the app.
 *
 * @module client/lib/editor/state
 */

/** @var instances The received instances */
let instances = []

/**
 * Updates the state when the model has been changed.
 */
export function modelChanged() {
    Session.set('model-updated', true)
    Session.set('model-shared', false)
    Session.set('log-message', '')
    Session.set('log-class', '')
    Session.set('currentInstance', 0)
    Session.set('currentState', 0)
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
    if (allInstances.alloy_error || allInstances[0].cnt == 0) {
        instances = allInstances
        Session.set('currentInstance', 0)
        Session.set('maxInstance', (allInstances[0] && !allInstances[0].unsat)?allInstances.length:-1)
    } else { // a continuation batch of instances
        instances = instances.concat(allInstances)
        Session.set('maxInstance', maxInstanceNumber + allInstances.length)
    }
}

/**
 * Returns the current instance.
 *
 * @returns the current stored instance.
 */
export function getCurrentState() {
    const instanceIndex = Session.get('currentInstance')
    const stateIndex = Session.get('currentState')
    if (!instances[instanceIndex]) return undefined
    return instances[instanceIndex].instance[stateIndex]
}

export function getCurrentTrace() {
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

    log_messages.push('This challenge has timed out. '+(Session.get('frozen-next')?' Please skip to the next challenge.':'You have completed all the challenges.'))
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
    const stateIndex = Session.get('currentState')
    Session.set('currentInstance', instanceIndex + 1)
    if (!instances[instanceIndex + 1])
        return undefined
    else if (instances[instanceIndex + 1].unsat)
        return instances[instanceIndex + 1]
    else 
        return instances[instanceIndex + 1].instance[stateIndex]
}

/**
 * Sets the previous known instance as the current one and returns it.
 *
 * @returns the next stored instance.
 */
export function getPreviousInstance() {
    const instanceIndex = Session.get('currentInstance')
    const stateIndex = Session.get('currentState')
    Session.set('currentInstance', instanceIndex - 1)
    return instances[instanceIndex - 1].instance[stateIndex]
}

export function nextState() {
    const stateIndex = Session.get('currentState')
    const instanceIndex = Session.get('currentInstance')
    if (stateIndex + 1 == instances[instanceIndex].instance.length)
        Session.set('currentState',instances[instanceIndex].loop)
    else
        Session.set('currentState',stateIndex+1)
    return getCurrentState()
}

export function resetState() {
    Session.set('currentState',0)
}

export function currentState() {
    return Session.get('currentState')
}

export function setCurrentState(st) {
    Session.set('currentState',st) 
}

export function lastState() {
    if (!instances || instances.length == 0)
        return -1
    const instanceIndex = Session.get('currentInstance')
    if (!instances[instanceIndex] || !instances[instanceIndex].instance)
        return -1
    return instances[instanceIndex].instance.length
}

export function prevState() {
    const stateIndex = Session.get('currentState')
    if (stateIndex > 0)
        Session.set('currentState',stateIndex-1)
    return getCurrentState()
}

/**
 * Whether an instance is unsat.
 *
 * @param {Number} the index of the instance to be tested
 * @returns whether the instance is unsat
 */
export function isUnsatInstance(i) {
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
