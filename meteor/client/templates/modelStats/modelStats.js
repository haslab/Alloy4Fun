import classie from 'classie'
import 'qtip2/src/core.css'
import { getCommandsFromCode } from '../../../lib/editor/text'
import { shareModel, shareInstance } from '../../lib/editor/genUrl'
import { executeModel, nextInstance, prevInstance } from '../../lib/editor/executeModel'
import { createTree } from '../../lib/editor/downloadTree'
import { copyToClipboard } from '../../lib/editor/clipboard'
import { cmdChanged, isUnsatInstance, prevState, nextState, 
    lastState, currentState, setCurrentState, storeInstances, 
    getCurrentState, getCurrentTrace, disableExec } from '../../lib/editor/state'
import { staticProjection, savePositions, applyPositions } from '../../lib/visualizer/projection'
import { markEditorInfo } from '../../lib/editor/feedback'
import { Model } from '../../../lib/collections/model'

Template.treeNodeTemplate.helpers({


    printMsg() {
        if (!this.msg)
            return "."
        else if (this.sat === -1)
            return ": "+this.msg
        else
            return ", warning: "+this.msg
    },

    hasChildren() {
        return this.children.length > 0
    },

    children() {
        return this.children
    },

    padding() {
        return this.depth*10
    },

    class() {
        if (this.sat === 0) return "list-group-item-success"
        if (this.sat === 1) return "list-group-item-warning"
        if (this.sat === -1) return "list-group-item-danger"
        else return "list-group-item-info"
    },

    state() {
        if (this.sat === 0) return "unsat"
        if (this.sat === 1) return "sat"
        if (this.sat === -1) return "error"
        else return "model shared"
    }
})


Template.treeNodeTemplate.events({
    'click .more'(e) {
      if (e.delegateTarget.classList.contains("well")) {
        // awful, but don't know how else to avoid bubbling and still collapse
        icon = e.target.querySelector("i")
        icon.classList.toggle("glyphicon-chevron-right");
        icon.classList.toggle("glyphicon-chevron-up");
      }
    }
})

Template.modelStats.treeItems = function() {
  return [Session.get('derivation')];

};

Template.modelStats.events({
    'click .grselect'(e) {
      Session.set('currentgraph',Session.get("graphdict")[e.target.id])
      drawGraph()
    },

    'click .chselect'(e) {
      sl = Session.get("chlsSelected")
      id = Session.get("chlsMaps")[e.target.parentElement.id+e.target.id]
      sl[e.target.parentElement.id] = id
      Session.set("chlsSelected",sl)
      drawCharts(Session.get("chlsMaps")[e.target.parentElement.id])
    }
})

Template.modelStats.helpers({

    cselected(x,y) {
      if (Session.get("chlsSelected")[x] == Session.get("chlsMaps")[x+y])
        return "selected"
      else
        return ""
    },

    gselected(x) {
      if (Session.get("graphdict")["graph"+x] == Session.get("currentgraph"))
        return "selected"
      else
        return ""
    },
    /**
     * Whether the execute command button is enabled, if the model has not
     * been updated and the selected command has not been changed.
     */
    round(num) {
        return Math.round(num * 100) / 100
    },

    getName() {
        return Router.current().data().stats.name
    },

    getDate() {
        return Router.current().data().stats.time
    },

    getCatalog() {
        return Router.current().data().stats.catalog
    },

    getNumChallenges() {
        return Router.current().data().stats.nchallenges
    },

    getScalars() {
        return Router.current().data().stats.scalars
    },

    getBars() {
        return Router.current().data().stats.bars
    },

    getClassified() {
        return Router.current().data().stats.classified
    },

    getChallenges() {
        return Router.current().data().stats.challenges
    },

    graphItems() {
        return Router.current().data().stats.graphs
    },

})

function getColor(label) {
    if (label == 'solved' || label == 'UNSAT') return '#D1E6D6'
    if (label == 'unsolved' || label == 'SAT') return '#F2EFD5'
    if (label == 'ERROR') return '#F6CCD5'
    if (label == 'SHARE') return '#C1C1DE'
    return '#C7DAEB'
}

