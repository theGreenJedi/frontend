define([
    'qwery',
    'bonzo',
    'common/utils/config',
    'common/utils/template',
    'common/utils/fastdom-promise',
    'common/modules/commercial/dfp/dfp-api',
    'common/modules/commercial/create-ad-slot',
    'common/modules/commercial/commercial-features',
    'text!common/views/commercial/badge.html',
    'lodash/utilities/identity'
], function (
    qwery,
    bonzo,
    config,
    template,
    fastdom,
    dfp,
    createAdSlot,
    commercialFeatures,
    badgeStr,
    identity
) {
    var badgesConfig = {
        sponsoredfeatures: {
            count:      0,
            header:     'Supported by',
            namePrefix: 'sp'
        },
        'advertisement-features': {
            count:      0,
            header:     'Paid for by',
            namePrefix: 'ad'
        },
        'foundation-features': {
            count:      0,
            header:     'Supported by',
            namePrefix: 'fo'
        }
    };

    var badgeTpl = template(badgeStr);

    function addPreBadge(adSlot, header, sponsor) {
        adSlot.insertAdjacentHTML('beforeend', badgeTpl({
            header:  header,
            sponsor: sponsor
        }));
    }

    function createSponsoredSlot(opts) {
        var badgeConfig = badgesConfig[opts.sponsorship];
        var slotTarget  = badgeConfig.namePrefix + 'badge';
        var name        = slotTarget + (++badgeConfig.count);
        var adSlot      = createAdSlot(
            name,
            ['paid-for-badge', 'paid-for-badge--front'],
            opts.series,
            opts.keywords,
            slotTarget
        );

        if (opts.sponsor) {
            addPreBadge(adSlot, badgeConfig.header, opts.sponsor);
        }

        return adSlot;
    }

    function insertSponsoredSlots(slots) {
        return fastdom.write(function () {
            return slots.map(function (pair) {
                var container = pair[0];
                var slot = pair[1];
                var placeholder = container.querySelector('.js-badge-placeholder');

                if (placeholder) {
                    placeholder.parentNode.replaceChild(slot, placeholder);
                } else {
                    var header = container.querySelector('.js-container__header');
                    header.parentNode.insertBefore(slot, header.nextSibling);
                }

                return slot;
            });
        });
    }

    function init() {
        if (!commercialFeatures.badges) {
            return false;
        }

        var sponsoredFrontSlots = qwery('.js-sponsored-front').map(function (front) {
            var container = front.querySelector('.fc-container');
            return [
                container,
                createSponsoredSlot({
                    sponsorship: front.getAttribute('data-sponsorship'),
                    sponsor:     front.getAttribute('data-sponsor')
                })
            ];
        });

        var sponsoredContainersSlots = qwery('.js-sponsored-container').map(function (container) {
            if (container.querySelector('.ad-slot--paid-for-badge')) {
                return null;
            }

            return [
                container,
                createSponsoredSlot({
                    sponsorship: container.getAttribute('data-sponsorship'),
                    sponsor:     container.getAttribute('data-sponsor'),
                    series:      container.getAttribute('data-series'),
                    keywords:    container.getAttribute('data-keywords')
                })
            ];
        }).filter(identity);

        return insertSponsoredSlots(sponsoredFrontSlots.concat(sponsoredContainersSlots));
    }

    function addBadge(container) {
        var $container = bonzo(container);
        if (
            !container.querySelector('.ad-slot--paid-for-badge') &&
            $container.hasClass('js-sponsored-container')
        ) {
            return insertSponsoredSlots([[
                container,
                createSponsoredSlot({
                    sponsorship: container.getAttribute('data-sponsorship'),
                    sponsor:     container.getAttribute('data-sponsor'),
                    series:      container.getAttribute('data-series'),
                    keywords:    container.getAttribute('data-keywords')
                })
            ]]).then(dfp.addSlot);
        }
    }

    return {

        init: init,

        // add a badge to a container (if appropriate)
        add: addBadge,

        // for testing
        reset: function () {
            for (var type in badgesConfig) {
                badgesConfig[type].count = 0;
            }
        }

    };

});
