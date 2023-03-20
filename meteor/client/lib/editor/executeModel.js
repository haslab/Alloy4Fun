/**
 * Module that handles model execution and navigation.
 *
 * @module client/lib/editor/genUrl
 */

import { displayError,
    markEditorError,
    markEditorWarning} from './feedback'
import { getCommandIndex,
    storeInstances,
    modelExecuted,
    getNextInstance,
    getPreviousInstance,
    instChanged,
    isUnsatInstance,
    getCommandLabel,
    resetState,
    currentState } from './state'
import { resetPositions,newInstanceSetup } from '../visualizer/projection'

const no_more_msg = 'No more satisfying instances!'

/**
 * Execute the model through the selected command. Will call the Alloy API.
 */
export function executeModel() {
    Session.set('is_running',true)
    const commandIndex = getCommandIndex()

    // no command to run
    if (commandIndex < 0) displayError('There are no commands to execute', '')

    // execute command
    else {
        modelExecuted()
        const model = textEditor.getValue()
        Meteor.call('getInstances', model, commandIndex, Session.get('from_private'), Session.get('last_id'), handleExecuteModel)
    }
}

/**
 * Show the next instance for the executed command or requests additional
 * instances if no more cached, unless already unsat. May call the Alloy API.
 */
export function nextInstance() {
    log_messages = Session.get('log-message')
    log_classes = Session.get('log-class')
    if (log_messages[log_messages.length-1] == no_more_msg) {
        log_messages.pop()
        log_classes.pop()
        Session.set('log-message',log_messages)
        Session.set('log-class',log_classes)
    }

    const instanceIndex = Session.get('currentInstance')
    const maxInstanceNumber = Session.get('maxInstance')
    // no more local instances but still not unsat
    if (instanceIndex == maxInstanceNumber - 1 && !isUnsatInstance(instanceIndex)) {
        const model = textEditor.getValue()
        Meteor.call('nextInstances', model, getCommandIndex(), Session.get('last_id'), handleExecuteModel)
    }
    const ni = getNextInstance()
    if (typeof ni !== 'undefined') {
        if (ni.unsat) {
            Session.set('currentInstance', instanceIndex)
            log_messages = Session.get('log-message')
            log_messages.push(no_more_msg)
            Session.set('log-message',log_messages)

            log_classes = Session.get('log-class')
            log_classes.push('log-info')
            Session.set('log-class',log_classes)
        } else {
            resetPositions()
            updateGraph(ni)
            newInstanceSetup()
            instChanged()
            Meteor.call('navInstance', 0, Session.get('currentInstance'), Session.get('last_id'))
        }
    }
}

/**
 * Show the previous instance for the executed command, always cached if any.
 */
export function prevInstance() {
    log_messages = Session.get('log-message')
    log_classes = Session.get('log-class')
    if (log_messages[log_messages.length-1] == no_more_msg) {
        log_messages.pop()
        log_classes.pop()
        Session.set('log-message',log_messages)
        Session.set('log-class',log_classes)
    }


    const ni = getPreviousInstance()
    if (typeof ni !== 'undefined') {
        resetPositions()
        updateGraph(ni)
        newInstanceSetup()
        instChanged()
        Meteor.call('navInstance', 1, Session.get('currentInstance'), Session.get('last_id'))
    }
}

/**
 * Handles the response of the Alloy API for the execution of a command,
 * either a fresh execution or a new batch of instances when iterating.
 *
 * @param {Error} err the possible meteor error
 * @param {Object} result the result to the getInstances or nextInstance
 *     meteor calls
 */
function handleExecuteModel(err, result) {
    Session.set('is_running',false)
    if (err) {
        maxInstanceNumber = -1
        return displayError(err)
    }
    Session.set('last_id', result.newModelId) // update the last_id for next derivations

    if (result.instances)
        result = result.instances

    if (!Session.get('frozen-message'))
        storeInstances(result)
    
    if (Array.isArray(result)) result = result[0]

    // if there error returned by Alloy
    if (result.alloy_error) {
        let resmsg = result.msg
        if (result.line) {
            resmsg = `${resmsg} (${result.line}:${result.column})-(${result.line2}:${result.column2})`
            markEditorError(result.line - 1, result.column - 1, result.line2 - 1, result.column2 - 1)
        }
        resmsg = `There was a problem running the model!\n${resmsg}.`
        console.error(resmsg)
        Session.set('log-message', resmsg)
        Session.set('log-class', 'log-error')
    }
    // else, show instance or unsat
    else {
        command = getCommandLabel()

        log_messages = []
        log_classes = []

        if (result.warning_error) {
            let resmsg = result.msg
            if (result.line) resmsg = `${resmsg} (${result.line}:${result.column})-(${result.line2}:${result.column2})`
            log_messages.push(`There is a possible problem with the model!\n${resmsg}\n`)
            log_classes.push('log-warning')
            markEditorWarning(result.line - 1, result.column - 1, result.line2 - 1, result.column2 - 1)
        }
        if (result.unsat) {
            log_messages.push(result.check ? (Session.get('frozen-message')?('The incorrect specification has been fixed.'+(Session.get('frozen-next')?' Please skip to the next challenge.':' You have completed all the challenges.')):`No counter-example found. ${command} may be valid.`) : `No instance found. ${command} may be inconsistent.`)
            log_classes.push(result.check ? 'log-complete' : 'log-wrong')
            if (Session.get('frozen-message'))
                textEditor.setOption("readOnly",true)
        } else {
            log_messages.push(result.check ? (Session.get('frozen-message')?`The specification is still incorrect. Please try again.`:`Counter-example found. ${command} is invalid.`) : `Instance found. ${command} is consistent.`)
            log_classes.push(result.check ? 'log-wrong' : 'log-complete')
            if (!Session.get('frozen-message')) {
                initGraphViewer('instance')
                resetPositions()
                resetState()
                updateGraph(result.instance[currentState()])
                newInstanceSetup()
            }
        }
        Session.set('unsat',result.unsat)
        Session.set('log-message', log_messages)
        Session.set('log-class', log_classes)
    }
}
