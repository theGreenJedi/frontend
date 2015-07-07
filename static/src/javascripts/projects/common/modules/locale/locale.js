import $ from 'common/utils/$';
import localise from './localise';
import storage from 'common/utils/storage';

const localStorage = storage.local;
const key = 'localise.hackday';
const defaults = {
    "currency": "USD",
    "distance": "imperial",
    "weight": "imperial",
    "volume": "imperial"
};

export default function() {
    if (!localStorage.get(key)) {
        localStorage.set(key, defaults);
    }

    $('[data-localise]').map((el) => localise($(el)));
};
