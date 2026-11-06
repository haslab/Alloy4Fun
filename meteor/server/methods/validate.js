import { postApi } from '../lib/api'

Meteor.methods({
    /**
      * Meteor method to validate an Alloy model. This will call the Alloy API
      * (webService).
      *
      * @param {String} code the Alloy code to validate
      *
      * @returns a JSON file with {success [, errorMessage, errorLocation]}
      */
    async validate(code) {
        return postApi('/validate', { model: code })
    }
})
