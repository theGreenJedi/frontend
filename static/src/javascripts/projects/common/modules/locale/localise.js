import _ from 'lodash';
import fx from 'money';
import Qty from 'quantities';
import ajaxPromise from 'common/utils/ajax-promise';

const key = '3916c4516c3842e8922ac3880867d583';

const options = {
    "currency": {
        "USD": "$",
        "GBP": "£",
        "AUD": "AU$"
    },
    "distance": {
        "imperial": ["in", "ft", "mi"],
        "metric": ["mm", "cm", "m", "km"]
    },
    "weight": {
        "imperial": ["oz", "lb", "ton"],
        "metric": ["g", "kg", "tonne"]
    }
};

let exchange = null;

function convert(type, value) {
    const qty = Qty(value);
    const conversions = locales[currentLocale][type].map(unit => qty.to(unit));

    return bestFit(conversions).toString();
}

function bestFit(array) {
    const range = { min: 0.2, max: 1000 };
    const suitable = array.filter(qty => {
        return qty.scalar > range.min && qty.scalar < range.max;
    }).sort().reverse();
    const best = suitable[0] || array.sort().reverse()[0];
    const rounding = best.scalar > 1000 ? 1000 :
                     best.scalar > 100 ? 100 :
                     best.scalar > 10 ? 10 :
                     best.scalar > 1 ? 1 :
                     0.5;

    return best.toPrec(rounding);
}


function getRates() {
    return new Promise((resolve) => {
        if(_.isEmpty(fx.rates)) {
            ajaxPromise({
                url: `//openexchangerates.org/api/latest.json?app_id=${key}`,
                type: 'json',
                method: 'get',
                crossOrigin: true
            }).then((data) => {
                fx.rates = data.rates;
                fx.base = data.base;
                resolve(fx);
            });
        } else {
            resolve(fx);
        }
    })
}

function localise($element) {
    var type = $element.attr('data-localise');
    var unit = $element.attr('data-unit');
    var value = $element.attr('data-value');

    switch (type) {
        case 'currency':
            getRates().then(function(fx) {
                appendConversion(
                    locales[currentLocale].currencySymbol +
                    parseFloat(
                        fx.convert(value, {from: unit, to: locales[currentLocale].currency})
                    ).toFixed(2)
                , $element);
            });
            break;

        case 'distance':
        case 'weight':
            appendConversion(convert(type, `${value}${unit}`), $element);
            break;

        default:
            appendConversion('bollocks', $element)
    }
}

function appendConversion (s, $element) {
    return $element.attr('data-localised-string', ` (${s})`);
}

export default localise;
