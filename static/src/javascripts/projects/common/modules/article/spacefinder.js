define([
    'common/utils/fastdom-promise',
    'qwery',
    'bean',
    'Promise',
    'common/utils/config',
    'common/utils/mediator',
    'lodash/functions/curry'
], function (
    fastdom,
    qwery,
    bean,
    Promise,
    config,
    mediator,
    curry
) {
    // maximum time (in ms) to wait for images to be loaded and rich links
    // to be upgraded
    var LOADING_TIMEOUT = 5000;

    // find spaces in articles for inserting ads and other inline content
    // minAbove and minBelow are measured in px from the top of the paragraph element being tested
    var defaultRules = { // these are written for adverts
        bodySelector: '.js-article__body',
        slotSelector: ' > p',
        absoluteMinAbove: 0, // minimum from slot to top of page
        minAbove: 250, // minimum from para to top of article
        minBelow: 300, // minimum from (top of) para to bottom of article
        clearContentMeta: 50, // vertical px to clear the content meta element (byline etc) by. 0 to ignore
        selectors: { // custom rules using selectors. format:
            //'.selector': {
            //   minAbove: <min px above para to bottom of els matching selector>,
            //   minBelow: <min px below (top of) para to top of els matching selector> }
            ' > h2': {minAbove: 0, minBelow: 250}, // hug h2s
            ' > *:not(p):not(h2)': {minAbove: 25, minBelow: 250} // require spacing for all other elements
        },

        // filter:(slot:Element, index:Integer, slots:Collection<Element>) -> Boolean
        // will run each slot through this fn to check if it must be counted in
        filter: null,

        // startAt:Element
        // will remove slots before this one
        startAt: null,

        // stopAt:Element
        // will remove slots from this one on
        stopAt: null,

        // fromBotton:Boolean
        // will reverse the order of slots (this is useful for lazy loaded content)
        fromBottom: false
    };

    function expire(resolve) {
        window.setTimeout(resolve, LOADING_TIMEOUT);
    }

    function onImagesLoaded(body) {
        var notLoaded = qwery('img', body).filter(function (img) {
            return !img.complete;
        });

        return notLoaded.length === 0 ?
            Promise.resolve(true) :
            new Promise(function (resolve) {
                var loadedCount = 0;
                bean.on(body, 'load', notLoaded, function onImgLoaded() {
                    loadedCount += 1;
                    if (loadedCount === notLoaded.length) {
                        bean.off(body, 'load', onImgLoaded);
                        notLoaded = null;
                        resolve();
                    }
                });
            });
    }

    function onRichLinksUpgraded(body) {
        return qwery('.element-rich-link--not-upgraded', body).length === 0 ?
            Promise.resolve(true) :
            new Promise(function (resolve) {
                mediator.once('rich-link:loaded', resolve);
            });
    }

    // test one element vs another for the given rules
    function _testCandidate(rules, challenger, opponent) {
        var isMinAbove = challenger.top - opponent.bottom >= rules.minAbove;
        var isMinBelow = opponent.top - challenger.top >= rules.minBelow;

        return isMinAbove || isMinBelow;
    }

    // test one element vs an array of other elements for the given rules
    function _testCandidates(rules, challenger, opponents) {
        return opponents.every(curry(_testCandidate)(rules, challenger));
    }

    function _mapElementToComputedDimensions(el) {
        var rect = el.getBoundingClientRect();
        return {
            top: rect.top,
            bottom: rect.bottom,
            element: el
        };
    }

    function _mapElementToDimensions(el) {
        return {
            top: el.offsetTop,
            bottom: el.offsetTop + el.offsetHeight,
            element: el
        };
    }

    function _enforceRules(data, rules) {
        var candidates = data.candidates;

        // enforce absoluteMinAbove rule
        if (rules.absoluteMinAbove) {
            candidates = candidates.filter(function (candidate) {
                return candidate.top >= rules.absoluteMinAbove;
            });
        }

        // enforce minAbove and minBelow rules
        candidates = candidates.filter(function (candidate) {
            var farEnoughFromTopOfBody = candidate.top >= rules.minAbove;
            var farEnoughFromBottomOfBody = candidate.top + rules.minBelow <= data.bodyHeight;
            return farEnoughFromTopOfBody && farEnoughFromBottomOfBody;
        });

        // enforce content meta rule
        if (rules.clearContentMeta) {
            candidates = candidates.filter(function (candidate) {
                return candidate.top > (data.contentMeta.bottom + rules.clearContentMeta);
            });
        }

        // enforce selector rules
        if (rules.selectors) {
            Object.keys(rules.selectors).forEach(function (selector) {
                candidates = candidates.filter(function (candidate) {
                    return _testCandidates(rules.selectors[selector], candidate, data.opponents[selector]);
                });
            });
        }

        if (rules.filter) {
            candidates = candidates.filter(rules.filter);
        }

        return candidates;
    }

    function getReady(body) {
        if (config.switches.viewability) {
            return Promise.race([
                new Promise(expire),
                Promise.all([onImagesLoaded(body), onRichLinksUpgraded(body)])
            ]);
        }

        return Promise.resolve(true);
    }

    // Rather than calling this directly, use spaceFiller to inject content into the page.
    // SpaceFiller will safely queue up all the various asynchronous DOM actions to avoid any race conditions.
    function findSpace(rules) {
        var body, getDimensions;

        rules || (rules = defaultRules);
        body = rules.bodySelector ? document.querySelector(rules.bodySelector) : document;
        getDimensions = rules.absoluteMinAbove ? _mapElementToComputedDimensions : _mapElementToDimensions;

        return getReady(body)
        .then(getCandidates)
        .then(getMeasurements)
        .then(enforceRules)
        .then(returnCandidates);

        function getCandidates() {
            var candidates = qwery(rules.bodySelector + rules.slotSelector);
            if (rules.fromBottom) {
                candidates.reverse();
            }
            if (rules.startAt) {
                var drop = true;
                candidates = candidates.filter(function (candidate) {
                    if (candidate === rules.startAt) {
                        drop = false;
                    }
                    return !drop;
                });
            }
            if (rules.stopAt) {
                var keep = true;
                candidates = candidates.filter(function (candidate) {
                    if (candidate === rules.stopAt) {
                        keep = false;
                    }
                    return keep;
                });
            }
            return candidates;
        }

        function getMeasurements(candidates) {
            var contentMeta = rules.clearContentMeta ?
                document.querySelector('.js-content-meta') :
                null;
            var opponents = rules.selectors ?
                Object.keys(rules.selectors).map(function (selector) {
                    return [selector, qwery(rules.bodySelector + selector)];
                }) :
                null;

            return fastdom.read(function () {
                var bodyDims = body.getBoundingClientRect();
                var candidatesWithDims = candidates.map(getDimensions);
                var contentMetaWithDims = rules.clearContentMeta ?
                    getDimensions(contentMeta) :
                    null;
                var opponentsWithDims = opponents ?
                    opponents.reduce(function (result, selectorAndElements) {
                        result[selectorAndElements[0]] = selectorAndElements[1].map(getDimensions);
                        return result;
                    }, {}) :
                    null;

                if (rules.absoluteMinAbove) {
                    rules.absoluteMinAbove -= bodyDims.top;
                }

                return {
                    bodyHeight: bodyDims.height,
                    candidates: candidatesWithDims,
                    contentMeta: contentMetaWithDims,
                    opponents: opponentsWithDims
                };
            });
        }

        function enforceRules(data) {
            return _enforceRules(data, rules);
        }

        function returnCandidates(candidates) {
            if (candidates.length) {
                return candidates.map(function (candidate) { return candidate.element; });
            } else {
                throw new Error('There is no space left matching rules ' + JSON.stringify(rules));
            }
        }
    }

    return {
        findSpace: findSpace,
        _testCandidate: _testCandidate, // exposed for unit testing
        _testCandidates: _testCandidates // exposed for unit testing
    };
});
