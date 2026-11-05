import { Blaze } from 'meteor/blaze'
import { Template } from 'meteor/templating'
import { Meteor } from 'meteor/meteor'
import '../../lib/router'

Template.router.helpers({
    view() {
        return Router.view()
    },
    // reactive, so that data changes update the context without re-rendering the template
    routeData() {
        const route = Router.current()
        return route && route.data ? route.data() : undefined
    }
})

Meteor.startup(() => {
    Blaze.render(Template.router, document.body)
})
