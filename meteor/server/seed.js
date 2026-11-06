export { seedWithModels }

/**
  Seed the database with a set of default models, with fixed ids so that they
  can be accessed from a fixed address.
*/
async function seedWithModels() {
    const load = async (file, collection) => {
        const lines = (await Assets.getTextAsync(file)).split('\n').filter(l => l.trim())
        for (const line of lines) await collection.insertAsync(JSON.parse(line))
        return lines.length
    }
    await load('Link.json', Link)
    await load('Instance.json', Instance)
    return await load('Model.json', Model)
}
