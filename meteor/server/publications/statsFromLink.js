/**
 * Publishes the link information to the client
 */
Meteor.publish('statsFromLink', function (linkId) {

    const publication = this
    Meteor.call('getStats', linkId, (err, model) => {
	    // see https://docs.meteor.com/api/pubsub.html#Subscription-added
        if (!err) publication.added('Stats', linkId, model)
        publication.ready()
    })
})
