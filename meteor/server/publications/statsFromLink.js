/**
 * Publishes the link information to the client
 */
Meteor.publish('statsFromLink', async function (linkId) {
    const publication = this

    try {
        const model = await Meteor.callAsync('getStats', linkId)
        // see https://docs.meteor.com/api/pubsub.html#Subscription-added
        publication.added('Stats', linkId, model)
    } catch (err) {
        // unavailable stats are published as nothing
    }
    publication.ready()
})
