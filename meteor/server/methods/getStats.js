import { Meteor } from 'meteor/meteor'

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
    getStats(linkId) {
        return new Promise((resolve, reject) => {
            
            const link = Link.findOne(linkId)
            if (!link) return // undefined if link does not exist
            const model = Model.findOne(link.model_id)

            HTTP.call('POST', `${Meteor.settings.env.API_URL}/getStats`, {
                data: {
                    model: model._id
                }
            }, (error, result) => {

                if (error) reject(error)
                else {
                  const content = JSON.parse(result.content)

                  // resolve the promise
                  resolve({
                      stats: content
                  })
                }
            })
        })
    }
})

