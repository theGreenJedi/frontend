import $ from 'common/utils/$';
import localise from './localise';

export default function() {
    $('p').map((el) => localise($(el)))
};
