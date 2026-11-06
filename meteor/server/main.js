import { Meteor } from 'meteor/meteor'

// collections are also exposed as globals (Model, Link, ...)
import '../lib/collections/instance'
import '../lib/collections/link'
import '../lib/collections/model'
import '../lib/collections/navigation'
import '../lib/collections/stats'
import '../lib/collections/theme'

import './methods/validate'
import './methods/genURL'
import './methods/getInstances'
import './methods/nextInstances'
import './methods/getProjection'
import './methods/shareInstance'
import './methods/getModel'
import './methods/getStats'
import './methods/downloadTree'
import './methods/navInstance'

import './publications/modelFromLink'
import './publications/statsFromLink'
import { seedWithModels } from './seed'

/**
  If the database is empty, seeds a set of default models after startup.
*/
Meteor.startup(async () => {
    if (!(await Model.find().countAsync())) {
        try {
            console.log(`Seeded Database with ${await seedWithModels()} models`)
        } catch (e) {
            console.warn('Database is empty and could not be seeded (missing private/*.json?):', e.message)
        }
    }
})
