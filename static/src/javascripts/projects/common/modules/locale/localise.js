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

let currentLocale = 'en-us';
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


function localise($element) {
    var type = $element.attr('data-localise');
    var unit = $element.attr('data-unit');
    var value = $element.attr('data-value');

    switch (type) {
        case 'currency':
            getRates().then(function(fx) {
                appendConversion(fx.convert(value, {
                    from: unit,
                    to: locales[currentLocale].currency
                }));
            })
        case 'distance':
            return new Distance(unit, value);
        case 'weight':
            return new Weight(unit, value);
        default:
            appendConversion('bollocks', $element)
    }
}

function appendConversion (s, $element) {
    return $element.after(` (${s})`);
}


export default localise;

// function() {
//     var elements = $('[data-localise]');
//     var converted = elements.map(function(el) {
//         return localise(element);
//     });

//     console.log(converted);
// }
