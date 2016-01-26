define([
    'components/qwery/qwery'
], function(qwery) {

  var oldQwery = qwery;

  var signatures = {};
  qwery = function() {
      var sig = '';
      for (var i = 0; i < arguments.length; i++) {
          sig += Object.prototype.toString.call(arguments[i]);
      }
      signatures[sig] = (signatures[sig] || 0) + 1;
      var result = oldQwery.apply(null, arguments);
      return result;
  }

  for (var key in oldQwery) {
    if (oldQwery.hasOwnProperty(key)) {
        qwery[key] = oldQwery[key];
    }
  }

  window.addEventListener('load', function () {
      console.table(signatures);
  });

  return qwery;
});