function drawGraph() {
    var gs = Router.current().data().stats.graphs
    const g = gs[Session.get("currentgraph")]
    nodes = g.nodes
    edges = g.edges 

    var container = document.getElementById("graph");

    var data = {
      nodes: nodes,
      edges: edges,
    };

    var options = {
        layout: {improvedLayout:true},

        groups: {
         SAT: {color:{background:getColor("SAT")}},
         UNSAT: {color:{background:getColor("UNSAT")}},
         ERROR: {color:{background:getColor("ERROR")}}
        },
       nodes: {
         shape: "dot",
         scaling: {
           customScalingFunction: function (min, max, total, value) {
             return value / total;
           },
           min: 5,
           max: 200,
         },
       }, edges: {
                scaling: {
               customScalingFunction: function (min, max, total, value) {
                 return value / total;
               },
               min: 1,
               max: 50,
             },
            arrows: { to: { enabled: true }, },
           },    
    };
    var network = new vis.Network(container, data, options);   
}

function drawCharts(c) {
    cs = Router.current().data().stats.challenges
    d = Session.get("chlsSelected")[cs[c].name]

    ds = cs[c].challenges
    var datasets = []

    for (x in ds[d].series) 
        datasets.push({label: ds[d].series[x].series, backgroundColor: getColor(ds[d].series[x].series), data: ds[d].series[x].values, maxBarThickness: 100})

    var ctx = Chart.getChart("g"+cs[c].name);
    if (ctx)
      ctx.destroy()

    var chart = new Chart("g"+cs[c].name, {
       type: 'bar',
       indexLabel: '#percent%',
       data: {
          labels: ds[d].indices,      
          datasets: datasets},
       options: options()
    });

}

Template.graphNodeTemplate.onRendered(() => {
    var gs = Router.current().data().stats.graphs
    gdict = {}
    for (xx in gs) 
        gdict["graph"+gs[xx].challenge] = xx


    Session.set("graphdict",gdict)
    Session.set("currentgraph",0)

    drawGraph()

})

function options() {
    return {
            tooltips: {
              enabled: true,
              mode: 'single',
              callbacks: {
                label: function(value, context) {
                  if (value == 0) return '';
                  let dataArr = context.datasets;
                  let sum = 0;
                  dataArr.map(data => {
                      sum += data.data[value.index];
                  });
                  return context.datasets[value.datasetIndex].label +": "+value.yLabel +' ('+Math.round(1000 / sum * value.yLabel) / 10 + '%)';
                }
              }
            },      
            responsive: true,
            plugins: {
              legend: {position: 'right'}},
              scales: {
                 x: {stacked: true},
                 y: {beginAtZero:true, stacked: true}
              }
           }
}

Template.modelStats.onRendered(() => {

    var bs = Router.current().data().stats.bars
    for (c in bs) {
        is = []
        minr = 0
        for (i in bs[c].indices) {
            ii = bs[c].indices[i].substring(0,80)
            if (ii.length > 60)
                minr = 75
            is.push(ii)
        }

        var chart = new Chart(bs[c].name, {
           type: 'bar',
           data: {
              labels: is,      
              datasets: 
                [ {backgroundColor: getColor(""), data: bs[c].values, maxBarThickness: 100 } ]},
           options: {
              responsive: true,
              plugins: { legend: {
                 display: false
              }},
              scales: {
                 x: {
                        ticks: {
                            autoSkip: false,
                            minRotation:minr,
                            maxRotation:90,
                        }
                    },
                 y: {
                    ticks: { beginAtZero: true }
                 }
              }
           }
        });

    }

    var cs = Router.current().data().stats.classified
    for (c in cs) {
        var datasets = []
        for (x in cs[c].series) 
            datasets.push({label: cs[c].series[x].series, backgroundColor: getColor(cs[c].series[x].series), data: cs[c].series[x].values, maxBarThickness: 100})
        var chart = new Chart(cs[c].name, {
           type: 'bar',
           indexLabel: '#percent%',
           data: {
              labels: cs[c].indices,      
              datasets: datasets},
           options: options()
        });

    }

    var cs = Router.current().data().stats.challenges
    chlsSelected = {}
    chlsMaps = {}
    for (c in cs) {
        chlsSelected[cs[c].name] = 0
        chlsMaps[cs[c].name] = c
        for (d in cs[c].challenges)
            chlsMaps[cs[c].name+cs[c].challenges[d].challenge] = d
    }
    Session.set("chlsSelected",chlsSelected)
    Session.set("chlsMaps",chlsMaps)

    for (c in cs)
        drawCharts(c)

    createTree()
})

