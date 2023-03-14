/**
 * Defines the routes for the application
 */

// global route settings
Router.configure({
    // Template displayed while loading data.
    loadingTemplate: 'loading',
    // Template displayed when there"s no route for the sub domain.
    notFoundTemplate: 'notFound'
})

// route settings for default endpoint "/"
Router.route('/', {
    name: 'editor',
    template: 'alloyEditor',
    where: 'client'
})

// route settings for endpoint with model id "/:_id"
Router.route('/:_id', {
    name: 'editorLoad',
    controller: 'editor',
    where: 'client'
})

Router.route('/stats/', {
    name: 'stats',
    template: 'modelStats',
    where: 'client'
})

// route settings for endpoint with model id "/:_id"
Router.route('/stats/:_id', {
    name: 'statsLoad',
    controller: 'stats',
    where: 'client'
})
