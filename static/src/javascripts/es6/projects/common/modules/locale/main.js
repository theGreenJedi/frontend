var currentLocale = 'en-us',
        locales = {
         "en-us": {
           "currency": "USD",
           "distance": ["in", "ft", "mi"],
           "weight": ["oz", "lb", "ton"]
         },

         "en-au": {
           "currency": "AUD",
           "distance": ["mm", "cm", "m", "km"],
           "weight": ["mg", "g", "kg"]
         }
        }


        function localise(element) {
          var type = element.getAttribute('data-localise');
          var unit = element.getAttribute('data-unit');
          var value = element.getAttribute('data-value');

          switch (type) {
            case 'currency':
              return new Currency(unit, value);
            case 'distance':
              return new Distance(unit, value);
            case 'weight':
              return new Weight(unit, value);
          }
        }

        function Currency(currency, value) {
          this.currency = currency;
          this.value = value;
        }

        Currency.prototype.convert = function() {
          return fx.convert(this.value, { from: this.currency, to: locales[currentLocale].currency });
        }


export default function() {
  console.log('locale');
}