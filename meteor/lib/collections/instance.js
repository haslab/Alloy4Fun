import { Mongo } from 'meteor/mongo'
import SimpleSchema from 'simpl-schema'
import 'meteor/aldeed:collection2/static'

/**
 * An instance created by executing a command of an Alloy model.
 */

const Instance = new Mongo.Collection('Instance')
globalThis.Instance = Instance

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
