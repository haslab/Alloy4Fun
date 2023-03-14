import { Stats } from '../../lib/collections/stats'

/**
 * This is used to retrieve data when the Route contains a link /stats/:_id
 */
stats = RouteController.extend({
    template: 'modelStats',

    // see http://iron-meteor.github.io/iron-router/#subscriptions
    subscriptions() {
        this.subscribe('statsFromLink', this.params._id).wait()
    },

    // see http://iron-meteor.github.io/iron-router/#the-waiton-option
    waitOn() {},

    data() {
        return Stats.findOne(this.params._id) || {
            code: 'Unable to retrieve Model from Link'
        }
    },

    onRun() {
        this.next()
    },
    onRerun() {
        this.next()
    },
    onBeforeAction() {
        this.next()
    },
    action() {
        this.render()
    },
    onAfterAction() {},
    onStop() {}
})
