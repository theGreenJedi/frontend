import $ from 'common/utils/$';
import localise from './localise';
import storage from 'common/utils/storage';

const localStorage = storage.local;
const key = 'localise.hackday';
const defaults = {
    "currency": "USD",
    "distance": ["in", "ft", "mi"],
    "weight": ["oz", "lb", "ton"]
};

export default function() {
    if (!localStorage.get(key)) {
        localStorage.set(key, defaults);
    }

    $('[data-localise]').map((el) => localise($(el)));
};
