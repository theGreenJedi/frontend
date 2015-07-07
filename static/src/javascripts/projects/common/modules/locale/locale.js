import $ from 'common/utils/$';
import localise from './localise';

export default function() {
    $('[data-localise]').map((el) => localise($(el)))
};
