// The scan page: decode a label (camera, photo or typed), ask the server who/where, show the answer.
(function () {
  "use strict";

  var texts = document.getElementById("scan-texts").dataset;
  var startBtn = document.getElementById("startCamera");
  var stopBtn = document.getElementById("stopCamera");
  var statusBox = document.getElementById("scanStatus");
  var resultCard = document.getElementById("scanResult");

  function showStatus(kind, message) {
    statusBox.className = "alert alert-" + kind;
    statusBox.textContent = message;
  }

  function hideStatus() {
    statusBox.className = "alert d-none";
  }

  function badgeClass(status) {
    if (status === "IN_STOCK") return "badge mb-2 text-bg-success";
    if (status === "IN_REPAIR") return "badge mb-2 text-bg-warning";
    return "badge mb-2 text-bg-secondary";
  }

  function lookup(raw) {
    resultCard.classList.add("d-none");
    showStatus("info", texts.looking);
    qramFetch("/api/assets/lookup?raw=" + encodeURIComponent(raw))
      .then(function (response) {
        return response.json().then(function (body) { return { ok: response.ok, body: body }; });
      })
      .then(function (r) {
        if (!r.ok) {
          showStatus("danger", r.body.message);
          return;
        }
        hideStatus();
        var a = r.body;
        document.getElementById("resultTag").textContent = a.tag;
        document.getElementById("resultName").textContent = a.name;
        var badge = document.getElementById("resultStatus");
        badge.className = badgeClass(a.status);
        badge.textContent = a.statusLabel;
        document.getElementById("resultHolderLabel").textContent = texts.holder;
        document.getElementById("resultHolder").textContent = a.holder || texts.nobody;
        document.getElementById("resultLocationLabel").textContent = texts.location;
        document.getElementById("resultLocation").textContent = a.location || "—";
        var link = document.getElementById("resultLink");
        link.href = a.url;
        link.textContent = texts.open;
        resultCard.classList.remove("d-none");
        if (navigator.vibrate) navigator.vibrate(80);
      })
      .catch(function () { showStatus("danger", texts.cameraError); });
  }

  var scanner = QramScanner(
    "reader",
    function (text) {
      lookup(text);
    },
    function () {
      showStatus("warning", texts.cameraError);
      startBtn.hidden = false;
      stopBtn.hidden = true;
    },
    function () {
      showStatus("warning", texts.noCode);
    }
  );

  startBtn.addEventListener("click", function () {
    hideStatus();
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
    var value = document.getElementById("manualInput").value.trim();
    if (value) lookup(value);
  });
})();
