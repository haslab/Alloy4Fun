import cytoscape from 'cytoscape'
import { updateRightClickContent } from '../../templates/visSettings/rightClickMenu'
import { instChanged,getCurrentTrace,getCurrentState } from '../editor/state'
import { savePositions } from './projection'

updateGraph = function (instance,v) {
    // Remove previous nodes and edges.
    cy.remove(cy.elements())
    // Add new ones.
    generalSettings.resetHierarchy()
    relationSettings.reset()
    allNodes = getAtoms(instance)
    cy.add(allNodes)
    cy.add(getEdges(instance))
    cy.resize()
    refreshGraph()
    // Draw data according to the selected layout.
    if (!v) applyCurrentLayout()
}

// Get atom information received from server ready to upload to cytoscape object.
getAtoms = function (inst) {
    const atoms = []
    inst.types.forEach((sig) => {
        // built-in sigs
        if (sig.name == 'String' || sig.name == 'Int') {
            const tp = sig.name
            generalSettings.addPrimSig(tp, sig.parent)
            sig.atoms.forEach((atom) => {
                atoms.push(
                    {
                        group: 'nodes',
                        classes: 'multiline-manual',
                        data: {
                            id: atom,
                            type: tp,
                            subsetSigs: []
                        }
                    }
                )
            })
        // module sigs
        } else {
            const tp = sig.name
            const pa = sig.parent
            generalSettings.addPrimSig(tp, pa)
            sig.atoms.forEach((atom) => {
                atoms.push(
                    {
                        group: 'nodes',
                        classes: 'multiline-manual',
                        data: {
                            id: atom,
                            type: tp,
                            subsetSigs: []
                        }
                    }
                )
            })
        }
    })

    inst.sets.forEach((set) => {
        set.atoms.forEach((atom) => {
            const tp = set.name
            for (let i = 0; i < atoms.length; i++) {
                if (atoms[i].data.id == atom) {
                    let paren = atoms[i].data.type
                    const canon = `${tp}:${paren}`
                    if (!generalSettings.hasSubsetSig(canon)) {
                        generalSettings.addSubSig(canon, paren)
                    }
                    atoms[i].data.subsetSigs.push(canon)
                }
            }
        })
    })

    return atoms
}

getEdges = function (inst) {
    const result = []
    inst.rels.forEach((field) => {
        field.atoms.forEach((relation) => {
            inst.types.forEach((sig) => {
                if (sig.atoms.includes(relation[0])) 
                    parent_sig = sig.name
            })
            result.push({
                group: 'edges',
                selectable: true,
                data: {
                    relation: field.name,
                    source: relation[0],
                    target: relation[relation.length - 1],
                    atoms: relation,
                    parent_sig: parent_sig
                }
            })
        })
    })
    
    return result
}

// Selecting an element on the cytoscape canvas recalculates its label.
refreshGraph = function () {
    const selected = cy.$(':selected')
    cy.elements().select().unselect()
    selected.select()
}

