import React from 'react';
import fastdom from 'fastdom';

import $ from 'common/utils/$';
import mediator from 'common/utils/mediator';

// $gs-baseline (px)
const baseline = 12;

export default class StickyWrapper extends React.Component {
    constructor(props) {
        super(props);
        this.state = { stuck: false };
    }

    componentDidMount() {
        this.$node = $(React.findDOMNode(this.refs.game));
        mediator.on('crosswords:scroll', this.stick.bind(this));
    }

    stick(offsets, scrollY) {
        const scrollOffset = scrollY + baseline - offsets.container.top;
        const inTheZone = scrollOffset >= 0 &&
                          scrollOffset + offsets.game.height <= offsets.container.height;

        if (inTheZone && !this.state.stuck) {
            fastdom.write(() => this.$node.css('top', ''));
            this.setState({ stuck: true });

            return this.$node.addClass('is-fixed');
        }

        if (!inTheZone && this.state.stuck) {
            const y = scrollOffset <= 0 ? 0 : this.$node.offset().top - offsets.container.top;

            fastdom.write(() => this.$node.css('top', y));
            this.setState({ stuck: false });

            return this.$node.removeClass('is-fixed');
        }
    }

    render() {
        return (
            <div className='crossword__container__game' ref='game'>
                {this.props.children}
            </div>
        );
    }
}
