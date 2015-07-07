import _ from 'lodash';
import fx from 'money';
import Qty from 'quantities';
import numeral from 'numeral';
import ajaxPromise from 'common/utils/ajax-promise';
import storage from 'common/utils/storage';
import template from 'common/utils/template';
import localChooserTemplate from './local-chooser.html!text';
import localChooserItemTemplate from './local-chooser-item.html!text';
import bean from 'bean';
import $ from 'common/utils/$';

const localStorage = storage.local;
const key = '3916c4516c3842e8922ac3880867d583';
const localStorageKey = 'localise.hackday';

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
    },
    "volume": {
        "imperial": ["quart", "pint", "gallon"],
        "metric": ["ml", "cl", "l"]
    }
}

bean.on(document.body, 'click', '[data-localise-picker-type]', toggleLocaleOption);
bean.on(document.body, 'click', '[data-localise]', toggleLocaleChooser);

function toggleLocaleChooser(e) {
    if(e.target.getAttribute('data-localise') === null) return;
    const $el = $(e.target);
    if (!$('.popup', $el).length) {
        console.log(options[$el.attr('data-localise')]);
        var items = Object.keys(options[$el.attr('data-localise')]).map(function(item){
            return template(localChooserItemTemplate, {
                title: options[$el.attr('data-localise')][item],
                val: item,
                type: $el.attr('data-localise')
            });
        }).join('');
        $el[0].innerHTML += template(localChooserTemplate, {list: items});
    } else {
        $('.popup', $el).toggle();
    }
}

function toggleLocaleOption(e) {
    var data = localStorage.get(localStorageKey);
    data[e.target.getAttribute('data-localise-picker-type')] = e.target.getAttribute('data-chooser-option');
    localStorage.set(localStorageKey, data);
    $('[data-localise]').map((el) => localise($(el)));
    $('[data-localise] .popup').hide();
}

function getUnit(type) {
    return options[type][localStorage.get(localStorageKey)[type]];
}

function convert(type, value) {
    const qty = Qty(value);
    const conversions = getUnit(type).map(unit => qty.to(unit));

    return bestFit(conversions).toString();
}

function bestFit(array) {
    const range = { min: 0.2, max: 1000 };
    const suitable = array.filter(qty => {
        return qty.scalar > range.min && qty.scalar < range.max;
    }).sort().reverse();
    const best = suitable[0] || array.sort().reverse()[0];
    const rounding = best.scalar > 10000 ? 1000 :
                     best.scalar > 100 ? 100 :
                     best.scalar > 50 ? 10 :
                     best.scalar > 1 ? 1 :
                     0.5;

    return best.toPrec(rounding);
}

function formatCurrency(value) {
    const length = value.toString().length;
    const format = length > 6 ? '0.00a' :
                   length > 4 ? '0,0.00' :
                   length > 2 ? '0,0[.]00' :
                   '0';

    return numeral(value).format(format);
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
                    getUnit('currency') +
                    formatCurrency(fx.convert(value, {from: unit, to: localStorage.get(localStorageKey)['currency']}))
                , $element);
            });
            break;

        case 'distance':
        case 'weight':
        case 'volume':
            appendConversion(convert(type, `${value}${unit}`), $element);
            break;

        default:
            appendConversion('bollocks', $element)
    }
}

function appendConversion (s, $element) {
    return $element.attr('data-localised-string', `(${s})`);
}

export default localise;
