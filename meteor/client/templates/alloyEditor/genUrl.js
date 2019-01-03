import {
    zeroclipboard,
    getAnchorWithLink
} from "../../lib/editor/clipboard"

export {
    clickGenUrl,
    containsValidSecretWithAnonymousCommand
};

/**
 * Function to handle click on "Share" button
 * @param {Event} evt 
 */
function clickGenUrl(evt) {
    var themeData = {
        atomSettings: atomSettings,
        relationSettings: relationSettings,
        generalSettings: generalSettings,
        currentFramePosition: currentFramePosition,
        currentlyProjectedTypes: currentlyProjectedTypes
    };
    if (!$("#genUrl > button").is(":disabled")) { //if button is not disabled
        var modelToShare = textEditor.getValue();

        if (id = Router.current().params._id) { //if its loaded through an URL its a derivationOf model
            //so acontece num link publico
            //handle SECRETs
            if ((secrets = Router.current().data().secrets) && containsValidSecret(modelToShare)) {
                swal({
                    title: "This model contains information that cannot be shared!",
                    text: "Are you sure you want to share it?",
                    type: "warning",
                    showCancelButton: true,
                    confirmButtonColor: "#DD6B55",
                    confirmButtonText: "Yes, share it!",
                    closeOnConfirm: true
                }, function() {
                    Meteor.call('genURL', modelToShare, "Original", false, Session.get("last_id"), themeData, handleGenURLEvent);
                });
            } else {
                if (secrets.length == 0) {
                    //se tiver um ou mais valid secret com run check e assert anonimos, pergunta
                    if (containsValidSecretWithAnonymousCommand(modelToShare)) {
                        swal({
                            title: "This model contains an anonymous Command!",
                            text: "Are you sure you want to share it?",
                            type: "warning",
                            showCancelButton: true,
                            confirmButtonColor: "#DD6B55",
                            confirmButtonText: "Yes, share it!",
                            closeOnConfirm: true
                        }, function() {
                            Meteor.call('genURL', modelToShare, id, false, Session.get("last_id"), themeData, handleGenURLEvent);
                        });
                    } else
                        Meteor.call('genURL', modelToShare, id, false, Session.get("last_id"), themeData, handleGenURLEvent);
                } else
                    Meteor.call('genURL', modelToShare + secrets, id, true, Session.get("last_id"), themeData, handleGenURLEvent)
            }
        } else { // Otherwise this a new model (not based in any other)
            if (containsValidSecretWithAnonymousCommand(modelToShare)) {
                swal({
                    title: "This model contains an anonymous Command!",
                    text: "Are you sure you want to share it?",
                    type: "warning",
                    showCancelButton: true,
                    confirmButtonColor: "#DD6B55",
                    confirmButtonText: "Yes, share it!",
                    closeOnConfirm: true
                }, function() {
                    Meteor.call('genURL', modelToShare, "Original", false, Session.get("last_id"), themeData, handleGenURLEvent);
                });
            } else
                Meteor.call('genURL', modelToShare, "Original", false, Session.get("last_id"), themeData, handleGenURLEvent);
        }
    }
}


function containsValidSecretWithAnonymousCommand(model) {
    let lastSecret = 0;
    while ((i = model.indexOf("//SECRET\n", lastSecret)) >= 0) {
        let s = model.substr(i + "//SECRET\n".length).trim();
        // if the remaning text matches the regex below than it has an anonymous command
        if (s.match("^(assert|run|check)([ \t\n])*[{]")) return true;
        lastSecret = i + 1;
    }
    return false;
}


/* genUrlbtn event handler after genUrl method */
function handleGenURLEvent(err, result) {
    if (err) return
    // if the URL was generated successfully, create and append a new element to the HTML containing it.
    let url = getAnchorWithLink(result['public'], "public link");
    let urlPrivate = getAnchorWithLink(result['private'], "private link");

    let textcenter = document.createElement('div');
    textcenter.className = "text-center";
    textcenter.id = "permalink";
    textcenter.appendChild(url);
    if (urlPrivate) textcenter.appendChild(urlPrivate);

    document.getElementById('url-permalink').appendChild(textcenter);
    $("#genUrl > button").prop('disabled', true);
    zeroclipboard();

    if (result.last_id) {
        Session.set("last_id", result.last_id);
    }
}