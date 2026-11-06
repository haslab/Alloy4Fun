import { Model } from '../../lib/collections/model'

Meteor.methods({
    /**
      * Meteor method to retrieve the information about the current node's
      * descendants so as to produce the derivation tree, relying on the
      * original field. The actual creation of the tree is done client-side
      * for efficiency purposes.
      *
      * @param {String} linkId the (private) link that identifies the root
      *     node
      *
      * @return all the descendants of the root node and itself
      */
    async downloadTree(linkId) {
        if (!linkId) throw new Meteor.Error(404, 'No link provided')
        const link = await Link.findOneAsync(linkId)
        if (!link) throw new Meteor.Error(404, 'Link not found')
        if (!link.private) throw new Meteor.Error(403, "No permission to get this model's tree (only private link will work)")
        // get the root model from the provided link
        const root = await Model.findOneAsync(link.model_id)
        // get all descendants (that have the root as original)
        if (!root) throw new Meteor.Error(404, 'Model not found')
        const ds = await Model.find({
            original: root._id
        }).fetchAsync()

        // treatment of result will be done client-side
        return {
            descendants: ds,
            root
        }
    }
})
