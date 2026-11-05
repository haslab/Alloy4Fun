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
