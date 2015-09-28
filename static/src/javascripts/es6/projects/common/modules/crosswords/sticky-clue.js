import React from 'react';
import fastdom from 'fastdom';
import classNames from 'classnames';

import $ from 'common/utils/$';
import detect from 'common/utils/detect';
import mediator from 'common/utils/mediator';

export default class StickyClue extends React.Component {
    componentDidMount() {
        const $stickyClueWrapper = $(React.findDOMNode(this.refs.stickyClueWrapper));
        const isIOS = detect.isIOS();

        mediator.on('crosswords:scroll', (gridOffset, gameOffset, scrollYPastGame) => {
            const stickyClueWrapperOffset = $stickyClueWrapper.offset();

            fastdom.write(() => {
                // Clear previous state
                $stickyClueWrapper
                    .css('top', '')
                    .css('bottom', '')
                    .removeClass('is-fixed');

                if (scrollYPastGame >= 0) {
                    if (scrollY > (gridOffset.bottom - stickyClueWrapperOffset.height)) {
                        $stickyClueWrapper
                        .css('top', 'auto')
                        .css('bottom', 0);
                    } else {
                        // iOS doesn't support sticky things when the keyboard
                        // is open, so we use absolute positioning and
                        // programatically update the value of top
                        if (isIOS) {
                            $stickyClueWrapper.css('top', scrollYPastGame);
                        } else {
                            $stickyClueWrapper.addClass('is-fixed');
                        }
                    }
                }
            });
        });
    }

    render() {
        return (
            <div className='crossword__sticky-clue-wrapper' ref='stickyClueWrapper'>
                <div
                    className={classNames({
                        'crossword__sticky-clue': true,
                        'is-hidden': !this.props.focussed
                    })}
                    ref='stickyClue'>
                    {this.props.focussed && (
                        <div className='crossword__sticky-clue__inner'>
                            <div className='crossword__sticky-clue__inner__inner'>
                                <strong>{this.props.focussed.number} <span className='crossword__sticky-clue__direction'>{this.props.focussed.direction}</span></strong> {this.props.focussed.clue}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        );
    }
}
