import { Navigation } from '../../lib/collections/navigation'

Meteor.methods({
    /**
      * Meteor method to execute the current model and get model instances.
      * This will call the Alloy API (webService). If the model contains
      * secrets and the previous didn't (if any), will become a new derivation
      * root (although it still registers the derivation).
      *
      * @param {String} code the Alloy model to execute
      * @param {Number} commandIndex the index of the command to execute
      * @param {String} currentModelId the id of the current model (from which
      *     the new will derive)
      *
      * @returns the instance data and the id of the new saved model
      */
    navInstance(operation, instIndex, currentModelId) {
    
            const new_nav = {
                time: new Date().toLocaleString(),
                // original code, without secrets
                operation,
                instIndex,
                model_id: currentModelId
            }

            // insert the new model
            const new_nav_id = Navigation.insert(new_nav)


    }
})

