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
        publication.added('Stats', linkId, {
            stats: { error: err.reason || err.message || 'Statistics are unavailable' }
        })
    }
    publication.ready()
})
