// The live inventory page: every scan is sent to the server, which classifies it
// (found / unexpected / unknown / duplicate) and returns the new state of the session.
(function () {
  "use strict";

  var config = document.getElementById("inventory-config").dataset;
  var sessionId = config.session;
  var finished = config.finished === "true";

  // The running clock (the server keeps the official start and finish times).
  var durationEl = document.getElementById("duration");
  if (!finished && durationEl) {
    var started = Number(durationEl.dataset.started);
    setInterval(function () {
      var seconds = Math.max(0, Math.floor((Date.now() - started) / 1000));
      var h = Math.floor(seconds / 3600);
      var m = Math.floor((seconds % 3600) / 60);
      var s = seconds % 60;
      var pad = function (n) { return (n < 10 ? "0" : "") + n; };
      durationEl.textContent = h > 0 ? h + ":" + pad(m) + ":" + pad(s) : m + ":" + pad(s);
    }, 1000);
  }

  if (finished || !document.getElementById("reader")) {
    return; // read-only view
  }

  var resultBox = document.getElementById("lastResult");
  var RESULT_STYLE = {
    FOUND: "alert-success",
    UNEXPECTED: "alert-warning",
    UNKNOWN: "alert-danger",
    DUPLICATE: "alert-secondary"
  };

  function fillList(id, rows, withLocation) {
    var list = document.getElementById(id);
    list.innerHTML = "";
    rows.forEach(function (row) {
      var li = document.createElement("li");
      li.className = "list-group-item px-0";
      var tag = document.createElement("span");
      tag.className = "font-monospace";
      tag.textContent = row.tag;
      li.appendChild(tag);
      li.appendChild(document.createTextNode(" " + row.name));
      if (withLocation && row.location) {
        var loc = document.createElement("span");
        loc.className = "text-body-secondary";
        loc.textContent = " " + row.location;
        li.appendChild(loc);
      }
      list.appendChild(li);
    });
  }

  function render(state) {
    document.getElementById("count-expected").textContent = state.expected;
    document.getElementById("count-found").textContent = state.found.length;
    document.getElementById("count-missing").textContent = state.missing.length;
    document.getElementById("count-unexpected").textContent = state.unexpected.length;
    document.getElementById("count-unknown").textContent = state.unknown;
    document.getElementById("count-duplicates").textContent = state.duplicates;
    fillList("list-missing", state.missing, false);
    fillList("list-found", state.found, false);
    fillList("list-unexpected", state.unexpected, true);
    var log = document.getElementById("scan-log");
    log.innerHTML = "";
    state.scans.forEach(function (scan) {
      var tr = document.createElement("tr");
      [scan.time, scan.tag || scan.raw, scan.resultLabel].forEach(function (text, i) {
        var td = document.createElement("td");
        if (i < 2) td.className = "font-monospace text-break";
        td.textContent = text;
        tr.appendChild(td);
      });
      log.appendChild(tr);
    });
  }

  function send(raw, manual) {
    qramFetch("/api/inventory/" + sessionId + "/scans", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ raw: raw, manual: manual })
    })
      .then(function (response) {
        if (response.status === 409) {
          window.location.reload(); // finished elsewhere
          return null;
        }
        return response.json().then(function (body) { return { ok: response.ok, body: body }; });
      })
      .then(function (r) {
        if (!r) return;
        if (!r.ok) {
          resultBox.className = "alert fs-5 text-center alert-danger";
          resultBox.textContent = r.body.message;
          return;
        }
        var b = r.body;
        resultBox.className = "alert fs-5 text-center " + (RESULT_STYLE[b.result] || "alert-info");
        resultBox.textContent = b.resultLabel + (b.tag ? ": " + b.tag + (b.name ? " · " + b.name : "") : "");
        if (navigator.vibrate) navigator.vibrate(b.result === "FOUND" ? 60 : [60, 60, 60]);
        render(b.state);
      });
  }

  var startBtn = document.getElementById("startCamera");
  var stopBtn = document.getElementById("stopCamera");
  var scanner = QramScanner(
    "reader",
    function (text) { send(text, false); },
    function () {
      resultBox.className = "alert alert-warning";
      resultBox.textContent = config.cameraError;
      startBtn.hidden = false;
      stopBtn.hidden = true;
    },
    function () {
      resultBox.className = "alert alert-warning";
      resultBox.textContent = config.noCode;
    }
  );

  startBtn.addEventListener("click", function () {
    scanner.start().then(function () {
      startBtn.hidden = true;
      stopBtn.hidden = false;
    }, function () { /* message already shown */ });
  });
  stopBtn.addEventListener("click", function () {
    scanner.stop().then(function () {
      startBtn.hidden = false;
      stopBtn.hidden = true;
    });
  });
  document.getElementById("photoInput").addEventListener("change", function (event) {
    var file = event.target.files[0];
    if (file) {
      startBtn.hidden = false;
      stopBtn.hidden = true;
      scanner.scanFile(file);
    }
    event.target.value = "";
  });
  document.getElementById("manualForm").addEventListener("submit", function (event) {
    event.preventDefault();
    var input = document.getElementById("manualInput");
    var value = input.value.trim();
    if (value) {
      send(value, true);
      input.value = "";
      input.focus();
    }
  });
})();
