// Small helpers used on several pages. No framework: plain JavaScript.
(function () {
  "use strict";

  // Forms with data-confirm="question" ask before they are sent (e.g. "return this asset?").
  document.querySelectorAll("form[data-confirm]").forEach(function (form) {
    form.addEventListener("submit", function (event) {
      if (!window.confirm(form.dataset.confirm)) {
        event.preventDefault();
      }
    });
  });

  // "Select all" checkbox on the asset list (for printing labels).
  var selectAll = document.getElementById("selectAll");
  if (selectAll) {
    selectAll.addEventListener("change", function () {
      document.querySelectorAll(".asset-check").forEach(function (box) {
        box.checked = selectAll.checked;
      });
    });
  }
})();
