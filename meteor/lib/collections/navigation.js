import SimpleSchema from 'simpl-schema'
import 'meteor/aldeed:collection2/static'

/**
 * TODO
 */

Navigation = new Meteor.Collection('Navigation')

Navigation.attachSchema(new SimpleSchema({
    _id: {
        type: String
    },
    /**
      * whether this is a private or public link (will show secrets for
      * private).
      */
    operation: {
        type: Number
    },
    instIndex: {
        type: Number
    },
    /** the id of the model that originated the instance navigation. */
    model_id: {
        type: String
    },
    /** the timestamp. */
    time: {
        type: String
    }
}))

export { Navigation }
