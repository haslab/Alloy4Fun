/**
 * Publishes the link information to the client
 */
Meteor.publish('modelFromLink', async function (linkId) {
    const publication = this

    try {
        const model = await Meteor.callAsync('getModel', linkId)
        // see https://docs.meteor.com/api/pubsub.html#Subscription-added
        publication.added('Model', linkId, model || {})
    } catch (err) {
        // an unavailable model is published as nothing
    }
    publication.ready()
})
