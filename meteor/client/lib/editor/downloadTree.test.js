import chai from 'chai'
import { descendantsToTree } from './downloadTree'


describe('downloadTree', () => {
    it('flat descendants converted to tree with no descendants', () => {
        chai.assert.equal(JSON.stringify(descendantsToTree({
            descendants: [],
            root: {
                _id: '1'
            }
        })), JSON.stringify({
            _id: '1',
            children: []
        }))
    })
    it('flat descendants converted to tree with multilevel descendants', () => {
        chai.assert.equal(JSON.stringify(descendantsToTree({
            descendants: [{
                _id: 2,
                derivationOf: 1
            }, {
                _id: 3,
                derivationOf: 2
            }, {
                _id: 4,
                derivationOf: 2
            }, {
                _id: 5,
                derivationOf: 1
            }, {
                _id: 6,
                derivationOf: 4
            }],
            root: {
                _id: '1'
            }
        })), '{"_id":"1","children":[{"_id":2,"derivationOf":1,"children":[{"_id":3,"derivationOf":2,"children":[]},{"_id":4,"derivationOf":2,"children":[{"_id":6,"derivationOf":4,"children":[]}]}]},{"_id":5,"derivationOf":1,"children":[]}]}')
    })
})
