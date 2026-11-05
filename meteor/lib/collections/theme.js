import { Mongo } from 'meteor/mongo'
import SimpleSchema from 'simpl-schema'
import 'meteor/aldeed:collection2/static'

/**
 * Theme that a user specified
 * Must be saved so that shared models can keep the instances appearance
 */

const Theme = new Mongo.Collection('Theme')
globalThis.Theme = Theme

Theme.attachSchema(new SimpleSchema({
    _id: {
        type: String
    },
    name: {
        type: String
    },

    // Node Colors
    nodeColors: {
        type: Array,
        optional: true
    },
    'nodeColors.$': {
        type: Object
    },
    'nodeColors.type': {
        type: String
    },
    'nodeColors.color': {
        type: String
    },

    // Node Shapes
    nodeShapes: {
        type: Array,
        optional: true
    },
    'nodeShapes.$': {
        type: Object
    },
    'nodeShapes.type': {
        type: String
    },
    'nodeShapes.shape': {
        type: String
    },

    /* nodePositions: {
        type: Array,
        optional : true
    },
    "nodePositions.$" : {
        type: Object
    },
    "nodePositions.id" : {
        type: String,
        optional : false
    },
    "nodePositions.pos" : {
        type: Object
    },
    "nodePositions.pos.x" : {
        type : Number
    },
    "nodePositions.pos.y" : {
        type: Number
    }, */

    // Node labels in case of renaming
    nodeLabels: {
        type: Array,
        optional: true
    },
    'nodeLabels.$': {
        type: Object
    },
    'nodeLabels.type': {
        type: String
    },
    'nodeLabels.label': {
        type: String
    },


    // Edge Colors
    edgeColors: {
        type: Array,
        optional: true
    },
    'edgeColors.$': {
        type: Object
    },
    'edgeColors.relation': {
        type: String
    },
    'edgeColors.color': {
        type: String
    },

    // Edge Labels
    edgeLabels: {
        type: Array,
        optional: true
    },
    'edgeLabels.$': {
        type: Object
    },
    'edgeLabels.relation': {
        type: String
    },
    'edgeLabels.label': {
        type: String
    }
}))

export { Theme }
