/**
 * Publishes the link information to the client
 */
Meteor.publish('modelFromLink', function (linkId) {
    const publication = this
	
    Meteor.call('getModel', linkId, (err, model) => {
    	// see https://docs.meteor.com/api/pubsub.html#Subscription-added
        if (!err) publication.added('Model', linkId, model)
        publication.ready()
    })
})
