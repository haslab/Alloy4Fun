import { Meteor } from 'meteor/meteor'
import { fetch } from 'meteor/fetch'

/**
 * POSTs a JSON body to the Alloy API. Like the former HTTP.call, rejects on
 * network errors and on non-2xx responses.
 *
 * @param {String} path the API endpoint, e.g. '/getInstances'
 * @param {Object} data the JSON payload
 * @returns {Promise<String>} the raw response body
 */
export async function postApi(path, data) {
    const response = await fetch(`${Meteor.settings.env.API_URL}${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    const content = await response.text()
    if (!response.ok) {
        const error = new Error(`API ${path} failed with status ${response.status}`)
        error.response = { statusCode: response.status, content }
        throw error
    }
    return content
}
