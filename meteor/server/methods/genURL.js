import { Model } from '../../lib/collections/model'
import { Link } from '../../lib/collections/link'
import { extractSecrets,
    containsValidSecret } from '../../lib/editor/text'

Meteor.methods({
    /**
      * Meteor method to get persistent links to share an Alloy model. Only
      * generates private link if secrets are present. If the model contains
      * secrets, will become a new derivation root (although it still
      * registers the derivation).
      *
      * @param {String} code the Alloy model to be shared
      * @param {String} currentModelId the id of the current model
      * @param {Object} themeData the theme information for cytoscape
      *
      * @return The 'id' of the model link, used in Share Model option
      */
    async genURL(code, currentModelId, themeData) {
        // a new model is always created, regardless of having secrets or not
        const model = {
            time: new Date().toLocaleString(),
            code,
            derivationOf: currentModelId,
            theme: themeData
        }

        // insert new model
        const modelId = await Model.insertAsync(model)

        // generate the public link
        const publicLinkId = await Link.insertAsync({
            model_id: modelId,
            private: false
        })

        // generate the private link if secrets are present
        let privateLinkId
        let original
        if (containsValidSecret(code) || !currentModelId) {
            original = modelId
            privateLinkId = await Link.insertAsync({
                model_id: modelId,
                private: true
            })
        } else {
            original = (await Model.findOneAsync(currentModelId)).original
        }

        await Model.updateAsync({ _id: modelId }, { $set: { original } })

        return {
            public: publicLinkId,
            // will be undefined if no secret is present
            private: privateLinkId,
            last_id: modelId
        }
    }
})
