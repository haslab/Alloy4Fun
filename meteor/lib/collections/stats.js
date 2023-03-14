/**
 * Alloy models created through the editor feature. A model may be derived
 * from another one, and also store the original root for efficiency purposes.
 *
 * This original model should only be udpated when a model with secrets is
 * shared (meaning that public versions always refer to the original model
 * unless new secrets are introduced).
 *
 * Models are created when executed or shared. If created when executed,
 * stores additional information.
 */

Stats = new Meteor.Collection('Stats')

Stats.attachSchema(new SimpleSchema({
    _id: {
        type: String
    },
    model: {
        type: String
    },
    time: {
        type: String
    },
    name: {
        type: String
    }
}))


Stats.publicFields = {
    model: 1,
    time: 1,
    name: 1,
    scalars: 1
}

export { Stats }
