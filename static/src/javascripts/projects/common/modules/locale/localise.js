import $ from 'common/utils/$';
import _ from 'lodash';
import fx from 'money';
import ajaxPromise from 'common/utils/ajax-promise';

const key = '3916c4516c3842e8922ac3880867d583';

const locales = {
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
};

var currentLocale = 'en-us';
let exchange = null;

function getRates() {
    return new Promise(function(resolve) {
        if(!_.isEmpty(fx.rates)) return resolve(fx);
        ajaxPromise({
            url: `//openexchangerates.org/api/latest.json?app_id=${key}`,
            type: 'json',
            method: 'get',
            crossOrigin: true
        }).then(function(data) {
            fx.rates = data.rates;
            fx.base = data.base;
            return resolve(fx);
        })
    })
}


function localise(element) {
    var type = element.getAttribute('data-localise');
    var unit = element.getAttribute('data-unit');
    var value = element.getAttribute('data-value');

    switch (type) {
        case 'currency':
            getRates().then(function(fx) {
                return new Currency(unit, value);
            })
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
    return fx.convert(this.value, {
        from: this.currency,
        to: locales[currentLocale].currency
    });
}


export default localise;

// function() {
//     var elements = $('[data-localise]');
//     var converted = elements.map(function(el) {
//         return localise(element);
//     });

//     console.log(converted);
// }
