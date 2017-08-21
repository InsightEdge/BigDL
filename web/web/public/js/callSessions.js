$(function () {
    $.demo = {};
    $.demo.refreshRate = 2000;
    $.demo.counter = 0;
    $.demo.debug = true;
});

function initialLoad() {
    var callSessions = jsRoutes.controllers.CallSessionEndpoint.getLastCallSessions(0);

    $.getJSON(callSessions.url, function(data) {
        $.each(data, function(index, callSession) {

            if (callSession.counter > $.demo.counter) {
                insertCallSession(callSession)
                $.demo.counter += 1;
            }
            $('#submittedCount').text($.demo.counter)

        });
    })

    log("Initial load complete")
}

function insertCallSession(callSession) {
    row = toSubmittedRow(callSession)
    if ($.demo.counter > 0) {
        $('#callSessionsTable tr:first').after(row);
    } else {
        $('#callSessionsTable tr:last').after(row);
    }
}

function getCallSessions() {
    var callSessions = jsRoutes.controllers.CallSessionEndpoint.getLastCallSessions($.demo.counter);

    $.getJSON(callSessions.url, function(data) {
        $.each(data, function(index, callSessions) {
            if (callSessions.counter > $.demo.counter) {
                insertCallSession(callSessions)
                $.demo.counter += 1;
            }
            $('#submittedCount').text($.demo.counter)
        });
    })

    setTimeout(function() {getCallSessions();}, $.demo.refreshRate);
}

function toSubmittedRow(callSession) {
    var row = [];
    row.push('<tr>')
    row.push('<td>'); row.push(callSession.id);       row.push('</td>');
    row.push('<td>'); row.push(callSession.category);  row.push('</td>');
    row.push('<td>'); row.push(callSession.agentId);     row.push('</td>');
    row.push('<td>'); row.push(callSession.text);      row.push('</td>');
    row.push('</tr>')
    var combinedRow = row.join("");
    return combinedRow;
}

function log(msg) {
    if ($.demo.debug) {
        console.log(msg);
    }
}

$(document).ready(initialLoad);
$(document).ready(getCallSessions);