initGraphViewer = function (element) {
    // the element must be visible for the layout to be correctly 
    // applied, but the template will only trigger after
    document.getElementById(element).parentElement.parentElement.removeAttribute('hidden')
    cy = cytoscape({
        container: document.getElementById(element), // container to render in
        elements: [ // list of graph elements to start with

        ],
        zoom: 1,
        pan: { x: 0, y: 0 },

        // interaction options:
        minZoom: 0.2,
        maxZoom: 5,
        wheelSensitivity: 0.5,

        style: [ // the stylesheet for the graph
            {
                selector: 'node',
                style: {
                    'background-color'(ele) {
                        let val
                        if (ele.data().subsetSigs.length > 0) {
                            // TODO: this randomly selects one of the subsigs (as does the AA)
                            val = sigSettings.getInheritedAtomColor(ele.data().subsetSigs[0])
                        } else val = sigSettings.getInheritedAtomColor(ele.data().type)
                        return val
                    },
                    label(ele) {
                        let l = ele.data().id
                    
                        // subsig labels
                        const subsigs = ele.data().subsetSigs.length > 0 ? `\n(${ele.data().subsetSigs.map(x => x.split(':')[0])})` : ''

                        // relations as attributes labels
                        let attributes = relationSettings.getAttributeLabel(ele)
                        return `${l}${subsigs}\n${attributes}`
                    },
                    'border-style'(ele) {
                        let val
                        if (ele.data().subsetSigs.length > 0) val = sigSettings.getInheritedAtomBorder(ele.data().subsetSigs[0])
                        else { val = sigSettings.getInheritedAtomBorder(ele.data().type) }
                        return val
                    },
                    'text-valign': 'center',
                    'text-outline-color': 'black',
                    shape(ele) {
                        let val
                        if (ele.data().subsetSigs.length > 0) val = sigSettings.getInheritedAtomShape(ele.data().subsetSigs[0])
                        else { val = sigSettings.getInheritedAtomShape(ele.data().type) }
                        return val
                    },
                    visibility(ele) {
                        if (ele.data().type === 'seq/Int') return 'hidden' // needed for retro-compatibilty with FOL models
                        let val1 = true
                        if (ele.data().subsetSigs.length > 0) {
                            // only hide if all subsigs want to hide
                            ele.data().subsetSigs.forEach(ss =>
                                val1 = val1 && sigSettings.getInheritedAtomVisibility(ss))
                        } else {
                            val1 = sigSettings.getInheritedAtomVisibility(ele.data().type)
                        }
                        return val1 ? 'hidden' : 'visible'
                    },
                    width: 'label',
                    height: 'label',
                    'padding-bottom': '10px',
                    'padding-top': '10px',
                    'padding-left': '10px',
                    'padding-right': '10px',
                    'border-color': 'black',
                    'border-width': 2,
                    'border-opacity': 0.8
                }
            },

            {
                selector: 'edge',
                style: {
                    width: 1,
                    'line-color'(ele) { 
                        return relationSettings.getEdgeColor(ele.data()) 
                    },
                    'target-arrow-color'(ele) { 
                        return relationSettings.getEdgeColor(ele.data()) 
                    },
                    'target-arrow-shape': 'triangle',
                    'label'(ele) {
                        return relationSettings.getEdgeLabel(ele.data())
                    },
                    'curve-style': 'bezier',
                    'text-valign': 'center',
                    'text-outline-color': '#ff3300',
                    'edge-text-rotation': 'autorotate',
                    'line-style'(ele) { return relationSettings.getEdgeStyle(ele.data()) },
                    visibility(ele) {
                        const val = relationSettings.isShowAsArcsOn(ele.data())
                        return val ? 'visible' : 'hidden'
                    }
                }
            },
            {
                selector: ':selected',
                style: {
                    'background-opacity': 0.5
                }
            },
            {
                selector: '.multiline-manual',
                style: {
                    'text-wrap': 'wrap'
                }
            },
            {
                selector: 'edge:selected',
                style: {
                    width: 5
                }
            },
            {
                selector: ':parent',
                style: {
                    'background-opacity': 0.3,
                    'text-valign': 'top'
                }
            }

        ],

        layout: {
            name: 'grid',
            fit: true,
            sort(a, b) {
                return a.data('label') < b.data('label')
            },
            avoidOverlap: true
        }

    })

    // right click event on cytoscape's background
    cy.on('cxttap', function(evt){
      var evtTarget = evt.cyTarget;    
      if( evtTarget === cy ){
        // Place right click options menu on mouse position and display it
        $('#optionsMenu').css({
            // overlap cytoscape canvas
            'z-index': 10,
            position: 'absolute',
            // +1 avoids recapturing right click event and opening browser's context menu.
            top: evt.originalEvent.offsetY + 1,
            // if the menu's width overflows page width, place it behind the cursor.
            left: evt.originalEvent.screenX + 1 + 300 > $(window).width() ? evt.originalEvent.offsetX + 1 - 300 : evt.originalEvent.offsetX + 1
        }).fadeIn('slow')
        Session.set('rightClickRel', undefined)
        Session.set('rightClickSig', undefined)
        updateRightClickContent()
        return false
      }
    });

    // right click event on cytoscape's node
    cy.on('cxttap', 'node', {}, (evt) => {
        // Place right click options menu on mouse position and display it
        $('#optionsMenu').css({
            // overlap cytoscape canvas
            'z-index': 10,
            position: 'absolute',
            // +1 avoids recapturing right click event and opening browser's context menu.
            top: evt.originalEvent.offsetY + 1,
            // if the menu's width overflows page width, place it behind the cursor.
            left: evt.originalEvent.screenX + 1 + 300 > $(window).width() ? evt.originalEvent.offsetX + 1 - 300 : evt.originalEvent.offsetX + 1
        }).fadeIn('slow')
        Session.set('rightClickRel', undefined)
        Session.set('rightClickSig', [evt.cyTarget.data().type].concat(evt.cyTarget.data().subsetSigs))
        updateRightClickContent()
        return false
    })

    // right click event on cytoscape's node
    cy.on('cxttap', 'edge', {}, (evt) => {
        // Place right click options menu on mouse position and display it
        $('#optionsMenu').css({
            // overlap cytoscape canvas
            'z-index': 10,
            position: 'absolute',
            // +1 avoids recapturing right click event and opening browser's context menu.
            top: evt.originalEvent.offsetY + 1,
            // if the menu's width overflows page width, place it behind the cursor.
            left: evt.originalEvent.screenX + 1 + 300 > $(window).width() ? evt.originalEvent.offsetX + 1 - 300 : evt.originalEvent.offsetX + 1
        }).fadeIn('slow')
        Session.set('rightClickSig', undefined)
        Session.set('rightClickRel', [relationSettings.rel_id(evt.cyTarget.data())])
        updateRightClickContent()
        return false
    })

    cy.on('tap', (event) => {
        // hide right click menu
        $('#optionsMenu').hide()
    })

    cy.on('render', (event) => {
        instChanged()
    })
}

/**
 * Applies the currently selected layout to the current instance.
 * Hidden elements are ignored. If a trace, all atoms (and relations)
 * from the complete trace are considered.
 * Should only be called once by trace.
 */
applyCurrentLayout = function () {
    // backup the models from the current state
    const tmp = cy.elements()
    // collect elements from all other states of the trace
    if (getCurrentTrace().instance) {
        getCurrentTrace().instance.forEach(x => {
            cy.add(getAtoms(x))
            cy.add(getEdges(x))
        })
    }
    // remove the hidden elements so that they are not affected by the layout
    const hds = cy.elements((element, i) => !i.visible())
    cy.remove(hds)
    // actually calculate the layout
    cy.layout(layouts[generalSettings.getLayout()])
    // save the position of all elements
    savePositions() 
    // keep only the original ones
    cy.remove(cy.elements().subtract(tmp))
}

