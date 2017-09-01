$(function () {
    $.demo = {};
    $.demo.refreshRate = 1000;
    $.demo.inProcessCallsRefreshRate = 1000;
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

    var stats = jsRoutes.controllers.CallSessionEndpoint.getModelStatistic();

    console.log("Stats")
    $.getJSON(stats.url, function(data) {
        console.log(data)
        $('#trainingTime').text(data.time)
        $('#accuracy').text(data.accuracy)
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
                $.demo.counter += 1;
            }
            $('#submittedCount').text($.demo.counter)
        });
    })

    setTimeout(function() {getCallSessions();}, $.demo.refreshRate);
}

function getInprocessCalls() {
    var inprocessCalls = jsRoutes.controllers.CallSessionEndpoint.getInprocessCalls();

    $.getJSON(inprocessCalls.url, function(data) {
        removeInprocessCalls()
        $.demo.inprocessCounter = 0
        $('#inprocessCallsCount').text($.demo.inprocessCounter)
        $.each(data, function(index, inprocessCall) {
            insertInprocessCall(inprocessCall.id, inprocessCall.speech)
        });
    })

    setTimeout(function() {getInprocessCalls();}, $.demo.inProcessCallsRefreshRate);
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

function insertInprocessCall(id, speech) {
    row = toInprocessRow(speech, id)
    $('#inprocessCallsTable').prepend(row);
    $.demo.inprocessCounter += 1
    $('#inprocessCallsCount').text($.demo.inprocessCounter)
}

function toInprocessRow(speech, counter) {
    var row = [];
    row.push('<tr id="inprocess_' + counter + '">');
    row.push('<td>'); row.push(counter); row.push('</td>');
    row.push('<td id="inprocess_speech">'); row.push(speech);  row.push('</td>');
    row.push('</tr>')
    var combinedRow = row.join("");
    return combinedRow;
}


function removeInprocessCalls() {
    $('#inprocessCallsTable > tbody  > tr').remove()
}

function toSubmittedRow(callSession) {
    var row = [];
    row.push('<tr>')
    row.push('<td>'); row.push(callSession.id);       row.push('</td>');
    row.push('<td>'); row.push(callSession.category);  row.push('</td>');
    row.push('<td>'); row.push(callSession.agentId);     row.push('<pre id="agentIdPre">       </pre></td>');
    row.push('<td>'); row.push(callSession.time);      row.push('</td>');
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
$(document).ready(getInprocessCalls);
