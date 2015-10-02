import React from 'react';
import fastdom from 'fastdom';

import $ from 'common/utils/$';
import detect from 'common/utils/detect';
import mediator from 'common/utils/mediator';

// $gs-baseline (px)
const baseline = 12;

export default class StickyWrapper extends React.Component {
    constructor(props) {
        super(props);
        this.state = { stuck: false };
    }

    componentDidMount() {
        const handler = (() => {
            if (detect.isIOS()) {
                this.$node.css('position', 'absolute');
                return this.stickIOS;
            }

            return this.stick;
        })().bind(this);

        this.$node = $(React.findDOMNode(this.refs.game));

        mediator.on('crosswords:scroll', (offsets, scrollY) => {
            return detect.getBreakpoint() === 'tablet' && handler(offsets, scrollY);
        });
    }

    stickIOS(offsets, scrollY) {
        const scrollOffset = scrollY + baseline - offsets.container.top;
        const above = scrollOffset < 0;
        const below = scrollOffset + offsets.game.height >= offsets.container.height;

        /**
         * Determine the y point for the grid:
         *     if we've scrolled above the container, it's 0
         *     if we're below the container, it's the lowest possible point
         *     otherwise we're in the the sticky zone, so it follows the scroll offset
         */
        const y
            = above ? 0
            : below ? offsets.container.height - offsets.game.height
            : scrollOffset;

        this.$node.css('top', y);
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
