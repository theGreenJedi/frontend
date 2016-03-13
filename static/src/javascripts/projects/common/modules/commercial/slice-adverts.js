define([
    'bonzo',
    'qwery',
    'common/utils/$',
    'common/utils/config',
    'common/utils/detect',
    'common/utils/fastdom-promise',
    'common/modules/commercial/create-ad-slot',
    'common/modules/user-prefs',
    'common/modules/commercial/commercial-features',
    'lodash/collections/contains',
    'lodash/utilities/identity'
], function (
    bonzo,
    qwery,
    $,
    config,
    detect,
    fastdom,
    createAdSlot,
    userPrefs,
    commercialFeatures,
    contains,
    identity
) {

    function init() {
        if (!commercialFeatures.sliceAdverts) {
            return false;
        }

        /* Skip every odd container */
        var skipNext = false;
        var maxAdsToShow = config.page.showMpuInAllContainers ? Infinity : 3;
        var prefs = userPrefs.get('container-states');
        var isFront = contains(['uk', 'us', 'au'], config.page.pageId);

        var adSlices = qwery('.fc-container').map(function (container, index) {
            if (skipNext) {
                skipNext = false;
                return null;
            }

            var adSlice = container.querySelector('.js-fc-slice-mpu-candidate');
            var isFrontFirst = isFront && index === 0;

            if (config.page.showMpuInAllContainers) {
                return adSlice;
            }

            if (adSlice && !isFrontFirst && (!prefs || prefs[container.id] !== 'closed')) {
                skipNext = true;
                return adSlice;
            }

            return null;
        }).filter(identity).slice(0, maxAdsToShow);

        // When we are inside the AB test we are adding inline1 manually so index needs to start from 2.
        var inlineIndexOffset = (config.tests.cmTopBannerPosition) ? 2 : 1;
        var isMobile = detect.getBreakpoint() === 'mobile';

        adSlices = adSlices.map(function (adSlice, index) {
            var adName = 'inline' + (index + inlineIndexOffset);
            var adSlot = createAdSlot(adName, 'container-inline');

            bonzo(adSlot).addClass(isMobile ? 'ad-slot--mobile' : 'ad-slot--not-mobile');
            return [adSlice, adSlot];
        });

        return fastdom.write(function () {
            return adSlices.map(function (pair) {
                var adSlice = pair[0];
                var adSlot = pair[1];

                if (isMobile) {
                    // add a mobile advert after the container
                    bonzo(adSlot)
                        .insertAfter($.ancestor(adSlice, 'fc-container'));
                } else {
                    // add a tablet+ ad to the slice
                    bonzo(adSlice)
                        .removeClass('fc-slice__item--no-mpu')
                        .append(adSlot);
                }
                return adSlice;
            });
        });
    }

    return {
        init: init
    };
});
