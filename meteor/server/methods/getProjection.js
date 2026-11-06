import { postApi } from '../lib/api'

Meteor.methods({

    /**
      * Meteor method to get the projection of an Alloy instance. This will
      * call the Alloy API (webService).
      *
      * @param uuid id of the session
      * @param frameInfo object with information about the frame
      *
      * @return JSON object with the projection
      */
    async getProjection(uuid, frameInfo, idx) {
        const type = []
        for (const key in frameInfo) {
            type.push(key + frameInfo[key])
        }
        const content = JSON.parse(await postApi('/getProjection', {
            sessionId: uuid,
            type,
            index: idx
        }))
        if (content.unsat) {
            content.check = true
        } else {
            Object.keys(content).forEach((k) => {
                content[k].check = true
            })
        }
        return content
    }
})
