define([
    'components/qwery/qwery'
], function(qwery) {

  var oldQwery = qwery;

  var markIndex = 0;
  qwery = function() {
      performance.mark('qwery_start' + index);
      var result = oldQwery.apply(null, arguments);
      performance.mark('qwery_end' + index);
      performance.measure('qwery_duration' + index, 'qwery_start' + index, 'qwery_end' + index);
      index += 1;
      return result;
  }

  for (var key in oldQwery) {
    if (oldQwery.hasOwnProperty(key)) {
        qwery[key] = oldQwery[key];
    }
  }

  window.addEventListener('load', function () {
      var entries = performance.getEntriesByType('measure');
      var total = 0;
      for (var i = 0; i < entries.length; i++) {
          if (entries[i].name.indexOf('qwery_duration') === -1) {
              continue;
          }
          total += entries[i].duration;
      }
      console.log("Total amount of time spent in qwery: " + total);
  });

  return qwery;
})
