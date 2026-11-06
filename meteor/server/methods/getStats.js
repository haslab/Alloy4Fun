import { Meteor } from 'meteor/meteor'
import { postApi } from '../lib/api'

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
    async getStats(linkId) {
        const link = await Link.findOneAsync(linkId)
        if (!link) return // undefined if link does not exist
        const model = await Model.findOneAsync(link.model_id)

        const content = JSON.parse(await postApi('/getStats', { model: model._id }))
        return { stats: content }
    }
})
