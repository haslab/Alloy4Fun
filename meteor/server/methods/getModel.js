import { Meteor } from 'meteor/meteor'
import { Model } from '../../lib/collections/model'
import { extractSecrets,
    getCommandsFromCode,
    containsValidSecret } from '../../lib/editor/text'

Meteor.methods({

    /**
      * Receives an id and returns the corresponding model or instance. If a
      * link to a model, returns the code with or without code depending on
      * whether it is a public or private link. If public, stores the secret
      * commands so that they are still publicly available.
      *
      * @param {String} id the document id
      * @return the respective model or instance
      */
    getModel(id) {
        return getModelFromLink(id) || getModelFromInstance(id)
    }
})

/**
  * Receives a link id and returns the corresponding model. If a link to a
  * model, returns the code with or without secrets depending on whether it is
  * a public or private link. If public, stores the secret commands so that
  * they are still publicly available.
  *
  * @param {String} linkId the potential link id
  * @return the respective model
  */
function getModelFromLink(linkId) {
    const link = Link.findOne(linkId)
    if (!link) return // undefined if link does not exist
    const model = Model.findOne(link.model_id)

    // if the link is public, hide the secrets and retrieve the secrets commands
    // from the root (original)
    if (!link.private) {
        const o = Model.findOne(model.original)
        const secs = extractSecrets(o.code).secret
        model.code = extractSecrets(model.code).public
        // register secret commands
        const seccms = getCommandsFromCode(secs)
        if (seccms) model.sec_commands = seccms
    }
    model.from_private = link.private // return info about the used link type
    model.model_id = model._id // this is necessary because publish is for the linkId
    return model
}

/**
  * Receives an instance id and returns the associated model. Does not
  * distinguish between public or private, will present model as was when
  * shared. If no secrets, will retrieve commands from original root.
  *
  * @param {String} instanceId the potential instance id
  * @return the model associated with the instance
  */
function getModelFromInstance(instanceId) {
    const instance = Instance.findOne(instanceId)
    if (!instance) return // undefined if instance does not exist
    const model = Model.findOne(instance.model_id)

    if (!containsValidSecret(model.code)) {
        const o = Model.findOne(model.original)
        const secs = extractSecrets(o.code).secret
        model.code = extractSecrets(model.code).public
        // register secret commands
        const seccms = getCommandsFromCode(secs)
        if (seccms) model.sec_commands = seccms
    }

    model.instance = instance // so that frontend can access the instance
    model.from_private = false // so as to load the secrets from original on execute
    model.model_id = model._id // this is necessary because publish is for the linkId
    return model
}
