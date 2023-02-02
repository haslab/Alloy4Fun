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

Model = new Meteor.Collection('Model')

Model.attachSchema(new SimpleSchema({
    _id: {
        type: String
    },
    /** the complete code of the model. */
    code: {
        type: String
    },
    /** which model does it derive from (null if original). */
    derivationOf: {
        type: String,
        optional: true
    },
    /**
     * the root of the derivation tree. Different from derivation, as this is
     * the original model and remains the same after derivation to preserve
     * the original secrets. Should only change when a model with secrets is
     * shared (i.e., sharing public versions of a model with secrets should
     * not break the derivaton).
     */
    original: {
        type: String,
        optional: true
    },
    /**
     * optional field for the index of the executed command, if created by
     * execution.
     */
    cmd_i: {
        type: Number,
        optional: true
    },
    /**
     * optional field for the name of the executed command, if created by
     * execution.
     */
    cmd_n: {
        type: String,
        optional: true
    },
    /**
     * optional field, whether the command was a check (1) or a run (0), if
     * created by execution.
     */
    cmd_c: {
        type: Boolean,
        optional: true
    },
    /**
     * optional field, whether the command was satisfiable (1) or unsatisfiable
     * (0), if created by execution. if execution fails, then -1.
     */
    sat: {
        type: Number,
        optional: true
    },
    /**
     * optional field, a possible error or warning message.
     */
    msg: {
        type: String,
        optional: true
    },
    /** the theme associated with this model. */
    theme: {
        type: Object,
        blackbox: true,
        optional: true
    },
    /** the timestamp. */
    time: {
        type: String
    },
    frozenmsg: {
        type: String,
        optional: true
    },
    frozennxt: {
        type: String,
        optional: true
    },
}))

Model.publicFields = {
    code: 1,
    derivationOf: 1
}
export { Model }
