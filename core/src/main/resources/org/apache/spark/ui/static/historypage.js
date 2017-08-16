/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
var appLimit = -1;

function setAppLimit(val) {
    appLimit = val;
}

function makeIdNumeric(id) {
  var strs = id.split("_");
  if (strs.length < 3) {
    return id;
  }
  var appSeqNum = strs[2];
  var resl = strs[0] + "_" + strs[1] + "_";
  var diff = 10 - appSeqNum.length;
  while (diff > 0) {
      resl += "0"; // padding 0 before the app sequence number to make sure it has 10 characters
      diff--;
  }
  resl += appSeqNum;
  return resl;
}

function formatDate(date) {
  if (date <= 0) return "-";
  else return date.split(".")[0].replace("T", " ");
}

function getParameterByName(name, searchString) {
  var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
  results = regex.exec(searchString);
  return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

jQuery.extend( jQuery.fn.dataTableExt.oSort, {
    "title-numeric-pre": function ( a ) {
        var x = a.match(/title="*(-?[0-9\.]+)/)[1];
        return parseFloat( x );
    },

    "title-numeric-asc": function ( a, b ) {
        return ((a < b) ? -1 : ((a > b) ? 1 : 0));
    },

    "title-numeric-desc": function ( a, b ) {
        return ((a < b) ? 1 : ((a > b) ? -1 : 0));
    }
} );

jQuery.extend( jQuery.fn.dataTableExt.oSort, {
    "appid-numeric-pre": function ( a ) {
        var x = a.match(/title="*(-?[0-9a-zA-Z\-\_]+)/)[1];
        return makeIdNumeric(x);
    },

    "appid-numeric-asc": function ( a, b ) {
        return ((a < b) ? -1 : ((a > b) ? 1 : 0));
    },

    "appid-numeric-desc": function ( a, b ) {
        return ((a < b) ? 1 : ((a > b) ? -1 : 0));
    }
} );

$(document).ajaxStop($.unblockUI);
$(document).ajaxStart(function(){
    $.blockUI({ message: '<h3>Loading history summary...</h3>'});
});

$(document).ready(function() {
    $.extend( $.fn.dataTable.defaults, {
      stateSave: true,
      lengthMenu: [[20,40,60,100,-1], [20, 40, 60, 100, "All"]],
      pageLength: 20
    });

    historySummary = $("#history-summary");
    searchString = historySummary["context"]["location"]["search"];
    requestedIncomplete = getParameterByName("showIncomplete", searchString);
    requestedIncomplete = (requestedIncomplete == "true" ? true : false);

    $.getJSON("api/v1/applications?limit=" + appLimit, function(response,status,jqXHR) {
      var array = [];
      var hasMultipleAttempts = false;
      for (i in response) {
        var app = response[i];
        if (app["attempts"][0]["completed"] == requestedIncomplete) {
          continue; // if we want to show for Incomplete, we skip the completed apps; otherwise skip incomplete ones.
        }
        var id = app["id"];
        var name = app["name"];
        if (app["attempts"].length > 1) {
            hasMultipleAttempts = true;
        }
        var num = app["attempts"].length;
        for (j in app["attempts"]) {
          var attempt = app["attempts"][j];
          attempt["startTime"] = formatDate(attempt["startTime"]);
          attempt["endTime"] = formatDate(attempt["endTime"]);
          attempt["lastUpdated"] = formatDate(attempt["lastUpdated"]);
          var app_clone = {"id" : id, "name" : name, "num" : num, "attempts" : [attempt]};
          array.push(app_clone);
        }
      }

      var data = {
        "uiroot": uiRoot,
        "applications": array
        }

      $.get("static/historypage-template.html", function(template) {
        historySummary.append(Mustache.render($(template).filter("#history-summary-template").html(),data));
        var selector = "#history-summary-table";
        var conf = {
                    "columns": [
                        {name: 'first', type: "appid-numeric"},
                        {name: 'second'},
                        {name: 'third'},
                        {name: 'fourth'},
                        {name: 'fifth'},
                        {name: 'sixth', type: "title-numeric"},
                        {name: 'seventh'},
                        {name: 'eighth'},
                        {name: 'ninth'},
                    ],
                    "columnDefs": [
                        {"searchable": false, "targets": [5]}
                    ],
                    "autoWidth": false,
                    "order": [[ 4, "desc" ]]
        };

        var rowGroupConf = {
                           "rowsGroup": [
                               'first:name',
                               'second:name'
                           ],
        };

        if (hasMultipleAttempts) {
          jQuery.extend(conf, rowGroupConf);
          var rowGroupCells = document.getElementsByClassName("rowGroupColumn");
          for (i = 0; i < rowGroupCells.length; i++) {
            rowGroupCells[i].style='background-color: #ffffff';
          }
        }

        if (!hasMultipleAttempts) {
          var attemptIDCells = document.getElementsByClassName("attemptIDSpan");
          for (i = 0; i < attemptIDCells.length; i++) {
            attemptIDCells[i].style.display='none';
          }
        }

        var durationCells = document.getElementsByClassName("durationClass");
        for (i = 0; i < durationCells.length; i++) {
          var timeInMilliseconds = parseInt(durationCells[i].title);
          durationCells[i].innerHTML = formatDuration(timeInMilliseconds);
        }

        if ($(selector.concat(" tr")).length < 20) {
          $.extend(conf, {paging: false});
        }

        $(selector).DataTable(conf);
        $('#hisotry-summary [data-toggle="tooltip"]').tooltip();
      });
    });
});
