import SimpleSchema from 'simpl-schema'
import 'meteor/aldeed:collection2/static'

/**
 * A link creates identifiers to access registed models. The identifier of the
 * link acts as the address of the resource.
 *
 * When a model (with secrets) is shared two links are provided: one public
 * and another private. This corresponds to two link instances (with different
 * ids) but both pointing to same model id. If the model has no secrets, only
 * the public link is created.
 */

Link = new Meteor.Collection('Link')

Link.attachSchema(new SimpleSchema({
    _id: {
        type: String
    },
    /**
      * whether this is a private or public link (will show secrets for
      * private).
      */
    private: {
        type: Boolean
    },
    /** the id of the model to be shared. */
    model_id: {
        type: String
    }
}))

Link.publicFields = {
    private: 1,
    model_id: 1
}

export { Link }
