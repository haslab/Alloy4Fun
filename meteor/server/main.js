import { Meteor } from 'meteor/meteor'

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

/**
  If the database is empty, seeds a set of default models after startup.
*/
Meteor.startup(() => {
    if (!Model.find().count()) {
        // if there are no models, insert default ones
        const res = seedWithModels()
        console.log(`Seeded Database with ${res} models`)
    }
})
