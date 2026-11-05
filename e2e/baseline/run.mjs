/**
 * Behaviour baseline for the Meteor server.
 *
 *   node baseline/run.mjs record    # run scenarios, store baseline/expected.json
 *   node baseline/run.mjs compare   # run scenarios, diff against expected.json
 *
 * Requires the full stack (Alloy API + Meteor + its MongoDB). Methods and
 * publications are called over DDP, exactly as the browser client does, and
 * the documents they create are read back from MongoDB. Ids and timestamps
 * are normalised so that runs are comparable across Meteor versions.
 */
import { mkdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { MongoClient } from 'mongodb'
import { DdpClient } from '../lib/ddp.mjs'
import { makeNormalizer } from '../lib/normalize.mjs'

const here = dirname(fileURLToPath(import.meta.url))
const DDP_URL = process.env.E2E_DDP_URL || 'ws://127.0.0.1:3000/websocket'
const MONGO_URL = process.env.E2E_MONGO_URL || 'mongodb://127.0.0.1:3001'
const MONGO_DB = process.env.E2E_MONGO_DB || 'meteor'
const COLLECTIONS = ['Model', 'Link', 'Instance', 'Navigation']

const PUBLIC_CODE = 'sig A {}\nrun { some A } for 3\n'
const BROKEN_CODE = 'sig A {\n'
const SECRET_CODE = 'sig A {}\nrun { some A } for 3\n//SECRET\ncheck { all a: A | some a } for 3\n'
const UNSAT_CODE = 'sig A {}\nrun { some A and no A } for 3\n'
const THEME = { nodeSettings: {}, edgeSettings: {} }

async function scenarios(connect) {
    const out = {}
    const c = await connect()
    const step = async (name, method, ...args) => {
        out[name] = await c.call(method, ...args)
        return out[name].result
    }
    // methods/subscriptions that may never answer run on their own connection
    const isolated = async (name, fn) => {
        const own = await connect(10000)
        out[name] = await fn(own)
        own.close()
    }

    await step('validate/valid', 'validate', PUBLIC_CODE)
    await step('validate/invalid', 'validate', BROKEN_CODE)

    const exec = await step('getInstances/public', 'getInstances', PUBLIC_CODE, 0, false, undefined)
    await step('getInstances/unsat', 'getInstances', UNSAT_CODE, 0, false, undefined)
    await step('getInstances/syntax-error', 'getInstances', BROKEN_CODE, 0, false, undefined)
    const derived = await step('getInstances/derived', 'getInstances', PUBLIC_CODE, 0, false, exec?.newModelId)
    await step('nextInstances', 'nextInstances', PUBLIC_CODE, 0, exec?.newModelId)
    await step('getProjection', 'getProjection', exec?.newModelId, {}, 0)

    const share = await step('genURL/public-no-parent', 'genURL', PUBLIC_CODE, undefined, THEME)
    await step('genURL/derived', 'genURL', PUBLIC_CODE, share?.last_id, THEME)
    const secret = await step('genURL/secret', 'genURL', SECRET_CODE, undefined, THEME)

    await step('getModel/public-link', 'getModel', share?.public)
    await step('getModel/private-link', 'getModel', share?.private)
    await step('getModel/secret-public-link', 'getModel', secret?.public)
    await step('getModel/secret-private-link', 'getModel', secret?.private)
    await step('getModel/unknown', 'getModel', 'doesNotExist00000')

    await step('getInstances/secret-from-public', 'getInstances', PUBLIC_CODE, 1, false, secret?.last_id)
    await step('getInstances/secret-from-private', 'getInstances', SECRET_CODE, 1, true, secret?.last_id)
    const fromPrivate = out['getInstances/secret-from-private'].result
    await step('nextInstances/secret', 'nextInstances', 'sig A {}\n', 1, fromPrivate?.newModelId)

    const inst = await step('storeInstance', 'storeInstance', derived?.newModelId, 0, { instance: [{ types: [], relations: [] }], zoom: 1, pan: { x: 0, y: 0 } }, THEME)
    await step('getModel/instance', 'getModel', inst)

    await step('downloadTree/private', 'downloadTree', secret?.private)
    await step('downloadTree/public', 'downloadTree', secret?.public)
    await step('downloadTree/missing-arg', 'downloadTree', undefined)
    await step('downloadTree/unknown', 'downloadTree', 'doesNotExist00000')

    await step('navInstance', 'navInstance', 1, 1, exec?.newModelId)
    await step('unknown-method', 'noSuchMethod')
    c.close()

    await isolated('getStats/public', (o) => o.call('getStats', share?.public))
    await isolated('getStats/unknown', (o) => o.call('getStats', 'doesNotExist00000'))
    await isolated('publish/modelFromLink', (o) => o.subscribe('modelFromLink', share?.public))
    await isolated('publish/modelFromLink-secret-public', (o) => o.subscribe('modelFromLink', secret?.public))
    await isolated('publish/modelFromLink-unknown', (o) => o.subscribe('modelFromLink', 'doesNotExist00000'))
    await isolated('publish/statsFromLink', (o) => o.subscribe('statsFromLink', share?.public))
    return out
}

async function existingIds(db) {
    const seen = {}
    for (const name of COLLECTIONS) seen[name] = new Set((await db.collection(name).find({}, { projection: { _id: 1 } }).toArray()).map((d) => d._id))
    return seen
}

async function createdDocuments(db, before) {
    const dump = {}
    for (const name of COLLECTIONS) {
        const docs = await db.collection(name).find({}).toArray()
        dump[name] = docs.filter((d) => !before[name].has(d._id))
    }
    return dump
}

function diff(a, b, path = '', acc = []) {
    if (JSON.stringify(a) === JSON.stringify(b)) return acc
    if (a && b && typeof a === 'object' && typeof b === 'object' && Array.isArray(a) === Array.isArray(b)) {
        for (const k of new Set([...Object.keys(a), ...Object.keys(b)])) diff(a[k], b[k], `${path}/${k}`, acc)
    } else acc.push({ path, expected: a, actual: b })
    return acc
}

async function main() {
    const mode = process.argv[2]
    if (!['record', 'compare'].includes(mode)) throw new Error('usage: run.mjs record|compare')

    const mongo = await MongoClient.connect(MONGO_URL)
    const db = mongo.db(MONGO_DB)
    const before = await existingIds(db)
    const connect = async (timeoutMs) => {
        const c = new DdpClient(DDP_URL, timeoutMs)
        await c.connect()
        return c
    }

    const calls = await scenarios(connect)
    const documents = await createdDocuments(db, before)
    await mongo.close()

    const result = makeNormalizer()({ calls, documents })
    const expectedFile = join(here, 'expected.json')

    if (mode === 'record') {
        writeFileSync(expectedFile, `${JSON.stringify(result, null, 2)}\n`)
        console.log(`Recorded ${Object.keys(calls).length} scenarios to ${expectedFile}`)
        return
    }

    if (!existsSync(expectedFile)) throw new Error('No expected.json; run "record" against the reference version first')
    const actualDir = join(here, 'actual')
    mkdirSync(actualDir, { recursive: true })
    writeFileSync(join(actualDir, 'result.json'), `${JSON.stringify(result, null, 2)}\n`)

    const differences = diff(JSON.parse(readFileSync(expectedFile, 'utf8')), result)
    if (!differences.length) {
        console.log(`OK: ${Object.keys(calls).length} scenarios match the baseline`)
        return
    }
    console.error(`${differences.length} difference(s) from the baseline:`)
    for (const d of differences.slice(0, 40)) {
        console.error(`- ${d.path}\n    expected: ${JSON.stringify(d.expected)?.slice(0, 160)}\n    actual:   ${JSON.stringify(d.actual)?.slice(0, 160)}`)
    }
    process.exitCode = 1
}

main().catch((e) => {
    console.error(e)
    process.exit(2)
})
