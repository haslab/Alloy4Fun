import SimpleSchema from 'simpl-schema'
import 'meteor/aldeed:collection2/static'

/**
 * An instance created by executing a command of an Alloy model.
 */

Instance = new Meteor.Collection('Instance')

Instance.attachSchema(new SimpleSchema({
    _id: {
        type: String
    },
    /** id of the model that generated the instance. */
    model_id: {
        type: String
    },
    /* index, within the model, of the command that generated the instance. */
    cmd_i: {
        type: Number
    },
    /** the entire cytoscape graph. */
    graph: {
        type: Object,
        blackbox: true
    },
    /** the timestamp. */
    time: {
        type: String
    }
}))

export { Instance }
