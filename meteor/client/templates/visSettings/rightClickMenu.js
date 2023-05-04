import { themeChanged,getCurrentState } from '../../lib/editor/state'
import { addSigToProjection,newInstanceSetup } from '../../lib/visualizer/projection'


Template.rightClickMenu.helpers({
    /**
     * The target of the right click menu, may be a sig or a relation.
     */
    getRightClickLabels() {
        let target = Session.get('rightClickSig')
        if (!target) target = Session.get('rightClickRel')
        if (!target) target = ['general']
        return target
    },

    /**
     * Whether to show the sig theme options.
     */
    showSigProps() {
        return Session.get('rightClickSig') ? '' : 'hidden'
    },

    showRelProps() {
        return Session.get('rightClickRel') ? '' : 'hidden'
    },

    showGeneralProps() {
        Session.get('theme-changed')
        return (Session.get('rightClickSig') || Session.get('rightClickRel')) ? 'hidden' : ''
    },

    showElemProps() {
        Session.get('theme-changed')
        return (Session.get('rightClickSig') || Session.get('rightClickRel')) ? '' : 'hidden'
    },

    showRelProps() {
        return Session.get('rightClickRel') ? '' : 'hidden'
    },

    /**
     * Whether to show projection theme options.
     */
    showProjectionProp() {
        Session.get('frame-updated')
        return (currentlyProjectedSigs.length != 0 || Session.get('from-instance')) ? 'hidden' : ''
    },

    isProjected() {
        Session.get('frame-updated')
        return (currentlyProjectedSigs.length == 0) ? 'hidden' : ''
    },

    showAttrLabel(event) {
        Session.get('theme-changed')
        return relationSettings.isShowAsAttributesOn(event) ? 'Hide' : 'Show'
    },

    hideAtomLabel(event) {
        Session.get('theme-changed')
        if (sigSettings.getAtomVisibility(event))
            return sigSettings.getAtomVisibility(event) ? 'Show' : 'Hide'
        else 
            return relationSettings.isShowAsArcsOn(event) ? 'Hide' : 'Show'
    },

    getAllHiddenAtoms() {
        Session.get('theme-changed')
        return sigSettings.getAllHiddenAtoms()
    },

    getAllHiddenRels() {
        Session.get('theme-changed')
        return relationSettings.getAllHiddenRels()
    }
})

/**
 * Updates the content of the right-click menu, depending on whether edge or
 * atom, with the current state of each property.
 */
export function updateRightClickContent() {
    let selected = Session.get('rightClickSig')
    if (selected) {
        $('.changeAtomColorPicker').each(function() {
            $(this).val(sigSettings.getAtomColor($(this).attr("elm")))
        })
        $('.changeAtomShapePicker').each(function() {
            $(this).val(sigSettings.getAtomShape($(this).attr("elm")))
        })
        $('.changeAtomBorderPicker').each(function() {
            $(this).val(sigSettings.getAtomBorder($(this).attr("elm")))
        })
    } else {
        selected = Session.get('rightClickRel')
        if (selected) {
            $('.changeAtomColorPicker').each(function() {
                $(this).val(relationSettings.getEdgeColor($(this).attr("elm")))
            })
        } else {
            $('.changeLayoutPicker').val(generalSettings.getLayout())
        }
    }
    Session.set('theme-changed', !Session.get('theme-changed'))
    themeChanged()
}

Template.rightClickMenu.events({
    'change .changeAtomColorPicker'(event) {
        const elem = event.target.getAttribute("elm")
        if (Session.get('rightClickSig')) {
            sigSettings.updateAtomColor(elem, event.target.value)
        } else if (Session.get('rightClickRel')) {
            relationSettings.updateEdgeColor(elem, event.target.value)
        }
        refreshGraph()
    },
    'change .changeAtomShapePicker'(event) {
        const elem = event.target.getAttribute("elm")
        sigSettings.updateAtomShape(elem, event.target.value)
        refreshGraph()
    },
    'change .changeLayoutPicker'(event) {
        generalSettings.updateLayout(event.target.value)
        refreshGraph()
        applyCurrentLayout()
    },
    'change .changeAtomBorderPicker'(event) {
        const elem = event.target.getAttribute("elm")
        if (Session.get('rightClickSig')) {
            sigSettings.updateAtomBorder(elem, event.target.value)
        } else if (Session.get('rightClickRel')) {
            relationSettings.updateEdgeStyle(elem, event.target.value)
        }
        refreshGraph()
    },
    'click .hideAtom'() {
        const elem = event.target.getAttribute("elm")
        if (Session.get('rightClickSig') || !elem.includes("<:")) {
            const val = sigSettings.getInheritedAtomVisibility(elem)
            sigSettings.updateAtomVisibility(elem, !val)
        } else if (Session.get('rightClickRel') || elem.includes("<:")) {
            const val = relationSettings.isShowAsArcsOn(elem)
            relationSettings.updateShowAsArcs(elem, !val)            
        }
        Session.set('theme-changed', !Session.get('theme-changed'))
        updateGraph(getCurrentState(),false)
        newInstanceSetup()
    },
    'click .showAsAttribute'() {
        const elem = event.target.getAttribute("elm")
        const val = relationSettings.isShowAsAttributesOn(elem)
        relationSettings.updateShowAsAttributes(elem, !val)
        Session.set('theme-changed', !Session.get('theme-changed'))
        refreshGraph()
    },
    'click .rightClickProject'() {
        const elem = event.target.getAttribute("elm")
        try {
            addSigToProjection(elem)
            $('#optionsMenu').hide()
        } catch (err) {
            console.error(err)
        }
    },
    'click .rightClickResetProject'() {
        currentlyProjectedSigs = []
        currentFramePosition = {}
        $(`.frame-navigation > select`).empty()
        Session.set('theme-changed', !Session.get('theme-changed'))
        Session.set('frame-updated',!Session.get('frame-updated'))
        updateGraph(getCurrentState(),false)
    },
    'click .rightClickReset'() {
        sigSettings.init(undefined)
        relationSettings.init(undefined)
        generalSettings.init(undefined)
        currentlyProjectedSigs = []
        currentFramePosition = {}
        $(`.frame-navigation > select`).empty()
        Session.set('frame-updated',!Session.get('frame-updated'))
        Session.set('theme-changed', !Session.get('theme-changed'))
        updateGraph(getCurrentState(),false)
    },
    'click #cssmenu li.has-sub>a'(event) {
        $(event.target).removeAttr('href')
        const element = $(event.target).parent('li')
        if (element.hasClass('open')) {
            element.removeClass('open')
            element.find('li').removeClass('open')
            element.find('ul').slideUp(200)
        } else {
            element.addClass('open')
            element.children('ul').slideDown(200)
            element.siblings('li').children('ul').slideUp(200)
            element.siblings('li').removeClass('open')
            element.siblings('li').find('li').removeClass('open')
            element.siblings('li').find('ul').slideUp(200)
        }
    }
})

Template.rightClickMenu.onRendered(() => {
    Session.set('theme-updated',false)
    $('#optionsMenu').hide()
})
