$(function () {
    $.demo = {};
    $.demo.refreshRate = 2000;
    $.demo.counter = 0;
    $.demo.inprocessCounter = 0;
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
            $('#inprocessCallsCount').text($.demo.inprocessCounter)

        });
    })

    log("Initial load complete")
}

function insertCallSession(callSession) {
    row = toSubmittedRow(callSession)
    $('#callSessionsTable').prepend(row)
}

function getCallSessions() {
    var callSessions = jsRoutes.controllers.CallSessionEndpoint.getLastCallSessions($.demo.counter);

    $.getJSON(callSessions.url, function(data) {
        $.each(data, function(index, callSession) {
            if (callSession.counter > $.demo.counter) {
                insertCallSession(callSession)
                removeInprocessRow(callSession)
                $.demo.counter += 1;
            }
            $('#submittedCount').text($.demo.counter)
        });
    })

    setTimeout(function() {getCallSessions();}, $.demo.refreshRate);
}

function removeInprocessRow(callSession) {
    console.log("removing " + callSession)
    $('#inprocessCallsTable > tbody  > tr').each(function() {
        tr_row = $(this);
        var speechTd = tr_row.find("#inprocess_speech");
        var speech = speechTd.html();
        if (callSession.text === speech) {
            tr_row.remove()
            $.demo.inprocessCounter -= 1
            $('#inprocessCallsCount').text($.demo.inprocessCounter)
        }
    });
}

function toSubmittedRow(callSession) {
    var row = [];
    row.push('<tr>')
    row.push('<td>'); row.push(callSession.id);       row.push('</td>');
    row.push('<td>'); row.push(callSession.category);  row.push('</td>');
    row.push('<td>'); row.push(callSession.agentId);     row.push('<pre id="agentIdPre">       </pre></td>');
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
