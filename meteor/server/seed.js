import { secretTag } from '../lib/editor/text'

export { seedWithModels }

/**
  Seed the database with a set of default models, with fixed ids so that they
  can be accessed from a fixed address.
*/
function seedWithModels() {
    var data = Assets.getText("Link.json").split("\n")
    data.forEach(function (item) {
        Link.insert(JSON.parse(item));
    })

    var data = Assets.getText("Instance.json").split("\n")
    data.forEach(function (item) {
        Instance.insert(JSON.parse(item));
    })

    var data = Assets.getText("Model.json").split("\n")
    data.forEach(function (item) {
        Model.insert(JSON.parse(item));
    })
    
    return data.length
}
