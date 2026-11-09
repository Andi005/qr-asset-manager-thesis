// A thin wrapper around html5-qrcode, shared by the scan page and the inventory page.
// - camera scanning with the back camera, and a 3-second debounce for the same code;
// - "scan from photo" fallback, which also works where the browser blocks the camera (plain HTTP).
// The decoded text is sent to the server as it is; the server decides whether it is a valid tag.
(function () {
  "use strict";

  var DEBOUNCE_MS = 3000;

  window.QramScanner = function (regionId, onDecoded, onCameraError, onPhotoError) {
    var reader = new Html5Qrcode(regionId);
    var last = { text: null, at: 0 };

    function handle(text) {
      var now = Date.now();
      if (text === last.text && now - last.at < DEBOUNCE_MS) {
        return;
      }
      last = { text: text, at: now };
      onDecoded(text);
    }

    function isScanning() {
      return reader.isScanning === true;
    }

    return {
      start: function () {
        return reader
          .start(
            { facingMode: "environment" },
            { fps: 10, qrbox: { width: 240, height: 240 }, aspectRatio: 1.0 },
            handle,
            function () { /* no code in this frame: normal, ignore */ }
          )
          .catch(function (err) {
            onCameraError(err);
            throw err;
          });
      },
      stop: function () {
        return isScanning() ? reader.stop() : Promise.resolve();
      },
      isScanning: isScanning,
      scanFile: function (file) {
        var stopFirst = isScanning() ? reader.stop() : Promise.resolve();
        return stopFirst
          .then(function () { return reader.scanFile(file, false); })
          .then(function (text) {
            last = { text: null, at: 0 }; // a photo is a deliberate action: never debounce it
            handle(text);
          })
          .catch(function (err) { onPhotoError(err); });
      }
    };
  };

  // Sends JSON with the CSRF token that Spring Security expects (read from the <meta> tags).
  window.qramFetch = function (url, options) {
    options = options || {};
    options.headers = options.headers || {};
    options.credentials = "same-origin";
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header && options.method && options.method !== "GET") {
      options.headers[header.content] = token.content;
    }
    return fetch(url, options).then(function (response) {
      if (response.status === 401) {
        // The session expired: go to the login page, then come back here.
        window.location.reload();
      }
      return response;
    });
  };
})();
