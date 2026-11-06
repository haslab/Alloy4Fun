import { postApi } from '../lib/api'
import { extractSecrets,
    containsValidSecret } from '../../lib/editor/text'

Meteor.methods({
    /**
      * Meteor method to get additional solutions to the current model. This
      * will call the Alloy API (webService) without creating a new model in
      * the database.
      *
      * @param {Number} commandIndex the index of the command to execute
      * @param {String} currentModelId the id of the current model (from which
      *     the new will derive)
      *
      * @returns the instance data and the id of the new saved model
      */
    async nextInstances(code, commandIndex, currentModelId) {
        // must send model in case session has expired and must be restarted
        // if no secrets, try to extract from original
        let code_with_secrets = code
        if (currentModelId && !containsValidSecret(code)) {
            const o = (await Model.findOneAsync(currentModelId)).original
            code_with_secrets = code + extractSecrets((await Model.findOneAsync(o)).code).secret
        }

        // call webservice to get instances
        const content = JSON.parse(await postApi('/getInstances', {
            model: code_with_secrets,
            numberOfInstances: Meteor.settings.env.MAX_INSTANCES,
            commandIndex,
            sessionId: currentModelId,
            parentId: ''
        }))

        return {
            instances: content,
            newModelId: currentModelId
        }
    }
})
