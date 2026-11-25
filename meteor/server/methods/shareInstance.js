import { Instance } from '../../lib/collections/instance'

Meteor.methods({
    /**
      * Store a canonical JSON instance document for sharing.
      *
      * @param {String} modelId the id of the model that originated the instance
      * @param {String} command the index of the command that was executed
      * @param {Object} instance canonical JSON states/loop document
      *
      * @return the id of the new instance
      */
    async storeInstance(modelId, command, instance) {
        return Instance.insertAsync({
            model_id: modelId,
            cmd_i: command,
            graph: instance,
            time: new Date().toLocaleString()
        })
    }
})
