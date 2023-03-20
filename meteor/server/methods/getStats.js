import { Meteor } from 'meteor/meteor'
import { Stats } from '../../lib/collections/stats'

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

            const models = Model.find({
              original: model._id
            }).fetch()

            const links = Link.find().fetch()

            const instances = Instance.find().fetch()

            const navigations = Navigation.find().fetch()

            console.log(links, models, instances)


            HTTP.call('POST', `${Meteor.settings.env.API_URL}/getStats`, {
                data: {
                    model: model._id, models, instances, links, navigations
                }
            }, (error, result) => {

                if (error) reject(error)

                const content = JSON.parse(result.content)
                
                const new_stats = {
                    time: content.time, 
                    model: content.model,
                    name: content.name,
                    scalars: content.scalars
                }

                Stats.insert(new_stats)  

                // resolve the promise
                resolve({
                    stats: content
                })
            })
        })
    }
})

